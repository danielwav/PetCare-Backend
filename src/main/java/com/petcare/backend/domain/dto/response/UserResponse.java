package com.petcare.backend.domain.dto.response;

import java.util.Set;

public record UserResponse(
		Long id,
		String fullName,
		String email,
		Boolean active,
		Set<String> roles
) {
}
