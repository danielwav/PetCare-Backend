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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
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
		if (staffUsed(clinicaId) >= staffLimit(clinic)) {
			throw new PlanException(HttpStatus.CONFLICT, "STAFF_LIMIT_REACHED", "Limite de personal activo alcanzado.");
		}
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public void assertPetCapacity(Long clinicaId) {
		Clinica clinic = lockClinic(clinicaId);
		assertWritable(clinic);
		if (petsUsed(clinicaId) >= petLimit(clinic)) {
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
		Clinica clinic = findClinic(clinicaId);
		String status = status(clinic, clock);
		boolean pro = clinic.getPlan() == PlanClinica.PRO || clinic.getPlan() == PlanClinica.TRIAL;
		long days = 0;
		if (clinic.getPlan() == PlanClinica.TRIAL && clinic.getTrialEndsAt() != null) {
			Duration remaining = Duration.between(now(clock), clinic.getTrialEndsAt());
			if (remaining.isPositive()) {
				days = remaining.toDays() + (remaining.minusDays(remaining.toDays()).isZero() ? 0 : 1);
			}
		}
		return new PlanResponse(clinic.getPlan().name(), pro ? "PRO" : clinic.getPlan().name(), status,
				isReadOnly(clinic, clock), clinic.getTrialStartedAt(), clinic.getTrialEndsAt(), days,
				staffUsed(clinicaId), staffLimit(clinic), petsUsed(clinicaId), petLimit(clinic),
				BigDecimal.valueOf(pro ? 129 : clinic.getPlan() == PlanClinica.CONSULTORIO ? 59 : 0),
				BigDecimal.valueOf(pro ? 1290 : clinic.getPlan() == PlanClinica.CONSULTORIO ? 590 : 0), "PEN");
	}

	public static boolean isReadOnly(Clinica clinic, Clock clock) {
		String status = status(clinic, clock);
		return !status.equals("TRIAL") && !status.equals("ACTIVE");
	}

	private static String status(Clinica clinic, Clock clock) {
		if (clinic.getEstado() != EstadoClinica.ACTIVA) return "SUSPENDED";
		if (clinic.getPlan() == PlanClinica.TRIAL) {
			return clinic.getTrialEndsAt() != null && now(clock).isBefore(clinic.getTrialEndsAt()) ? "TRIAL" : "EXPIRED";
		}
		return clinic.getPlan() == PlanClinica.PRO || clinic.getPlan() == PlanClinica.CONSULTORIO ? "ACTIVE" : "READ_ONLY";
	}

	private void assertWritable(Clinica clinic) {
		String status = status(clinic, clock);
		if (!status.equals("TRIAL") && !status.equals("ACTIVE")) {
			throw new PlanException(HttpStatus.FORBIDDEN, status.equals("EXPIRED") ? "TRIAL_EXPIRED" : "CLINIC_READ_ONLY",
					status.equals("EXPIRED") ? "El periodo de prueba ha finalizado." : "La clinica esta en modo solo lectura.");
		}
	}

	private Clinica findClinic(Long id) {
		return clinicaRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Clinica no encontrada."));
	}

	private Clinica lockClinic(Long id) {
		return clinicaRepository.findByIdForUpdate(id).orElseThrow(() -> new EntityNotFoundException("Clinica no encontrada."));
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

	private static int staffLimit(Clinica clinic) {
		return clinic.getPlan() == PlanClinica.CONSULTORIO ? 2 : clinic.getPlan() == PlanClinica.FREE ? 0 : 5;
	}

	private static int petLimit(Clinica clinic) {
		return clinic.getPlan() == PlanClinica.CONSULTORIO ? 500 : clinic.getPlan() == PlanClinica.FREE ? 0 : 2500;
	}

	private static LocalDateTime now(Clock clock) {
		return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
	}
}
