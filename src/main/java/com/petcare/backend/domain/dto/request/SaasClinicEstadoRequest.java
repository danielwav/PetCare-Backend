package com.petcare.backend.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SaasClinicEstadoRequest(
		@NotBlank @Pattern(regexp = "ACTIVA|SUSPENDIDA") String estado,
		@NotBlank @Size(min = 5, max = 500) String reason
) {
	public SaasClinicEstadoRequest {
		reason = reason == null ? null : reason.trim();
	}
}
