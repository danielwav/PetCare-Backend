package com.petcare.backend.domain.dto.response;

import java.time.LocalDateTime;

public record SaasClinicAuditResponse(
		Long id, Long actorId, String actorEmail, String action,
		String previousPlan, String newPlan, String previousEstado, String newEstado,
		String reason, LocalDateTime createdAt
) {}
