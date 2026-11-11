package com.petcare.backend.domain.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record MonthlyPaymentRequest(
		@NotBlank @Pattern(regexp = "CONSULTORIO|PRO") String plan,
		@NotBlank @Pattern(regexp = "ACTIVATE|RENEW") String operation,
		@NotBlank String paymentReference,
		@NotBlank @Size(min = 5, max = 500) String reason,
		@NotNull @AssertTrue Boolean paymentConfirmed,
		@Positive Long expectedPaymentId
) {
	public MonthlyPaymentRequest {
		reason = reason == null ? null : reason.trim();
	}
}
