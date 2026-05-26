package com.petcare.backend.domain.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record HorarioVeterinarioRequest(
		@NotNull DayOfWeek diaSemana,
		@NotNull LocalTime horaInicio,
		@NotNull LocalTime horaFin,
		@NotNull @Min(15) @Max(240) Integer duracionBloqueMinutos
) {
}
