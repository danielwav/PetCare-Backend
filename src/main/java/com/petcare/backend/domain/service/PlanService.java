package com.petcare.backend.domain.service;

import com.petcare.backend.domain.dto.response.PlanResponse;
import com.petcare.backend.domain.repository.ClinicaRepository;
import com.petcare.backend.domain.repository.UsuarioRepository;
import com.petcare.backend.persistence.entity.Clinica;
import com.petcare.backend.persistence.enums.EstadoClinica;
import com.petcare.backend.persistence.enums.PlanClinica;
import com.petcare.backend.persistence.enums.RoleName;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PlanService {
	private final ClinicaRepository clinicaRepository;
	private final UsuarioRepository usuarioRepository;
	private final EntityManager entityManager;
	private final Clock clock;

	@Transactional(readOnly = true)
	public void assertCanWrite(Long clinicaId) {
		assertWritable(findClinic(clinicaId));
	}

	// Serialize even seat-neutral changes before reading active state or roles.
	@Transactional(propagation = Propagation.MANDATORY)
	public void lockForWrite(Long clinicaId) {
		assertWritable(lockClinic(clinicaId));
	}

	// Call before changing active state or roles; the caller retains the row lock through its write.
	@Transactional(propagation = Propagation.MANDATORY)
	public void assertStaffCapacity(Long clinicaId) {
		Clinica clinic = lockClinic(clinicaId);
		assertWritable(clinic);
		if (staffUsed(clinicaId) >= staffLimit(clinic.getPlan())) {
			throw new PlanException(HttpStatus.CONFLICT, "STAFF_LIMIT_REACHED", "Limite de personal activo alcanzado.");
		}
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public void assertPetCapacity(Long clinicaId) {
		Clinica clinic = lockClinic(clinicaId);
		assertWritable(clinic);
		if (petsUsed(clinicaId) >= petLimit(clinic.getPlan())) {
			throw new PlanException(HttpStatus.CONFLICT, "PET_LIMIT_REACHED", "Limite de mascotas activas alcanzado.");
		}
	}

	@Transactional(readOnly = true)
	public PlanResponse getMyPlan(String email) {
		var user = usuarioRepository.findByEmail(email.toLowerCase(Locale.ROOT))
				.orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado."));
		if (user.getClinica() == null) throw new EntityNotFoundException("El usuario no tiene una clinica asignada.");
		return getPlan(user.getClinica().getId());
	}

	@Transactional(readOnly = true)
	public PlanResponse getPlan(Long clinicaId) {
		return getPlan(findClinic(clinicaId));
	}

	@Transactional(readOnly = true)
	public PlanResponse getPlan(Clinica clinic) {
		Instant now = clock.instant();
		String status = status(clinic, now);
		boolean futureCoverage = hasValidPaidCoverage(clinic) && now.isBefore(clinic.getSubscriptionStartedAt());
		boolean pro = clinic.getPlan() == PlanClinica.PRO || clinic.getPlan() == PlanClinica.TRIAL;
		long days = clinic.getPlan() == PlanClinica.TRIAL && clinic.getTrialEndsAt() != null
				? daysRemaining(now, clinic.getTrialEndsAt().toInstant(ZoneOffset.UTC)) : 0;
		return new PlanResponse(clinic.getPlan().name(), pro ? "PRO" : clinic.getPlan().name(), status,
				!status.equals("TRIAL") && !status.equals("ACTIVE"), clinic.getTrialStartedAt(), clinic.getTrialEndsAt(), days,
				staffUsed(clinic.getId()), staffLimit(clinic.getPlan()), petsUsed(clinic.getId()), petLimit(clinic.getPlan()),
				monthlyPrice(clinic.getPlan()), "PEN", "MONTHLY", clinic.getSubscriptionStartedAt(),
				clinic.getSubscriptionEndsAt(), hasValidPaidCoverage(clinic) ? daysRemaining(now, clinic.getSubscriptionEndsAt()) : 0,
				clinic.getLastPaymentId(), !hasActivePaidCoverage(clinic, now) && !futureCoverage,
				hasValidPaidCoverage(clinic) && !futureCoverage);
	}

	// The caller holds the clinic row lock, also used by capacity-increasing writes.
	@Transactional(propagation = Propagation.MANDATORY)
	public void assertPlanChangeCapacity(Clinica clinic, PlanClinica target) {
		if (staffLimit(target) < staffLimit(clinic.getPlan()) && staffUsed(clinic.getId()) > staffLimit(target)) {
			throw new PlanException(HttpStatus.CONFLICT, "STAFF_LIMIT_EXCEEDED", "El personal activo excede el limite del plan destino.");
		}
		if (petLimit(target) < petLimit(clinic.getPlan()) && petsUsed(clinic.getId()) > petLimit(target)) {
			throw new PlanException(HttpStatus.CONFLICT, "PET_LIMIT_EXCEEDED", "Las mascotas activas exceden el limite del plan destino.");
		}
	}

	public static boolean isReadOnly(Clinica clinic, Clock clock) {
		String status = status(clinic, clock);
		return !status.equals("TRIAL") && !status.equals("ACTIVE");
	}

	public static String status(Clinica clinic, Clock clock) {
		return status(clinic, clock.instant());
	}

	private static String status(Clinica clinic, Instant now) {
		if (clinic.getEstado() != EstadoClinica.ACTIVA) return "SUSPENDED";
		if (clinic.getPlan() == PlanClinica.TRIAL) {
			return clinic.getTrialEndsAt() != null && now.isBefore(clinic.getTrialEndsAt().toInstant(ZoneOffset.UTC)) ? "TRIAL" : "EXPIRED";
		}
		if (clinic.getPlan() == PlanClinica.PRO || clinic.getPlan() == PlanClinica.CONSULTORIO) {
			if (!hasValidPaidCoverage(clinic)) return "PAYMENT_PENDING";
			if (now.isBefore(clinic.getSubscriptionStartedAt())) return "PAYMENT_PENDING";
			return hasActivePaidCoverage(clinic, now) ? "ACTIVE" : "SUBSCRIPTION_EXPIRED";
		}
		return "READ_ONLY";
	}

	public static boolean hasValidPaidCoverage(Clinica clinic) {
		return (clinic.getPlan() == PlanClinica.PRO || clinic.getPlan() == PlanClinica.CONSULTORIO)
				&& clinic.getSubscriptionStartedAt() != null && clinic.getSubscriptionEndsAt() != null
				&& clinic.getSubscriptionStartedAt().isBefore(clinic.getSubscriptionEndsAt())
				&& clinic.getSubscriptionAnchorDay() != null && clinic.getSubscriptionAnchorDay() >= 1
				&& clinic.getSubscriptionAnchorDay() <= 31
				&& clinic.getLastPaymentId() != null && clinic.getLastPaymentId() > 0;
	}

	public static boolean hasActivePaidCoverage(Clinica clinic, Instant now) {
		return hasValidPaidCoverage(clinic) && !now.isBefore(clinic.getSubscriptionStartedAt())
				&& now.isBefore(clinic.getSubscriptionEndsAt());
	}

	public static BigDecimal monthlyPrice(PlanClinica plan) {
		return new BigDecimal(plan == PlanClinica.PRO || plan == PlanClinica.TRIAL ? "129.00"
				: plan == PlanClinica.CONSULTORIO ? "59.00" : "0.00");
	}

	private void assertWritable(Clinica clinic) {
		String status = status(clinic, clock);
		if (!status.equals("TRIAL") && !status.equals("ACTIVE")) {
			String code = switch (status) {
				case "EXPIRED" -> "TRIAL_EXPIRED";
				case "PAYMENT_PENDING" -> "PAYMENT_REQUIRED";
				case "SUBSCRIPTION_EXPIRED" -> "SUBSCRIPTION_EXPIRED";
				default -> "CLINIC_READ_ONLY";
			};
			String message = switch (status) {
				case "EXPIRED" -> "El periodo de prueba ha finalizado.";
				case "PAYMENT_PENDING" -> "Se requiere registrar una mensualidad confirmada.";
				case "SUBSCRIPTION_EXPIRED" -> "La suscripcion mensual ha finalizado.";
				default -> "La clinica esta en modo solo lectura.";
			};
			throw new PlanException(HttpStatus.FORBIDDEN, code, message);
		}
	}

	private Clinica findClinic(Long id) {
		return clinicaRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Clinica no encontrada."));
	}

	// Lock before editing clinic fields. Repeated checks must retain changes made under this lock.
	@Transactional(propagation = Propagation.MANDATORY)
	public Clinica lockClinic(Long id) {
		Clinica clinic = findClinic(id);
		if (entityManager.getLockMode(clinic) != LockModeType.PESSIMISTIC_WRITE) {
			clinicaRepository.findByIdForUpdate(id).orElseThrow(() -> new EntityNotFoundException("Clinica no encontrada."));
			// A locking query does not replace a stale first-level-cache snapshot.
			entityManager.refresh(clinic, LockModeType.PESSIMISTIC_WRITE);
		}
		return clinic;
	}

	private long staffUsed(Long id) {
		return entityManager.createQuery("""
				select count(distinct u.id) from Usuario u join u.roles r
				where u.clinica.id = :id and u.active = true and r.name in :roles
				""", Long.class).setParameter("id", id)
				.setParameter("roles", List.of(RoleName.ROLE_ADMIN, RoleName.ROLE_VETERINARIO, RoleName.ROLE_ASISTENTE))
				.getSingleResult();
	}

	private long petsUsed(Long id) {
		return entityManager.createQuery("select count(m.id) from Mascota m where m.clinica.id = :id and m.active = true", Long.class)
				.setParameter("id", id).getSingleResult();
	}

	private static int staffLimit(PlanClinica plan) {
		return plan == PlanClinica.CONSULTORIO ? 2 : plan == PlanClinica.FREE ? 0 : 5;
	}

	private static int petLimit(PlanClinica plan) {
		return plan == PlanClinica.CONSULTORIO ? 500 : plan == PlanClinica.FREE ? 0 : 2500;
	}

	private static long daysRemaining(Instant now, Instant end) {
		Duration remaining = Duration.between(now, end);
		return remaining.isPositive()
				? remaining.toDays() + (remaining.minusDays(remaining.toDays()).isZero() ? 0 : 1) : 0;
	}
}
