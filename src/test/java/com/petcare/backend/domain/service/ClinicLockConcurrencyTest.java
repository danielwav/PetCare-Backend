package com.petcare.backend.domain.service;

import com.petcare.backend.domain.dto.request.SaasClinicEstadoRequest;
import com.petcare.backend.domain.dto.request.SaasClinicPlanRequest;
import com.petcare.backend.domain.dto.request.UpdateClinicaRequest;
import com.petcare.backend.persistence.entity.*;
import com.petcare.backend.persistence.enums.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.petcare.backend.domain.service.SaasClinicServiceTest.NOW;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@Import({PlanService.class, ClinicaService.class, SaasClinicService.class, SaasClinicServiceTest.FixedClock.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ClinicLockConcurrencyTest {
	@Autowired PlanService plans;
	@Autowired ClinicaService profiles;
	@Autowired SaasClinicService saas;
	@Autowired EntityManager em;
	@Autowired PlatformTransactionManager transactionManager;

	@Test
	void preloadedProCannotAddThirdStaffAfterCommittedDowngrade() {
		withFixture(2, 0, fixture -> {
			assertPlanFailure(() -> tx().executeWithoutResult(ignored -> {
				Clinica stale = em.find(Clinica.class, fixture.clinicId());
				plans.assertCanWrite(stale.getId());
				concurrently(() -> downgrade(fixture));
				assertThat(stale.getPlan()).isEqualTo(PlanClinica.PRO);
				plans.assertStaffCapacity(stale.getId());
				staff(stale);
			}), HttpStatus.CONFLICT, "STAFF_LIMIT_REACHED");
			var plan = plans.getPlan(fixture.clinicId());
			assertThat(plan.plan()).isEqualTo("CONSULTORIO");
			assertThat(plan.staffUsed()).isEqualTo(2);
			assertThat(saas.audit(fixture.operator(), fixture.clinicId(), 0, 20).totalElements()).isEqualTo(1);
		});
	}

	@Test
	void preloadedProCannotAdd501stPetAfterCommittedDowngrade() {
		withFixture(0, 500, fixture -> {
			assertPlanFailure(() -> tx().executeWithoutResult(ignored -> {
				Clinica stale = em.find(Clinica.class, fixture.clinicId());
				plans.assertCanWrite(stale.getId());
				concurrently(() -> downgrade(fixture));
				assertThat(stale.getPlan()).isEqualTo(PlanClinica.PRO);
				plans.assertPetCapacity(stale.getId());
				pet(stale, em.find(Duenio.class, fixture.ownerId()));
			}), HttpStatus.CONFLICT, "PET_LIMIT_REACHED");
			var plan = plans.getPlan(fixture.clinicId());
			assertThat(plan.plan()).isEqualTo("CONSULTORIO");
			assertThat(plan.petsUsed()).isEqualTo(500);
		});
	}

	@ParameterizedTest
	@ValueSource(strings = {"staff", "pets", "write"})
	void preloadedActiveClinicRejectsCapacityAndWriteLocksAfterCommittedSuspension(String guard) {
		withFixture(0, 0, fixture -> {
			assertPlanFailure(() -> tx().executeWithoutResult(ignored -> {
				Clinica stale = em.find(Clinica.class, fixture.clinicId());
				plans.assertCanWrite(stale.getId());
				concurrently(() -> suspend(fixture));
				assertThat(stale.getEstado()).isEqualTo(EstadoClinica.ACTIVA);
				switch (guard) {
					case "staff" -> plans.assertStaffCapacity(stale.getId());
					case "pets" -> plans.assertPetCapacity(stale.getId());
					case "write" -> plans.lockForWrite(stale.getId());
					default -> throw new AssertionError(guard);
				}
			}), HttpStatus.FORBIDDEN, "CLINIC_READ_ONLY");
			assertThat(plans.getPlan(fixture.clinicId()).status()).isEqualTo("SUSPENDED");
		});
	}

	@Test
	void profileUpdateRefreshesPreloadedClinicAndPreservesOperatorPlanAndTrialClocks() {
		withFixture(0, 0, fixture -> {
			tx().executeWithoutResult(ignored -> {
				Clinica stale = em.find(Clinica.class, fixture.clinicId());
				concurrently(() -> downgrade(fixture));
				assertThat(stale.getPlan()).isEqualTo(PlanClinica.PRO);
				var updated = profiles.updateMyClinic(fixture.adminEmail(), profileRequest(fixture));
				assertThat(updated.nombre()).isEqualTo("Updated profile");
				assertThat(updated.plan()).isEqualTo("CONSULTORIO");
			});
			tx().executeWithoutResult(ignored -> {
				Clinica saved = em.find(Clinica.class, fixture.clinicId());
				assertThat(saved.getNombre()).isEqualTo("Updated profile");
				assertThat(saved.getPlan()).isEqualTo(PlanClinica.CONSULTORIO);
				assertThat(saved.getEstado()).isEqualTo(EstadoClinica.ACTIVA);
				assertThat(saved.getTrialStartedAt()).isEqualTo(NOW.minusDays(20));
				assertThat(saved.getTrialEndsAt()).isEqualTo(NOW.minusDays(6));
			});
		});
	}

	@Test
	void profileUpdateRejectsPreloadedActiveClinicAfterOperatorSuspendsIt() {
		withFixture(0, 0, fixture -> {
			assertPlanFailure(() -> tx().executeWithoutResult(ignored -> {
				Clinica stale = em.find(Clinica.class, fixture.clinicId());
				concurrently(() -> {
					downgrade(fixture);
					suspend(fixture);
				});
				assertThat(stale.getPlan()).isEqualTo(PlanClinica.PRO);
				assertThat(stale.getEstado()).isEqualTo(EstadoClinica.ACTIVA);
				profiles.updateMyClinic(fixture.adminEmail(), profileRequest(fixture));
			}), HttpStatus.FORBIDDEN, "CLINIC_READ_ONLY");
			tx().executeWithoutResult(ignored -> {
				Clinica saved = em.find(Clinica.class, fixture.clinicId());
				assertThat(saved.getNombre()).isEqualTo("Original profile");
				assertThat(saved.getPlan()).isEqualTo(PlanClinica.CONSULTORIO);
				assertThat(saved.getEstado()).isEqualTo(EstadoClinica.SUSPENDIDA);
			});
		});
	}

	@Test
	void saasMutationRefreshesPreloadedPlanAndStateAndAuditsAuthoritativeSnapshots() {
		withFixture(0, 0, fixture -> {
			tx().executeWithoutResult(ignored -> {
				Clinica stale = em.find(Clinica.class, fixture.clinicId());
				concurrently(() -> {
					downgrade(fixture);
					suspend(fixture);
				});
				assertThat(stale.getPlan()).isEqualTo(PlanClinica.PRO);
				var result = saas.changePlan(fixture.operator(), fixture.clinicId(), new SaasClinicPlanRequest("PRO", "Restore paid plan"));
				assertThat(result.plan().plan()).isEqualTo("PRO");
				assertThat(result.estado()).isEqualTo("SUSPENDIDA");
			});
			var entries = saas.audit(fixture.operator(), fixture.clinicId(), 0, 20);
			assertThat(entries.totalElements()).isEqualTo(3);
			var latest = entries.items().getFirst();
			assertThat(latest.previousPlan()).isEqualTo("CONSULTORIO");
			assertThat(latest.newPlan()).isEqualTo("PRO");
			assertThat(latest.previousEstado()).isEqualTo("SUSPENDIDA");
			assertThat(latest.newEstado()).isEqualTo("SUSPENDIDA");
		});
	}

	@Test
	void repeatedLocksPreserveUnflushedChangesMadeUnderTheExistingWriteLock() {
		withFixture(0, 0, fixture -> {
			tx().executeWithoutResult(ignored -> {
				Clinica locked = plans.lockClinic(fixture.clinicId());
				locked.setNombre("Pending profile");
				locked.setPlan(PlanClinica.CONSULTORIO);
				plans.lockForWrite(locked.getId());
				assertThat(locked.getNombre()).isEqualTo("Pending profile");
				assertThat(locked.getPlan()).isEqualTo(PlanClinica.CONSULTORIO);
				plans.assertStaffCapacity(locked.getId());
				plans.assertPetCapacity(locked.getId());
				var result = saas.changeEstado(fixture.operator(), locked.getId(), new SaasClinicEstadoRequest("SUSPENDIDA", "Suspend after edits"));
				assertThat(result.nombre()).isEqualTo("Pending profile");
				assertThat(result.plan().plan()).isEqualTo("CONSULTORIO");
			});
			tx().executeWithoutResult(ignored -> {
				Clinica saved = em.find(Clinica.class, fixture.clinicId());
				assertThat(saved.getNombre()).isEqualTo("Pending profile");
				assertThat(saved.getPlan()).isEqualTo(PlanClinica.CONSULTORIO);
				assertThat(saved.getEstado()).isEqualTo(EstadoClinica.SUSPENDIDA);
			});
		});
	}

	private void withFixture(int staffCount, int petCount, Consumer<Fixture> test) {
		Fixture fixture = tx().execute(ignored -> {
			Clinica clinic = Clinica.builder().nombre("Original profile").slug(UUID.randomUUID().toString())
					.plan(PlanClinica.PRO).estado(EstadoClinica.ACTIVA).createdAt(NOW).updatedAt(NOW)
					.trialStartedAt(NOW.minusDays(20)).trialEndsAt(NOW.minusDays(6)).build();
			em.persist(clinic);
			Usuario operator = user(null, role(RoleName.ROLE_SAAS_OPERATOR));
			Usuario admin = user(clinic, role(RoleName.ROLE_ADMIN));
			for (int i = 1; i < staffCount; i++) staff(clinic);
			Duenio owner = null;
			if (petCount > 0) {
				owner = Duenio.builder().clinica(clinic).nombres("Owner").apellidos("Test").tipoDocumento("DNI")
						.numeroDocumento("12345678").telefono("123456789").email("owner@test.com").active(true).createdAt(NOW).updatedAt(NOW).build();
				em.persist(owner);
				for (int i = 0; i < petCount; i++) pet(clinic, owner);
			}
			Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(operator.getEmail(), "unused",
					List.of(new SimpleGrantedAuthority("ROLE_SAAS_OPERATOR")));
			return new Fixture(clinic.getId(), clinic.getSlug(), operator.getId(), authentication, admin.getEmail(), owner == null ? null : owner.getId());
		});
		try {
			test.accept(fixture);
		} finally {
			tx().executeWithoutResult(ignored -> {
				em.createQuery("delete from SaasClinicAudit where clinicId = :id").setParameter("id", fixture.clinicId()).executeUpdate();
				em.createQuery("delete from Mascota where clinica.id = :id").setParameter("id", fixture.clinicId()).executeUpdate();
				em.createQuery("delete from Duenio where clinica.id = :id").setParameter("id", fixture.clinicId()).executeUpdate();
				em.createQuery("select u from Usuario u where u.clinica.id = :id or u.id = :operator", Usuario.class)
						.setParameter("id", fixture.clinicId()).setParameter("operator", fixture.operatorId()).getResultList().forEach(em::remove);
				em.flush();
				em.remove(em.find(Clinica.class, fixture.clinicId()));
			});
		}
	}

	private TransactionTemplate tx() {
		return new TransactionTemplate(transactionManager);
	}

	private void concurrently(Runnable action) {
		// The other transaction must commit while this thread retains its original managed snapshot.
		try (var executor = Executors.newSingleThreadExecutor()) {
			executor.submit(action).get(10, TimeUnit.SECONDS);
		} catch (Exception e) {
			throw new AssertionError("Concurrent operator mutation did not commit", e);
		}
	}

	private void downgrade(Fixture fixture) {
		saas.changePlan(fixture.operator(), fixture.clinicId(), new SaasClinicPlanRequest("CONSULTORIO", "Lower paid plan"));
	}

	private void suspend(Fixture fixture) {
		saas.changeEstado(fixture.operator(), fixture.clinicId(), new SaasClinicEstadoRequest("SUSPENDIDA", "Suspend clinic"));
	}

	private UpdateClinicaRequest profileRequest(Fixture fixture) {
		return new UpdateClinicaRequest("Updated profile", fixture.slug(), "Updated address", null, null, null, null);
	}

	private Usuario user(Clinica clinic, Rol role) {
		Usuario user = Usuario.builder().clinica(clinic).fullName("Test").email(UUID.randomUUID() + "@test.com").password("unused")
				.active(true).forcePasswordChange(false).roles(Set.of(role)).createdAt(NOW).build();
		em.persist(user);
		return user;
	}

	private void staff(Clinica clinic) {
		user(clinic, role(RoleName.ROLE_ADMIN));
	}

	private Rol role(RoleName name) {
		var roles = em.createQuery("select r from Rol r where r.name = :name", Rol.class).setParameter("name", name).getResultList();
		if (!roles.isEmpty()) return roles.getFirst();
		Rol role = Rol.builder().name(name).description(name.name()).active(true).build();
		em.persist(role);
		return role;
	}

	private void pet(Clinica clinic, Duenio owner) {
		em.persist(Mascota.builder().clinica(clinic).duenio(owner).nombre("Pet").especie("Canino").raza("Test")
				.sexo(SexoMascota.MACHO).fechaNacimiento(LocalDate.of(2020, 1, 1)).active(true).createdAt(NOW).updatedAt(NOW).build());
	}

	private void assertPlanFailure(org.assertj.core.api.ThrowableAssert.ThrowingCallable action, HttpStatus status, String code) {
		assertThatThrownBy(action).isInstanceOfSatisfying(PlanException.class, failure -> {
			assertThat(failure.getStatus()).isEqualTo(status);
			assertThat(failure.getCode()).isEqualTo(code);
		});
	}

	private record Fixture(Long clinicId, String slug, Long operatorId, Authentication operator, String adminEmail, Long ownerId) {}
}
