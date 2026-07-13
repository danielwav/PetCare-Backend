package com.petcare.backend.domain.dto.response;

import java.util.Set;

public record UserResponse(
		Long id,
		String fullName,
		String email,
		String telefono,
		Boolean active,
		Boolean forcePasswordChange,
		Set<String> roles
) {
}
