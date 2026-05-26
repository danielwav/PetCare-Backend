package com.petcare.backend.domain.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VacunaRequest(
		@NotBlank @Size(max = 120) String nombre,
		@NotBlank @Size(max = 500) String descripcion,
		@Min(1) Integer intervaloProximaDosisDias
) {
}
