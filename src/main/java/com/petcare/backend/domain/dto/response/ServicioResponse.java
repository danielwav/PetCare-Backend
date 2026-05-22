package com.petcare.backend.domain.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ServicioResponse(
		Long id,
		String nombre,
		String descripcion,
		BigDecimal costoBase,
		Boolean active,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}
