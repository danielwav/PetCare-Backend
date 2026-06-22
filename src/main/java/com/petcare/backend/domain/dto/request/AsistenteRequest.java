package com.petcare.backend.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AsistenteRequest(
		Long usuarioId,
		@Size(max = 80) String nombres,
		@Size(max = 80) String apellidos,
		@NotBlank @Size(max = 30) String tipoDocumento,
		@NotBlank @Size(max = 20) String numeroDocumento,
		@Size(max = 20) String telefono,
		@Size(max = 120) String email,
		@NotBlank @Size(max = 500) String funciones,
		@Size(min = 8, max = 72) String password
) {
}
