package com.petcare.backend.domain.dto.response;

import java.math.BigDecimal;

public record ServicioPublicResponse(
		Long id,
		String nombre,
		String descripcion,
		BigDecimal costoBase
) {
}