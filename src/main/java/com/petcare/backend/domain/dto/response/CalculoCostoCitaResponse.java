package com.petcare.backend.domain.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record CalculoCostoCitaResponse(
		List<DetalleCostoCitaResponse> detalles,
		BigDecimal subtotal,
		BigDecimal descuento,
		BigDecimal total
) {
}
