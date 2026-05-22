package com.petcare.backend.domain.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;

import java.math.BigDecimal;
import java.util.List;

public record CalculoCostoCitaRequest(
		@NotEmpty List<@Valid CostoCitaServicioRequest> servicios,
		@DecimalMin(value = "0.00") BigDecimal descuento
) {
}
