package com.petcare.backend.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AtencionClinicaRequest(
		@NotBlank @Size(max = 250) String motivo,
		@NotBlank @Size(max = 1000) String diagnostico,
		@NotBlank @Size(max = 1000) String tratamiento,
		@Size(max = 1000) String recomendaciones,
		@Size(max = 1000) String observacionesClinicas,
		@Size(max = 1000) String notasInternas
) {
}
