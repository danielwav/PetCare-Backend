package com.petcare.backend.domain.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record ReporteCostoCitaResponse(
		Long citaId,
		BigDecimal subtotal,
		BigDecimal descuento,
		BigDecimal total,
		List<DetalleCostoCitaResponse> detalles
) {
}
