package com.petcare.backend.domain.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ControlMensualMascotaResponse(
		Long id,
		Long mascotaId,
		String mascotaNombre,
		Long veterinarioId,
		String veterinarioNombreCompleto,
		LocalDate fechaControl,
		Integer anio,
		Integer mes,
		BigDecimal pesoKg,
		String alimentacion,
		String observaciones,
		String recomendaciones,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}
