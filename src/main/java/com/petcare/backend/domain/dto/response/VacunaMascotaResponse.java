package com.petcare.backend.domain.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record VacunaMascotaResponse(
		Long id,
		Long mascotaId,
		String mascotaNombre,
		Long vacunaId,
		String vacunaNombre,
		Long veterinarioId,
		String veterinarioNombreCompleto,
		Long citaId,
		LocalDate fechaAplicacion,
		String lote,
		LocalDate fechaProximaDosis,
		String observaciones,
		String estadoAlerta,
		LocalDateTime createdAt
) {
}
