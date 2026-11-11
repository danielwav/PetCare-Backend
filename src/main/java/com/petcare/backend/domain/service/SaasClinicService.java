package com.petcare.backend.domain.service;

import com.petcare.backend.domain.dto.request.SaasClinicEstadoRequest;
import com.petcare.backend.domain.dto.request.SaasClinicPlanRequest;
import com.petcare.backend.domain.dto.request.MonthlyPaymentRequest;
import com.petcare.backend.domain.dto.response.MonthlyPaymentResponse;
import com.petcare.backend.domain.dto.response.SaasClinicAuditResponse;
import com.petcare.backend.domain.dto.response.SaasClinicResponse;
import com.petcare.backend.domain.dto.response.SaasPageResponse;
import com.petcare.backend.domain.repository.ClinicaRepository;
import com.petcare.backend.domain.repository.MonthlyPaymentRepository;
import com.petcare.backend.domain.repository.SaasClinicAuditRepository;
import com.petcare.backend.domain.repository.UsuarioRepository;
import com.petcare.backend.persistence.entity.Clinica;
import com.petcare.backend.persistence.entity.MonthlyPayment;
import com.petcare.backend.persistence.entity.SaasClinicAudit;
import com.petcare.backend.persistence.entity.Usuario;
import com.petcare.backend.persistence.enums.EstadoClinica;
import com.petcare.backend.persistence.enums.PlanClinica;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SaasClinicService {
	private static final String OPERATOR = "ROLE_SAAS_OPERATOR";
	private static final ZoneId BILLING_ZONE = ZoneId.of("America/Lima");
	private final ClinicaRepository clinics;
	private final UsuarioRepository users;
	private final SaasClinicAuditRepository audits;
	private final PlanService plans;
	private final Clock clock;
	private final MonthlyPaymentRepository payments;
	private final PlatformTransactionManager transactionManager;

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
		requireOperator(authentication);
		throw new PlanException(HttpStatus.GONE, "MONTHLY_PAYMENT_REQUIRED", "Registre una mensualidad confirmada para activar o renovar un plan.");
	}

	public SaasClinicResponse monthlyPayment(Authentication authentication, Long id, MonthlyPaymentRequest request) {
		try {
			return new TransactionTemplate(transactionManager).execute(transaction -> {
				Usuario actor = requireOperator(authentication);
				if (request == null) throw new IllegalArgumentException("Se requiere una mensualidad.");
				String reason = validateReason(request.reason());
				String reference = normalizeReference(request.paymentReference());
				if (!"CONSULTORIO".equals(request.plan()) && !"PRO".equals(request.plan())) {
					throw new IllegalArgumentException("El plan debe ser CONSULTORIO o PRO.");
				}
				if (!"ACTIVATE".equals(request.operation()) && !"RENEW".equals(request.operation())) {
					throw new IllegalArgumentException("operation debe ser ACTIVATE o RENEW.");
				}
				if (!Boolean.TRUE.equals(request.paymentConfirmed())) {
					throw new IllegalArgumentException("paymentConfirmed debe ser true.");
				}
				if (request.expectedPaymentId() != null && request.expectedPaymentId() <= 0) {
					throw new IllegalArgumentException("expectedPaymentId debe ser positivo o null.");
				}
				PlanClinica target = PlanClinica.valueOf(request.plan());
				Clinica clinic = lockClinic(id);
				Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
				var existing = payments.findByPaymentReference(reference);
				// A historical replay must not extend coverage, even after subsequent payments.
				if (existing.isPresent()) {
					MonthlyPayment payment = existing.get();
					if (!payment.getClinicId().equals(clinic.getId()) || payment.getPlan() != target
							|| !payment.getOperation().equals(request.operation()) || !payment.getReason().equals(reason)
							|| !Objects.equals(payment.getPreviousPaymentId(), request.expectedPaymentId())) {
						throw referenceUsed();
					}
					return response(clinic);
				}
				if (!Objects.equals(clinic.getLastPaymentId(), request.expectedPaymentId())) {
					throw new PlanException(HttpStatus.CONFLICT, "PAYMENT_VERSION_CONFLICT", "La mensualidad actual ha cambiado. Actualice la clinica antes de continuar.");
				}
				if (PlanService.hasValidPaidCoverage(clinic) && now.isBefore(clinic.getSubscriptionStartedAt())) {
					throw new PlanException(HttpStatus.CONFLICT, "SUBSCRIPTION_NOT_STARTED", "La vigencia registrada aun no ha comenzado. No se puede reemplazar ni renovar anticipadamente.");
				}
				boolean active = PlanService.hasActivePaidCoverage(clinic, now);
				boolean renew = "RENEW".equals(request.operation());
				if (active && (!renew || clinic.getPlan() != target)) {
					throw new PlanException(HttpStatus.CONFLICT, "SUBSCRIPTION_ACTIVE", "La suscripcion esta activa; solo puede renovar el mismo plan.");
				}
				if (renew && !PlanService.hasValidPaidCoverage(clinic)) {
					throw new PlanException(HttpStatus.CONFLICT, "PAYMENT_REQUIRED", "Se requiere una mensualidad previa valida para renovar.");
				}
				if (renew && clinic.getPlan() != target) {
					throw new PlanException(HttpStatus.CONFLICT, "PLAN_MISMATCH", "La renovacion debe conservar el mismo plan.");
				}
				plans.assertPlanChangeCapacity(clinic, target);
				Instant start = renew && active ? clinic.getSubscriptionEndsAt() : now;
				int anchor = renew && active ? clinic.getSubscriptionAnchorDay() : now.atZone(BILLING_ZONE).getDayOfMonth();
				ZonedDateTime nextMonth = start.atZone(BILLING_ZONE).plusMonths(1);
				Instant end = nextMonth.withDayOfMonth(Math.min(anchor, nextMonth.toLocalDate().lengthOfMonth())).toInstant();
				LocalDateTime updatedAt = LocalDateTime.ofInstant(now, ZoneOffset.UTC);
				SaasClinicAudit audit = audits.save(new SaasClinicAudit(clinic.getId(), actor.getId(), actor.getEmail(),
						renew ? "MONTHLY_RENEWED" : "MONTHLY_ACTIVATED", clinic.getPlan(), target,
						clinic.getEstado(), clinic.getEstado(), reason, updatedAt));
				MonthlyPayment payment = payments.saveAndFlush(new MonthlyPayment(clinic.getId(), target, request.operation(),
						reference, PlanService.monthlyPrice(target), start, end, now, anchor, actor.getId(), actor.getEmail(),
						reason, clinic.getLastPaymentId(), audit.getId()));
				if (!renew || !active) clinic.setSubscriptionStartedAt(start);
				clinic.setSubscriptionEndsAt(end);
				clinic.setSubscriptionAnchorDay(anchor);
				clinic.setLastPaymentId(payment.getId());
				clinic.setPlan(target);
				clinic.setUpdatedAt(updatedAt);
				return response(clinic);
			});
		} catch (DataIntegrityViolationException exception) {
			// Translate outside the write callback; never query from this error path.
			for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
				if (cause instanceof ConstraintViolationException violation
						&& violation.getConstraintName() != null) {
					String constraint = violation.getConstraintName().replace("\"", "").toLowerCase(Locale.ROOT);
					if (constraint.equals("uk_monthly_payment_reference")
							|| constraint.startsWith("public.uk_monthly_payment_reference index ")
							|| constraint.startsWith("public.uk_monthly_payment_reference_index_")) throw referenceUsed();
				}
			}
			throw exception;
		}
	}

	@Transactional(readOnly = true)
	public SaasPageResponse<MonthlyPaymentResponse> monthlyPayments(Authentication authentication, Long id, int page, int size) {
		requireOperator(authentication);
		validatePage(page, size);
		if (!clinics.existsById(id)) throw new EntityNotFoundException("Clinica no encontrada.");
		return SaasPageResponse.from(payments.findByClinicId(id,
				PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "confirmedAt", "id")))
				.map(MonthlyPaymentResponse::from));
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
						a.getNewEstado().name(), a.getReason(), a.getCreatedAt(),
						payments.findByAuditId(a.getId()).map(MonthlyPaymentResponse::from).orElse(null))));
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

	private String normalizeReference(String reference) {
		if (reference == null || !reference.matches("[A-Za-z0-9 ._/-]+")) {
			throw new IllegalArgumentException("paymentReference solo permite letras A-Z, numeros 0-9, espacios y . _ / - sin caracteres de control.");
		}
		String normalized = reference.trim().replaceAll(" +", " ").toUpperCase(Locale.ROOT);
		if (normalized.length() < 5 || normalized.length() > 120) {
			throw new IllegalArgumentException("paymentReference debe tener entre 5 y 120 caracteres normalizados.");
		}
		return normalized;
	}

	private PlanException referenceUsed() {
		return new PlanException(HttpStatus.CONFLICT, "PAYMENT_REFERENCE_USED", "La referencia de pago ya fue utilizada con otros datos.");
	}
}
