package com.petcare.backend.persistence.entity;

import com.petcare.backend.persistence.enums.PlanClinica;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "monthly_payments", uniqueConstraints = {
		@UniqueConstraint(name = "uk_monthly_payment_reference", columnNames = "payment_reference"),
		@UniqueConstraint(name = "uk_monthly_payment_audit", columnNames = "audit_id")
}, indexes = @Index(name = "idx_monthly_payment_clinic_confirmed", columnList = "clinic_id,confirmed_at,id"))
@Immutable
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class MonthlyPayment {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "clinic_id", nullable = false, updatable = false)
	private Long clinicId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false, length = 20)
	private PlanClinica plan;

	@Column(nullable = false, updatable = false, length = 20)
	private String operation;

	@Column(name = "payment_reference", nullable = false, updatable = false, length = 120)
	private String paymentReference;

	@Column(nullable = false, updatable = false, precision = 10, scale = 2)
	private BigDecimal amount;

	@Column(nullable = false, updatable = false, length = 3)
	private String currency;

	@Column(name = "billing_cycle", nullable = false, updatable = false, length = 10)
	private String billingCycle;

	@Column(name = "period_started_at", nullable = false, updatable = false)
	private Instant periodStartedAt;

	@Column(name = "period_ends_at", nullable = false, updatable = false)
	private Instant periodEndsAt;

	@Column(name = "confirmed_at", nullable = false, updatable = false)
	private Instant confirmedAt;

	@Column(name = "anchor_day", nullable = false, updatable = false)
	private Integer anchorDay;

	@Column(name = "actor_id", nullable = false, updatable = false)
	private Long actorId;

	@Column(name = "actor_email", nullable = false, updatable = false, length = 120)
	private String actorEmail;

	@Column(nullable = false, updatable = false, length = 500)
	private String reason;

	@Column(name = "previous_payment_id", updatable = false)
	private Long previousPaymentId;

	@Column(name = "audit_id", nullable = false, updatable = false)
	private Long auditId;

	public MonthlyPayment(Long clinicId, PlanClinica plan, String operation, String paymentReference,
			BigDecimal amount, Instant periodStartedAt, Instant periodEndsAt, Instant confirmedAt,
			Integer anchorDay, Long actorId, String actorEmail, String reason, Long previousPaymentId, Long auditId) {
		this.clinicId = clinicId;
		this.plan = plan;
		this.operation = operation;
		this.paymentReference = paymentReference;
		this.amount = amount;
		this.currency = "PEN";
		this.billingCycle = "MONTHLY";
		this.periodStartedAt = periodStartedAt;
		this.periodEndsAt = periodEndsAt;
		this.confirmedAt = confirmedAt;
		this.anchorDay = anchorDay;
		this.actorId = actorId;
		this.actorEmail = actorEmail;
		this.reason = reason;
		this.previousPaymentId = previousPaymentId;
		this.auditId = auditId;
	}
}
