package com.petcare.backend.domain.dto.response;

public record AuthResponse(
		String tokenType,
		String accessToken,
		UserResponse user
) {

	public AuthResponse(String accessToken, UserResponse user) {
		this("Bearer", accessToken, user);
	}
}
