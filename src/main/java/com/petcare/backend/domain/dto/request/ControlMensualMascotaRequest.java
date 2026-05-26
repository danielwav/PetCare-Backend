package com.petcare.backend.domain.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ControlMensualMascotaRequest(
		@NotNull Long veterinarioId,
		@NotNull @PastOrPresent LocalDate fechaControl,
		@DecimalMin(value = "0.01") BigDecimal pesoKg,
		@Size(max = 500) String alimentacion,
		@Size(max = 500) String observaciones,
		@Size(max = 500) String recomendaciones
) {
}
