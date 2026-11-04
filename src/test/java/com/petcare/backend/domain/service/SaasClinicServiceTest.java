package com.petcare.backend.domain.service;

import com.petcare.backend.domain.dto.request.SaasClinicEstadoRequest;
import com.petcare.backend.domain.dto.request.SaasClinicPlanRequest;
import com.petcare.backend.domain.repository.ClinicaRepository;
import com.petcare.backend.domain.repository.SaasClinicAuditRepository;
import com.petcare.backend.persistence.entity.*;
import com.petcare.backend.persistence.enums.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.*;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@Import({SaasClinicService.class, PlanService.class, SaasClinicServiceTest.FixedClock.class})
class SaasClinicServiceTest {
	static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 3, 12, 0);
	@TestConfiguration
	static class FixedClock {
		@Bean Clock clock() { return Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC); }
	}

	@Autowired SaasClinicService service;
	@Autowired PlanService plans;
	@Autowired ClinicaRepository clinics;
	@Autowired SaasClinicAuditRepository audits;
	@Autowired EntityManager em;
	@Autowired PlatformTransactionManager transactionManager;

	@Test
	void planChangePreservesStateAndTrialClocksAndStoresTrimmedAuditSnapshot() {
		Usuario actor = operator();
		Clinica clinic = clinic(PlanClinica.TRIAL, EstadoClinica.SUSPENDIDA);
		var result = service.changePlan(auth(actor), clinic.getId(), new SaasClinicPlanRequest("PRO", "  Pago confirmado  "));
		assertThat(result.plan().plan()).isEqualTo("PRO");
		assertThat(result.estado()).isEqualTo("SUSPENDIDA");
		assertThat(result.plan().readOnly()).isTrue();
		assertThat(result.plan().trialStartedAt()).isEqualTo(NOW.minusDays(20));
		assertThat(result.plan().trialEndsAt()).isEqualTo(NOW.minusDays(6));
		em.flush();
		em.clear();
		var audit = service.audit(auth(actor), clinic.getId(), 0, 20);
		assertThat(audit.totalElements()).isEqualTo(1);
		var entry = audit.items().getFirst();
		assertThat(entry.id()).isNotNull();
		assertThat(entry.actorId()).isEqualTo(actor.getId());
		assertThat(entry.actorEmail()).isEqualTo(actor.getEmail());
		assertThat(entry.action()).isEqualTo("PLAN_CHANGED");
		assertThat(entry.previousPlan()).isEqualTo("TRIAL");
		assertThat(entry.newPlan()).isEqualTo("PRO");
		assertThat(entry.previousEstado()).isEqualTo("SUSPENDIDA");
		assertThat(entry.newEstado()).isEqualTo("SUSPENDIDA");
		assertThat(entry.reason()).isEqualTo("Pago confirmado");
		assertThat(entry.createdAt()).isEqualTo(NOW);
	}

	@Test
	void reactivationDoesNotRestartExpiredTrialOrChangePlan() {
		Usuario actor = operator();
		Clinica clinic = clinic(PlanClinica.TRIAL, EstadoClinica.SUSPENDIDA);
		var result = service.changeEstado(auth(actor), clinic.getId(), new SaasClinicEstadoRequest("ACTIVA", "  Reactivar cuenta  "));
		assertThat(result.estado()).isEqualTo("ACTIVA");
		assertThat(result.plan().plan()).isEqualTo("TRIAL");
		assertThat(result.plan().status()).isEqualTo("EXPIRED");
		assertThat(result.plan().trialStartedAt()).isEqualTo(NOW.minusDays(20));
		assertThat(result.plan().trialEndsAt()).isEqualTo(NOW.minusDays(6));
		var audit = service.audit(auth(actor), clinic.getId(), 0, 20).items().getFirst();
		assertThat(audit.action()).isEqualTo("ESTADO_CHANGED");
		assertThat(audit.previousPlan()).isEqualTo("TRIAL");
		assertThat(audit.newPlan()).isEqualTo("TRIAL");
		assertThat(audit.previousEstado()).isEqualTo("SUSPENDIDA");
		assertThat(audit.newEstado()).isEqualTo("ACTIVA");
		assertThat(audit.reason()).isEqualTo("Reactivar cuenta");
	}

	@Test
	void noOpChangesAreCodedConflictsAndDoNotCreateAudit() {
		Usuario actor = operator();
		Clinica clinic = clinic(PlanClinica.PRO, EstadoClinica.ACTIVA);
		assertCode(() -> service.changePlan(auth(actor), clinic.getId(), new SaasClinicPlanRequest("PRO", "Mismo plan")), "PLAN_UNCHANGED");
		assertCode(() -> service.changeEstado(auth(actor), clinic.getId(), new SaasClinicEstadoRequest("ACTIVA", "Mismo estado")), "ESTADO_UNCHANGED");
		assertThat(audits.findByClinicId(clinic.getId(), Pageable.unpaged())).isEmpty();
	}

	@Test
	void trialToProPermitsGrandfatheredTenStaffButNewSeatsRemainBlocked() {
		Usuario actor = operator();
		Clinica clinic = clinic(PlanClinica.TRIAL, EstadoClinica.ACTIVA);
		for (int i = 0; i < 10; i++) staff(clinic);
		var result = service.changePlan(auth(actor), clinic.getId(), new SaasClinicPlanRequest("PRO", "Activar demo"));
		assertThat(result.plan().staffUsed()).isEqualTo(10);
		assertThat(result.plan().staffLimit()).isEqualTo(5);
		assertCode(() -> plans.assertStaffCapacity(clinic.getId()), "STAFF_LIMIT_REACHED");
		assertThat(audits.findByClinicId(clinic.getId(), Pageable.unpaged())).hasSize(1);
	}

	@Test
	void freeToProPermitsExistingOverLimitRecords() {
		Usuario actor = operator();
		Clinica clinic = clinic(PlanClinica.FREE, EstadoClinica.ACTIVA);
		for (int i = 0; i < 6; i++) staff(clinic);
		var result = service.changePlan(auth(actor), clinic.getId(), new SaasClinicPlanRequest("PRO", "Activar cuenta"));
		assertThat(result.plan().plan()).isEqualTo("PRO");
		assertThat(result.plan().staffUsed()).isEqualTo(6);
		assertCode(() -> plans.assertStaffCapacity(clinic.getId()), "STAFF_LIMIT_REACHED");
	}

	@Test
	void loweringStaffLimitRejectsProAndTrialWithoutAudit() {
		Usuario actor = operator();
		for (PlanClinica source : List.of(PlanClinica.PRO, PlanClinica.TRIAL)) {
			Clinica clinic = clinic(source, EstadoClinica.ACTIVA);
			for (int i = 0; i < 3; i++) staff(clinic);
			assertCode(() -> service.changePlan(auth(actor), clinic.getId(), new SaasClinicPlanRequest("CONSULTORIO", "Reducir plan")), "STAFF_LIMIT_EXCEEDED");
			assertThat(clinic.getPlan()).isEqualTo(source);
			assertThat(audits.findByClinicId(clinic.getId(), Pageable.unpaged())).isEmpty();
		}
	}

	@Test
	void loweringPetLimitRejects501ActivePets() {
		Usuario actor = operator();
		Clinica clinic = clinic(PlanClinica.PRO, EstadoClinica.ACTIVA);
		pets(clinic, 501);
		assertCode(() -> service.changePlan(auth(actor), clinic.getId(), new SaasClinicPlanRequest("CONSULTORIO", "Reducir plan")), "PET_LIMIT_EXCEEDED");
		assertThat(clinic.getPlan()).isEqualTo(PlanClinica.PRO);
		assertThat(audits.findByClinicId(clinic.getId(), Pageable.unpaged())).isEmpty();
	}

	@Test
	void loweringLimitsAllowsExactCapacityAndDoesNotDeleteRecords() {
		Usuario actor = operator();
		Clinica clinic = clinic(PlanClinica.PRO, EstadoClinica.ACTIVA);
		staff(clinic);
		staff(clinic);
		pets(clinic, 500);
		var result = service.changePlan(auth(actor), clinic.getId(), new SaasClinicPlanRequest("CONSULTORIO", "Reducir plan"));
		assertThat(result.plan().staffUsed()).isEqualTo(2);
		assertThat(result.plan().petsUsed()).isEqualTo(500);
		assertThat(result.plan().staffLimit()).isEqualTo(2);
		assertThat(result.plan().petsLimit()).isEqualTo(500);
	}

	@Test
	void listIncludesSuspendedClinicsSearchesNameOrSlugLiterallyAndPaginates() {
		Usuario actor = operator();
		Clinica active = clinic(PlanClinica.PRO, EstadoClinica.ACTIVA);
		active.setNombre("Needle%_! Clinic");
		Clinica suspended = clinic(PlanClinica.FREE, EstadoClinica.SUSPENDIDA);
		suspended.setNombre("Other");
		suspended.setSlug("needle-second");
		var first = service.list(auth(actor), "NEEDLE", 0, 1);
		assertThat(first.totalElements()).isEqualTo(2);
		assertThat(first.totalPages()).isEqualTo(2);
		assertThat(first.page()).isZero();
		assertThat(first.items()).extracting("id").containsExactly(suspended.getId());
		var second = service.list(auth(actor), "needle", 1, 1);
		assertThat(second.page()).isEqualTo(1);
		assertThat(second.items()).extracting("id").containsExactly(active.getId());
		assertThat(service.list(auth(actor), "%_!", 0, 20).items()).extracting("id").containsExactly(active.getId());
		assertThat(service.list(auth(actor), "   ", 0, 100).totalElements()).isEqualTo(2);
		assertThat(service.list(auth(actor), "not-present", 0, 20).totalPages()).isZero();
	}

	@Test
	void auditPaginationIsClinicScopedNewestFirstAndSnapshotsSurviveActorChanges() {
		Usuario actor = operator();
		Authentication authentication = auth(actor);
		Clinica clinic = clinic(PlanClinica.TRIAL, EstadoClinica.ACTIVA);
		Clinica other = clinic(PlanClinica.TRIAL, EstadoClinica.ACTIVA);
		service.changePlan(authentication, clinic.getId(), new SaasClinicPlanRequest("PRO", "Activar plan"));
		service.changeEstado(authentication, clinic.getId(), new SaasClinicEstadoRequest("SUSPENDIDA", "Suspender cuenta"));
		service.changePlan(authentication, other.getId(), new SaasClinicPlanRequest("PRO", "Otra clinica"));
		String originalEmail = actor.getEmail();
		actor.setEmail("changed-" + originalEmail);
		em.flush();
		var page = service.audit(auth(actor), clinic.getId(), 0, 1);
		assertThat(page.totalElements()).isEqualTo(2);
		assertThat(page.totalPages()).isEqualTo(2);
		assertThat(page.items().getFirst().action()).isEqualTo("ESTADO_CHANGED");
		assertThat(page.items().getFirst().actorEmail()).isEqualTo(originalEmail);
		assertThat(service.audit(auth(actor), clinic.getId(), 1, 1).items().getFirst().action()).isEqualTo("PLAN_CHANGED");
	}

	@Test
	void actorIsValidatedAgainstDatabaseNotOnlyAuthenticationAuthority() {
		Usuario actor = operator();
		Authentication authentication = auth(actor);
		actor.setForcePasswordChange(true);
		assertForbidden(() -> service.list(authentication, null, 0, 20));
		actor.setForcePasswordChange(null);
		assertForbidden(() -> service.list(authentication, null, 0, 20));
		actor.setForcePasswordChange(false);
		actor.setActive(false);
		assertForbidden(() -> service.list(authentication, null, 0, 20));
		actor.setActive(true);
		actor.setClinica(clinic(PlanClinica.PRO, EstadoClinica.ACTIVA));
		assertForbidden(() -> service.list(authentication, null, 0, 20));
		actor.setClinica(null);
		actor.setRoles(Set.of(role(RoleName.ROLE_SAAS_OPERATOR), role(RoleName.ROLE_ADMIN)));
		assertForbidden(() -> service.list(authentication, null, 0, 20));
		actor.setRoles(Set.of(role(RoleName.ROLE_ADMIN)));
		assertForbidden(() -> service.list(authentication, null, 0, 20));
		assertForbidden(() -> service.list(null, null, 0, 20));
		assertForbidden(() -> service.list(UsernamePasswordAuthenticationToken.authenticated(actor.getEmail(), "unused",
				List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))), null, 0, 20));
	}

	@Test
	void invalidBoundsTargetsReasonsAndMissingClinicAreRejectedWithoutAudit() {
		Usuario actor = operator();
		Clinica clinic = clinic(PlanClinica.TRIAL, EstadoClinica.ACTIVA);
		for (int[] bounds : List.of(new int[]{-1, 20}, new int[]{0, 0}, new int[]{0, 101})) {
			assertThatIllegalArgumentException().isThrownBy(() -> service.list(auth(actor), null, bounds[0], bounds[1]));
			assertThatIllegalArgumentException().isThrownBy(() -> service.audit(auth(actor), clinic.getId(), bounds[0], bounds[1]));
		}
		assertThatIllegalArgumentException().isThrownBy(() -> service.list(auth(actor), "x".repeat(101), 0, 20));
		for (String reason : List.of("    ", " abcd ", "x".repeat(501))) {
			assertThatIllegalArgumentException().isThrownBy(() -> service.changePlan(auth(actor), clinic.getId(), new SaasClinicPlanRequest("PRO", reason)));
			assertThatIllegalArgumentException().isThrownBy(() -> service.changeEstado(auth(actor), clinic.getId(), new SaasClinicEstadoRequest("SUSPENDIDA", reason)));
		}
		assertThatIllegalArgumentException().isThrownBy(() -> service.changePlan(auth(actor), clinic.getId(), new SaasClinicPlanRequest("TRIAL", "Valid reason")));
		assertThatIllegalArgumentException().isThrownBy(() -> service.changeEstado(auth(actor), clinic.getId(), new SaasClinicEstadoRequest("OTHER", "Valid reason")));
		assertThatThrownBy(() -> service.changePlan(auth(actor), Long.MAX_VALUE, new SaasClinicPlanRequest("PRO", "Valid reason"))).isInstanceOf(EntityNotFoundException.class);
		assertThatThrownBy(() -> service.audit(auth(actor), Long.MAX_VALUE, 0, 20)).isInstanceOf(EntityNotFoundException.class);
		assertThat(audits.findByClinicId(clinic.getId(), Pageable.unpaged())).isEmpty();
	}

	@Test
	void requestValidationUsesTrimmedReasonAndRejectsNullAndUnsupportedTargets() {
		try (var factory = Validation.buildDefaultValidatorFactory()) {
			var validator = factory.getValidator();
			assertThat(validator.validate(new SaasClinicPlanRequest("PRO", " abcde "))).isEmpty();
			assertThat(validator.validate(new SaasClinicEstadoRequest("SUSPENDIDA", "x".repeat(500)))).isEmpty();
			assertThat(validator.validate(new SaasClinicPlanRequest("FREE", "Valid reason"))).isNotEmpty();
			assertThat(validator.validate(new SaasClinicPlanRequest(null, "Valid reason"))).isNotEmpty();
			assertThat(validator.validate(new SaasClinicPlanRequest("PRO", null))).isNotEmpty();
			assertThat(validator.validate(new SaasClinicEstadoRequest("ACTIVA", " abcd "))).isNotEmpty();
		}
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void clinicMutationAndAuditRollbackTogether() {
		TransactionTemplate tx = new TransactionTemplate(transactionManager);
		Usuario actor = tx.execute(ignored -> operator());
		Clinica clinic = tx.execute(ignored -> clinic(PlanClinica.TRIAL, EstadoClinica.ACTIVA));
		try {
			assertThatThrownBy(() -> tx.executeWithoutResult(ignored -> {
				service.changePlan(auth(actor), clinic.getId(), new SaasClinicPlanRequest("PRO", "Activar plan"));
				em.flush();
				assertThat(audits.findByClinicId(clinic.getId(), Pageable.unpaged())).hasSize(1);
				throw new IllegalStateException("Force rollback after both writes");
			})).isInstanceOf(IllegalStateException.class);
			tx.executeWithoutResult(ignored -> {
				assertThat(clinics.findById(clinic.getId()).orElseThrow().getPlan()).isEqualTo(PlanClinica.TRIAL);
				assertThat(audits.findByClinicId(clinic.getId(), Pageable.unpaged())).isEmpty();
			});
		} finally {
			cleanup(tx, actor, clinic);
		}
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void concurrentSamePlanChangesSerializeToOneAuditAndOneConflict() throws Exception {
		TransactionTemplate tx = new TransactionTemplate(transactionManager);
		Usuario actor = tx.execute(ignored -> operator());
		Clinica clinic = tx.execute(ignored -> clinic(PlanClinica.TRIAL, EstadoClinica.ACTIVA));
		CountDownLatch locked = new CountDownLatch(1), release = new CountDownLatch(1), secondStarted = new CountDownLatch(1);
		try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
			Future<?> first = executor.submit(() -> tx.executeWithoutResult(ignored -> {
				service.changePlan(auth(actor), clinic.getId(), new SaasClinicPlanRequest("PRO", "Primer cambio"));
				locked.countDown();
				await(release);
			}));
			try {
				assertThat(locked.await(10, TimeUnit.SECONDS)).isTrue();
				Future<?> second = executor.submit(() -> {
					secondStarted.countDown();
					assertCode(() -> service.changePlan(auth(actor), clinic.getId(), new SaasClinicPlanRequest("PRO", "Segundo cambio")), "PLAN_UNCHANGED");
				});
				assertThat(secondStarted.await(10, TimeUnit.SECONDS)).isTrue();
				assertThatThrownBy(() -> second.get(150, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
				release.countDown();
				first.get(10, TimeUnit.SECONDS);
				second.get(10, TimeUnit.SECONDS);
				assertThat(service.audit(auth(actor), clinic.getId(), 0, 20).totalElements()).isEqualTo(1);
			} finally {
				release.countDown();
			}
		} finally {
			cleanup(tx, actor, clinic);
		}
	}

	private void cleanup(TransactionTemplate tx, Usuario actor, Clinica clinic) {
		tx.executeWithoutResult(ignored -> {
			em.createQuery("delete from SaasClinicAudit where clinicId = :id").setParameter("id", clinic.getId()).executeUpdate();
			em.remove(em.find(Usuario.class, actor.getId()));
			em.remove(em.find(Clinica.class, clinic.getId()));
		});
	}

	private void await(CountDownLatch latch) {
		try {
			if (!latch.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Timed out waiting for concurrent transaction");
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e);
		}
	}

	private Usuario operator() {
		Usuario user = Usuario.builder().fullName("Operator").email(UUID.randomUUID() + "@test.com").password("unused")
				.active(true).forcePasswordChange(false).roles(Set.of(role(RoleName.ROLE_SAAS_OPERATOR))).createdAt(NOW).build();
		em.persist(user);
		return user;
	}

	private Authentication auth(Usuario actor) {
		return UsernamePasswordAuthenticationToken.authenticated(actor.getEmail(), "unused",
				List.of(new SimpleGrantedAuthority("ROLE_SAAS_OPERATOR")));
	}

	private Clinica clinic(PlanClinica plan, EstadoClinica estado) {
		Clinica clinic = Clinica.builder().nombre("Clinic").slug(UUID.randomUUID().toString()).plan(plan).estado(estado)
				.createdAt(NOW).updatedAt(NOW.minusDays(1)).trialStartedAt(NOW.minusDays(20)).trialEndsAt(NOW.minusDays(6)).build();
		em.persist(clinic);
		return clinic;
	}

	private Rol role(RoleName name) {
		var existing = em.createQuery("select r from Rol r where r.name = :name", Rol.class).setParameter("name", name).getResultList();
		if (!existing.isEmpty()) return existing.getFirst();
		Rol role = Rol.builder().name(name).description(name.name()).active(true).build();
		em.persist(role);
		return role;
	}

	private void staff(Clinica clinic) {
		em.persist(Usuario.builder().clinica(clinic).fullName("Staff").email(UUID.randomUUID() + "@test.com").password("unused")
				.active(true).roles(Set.of(role(RoleName.ROLE_ADMIN))).createdAt(NOW).build());
	}

	private void pets(Clinica clinic, int count) {
		Duenio owner = Duenio.builder().clinica(clinic).nombres("Owner").apellidos("Test").tipoDocumento("DNI")
				.numeroDocumento("12345678").telefono("123456789").email("owner@test.com").active(true).createdAt(NOW).updatedAt(NOW).build();
		em.persist(owner);
		for (int i = 0; i < count; i++) {
			em.persist(Mascota.builder().clinica(clinic).duenio(owner).nombre("Pet").especie("Canino").raza("Test")
					.sexo(SexoMascota.MACHO).fechaNacimiento(LocalDate.of(2020, 1, 1)).active(true).createdAt(NOW).updatedAt(NOW).build());
		}
	}

	private void assertCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, String code) {
		assertThatThrownBy(call).isInstanceOfSatisfying(PlanException.class, e -> {
			assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
			assertThat(e.getCode()).isEqualTo(code);
		});
	}

	private void assertForbidden(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
		assertThatThrownBy(call).isInstanceOfSatisfying(PlanException.class, e -> {
			assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
			assertThat(e.getCode()).isEqualTo("SAAS_OPERATOR_REQUIRED");
		});
	}
}
