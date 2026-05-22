package com.petcare.backend.domain.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record VeterinarioResponse(
		Long id,
		Long usuarioId,
		String nombres,
		String apellidos,
		String numeroColegiatura,
		String especialidad,
		String telefono,
		String email,
		Boolean active,
		List<HorarioVeterinarioResponse> horarios,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}
