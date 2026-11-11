package com.petcare.backend.domain.dto.response;

import com.petcare.backend.persistence.entity.MonthlyPayment;

import java.math.BigDecimal;
import java.time.Instant;

public record MonthlyPaymentResponse(
		Long id, Long clinicId, String plan, String operation, String paymentReference,
		BigDecimal amount, String currency, String billingCycle, Instant periodStartedAt,
		Instant periodEndsAt, Instant confirmedAt, Integer anchorDay, Long actorId,
		String actorEmail, String reason, Long previousPaymentId, Long auditId
) {
	public static MonthlyPaymentResponse from(MonthlyPayment payment) {
		return new MonthlyPaymentResponse(payment.getId(), payment.getClinicId(), payment.getPlan().name(),
				payment.getOperation(), payment.getPaymentReference(), payment.getAmount(), payment.getCurrency(),
				payment.getBillingCycle(), payment.getPeriodStartedAt(), payment.getPeriodEndsAt(),
				payment.getConfirmedAt(), payment.getAnchorDay(), payment.getActorId(), payment.getActorEmail(),
				payment.getReason(), payment.getPreviousPaymentId(), payment.getAuditId());
	}
}
