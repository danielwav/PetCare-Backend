package com.petcare.backend.domain.dto.response;

import com.petcare.backend.persistence.enums.EstadoCita;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record ReporteCitaResponse(
		Long id,
		LocalDate fecha,
		LocalTime horaInicio,
		EstadoCita estado,
		Long duenioId,
		String duenioNombreCompleto,
		Long mascotaId,
		String mascotaNombre,
		Long veterinarioId,
		String veterinarioNombreCompleto,
		String motivo,
		BigDecimal total
) {
}
