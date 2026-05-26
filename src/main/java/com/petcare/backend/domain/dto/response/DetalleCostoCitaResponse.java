package com.petcare.backend.domain.dto.response;

import java.math.BigDecimal;

public record DetalleCostoCitaResponse(
		Long servicioId,
		String nombreServicio,
		BigDecimal costoUnitario,
		Integer cantidad,
		BigDecimal subtotal
) {
}
