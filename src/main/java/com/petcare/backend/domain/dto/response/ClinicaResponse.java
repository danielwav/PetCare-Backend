package com.petcare.backend.domain.dto.response;

import java.time.LocalDateTime;

public record ClinicaResponse(
		Long id,
		String nombre,
		String slug,
		String plan,
		String estado,
		LocalDateTime createdAt
) {
}