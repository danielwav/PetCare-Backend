package com.petcare.backend.domain.service;

import com.petcare.backend.domain.dto.request.SaasClinicEstadoRequest;
import com.petcare.backend.domain.dto.request.SaasClinicPlanRequest;
import com.petcare.backend.domain.dto.response.SaasClinicAuditResponse;
import com.petcare.backend.domain.dto.response.SaasClinicResponse;
import com.petcare.backend.domain.dto.response.SaasPageResponse;
import com.petcare.backend.domain.repository.ClinicaRepository;
import com.petcare.backend.domain.repository.SaasClinicAuditRepository;
import com.petcare.backend.domain.repository.UsuarioRepository;
import com.petcare.backend.persistence.entity.Clinica;
import com.petcare.backend.persistence.entity.SaasClinicAudit;
import com.petcare.backend.persistence.entity.Usuario;
import com.petcare.backend.persistence.enums.EstadoClinica;
import com.petcare.backend.persistence.enums.PlanClinica;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SaasClinicService {
	private static final String OPERATOR = "ROLE_SAAS_OPERATOR";
	private final ClinicaRepository clinics;
	private final UsuarioRepository users;
	private final SaasClinicAuditRepository audits;
	private final PlanService plans;
	private final Clock clock;

	@Transactional(readOnly = true)
	public SaasPageResponse<SaasClinicResponse> list(Authentication authentication, String q, int page, int size) {
		requireOperator(authentication);
		validatePage(page, size);
		if (q != null && q.length() > 100) throw new IllegalArgumentException("q debe tener como maximo 100 caracteres.");
		String pattern = q == null || q.isBlank() ? null : "%" + q.trim().toLowerCase(Locale.ROOT)
				.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
		return SaasPageResponse.from(clinics.findForSaas(pattern,
				PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"))).map(this::response));
	}

	@Transactional
	public SaasClinicResponse changePlan(Authentication authentication, Long id, SaasClinicPlanRequest request) {
		Usuario actor = requireOperator(authentication);
		String reason = validateReason(request.reason());
		if (!"CONSULTORIO".equals(request.plan()) && !"PRO".equals(request.plan())) {
			throw new IllegalArgumentException("El plan debe ser CONSULTORIO o PRO.");
		}
		PlanClinica target = PlanClinica.valueOf(request.plan());
		Clinica clinic = lockClinic(id);
		PlanClinica previous = clinic.getPlan();
		if (previous == target) throw new PlanException(HttpStatus.CONFLICT, "PLAN_UNCHANGED", "La clinica ya tiene ese plan.");
		plans.assertPlanChangeCapacity(clinic, target);
		clinic.setPlan(target);
		LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
		clinic.setUpdatedAt(now);
		audits.save(new SaasClinicAudit(clinic.getId(), actor.getId(), actor.getEmail(), "PLAN_CHANGED",
				previous, target, clinic.getEstado(), clinic.getEstado(), reason, now));
		return response(clinic);
	}

	@Transactional
	public SaasClinicResponse changeEstado(Authentication authentication, Long id, SaasClinicEstadoRequest request) {
		Usuario actor = requireOperator(authentication);
		String reason = validateReason(request.reason());
		if (!"ACTIVA".equals(request.estado()) && !"SUSPENDIDA".equals(request.estado())) {
			throw new IllegalArgumentException("El estado debe ser ACTIVA o SUSPENDIDA.");
		}
		EstadoClinica target = EstadoClinica.valueOf(request.estado());
		Clinica clinic = lockClinic(id);
		EstadoClinica previous = clinic.getEstado();
		if (previous == target) throw new PlanException(HttpStatus.CONFLICT, "ESTADO_UNCHANGED", "La clinica ya tiene ese estado.");
		clinic.setEstado(target);
		LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
		clinic.setUpdatedAt(now);
		audits.save(new SaasClinicAudit(clinic.getId(), actor.getId(), actor.getEmail(), "ESTADO_CHANGED",
				clinic.getPlan(), clinic.getPlan(), previous, target, reason, now));
		return response(clinic);
	}

	@Transactional(readOnly = true)
	public SaasPageResponse<SaasClinicAuditResponse> audit(Authentication authentication, Long id, int page, int size) {
		requireOperator(authentication);
		validatePage(page, size);
		if (!clinics.existsById(id)) throw new EntityNotFoundException("Clinica no encontrada.");
		return SaasPageResponse.from(audits.findByClinicId(id,
				PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id")))
				.map(a -> new SaasClinicAuditResponse(a.getId(), a.getActorId(), a.getActorEmail(), a.getAction(),
						a.getPreviousPlan().name(), a.getNewPlan().name(), a.getPreviousEstado().name(),
						a.getNewEstado().name(), a.getReason(), a.getCreatedAt())));
	}

	private Usuario requireOperator(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()
				|| authentication.getAuthorities().size() != 1
				|| authentication.getAuthorities().stream().noneMatch(a -> OPERATOR.equals(a.getAuthority()))) {
			throw operatorDenied();
		}
		Usuario actor = users.findByEmail(authentication.getName().toLowerCase(Locale.ROOT)).orElseThrow(this::operatorDenied);
		if (!Boolean.TRUE.equals(actor.getActive()) || !Boolean.FALSE.equals(actor.getForcePasswordChange())
				|| actor.getClinica() != null || actor.getRoles().size() != 1
				|| actor.getRoles().stream().noneMatch(r -> OPERATOR.equals(r.getName().name()) && Boolean.TRUE.equals(r.getActive()))) {
			throw operatorDenied();
		}
		return actor;
	}

	private PlanException operatorDenied() {
		return new PlanException(HttpStatus.FORBIDDEN, "SAAS_OPERATOR_REQUIRED", "Se requiere un operador SaaS activo sin clinica y con rol exclusivo.");
	}

	private Clinica lockClinic(Long id) {
		return plans.lockClinic(id);
	}

	private SaasClinicResponse response(Clinica clinic) {
		return new SaasClinicResponse(clinic.getId(), clinic.getNombre(), clinic.getSlug(), clinic.getEstado().name(),
				clinic.getCreatedAt(), plans.getPlan(clinic));
	}

	private void validatePage(int page, int size) {
		if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("page debe ser >= 0 y size debe estar entre 1 y 100.");
	}

	private String validateReason(String reason) {
		if (reason == null || reason.trim().length() < 5 || reason.trim().length() > 500) {
			throw new IllegalArgumentException("reason debe tener entre 5 y 500 caracteres sin espacios exteriores.");
		}
		return reason.trim();
	}
}
