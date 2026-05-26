package com.petcare.backend.domain.dto.response;

import java.math.BigDecimal;

public record ServicioSolicitadoResponse(
		String nombreServicio,
		Long cantidadSolicitada,
		BigDecimal totalGenerado
) {
}
