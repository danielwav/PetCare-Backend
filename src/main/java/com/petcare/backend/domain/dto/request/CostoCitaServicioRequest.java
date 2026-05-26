package com.petcare.backend.domain.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CostoCitaServicioRequest(
		@NotNull Long servicioId,
		@NotNull @Min(1) Integer cantidad
) {
}
