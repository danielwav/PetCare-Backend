package com.petcare.backend.domain.dto.response;

public record AuthResponse(
		String tokenType,
		String accessToken,
		String refreshToken,
		Long expiresInSeconds,
		UserResponse user
) {

	public AuthResponse(String accessToken, UserResponse user) {
		this("Bearer", accessToken, null, null, user);
	}

	public AuthResponse(String accessToken, String refreshToken, Long expiresInSeconds, UserResponse user) {
		this("Bearer", accessToken, refreshToken, expiresInSeconds, user);
	}
}
