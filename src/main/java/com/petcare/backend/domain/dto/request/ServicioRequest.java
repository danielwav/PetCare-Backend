package com.petcare.backend.domain.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ServicioRequest(
		@NotBlank @Size(max = 100) String nombre,
		@NotBlank @Size(max = 300) String descripcion,
		@NotNull @DecimalMin(value = "0.00", inclusive = false) BigDecimal costoBase
) {
}
