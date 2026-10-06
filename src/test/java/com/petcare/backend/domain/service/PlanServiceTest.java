package com.petcare.backend.domain.service;

import com.petcare.backend.persistence.entity.*;
import com.petcare.backend.persistence.enums.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.*;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(PlanServiceTest.FixedClock.class)
@Transactional
class PlanServiceTest {
	static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 6, 12, 0);
	@TestConfiguration
	static class FixedClock {
		@Bean @Primary Clock fixedClock() { return Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC); }
	}
	@Autowired PlanService plans;
	@Autowired ClinicaService clinics;
	@Autowired EntityManager em;
	@Autowired PlatformTransactionManager transactionManager;

	@Test
	void newClinicHasFourteenDayUtcProTrial() {
		Clinica c = clinics.createClinic("Test", "test");
		assertThat(c.getTrialStartedAt()).isEqualTo(NOW);
		assertThat(c.getTrialEndsAt()).isEqualTo(NOW.plusDays(14));
		var response = plans.getPlan(c.getId());
		assertThat(response.status()).isEqualTo("TRIAL");
		assertThat(response.tier()).isEqualTo("PRO");
		assertThat(response.daysRemaining()).isEqualTo(14);
		assertThat(response.staffLimit()).isEqualTo(5);
		assertThat(response.petsLimit()).isEqualTo(2500);
		assertThat(ClinicaService.toResponse(c, Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC)).readOnly()).isFalse();
	}

	@Test
	void expiryBoundaryAndMissingExpiryFailClosedWithoutChangingState() {
		Clinica c = clinics.createClinic("Test", "test");
		c.setTrialEndsAt(NOW.plusNanos(1));
		plans.assertCanWrite(c.getId());
		assertThat(plans.getPlan(c.getId()).daysRemaining()).isEqualTo(1);
		c.setTrialEndsAt(NOW);
		assertCode(() -> plans.assertCanWrite(c.getId()), "TRIAL_EXPIRED");
		assertThat(plans.getPlan(c.getId()).status()).isEqualTo("EXPIRED");
		assertThat(c.getEstado()).isEqualTo(EstadoClinica.ACTIVA);
		c.setTrialEndsAt(null);
		assertCode(() -> plans.assertCanWrite(c.getId()), "TRIAL_EXPIRED");
		c.setPlan(PlanClinica.FREE);
		assertCode(() -> plans.assertCanWrite(c.getId()), "CLINIC_READ_ONLY");
		assertThat(plans.getPlan(c.getId()).status()).isEqualTo("READ_ONLY");
		c.setPlan(PlanClinica.PRO);
		c.setEstado(EstadoClinica.SUSPENDIDA);
		assertCode(() -> plans.assertCanWrite(c.getId()), "CLINIC_READ_ONLY");
		assertThat(plans.getPlan(c.getId()).status()).isEqualTo("SUSPENDED");
	}

	@Test
	void distinctActiveStaffCountExcludesOwnersInactiveAndOtherClinics() {
		Clinica c = clinics.createClinic("Test", "test");
		c.setPlan(PlanClinica.CONSULTORIO);
		Rol admin = role(RoleName.ROLE_ADMIN), vet = role(RoleName.ROLE_VETERINARIO);
		Rol assistant = role(RoleName.ROLE_ASISTENTE), owner = role(RoleName.ROLE_DUENIO);
		user(c, true, Set.of(admin, vet, assistant));
		user(c, true, Set.of(owner));
		user(c, false, Set.of(admin));
		user(clinics.createClinic("Other", "other"), true, Set.of(admin));
		assertThat(plans.getPlan(c.getId()).staffUsed()).isEqualTo(1);
		plans.assertStaffCapacity(c.getId());
		user(c, true, Set.of(assistant));
		assertCode(() -> plans.assertStaffCapacity(c.getId()), "STAFF_LIMIT_REACHED");
		var response = plans.getPlan(c.getId());
		assertThat(response.staffUsed()).isEqualTo(2);
		assertThat(response.monthlyPrice()).isEqualByComparingTo("59");
		assertThat(response.annualPrice()).isEqualByComparingTo("590");
		assertThat(response.currency()).isEqualTo("PEN");
		c.setPlan(PlanClinica.PRO);
		plans.assertStaffCapacity(c.getId());
		for (int i = 0; i < 3; i++) user(c, true, Set.of(admin));
		assertCode(() -> plans.assertStaffCapacity(c.getId()), "STAFF_LIMIT_REACHED");
	}

	@Test
	void petQuotaBoundaryAndOverLimitRecordsAreRetained() {
		Clinica c = clinics.createClinic("Test", "test");
		c.setPlan(PlanClinica.CONSULTORIO);
		Duenio owner = Duenio.builder().clinica(c).nombres("Owner").apellidos("Test").tipoDocumento("DNI")
				.numeroDocumento("12345678").telefono("123456789").email("owner@test.com").active(true)
				.createdAt(NOW).updatedAt(NOW).build();
		em.persist(owner);
		for (int i = 0; i < 499; i++) pet(c, owner, true);
		pet(c, owner, false);
		assertThat(plans.getPlan(c.getId()).petsUsed()).isEqualTo(499);
		plans.assertPetCapacity(c.getId());
		pet(c, owner, true);
		assertCode(() -> plans.assertPetCapacity(c.getId()), "PET_LIMIT_REACHED");
		pet(c, owner, true);
		assertThat(plans.getPlan(c.getId()).petsUsed()).isEqualTo(501);
		plans.assertCanWrite(c.getId());
		c.setPlan(PlanClinica.PRO);
		plans.assertPetCapacity(c.getId());
		assertThat(plans.getPlan(c.getId()).petsLimit()).isEqualTo(2500);
		for (int i = 501; i < 2499; i++) pet(c, owner, true);
		plans.assertPetCapacity(c.getId());
		pet(c, owner, true);
		assertCode(() -> plans.assertPetCapacity(c.getId()), "PET_LIMIT_REACHED");
		assertThat(plans.getPlan(c.getId()).petsUsed()).isEqualTo(2500);
		assertThat(plans.getPlan(c.getId()).monthlyPrice()).isEqualByComparingTo("129");
		assertThat(plans.getPlan(c.getId()).annualPrice()).isEqualByComparingTo("1290");
	}

	@Test
	void capacityRequiresExistingTransaction() {
		TransactionTemplate outside = new TransactionTemplate(transactionManager);
		outside.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
		outside.executeWithoutResult(ignored -> {
			assertThatThrownBy(() -> plans.assertStaffCapacity(1L))
					.isInstanceOf(org.springframework.transaction.IllegalTransactionStateException.class);
			assertThatThrownBy(() -> plans.assertPetCapacity(1L))
					.isInstanceOf(org.springframework.transaction.IllegalTransactionStateException.class);
		});
	}

	private Rol role(RoleName name) {
		var existing = em.createQuery("select r from Rol r where r.name = :name", Rol.class)
				.setParameter("name", name).getResultList();
		if (!existing.isEmpty()) return existing.getFirst();
		Rol r = Rol.builder().name(name).description(name.name()).active(true).build();
		em.persist(r);
		return r;
	}
	private void user(Clinica c, boolean active, Set<Rol> roles) {
		em.persist(Usuario.builder().clinica(c).fullName("Test").email(java.util.UUID.randomUUID() + "@test.com")
				.password("test").active(active).roles(roles).createdAt(NOW).build());
	}
	private void pet(Clinica c, Duenio owner, boolean active) {
		em.persist(Mascota.builder().clinica(c).duenio(owner).nombre("Pet").especie("Canino").raza("Test")
				.sexo(SexoMascota.MACHO).fechaNacimiento(LocalDate.of(2020, 1, 1)).active(active)
				.createdAt(NOW).updatedAt(NOW).build());
	}
	private void assertCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, String code) {
		assertThatThrownBy(call).isInstanceOfSatisfying(PlanException.class, e -> assertThat(e.getCode()).isEqualTo(code));
	}
}
