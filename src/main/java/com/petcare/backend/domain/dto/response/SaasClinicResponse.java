package com.petcare.backend.domain.dto.response;

import java.time.LocalDateTime;

public record SaasClinicResponse(
		Long id, String nombre, String slug, String estado, LocalDateTime createdAt, PlanResponse plan
) {}
