package com.petcare.backend.domain.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DuenioRequest(
		Long usuarioId,
		@Size(max = 80) String nombres,
		@Size(max = 80) String apellidos,
		@NotBlank @Size(max = 30) String tipoDocumento,
		@NotBlank @Size(max = 20) String numeroDocumento,
		@NotBlank @Size(min = 8, max = 20) @Pattern(regexp = "^[+\\d\\s-]+$", message = "Número de teléfono inválido") String telefono,
		@NotBlank @Email @Size(max = 120) String email,
		@Size(max = 180) String direccion
) {
}
