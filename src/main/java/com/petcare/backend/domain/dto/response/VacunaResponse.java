package com.petcare.backend.domain.dto.response;

import java.time.LocalDateTime;

public record VacunaResponse(
		Long id,
		String nombre,
		String descripcion,
		Integer intervaloProximaDosisDias,
		Boolean active,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}
