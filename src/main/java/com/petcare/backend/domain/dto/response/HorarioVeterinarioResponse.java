package com.petcare.backend.domain.dto.response;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record HorarioVeterinarioResponse(
		Long id,
		DayOfWeek diaSemana,
		LocalTime horaInicio,
		LocalTime horaFin,
		Integer duracionBloqueMinutos,
		Boolean active
) {
}
