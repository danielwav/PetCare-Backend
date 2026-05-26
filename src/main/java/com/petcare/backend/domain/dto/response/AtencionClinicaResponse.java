package com.petcare.backend.domain.dto.response;

import java.time.LocalDateTime;

public record AtencionClinicaResponse(
		Long id,
		Long citaId,
		Long mascotaId,
		String mascotaNombre,
		Long veterinarioId,
		String veterinarioNombreCompleto,
		String motivo,
		String diagnostico,
		String tratamiento,
		String recomendaciones,
		String observacionesClinicas,
		String notasInternas,
		LocalDateTime fechaRegistro
) {
}
