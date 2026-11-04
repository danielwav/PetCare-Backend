package com.petcare.backend.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SaasClinicPlanRequest(
		@NotBlank @Pattern(regexp = "CONSULTORIO|PRO") String plan,
		@NotBlank @Size(min = 5, max = 500) String reason
) {
	public SaasClinicPlanRequest {
		reason = reason == null ? null : reason.trim();
	}
}
