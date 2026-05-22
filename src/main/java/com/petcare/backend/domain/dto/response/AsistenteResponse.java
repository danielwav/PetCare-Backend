package com.petcare.backend.domain.dto.response;

import java.time.LocalDateTime;

public record AsistenteResponse(
		Long id,
		Long usuarioId,
		String nombres,
		String apellidos,
		String tipoDocumento,
		String numeroDocumento,
		String telefono,
		String email,
		String funciones,
		Boolean active,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}
