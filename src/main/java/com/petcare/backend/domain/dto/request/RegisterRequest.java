package com.petcare.backend.domain.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
		@NotBlank @Size(max = 120) String fullName,
		@NotBlank @Email @Size(max = 120) String email,
		@NotBlank @Size(min = 6, max = 100) String password,
		@NotBlank @Size(min = 8, max = 20) @Pattern(regexp = "^[+\\d\\s-]+$", message = "Número de teléfono inválido") String telefono
) {
}
