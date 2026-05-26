package com.petcare.backend.domain.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
		@NotBlank String refreshToken
) {
}
