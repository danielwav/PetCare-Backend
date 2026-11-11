package com.petcare.backend.domain.service;

import com.petcare.backend.domain.dto.request.CreateInternalUserRequest;
import com.petcare.backend.domain.repository.UsuarioRepository;
import com.petcare.backend.persistence.entity.*;
import com.petcare.backend.persistence.enums.RoleName;
import jakarta.persistence.EntityManager;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

final class ServiceTestFixtures {

	private ServiceTestFixtures() {
	}

	static void paidCoverage(EntityManager em, Clinica clinic, Instant start) {
		var localStart = start.atZone(ZoneId.of("America/Lima"));
		Instant end = localStart.plusMonths(1).toInstant();
		LocalDateTime confirmedAt = LocalDateTime.ofInstant(start, ZoneOffset.UTC);
		var roles = em.createQuery("select r from Rol r where r.name = :name", Rol.class)
				.setParameter("name", RoleName.ROLE_SAAS_OPERATOR).getResultList();
		Rol role = roles.isEmpty() ? Rol.builder().name(RoleName.ROLE_SAAS_OPERATOR)
				.description("Fixture operator").active(true).build() : roles.getFirst();
		if (roles.isEmpty()) em.persist(role);
		Usuario actor = Usuario.builder().fullName("Fixture operator").email(UUID.randomUUID() + "@test.com")
				.password("unused").active(true).forcePasswordChange(false).roles(Set.of(role)).createdAt(confirmedAt).build();
		em.persist(actor);
		SaasClinicAudit audit = new SaasClinicAudit(clinic.getId(), actor.getId(), actor.getEmail(),
				"MONTHLY_ACTIVATED", clinic.getPlan(), clinic.getPlan(), clinic.getEstado(), clinic.getEstado(),
				"Paid coverage fixture", confirmedAt);
		em.persist(audit);
		int anchor = localStart.getDayOfMonth();
		MonthlyPayment payment = new MonthlyPayment(clinic.getId(), clinic.getPlan(), "ACTIVATE", UUID.randomUUID().toString(),
				PlanService.monthlyPrice(clinic.getPlan()), start, end, start, anchor, actor.getId(), actor.getEmail(),
				"Paid coverage fixture", clinic.getLastPaymentId(), audit.getId());
		em.persist(payment);
		clinic.setSubscriptionStartedAt(start);
		clinic.setSubscriptionEndsAt(end);
		clinic.setSubscriptionAnchorDay(anchor);
		clinic.setLastPaymentId(payment.getId());
	}

	static Long veterinarioUser(AuthService authService, UsuarioRepository usuarioRepository,
			Long clinicaId, String email) {
		return usuarioRepository.findByEmail(email)
				.map(usuario -> usuario.getId())
				.orElseGet(() -> {
					authService.createInternalUser(new CreateInternalUserRequest(
							"Ana", "Salas", email, "VETERINARIO"), clinicaId);
					return authService.me(email).id();
				});
	}
}
