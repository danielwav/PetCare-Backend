package com.petcare.backend.domain.dto.response;

import com.petcare.backend.persistence.enums.EstadoCita;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record CitaResponse(
		Long id,
		Long duenioId,
		String duenioNombreCompleto,
		Long mascotaId,
		String mascotaNombre,
		Long veterinarioId,
		String veterinarioNombreCompleto,
		LocalDate fecha,
		LocalTime horaInicio,
		LocalTime horaFin,
		Integer duracionMinutos,
		String motivo,
		EstadoCita estado,
		List<DetalleCostoCitaResponse> detallesCosto,
		BigDecimal subtotal,
		BigDecimal descuento,
		BigDecimal total,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}
