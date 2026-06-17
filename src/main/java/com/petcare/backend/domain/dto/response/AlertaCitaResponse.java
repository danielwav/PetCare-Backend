package com.petcare.backend.domain.dto.response;

import com.petcare.backend.persistence.enums.EstadoCita;

import java.time.LocalDate;
import java.time.LocalTime;

public record AlertaCitaResponse(
		Long citaId,
		Long duenioId,
		String duenioNombreCompleto,
		String duenioTelefono,
		Long mascotaId,
		String mascotaNombre,
		Long veterinarioId,
		String veterinarioNombreCompleto,
		LocalDate fecha,
		LocalTime horaInicio,
		EstadoCita estado,
		String motivo
) {
}
