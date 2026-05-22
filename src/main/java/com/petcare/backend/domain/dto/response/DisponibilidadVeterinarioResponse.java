package com.petcare.backend.domain.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record DisponibilidadVeterinarioResponse(
		Long veterinarioId,
		String veterinarioNombreCompleto,
		LocalDate fecha,
		List<LocalTime> horariosDisponibles
) {
}
