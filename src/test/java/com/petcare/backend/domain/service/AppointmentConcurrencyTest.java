package com.petcare.backend.domain.service;

import com.petcare.backend.domain.dto.request.AtencionClinicaRequest;
import com.petcare.backend.domain.dto.request.CitaRequest;
import com.petcare.backend.domain.dto.request.HorarioVeterinarioRequest;
import com.petcare.backend.domain.dto.request.InasistenciaRequest;
import com.petcare.backend.domain.dto.request.MascotaRequest;
import com.petcare.backend.domain.dto.request.VeterinarioRequest;
import com.petcare.backend.persistence.entity.Cita;
import com.petcare.backend.persistence.entity.Clinica;
import com.petcare.backend.persistence.entity.DetalleCostoCita;
import com.petcare.backend.persistence.entity.Duenio;
import com.petcare.backend.persistence.entity.HorarioVeterinario;
import com.petcare.backend.persistence.entity.Mascota;
import com.petcare.backend.persistence.entity.Servicio;
import com.petcare.backend.persistence.entity.Usuario;
import com.petcare.backend.persistence.entity.Veterinario;
import com.petcare.backend.persistence.enums.EstadoCita;
import com.petcare.backend.persistence.enums.EstadoClinica;
import com.petcare.backend.persistence.enums.EstadoMascota;
import com.petcare.backend.persistence.enums.PlanClinica;
import com.petcare.backend.persistence.enums.SexoMascota;
import com.petcare.backend.web.ApiExceptionHandler;
import com.petcare.backend.web.AtencionClinicaController;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@Import({CitaService.class, VeterinarioService.class, AtencionClinicaService.class, InasistenciaService.class,
		MascotaService.class, AuthenticatedDuenioService.class, PlanService.class, ClinicaService.class, AppointmentConcurrencyTest.TestClock.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AppointmentConcurrencyTest {
	@TestConfiguration
	static class TestClock {
		@Bean Clock clock() { return Clock.systemDefaultZone(); }
	}

	@Autowired CitaService citas;
	@Autowired VeterinarioService veterinarios;
	@Autowired AtencionClinicaService atenciones;
	@Autowired InasistenciaService inasistencias;
	@Autowired MascotaService mascotas;
	@Autowired PlanService plans;
	@Autowired ClinicaService clinics;
	@Autowired EntityManager em;
	@Autowired PlatformTransactionManager transactionManager;

	@ParameterizedTest
	@ValueSource(strings = {"staff", "owner"})
	void simultaneousCreatesCommitOnlyOneOverlappingAppointment(String actor) {
		withFixture(false, fixture -> {
			Runnable create = actor.equals("owner")
					? () -> citas.createAsDuenio(request(fixture), fixture.ownerEmail())
					: () -> citas.create(request(fixture), fixture.clinicId());
			assertOneConflict(race(fixture, create, create));
			tx().executeWithoutResult(ignored -> assertThat(em.createQuery(
					"select count(c) from Cita c where c.clinica.id = :id and c.horaInicio = :start", Long.class)
					.setParameter("id", fixture.clinicId()).setParameter("start", LocalTime.of(10, 0))
					.getSingleResult()).isEqualTo(1L));
		});
	}

	@Test
	void concurrentReschedulesCannotMoveDifferentAppointmentsIntoTheSameSlot() {
		withFixture(false, fixture -> {
			CitaRequest later = new CitaRequest(fixture.ownerId(), fixture.petId(), fixture.vetId(), fixture.date(),
					LocalTime.of(11, 0), 30, "Otra cita", List.of(), BigDecimal.ZERO);
			Long otherId = citas.create(later, fixture.clinicId()).id();
			assertOneConflict(race(fixture,
					() -> citas.update(fixture.citaId(), request(fixture), fixture.clinicId()),
					() -> citas.update(otherId, request(fixture), fixture.clinicId())));
			tx().executeWithoutResult(ignored -> {
				assertThat(em.find(Cita.class, fixture.citaId()).getHoraInicio()).isEqualTo(LocalTime.of(10, 0));
				assertThat(em.find(Cita.class, otherId).getHoraInicio()).isEqualTo(LocalTime.of(11, 0));
			});
		});
	}

	@ParameterizedTest
	@CsvSource({"CANCELADA,update", "CANCELADA,confirm", "CANCELADA,ownerConfirm",
			"ATENDIDA,update", "ATENDIDA,cancel", "ATENDIDA,ownerCancel", "ATENDIDA,confirm", "ATENDIDA,noShow",
			"NO_ASISTIO,update", "NO_ASISTIO,cancel", "NO_ASISTIO,confirm", "NO_ASISTIO,attention"})
	void preloadedAppointmentCannotOverwriteCommittedTerminalState(EstadoCita terminal, String operation) {
		withFixture(terminal != EstadoCita.CANCELADA, fixture -> {
			String message = terminal == EstadoCita.CANCELADA ? "cancelada"
					: operation.equals("attention") || operation.equals("noShow") ? "programadas o confirmadas" : "cerrada";
			assertThatThrownBy(() -> tx().executeWithoutResult(ignored -> {
				Cita stale = em.find(Cita.class, fixture.citaId());
				concurrently(() -> close(fixture, terminal));
				assertThat(stale.getEstado()).isEqualTo(EstadoCita.PROGRAMADA);
				mutate(fixture, operation);
			})).isInstanceOf(IllegalArgumentException.class).hasMessageContaining(message);
			assertState(fixture, terminal);
		});
	}

	@ParameterizedTest
	@ValueSource(strings = {"cancel", "confirm", "ownerCancel", "ownerConfirm"})
	void stateChangesRefreshPreloadedReschedulesAndCancellationRemainsIdempotent(String operation) {
		withFixture(false, fixture -> {
			tx().executeWithoutResult(ignored -> {
				Cita stale = em.find(Cita.class, fixture.citaId());
				assertThat(stale.getDetallesCosto()).hasSize(1);
				concurrently(() -> citas.update(fixture.citaId(), request(fixture), fixture.clinicId()));
				assertThat(stale.getHoraInicio()).isEqualTo(LocalTime.of(9, 0));
				mutate(fixture, operation);
				if (operation.equals("cancel") || operation.equals("ownerCancel")) mutate(fixture, operation);
			});
			tx().executeWithoutResult(ignored -> {
				Cita saved = em.find(Cita.class, fixture.citaId());
				assertThat(saved.getEstado()).isEqualTo(operation.equals("cancel") || operation.equals("ownerCancel")
						? EstadoCita.CANCELADA : EstadoCita.CONFIRMADA);
				assertThat(saved.getHoraInicio()).isEqualTo(LocalTime.of(10, 0));
				assertThat(saved.getDetallesCosto()).isEmpty();
			});
		});
	}

	@ParameterizedTest
	@ValueSource(strings = {"deactivate", "schedule"})
	void bookingRefreshesPreloadedVeterinarianAfterCommittedScheduleChange(String change) {
		withFixture(false, fixture -> {
			assertThatThrownBy(() -> tx().executeWithoutResult(ignored -> {
				Veterinario stale = em.find(Veterinario.class, fixture.vetId());
				stale.getHorarios().size();
				concurrently(() -> {
					if (change.equals("deactivate")) veterinarios.deactivate(fixture.vetId(), fixture.clinicId());
					else veterinarios.update(fixture.vetId(), vetRequest(fixture, LocalTime.of(11, 0)), fixture.clinicId());
				});
				assertThat(stale.getActive()).isTrue();
				citas.create(request(fixture), fixture.clinicId());
			})).isInstanceOf(IllegalArgumentException.class);
		});
	}

	@Test
	void staleScheduleUpdateDoesNotReactivateADeactivatedVeterinarian() {
		withFixture(false, fixture -> {
			tx().executeWithoutResult(ignored -> {
				Veterinario stale = em.find(Veterinario.class, fixture.vetId());
				stale.getHorarios().size();
				concurrently(() -> veterinarios.deactivate(fixture.vetId(), fixture.clinicId()));
				assertThat(stale.getActive()).isTrue();
				veterinarios.update(fixture.vetId(), vetRequest(fixture, LocalTime.of(11, 0)), fixture.clinicId());
			});
			tx().executeWithoutResult(ignored -> assertThat(em.find(Veterinario.class, fixture.vetId()).getActive()).isFalse());
		});
	}

	@Test
	void duplicateAttentionCommitsOnceAndReturnsStructured400RatherThanConstraintFailure() {
		withFixture(true, fixture -> {
			Runnable register = () -> atenciones.register(fixture.citaId(), attention(), fixture.clinicId());
			assertOneConflict(race(fixture, register, register));
			assertState(fixture, EstadoCita.ATENDIDA);
			tx().executeWithoutResult(ignored -> assertThat(em.createQuery(
					"select count(a) from AtencionClinica a where a.cita.id = :id", Long.class)
					.setParameter("id", fixture.citaId()).getSingleResult()).isEqualTo(1L));
			var mvc = MockMvcBuilders.standaloneSetup(new AtencionClinicaController(atenciones, clinics))
					.setControllerAdvice(new ApiExceptionHandler()).build();
			try {
				mvc.perform(post("/api/citas/{id}/atencion", fixture.citaId())
						.principal(new UsernamePasswordAuthenticationToken(fixture.ownerEmail(), "unused", List.of()))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"motivo":"Control","diagnostico":"Estable","tratamiento":"Reposo","estadoMascota":"SALUDABLE"}
								"""))
						.andExpect(status().isBadRequest())
						.andExpect(jsonPath("$.status").value(400))
						.andExpect(jsonPath("$.message").isNotEmpty());
			} catch (Exception e) {
				throw new AssertionError("Duplicate attention must be a structured client error", e);
			}
		});
	}

	@Test
	void attentionAndNoShowCannotBothCommit() {
		withFixture(true, fixture -> {
			assertOneConflict(race(fixture,
					() -> atenciones.register(fixture.citaId(), attention(), fixture.clinicId()),
					() -> inasistencias.register(fixture.citaId(), new InasistenciaRequest("No asistio"), "staff", fixture.clinicId())));
			tx().executeWithoutResult(ignored -> {
				Cita saved = em.find(Cita.class, fixture.citaId());
				assertThat(saved.getEstado()).isIn(EstadoCita.ATENDIDA, EstadoCita.NO_ASISTIO);
				Long attentionCount = em.createQuery("select count(a) from AtencionClinica a where a.cita.id = :id", Long.class)
						.setParameter("id", fixture.citaId()).getSingleResult();
				Long noShowCount = em.createQuery("select count(i) from Inasistencia i where i.cita.id = :id", Long.class)
						.setParameter("id", fixture.citaId()).getSingleResult();
				assertThat(attentionCount + noShowCount).isEqualTo(1L);
			});
		});
	}

	@ParameterizedTest
	@ValueSource(strings = {"petUpdate", "ownerPetUpdate", "petDeactivate", "ownerPetDeactivate"})
	void preloadedPetWritersPreserveCommittedAttentionAndClinicalState(String operation) {
		withFixture(true, fixture -> {
			tx().executeWithoutResult(ignored -> {
				Mascota stale = em.find(Mascota.class, fixture.petId());
				concurrently(() -> atenciones.register(fixture.citaId(), attention(), fixture.clinicId()));
				assertThat(stale.getEstado()).isEqualTo(EstadoMascota.PENDIENTE);
				mutate(fixture, operation);
			});
			assertState(fixture, EstadoCita.ATENDIDA);
			tx().executeWithoutResult(ignored -> {
				Mascota saved = em.find(Mascota.class, fixture.petId());
				assertThat(saved.getEstado()).isEqualTo(EstadoMascota.SALUDABLE);
				assertThat(saved.getFechaEstado()).isNotNull();
				assertThat(saved.getVeterinarioEstado()).isEqualTo("Vet Test");
				if (operation.endsWith("Update")) assertPetProfile(saved);
				else {
					assertThat(saved.getActive()).isFalse();
					assertThat(saved.getEspecie()).isEqualTo("Perro");
					assertThat(saved.getRaza()).isEqualTo("Test");
				}
				assertThat(em.createQuery("select count(a) from AtencionClinica a where a.cita.id = :id", Long.class)
						.setParameter("id", fixture.citaId()).getSingleResult()).isEqualTo(1L);
			});
		});
	}

	@ParameterizedTest
	@ValueSource(strings = {"petUpdate", "ownerPetUpdate"})
	void stalePetProfileUpdateDoesNotReactivateADeactivatedPet(String operation) {
		withFixture(false, fixture -> {
			tx().executeWithoutResult(ignored -> {
				Mascota stale = em.find(Mascota.class, fixture.petId());
				concurrently(() -> mascotas.deactivate(fixture.petId(), fixture.clinicId()));
				assertThat(stale.getActive()).isTrue();
				mutate(fixture, operation);
			});
			assertState(fixture, EstadoCita.CANCELADA);
			tx().executeWithoutResult(ignored -> {
				Mascota saved = em.find(Mascota.class, fixture.petId());
				assertPetProfile(saved);
				assertThat(saved.getActive()).isFalse();
			});
		});
	}

	@ParameterizedTest
	@ValueSource(strings = {"petDeactivate", "ownerPetDeactivate"})
	void petDeactivationReloadsPreloadedAppointmentListAfterRescheduleAndConfirmation(String operation) {
		withFixture(false, fixture -> {
			tx().executeWithoutResult(ignored -> {
				List<Cita> stale = em.createQuery("select c from Cita c where c.mascota.id = :id and c.estado in :states", Cita.class)
						.setParameter("id", fixture.petId()).setParameter("states", List.of(EstadoCita.PROGRAMADA, EstadoCita.CONFIRMADA))
						.getResultList();
				assertThat(stale.getFirst().getDetallesCosto()).hasSize(1);
				concurrently(() -> {
					citas.update(fixture.citaId(), request(fixture), fixture.clinicId());
					citas.confirm(fixture.citaId(), "staff", fixture.clinicId());
				});
				assertThat(stale.getFirst().getHoraInicio()).isEqualTo(LocalTime.of(9, 0));
				assertThat(stale.getFirst().getEstado()).isEqualTo(EstadoCita.PROGRAMADA);
				mutate(fixture, operation);
			});
			tx().executeWithoutResult(ignored -> {
				Cita saved = em.find(Cita.class, fixture.citaId());
				assertThat(saved.getEstado()).isEqualTo(EstadoCita.CANCELADA);
				assertThat(saved.getHoraInicio()).isEqualTo(LocalTime.of(10, 0));
				assertThat(saved.getFechaConfirmacion()).isNotNull();
				assertThat(saved.getConfirmadaPor()).isEqualTo("staff");
				assertThat(saved.getDetallesCosto()).isEmpty();
				assertThat(em.find(Mascota.class, fixture.petId()).getActive()).isFalse();
			});
		});
	}

	@ParameterizedTest
	@CsvSource({"petDeactivate,true", "petDeactivate,false", "ownerPetDeactivate,true", "ownerPetDeactivate,false"})
	void petDeactivationSerializesWithBookingAndFreedSlotsCannotBeDoubleBooked(String operation, boolean deactivateFirst) {
		withFixture(false, fixture -> {
			Runnable deactivate = () -> mutate(fixture, operation);
			Runnable book = () -> citas.create(request(fixture), fixture.clinicId());
			if (deactivateFirst) {
				assertOneConflict(race(fixture, deactivate, () -> {
					assertThat(em.find(Mascota.class, fixture.petId()).getActive()).isTrue();
					book.run();
				}));
			} else {
				assertThat(race(fixture, book, deactivate)).allSatisfy(outcome -> assertThat(outcome).isNull());
			}
			tx().executeWithoutResult(ignored -> {
				assertThat(em.find(Mascota.class, fixture.petId()).getActive()).isFalse();
				assertThat(em.createQuery("select c from Cita c where c.mascota.id = :id", Cita.class)
						.setParameter("id", fixture.petId()).getResultList()).allSatisfy(cita ->
						assertThat(cita.getEstado()).isEqualTo(EstadoCita.CANCELADA));
			});
			Long replacementPetId = mascotas.create(petRequest(fixture), fixture.clinicId()).id();
			CitaRequest replacement = new CitaRequest(fixture.ownerId(), replacementPetId, fixture.vetId(), fixture.date(),
					LocalTime.of(10, 0), 30, "Reemplazo", List.of(), BigDecimal.ZERO);
			Runnable replace = () -> citas.create(replacement, fixture.clinicId());
			assertOneConflict(race(fixture, replace, replace));
			tx().executeWithoutResult(ignored -> assertThat(em.createQuery("""
					select c from Cita c where c.clinica.id = :id and c.horaInicio = :start and c.estado <> :cancelled
					""", Cita.class).setParameter("id", fixture.clinicId()).setParameter("start", LocalTime.of(10, 0))
					.setParameter("cancelled", EstadoCita.CANCELADA).getResultList())
					.satisfiesExactly(cita -> assertThat(cita.getMascota().getId()).isEqualTo(replacementPetId)));
		});
	}

	@Test
	void petDeactivationOnlyCancelsFuturePendingAppointments() {
		withFixture(false, fixture -> {
			List<Long> preserved = tx().execute(ignored -> List.of(
					seedAppointment(fixture, LocalDate.now(), EstadoCita.PROGRAMADA),
					seedAppointment(fixture, LocalDate.now(), EstadoCita.CONFIRMADA),
					seedAppointment(fixture, fixture.date(), EstadoCita.ATENDIDA),
					seedAppointment(fixture, fixture.date(), EstadoCita.NO_ASISTIO)));
			mascotas.deactivate(fixture.petId(), fixture.clinicId());
			assertState(fixture, EstadoCita.CANCELADA);
			tx().executeWithoutResult(ignored -> assertThat(preserved.stream().map(id -> em.find(Cita.class, id).getEstado()).toList())
					.containsExactly(EstadoCita.PROGRAMADA, EstadoCita.CONFIRMADA, EstadoCita.ATENDIDA, EstadoCita.NO_ASISTIO));
		});
	}

	@ParameterizedTest
	@ValueSource(strings = {"staff", "owner"})
	void sameTransactionProfileAttentionAndDeactivationPreserveCompletedWrites(String actor) {
		withFixture(true, fixture -> {
			tx().executeWithoutResult(ignored -> {
				mutate(fixture, actor.equals("owner") ? "ownerPetUpdate" : "petUpdate");
				atenciones.register(fixture.citaId(), attention(), fixture.clinicId());
				mutate(fixture, actor.equals("owner") ? "ownerPetUpdate" : "petUpdate");
				mutate(fixture, actor.equals("owner") ? "ownerPetDeactivate" : "petDeactivate");
			});
			assertState(fixture, EstadoCita.ATENDIDA);
			tx().executeWithoutResult(ignored -> {
				Mascota saved = em.find(Mascota.class, fixture.petId());
				assertPetProfile(saved);
				assertThat(saved.getEstado()).isEqualTo(EstadoMascota.SALUDABLE);
				assertThat(saved.getFechaEstado()).isNotNull();
				assertThat(saved.getVeterinarioEstado()).isEqualTo("Vet Test");
				assertThat(saved.getActive()).isFalse();
			});
		});
	}

	@ParameterizedTest
	@ValueSource(strings = {"create", "update", "cancel", "confirm", "ownerCreate", "ownerCancel", "ownerConfirm",
			"vetUpdate", "vetDeactivate", "attention", "noShow", "petUpdate", "ownerPetUpdate", "petDeactivate", "ownerPetDeactivate"})
	void everyWriterRejectsAuthoritativeSuspensionEvenWithPreloadedEntities(String operation) {
		withFixture(false, fixture -> {
			assertThatThrownBy(() -> tx().executeWithoutResult(ignored -> {
				em.find(Cita.class, fixture.citaId());
				em.find(Veterinario.class, fixture.vetId());
				Clinica stale = em.find(Clinica.class, fixture.clinicId());
				concurrently(() -> tx().executeWithoutResult(other -> plans.lockClinic(fixture.clinicId()).setEstado(EstadoClinica.SUSPENDIDA)));
				assertThat(stale.getEstado()).isEqualTo(EstadoClinica.ACTIVA);
				mutate(fixture, operation);
			})).isInstanceOfSatisfying(PlanException.class, failure -> {
				assertThat(failure.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
				assertThat(failure.getCode()).isEqualTo("CLINIC_READ_ONLY");
			});
			assertState(fixture, EstadoCita.PROGRAMADA);
		});
	}

	@ParameterizedTest
	@ValueSource(strings = {"create", "cancel", "confirm", "petUpdate", "petDeactivate"})
	void ownerDenialDoesNotTryToLockTheForeignClinic(String operation) {
		withFixture(false, owner -> withFixture(false, foreign -> {
			tx().executeWithoutResult(ignored -> {
				plans.lockForWrite(foreign.clinicId());
				concurrently(() -> assertThatThrownBy(() -> {
					switch (operation) {
						case "create" -> citas.createAsDuenio(request(foreign), owner.ownerEmail());
						case "cancel" -> citas.cancelAsDuenio(foreign.citaId(), owner.ownerEmail());
						case "confirm" -> citas.confirmAsDuenio(foreign.citaId(), owner.ownerEmail());
						case "petUpdate" -> mascotas.updateForDuenio(foreign.petId(), petRequest(foreign), owner.ownerEmail());
						case "petDeactivate" -> mascotas.deactivateForDuenio(foreign.petId(), owner.ownerEmail());
						default -> throw new AssertionError(operation);
					}
				}).isInstanceOf(AccessDeniedException.class));
			});
			assertState(foreign, EstadoCita.PROGRAMADA);
		}));
	}

	private void mutate(Fixture fixture, String operation) {
		switch (operation) {
			case "create" -> citas.create(request(fixture), fixture.clinicId());
			case "update" -> citas.update(fixture.citaId(), request(fixture), fixture.clinicId());
			case "cancel" -> citas.cancel(fixture.citaId(), fixture.clinicId());
			case "confirm" -> citas.confirm(fixture.citaId(), "staff", fixture.clinicId());
			case "ownerCreate" -> citas.createAsDuenio(request(fixture), fixture.ownerEmail());
			case "ownerCancel" -> citas.cancelAsDuenio(fixture.citaId(), fixture.ownerEmail());
			case "ownerConfirm" -> citas.confirmAsDuenio(fixture.citaId(), fixture.ownerEmail());
			case "attention" -> atenciones.register(fixture.citaId(), attention(), fixture.clinicId());
			case "noShow" -> inasistencias.register(fixture.citaId(), new InasistenciaRequest("No asistio"), "staff", fixture.clinicId());
			case "vetUpdate" -> veterinarios.update(fixture.vetId(), vetRequest(fixture, LocalTime.of(11, 0)), fixture.clinicId());
			case "vetDeactivate" -> veterinarios.deactivate(fixture.vetId(), fixture.clinicId());
			case "petUpdate" -> mascotas.update(fixture.petId(), petRequest(fixture), fixture.clinicId());
			case "ownerPetUpdate" -> mascotas.updateForDuenio(fixture.petId(), petRequest(fixture), fixture.ownerEmail());
			case "petDeactivate" -> mascotas.deactivate(fixture.petId(), fixture.clinicId());
			case "ownerPetDeactivate" -> mascotas.deactivateForDuenio(fixture.petId(), fixture.ownerEmail());
			default -> throw new AssertionError(operation);
		}
	}

	private void close(Fixture fixture, EstadoCita terminal) {
		switch (terminal) {
			case CANCELADA -> citas.cancel(fixture.citaId(), fixture.clinicId());
			case ATENDIDA -> atenciones.register(fixture.citaId(), attention(), fixture.clinicId());
			case NO_ASISTIO -> inasistencias.register(fixture.citaId(), new InasistenciaRequest("No asistio"), "staff", fixture.clinicId());
			default -> throw new AssertionError(terminal);
		}
	}

	private List<Throwable> race(Fixture fixture, Runnable first, Runnable second) {
		CountDownLatch locked = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		CountDownLatch secondStarted = new CountDownLatch(1);
		try (var executor = Executors.newFixedThreadPool(2)) {
			var a = executor.submit(() -> attempt(() -> {
				plans.lockForWrite(fixture.clinicId());
				locked.countDown();
				await(release);
				first.run();
			}));
			await(locked);
			var b = executor.submit(() -> attempt(() -> {
				secondStarted.countDown();
				second.run();
			}));
			try {
				await(secondStarted);
				assertThatThrownBy(() -> b.get(250, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
			} finally {
				release.countDown();
			}
			return Arrays.asList(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS));
		} catch (Exception e) {
			throw new AssertionError("Concurrent appointment transactions did not finish", e);
		}
	}

	private Throwable attempt(Runnable action) {
		try {
			tx().executeWithoutResult(ignored -> action.run());
			return null;
		} catch (RuntimeException failure) {
			return failure;
		}
	}

	private void await(CountDownLatch latch) {
		try {
			assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new AssertionError("Interrupted while coordinating appointment transactions", e);
		}
	}

	private void assertOneConflict(List<Throwable> outcomes) {
		assertThat(outcomes.stream().filter(Objects::isNull).count()).isEqualTo(1L);
		assertThat(outcomes.stream().filter(Objects::nonNull).toList())
				.satisfiesExactly(failure -> assertThat(failure).isInstanceOf(IllegalArgumentException.class));
	}

	private void concurrently(Runnable action) {
		try (var executor = Executors.newSingleThreadExecutor()) {
			executor.submit(action).get(10, TimeUnit.SECONDS);
		} catch (Exception e) {
			throw new AssertionError("Concurrent state mutation did not commit", e);
		}
	}

	private void assertState(Fixture fixture, EstadoCita state) {
		tx().executeWithoutResult(ignored -> assertThat(em.find(Cita.class, fixture.citaId()).getEstado()).isEqualTo(state));
	}

	private CitaRequest request(Fixture fixture) {
		return new CitaRequest(fixture.ownerId(), fixture.petId(), fixture.vetId(), fixture.date(),
				LocalTime.of(10, 0), 30, "Control", List.of(), BigDecimal.ZERO);
	}

	private VeterinarioRequest vetRequest(Fixture fixture, LocalTime start) {
		return new VeterinarioRequest(null, "Vet", "Test", fixture.colegiatura(), "General", "999888777", null,
				List.of(new HorarioVeterinarioRequest(fixture.date().getDayOfWeek(), start, LocalTime.of(12, 0), 30)));
	}

	private AtencionClinicaRequest attention() {
		return new AtencionClinicaRequest("Control", "Estable", "Reposo", null, null, "Privada", EstadoMascota.SALUDABLE);
	}

	private MascotaRequest petRequest(Fixture fixture) {
		return new MascotaRequest(fixture.ownerId(), "Perfil actualizado", "Gato", "Siames", SexoMascota.HEMBRA,
				LocalDate.of(2021, 2, 12), "Crema", new BigDecimal("4.80"), "Observaciones de perfil", "https://example.test/pet.jpg");
	}

	private void assertPetProfile(Mascota pet) {
		assertThat(pet.getNombre()).isEqualTo("Perfil actualizado");
		assertThat(pet.getEspecie()).isEqualTo("Gato");
		assertThat(pet.getRaza()).isEqualTo("Siames");
		assertThat(pet.getSexo()).isEqualTo(SexoMascota.HEMBRA);
		assertThat(pet.getFechaNacimiento()).isEqualTo(LocalDate.of(2021, 2, 12));
		assertThat(pet.getColor()).isEqualTo("Crema");
		assertThat(pet.getPesoKg()).isEqualByComparingTo("4.80");
		assertThat(pet.getObservaciones()).isEqualTo("Observaciones de perfil");
		assertThat(pet.getFotoUrl()).isEqualTo("https://example.test/pet.jpg");
	}

	private Long seedAppointment(Fixture fixture, LocalDate date, EstadoCita state) {
		LocalDateTime now = LocalDateTime.now();
		Cita cita = Cita.builder().clinica(em.find(Clinica.class, fixture.clinicId())).duenio(em.find(Duenio.class, fixture.ownerId()))
				.mascota(em.find(Mascota.class, fixture.petId())).veterinario(em.find(Veterinario.class, fixture.vetId()))
				.fecha(date).horaInicio(LocalTime.MIN).horaFin(LocalTime.of(0, 30)).duracionMinutos(30).motivo("Historial")
				.estado(state).subtotal(BigDecimal.ZERO).descuento(BigDecimal.ZERO).total(BigDecimal.ZERO)
				.requiereConfirmacion(false).createdAt(now).updatedAt(now).build();
		em.persist(cita);
		return cita.getId();
	}

	private TransactionTemplate tx() {
		return new TransactionTemplate(transactionManager);
	}

	private void withFixture(boolean past, Consumer<Fixture> test) {
		Fixture fixture = tx().execute(ignored -> {
			LocalDateTime now = LocalDateTime.now();
			LocalDate date = now.toLocalDate().plusDays(past ? -1 : 1);
			Clinica clinic = Clinica.builder().nombre("Scheduling test").slug(UUID.randomUUID().toString())
					.plan(PlanClinica.PRO).estado(EstadoClinica.ACTIVA).createdAt(now).updatedAt(now).build();
			em.persist(clinic);
			String email = UUID.randomUUID() + "@test.com";
			Usuario user = Usuario.builder().clinica(clinic).fullName("Owner").email(email).password("unused")
					.active(true).createdAt(now).build();
			em.persist(user);
			Duenio owner = Duenio.builder().clinica(clinic).usuario(user).nombres("Owner").apellidos("Test")
					.tipoDocumento("DNI").numeroDocumento("12345678").telefono("999888777").email(email)
					.active(true).createdAt(now).updatedAt(now).build();
			em.persist(owner);
			Mascota pet = Mascota.builder().clinica(clinic).duenio(owner).nombre("Pet").especie("Perro").raza("Test")
					.sexo(SexoMascota.MACHO).fechaNacimiento(LocalDate.of(2020, 1, 1)).estado(EstadoMascota.PENDIENTE)
					.active(true).createdAt(now).updatedAt(now).build();
			em.persist(pet);
			Veterinario vet = Veterinario.builder().clinica(clinic).nombres("Vet").apellidos("Test")
					.numeroColegiatura(UUID.randomUUID().toString().substring(0, 20)).especialidad("General")
					.active(true).createdAt(now).updatedAt(now).build();
			em.persist(vet);
			HorarioVeterinario schedule = HorarioVeterinario.builder().veterinario(vet).diaSemana(date.getDayOfWeek())
					.horaInicio(LocalTime.of(9, 0)).horaFin(LocalTime.of(12, 0)).duracionBloqueMinutos(30).active(true).build();
			vet.getHorarios().add(schedule);
			em.persist(schedule);
			Cita cita = Cita.builder().clinica(clinic).duenio(owner).mascota(pet).veterinario(vet).fecha(date)
					.horaInicio(LocalTime.of(9, 0)).horaFin(LocalTime.of(9, 30)).duracionMinutos(30).motivo("Original")
					.estado(EstadoCita.PROGRAMADA).subtotal(BigDecimal.ZERO).descuento(BigDecimal.ZERO).total(BigDecimal.ZERO)
					.requiereConfirmacion(true).createdAt(now).updatedAt(now).build();
			em.persist(cita);
			Servicio service = Servicio.builder().clinica(clinic).nombre("Consulta").descripcion("Control")
					.costoBase(BigDecimal.TEN).active(true).createdAt(now).updatedAt(now).build();
			em.persist(service);
			DetalleCostoCita cost = DetalleCostoCita.builder().cita(cita).servicio(service).nombreServicio("Consulta")
					.costoUnitario(BigDecimal.TEN).cantidad(1).subtotal(BigDecimal.TEN).descuento(BigDecimal.ZERO)
					.total(BigDecimal.TEN).createdAt(now).build();
			cita.getDetallesCosto().add(cost);
			cita.setSubtotal(BigDecimal.TEN);
			cita.setTotal(BigDecimal.TEN);
			em.persist(cost);
			return new Fixture(clinic.getId(), user.getId(), owner.getId(), pet.getId(), vet.getId(), cita.getId(),
					email, vet.getNumeroColegiatura(), date);
		});
		try {
			test.accept(fixture);
		} finally {
			tx().executeWithoutResult(ignored -> {
				em.createQuery("delete from AtencionClinica a where a.cita.id in (select c.id from Cita c where c.clinica.id = :id)")
						.setParameter("id", fixture.clinicId()).executeUpdate();
				em.createQuery("delete from Inasistencia i where i.cita.id in (select c.id from Cita c where c.clinica.id = :id)")
						.setParameter("id", fixture.clinicId()).executeUpdate();
				em.createQuery("delete from DetalleCostoCita d where d.cita.id in (select c.id from Cita c where c.clinica.id = :id)")
						.setParameter("id", fixture.clinicId()).executeUpdate();
				em.createQuery("delete from Cita c where c.clinica.id = :id").setParameter("id", fixture.clinicId()).executeUpdate();
				em.createQuery("delete from Servicio s where s.clinica.id = :id").setParameter("id", fixture.clinicId()).executeUpdate();
				em.remove(em.find(Veterinario.class, fixture.vetId()));
				em.createQuery("delete from Mascota m where m.clinica.id = :id").setParameter("id", fixture.clinicId()).executeUpdate();
				em.remove(em.find(Duenio.class, fixture.ownerId()));
				em.remove(em.find(Usuario.class, fixture.userId()));
				em.flush();
				em.remove(em.find(Clinica.class, fixture.clinicId()));
			});
		}
	}

	private record Fixture(Long clinicId, Long userId, Long ownerId, Long petId, Long vetId, Long citaId,
			String ownerEmail, String colegiatura, LocalDate date) {}
}
