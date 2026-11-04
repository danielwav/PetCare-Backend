package com.petcare.backend.persistence.entity;

import com.petcare.backend.persistence.enums.EstadoClinica;
import com.petcare.backend.persistence.enums.PlanClinica;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

@Entity
@Table(name = "saas_clinic_audit", indexes = @Index(name = "idx_saas_audit_clinic_created", columnList = "clinic_id,created_at,id"))
@Immutable
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class SaasClinicAudit {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "clinic_id", nullable = false, updatable = false)
	private Long clinicId;

	@Column(nullable = false, updatable = false)
	private Long actorId;

	@Column(nullable = false, updatable = false, length = 120)
	private String actorEmail;

	@Column(nullable = false, updatable = false, length = 30)
	private String action;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false, length = 20)
	private PlanClinica previousPlan;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false, length = 20)
	private PlanClinica newPlan;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false, length = 20)
	private EstadoClinica previousEstado;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false, length = 20)
	private EstadoClinica newEstado;

	@Column(nullable = false, updatable = false, length = 500)
	private String reason;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	public SaasClinicAudit(Long clinicId, Long actorId, String actorEmail, String action,
			PlanClinica previousPlan, PlanClinica newPlan, EstadoClinica previousEstado,
			EstadoClinica newEstado, String reason, LocalDateTime createdAt) {
		this.clinicId = clinicId;
		this.actorId = actorId;
		this.actorEmail = actorEmail;
		this.action = action;
		this.previousPlan = previousPlan;
		this.newPlan = newPlan;
		this.previousEstado = previousEstado;
		this.newEstado = newEstado;
		this.reason = reason;
		this.createdAt = createdAt;
	}
}
