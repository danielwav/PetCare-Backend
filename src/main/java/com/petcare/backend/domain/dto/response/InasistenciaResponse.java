package com.petcare.backend.domain.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record InasistenciaResponse(
		Long id,
		Long citaId,
		Long duenioId,
		String duenioNombreCompleto,
		Long mascotaId,
		String mascotaNombre,
		LocalDate fechaCita,
		LocalTime horaInicioCita,
		String observacion,
		String registradoPor,
		LocalDateTime fechaRegistro
) {
}
