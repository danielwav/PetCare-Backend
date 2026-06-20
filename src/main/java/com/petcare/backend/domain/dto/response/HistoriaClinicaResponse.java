package com.petcare.backend.domain.dto.response;

import com.petcare.backend.persistence.enums.EstadoMascota;

import java.time.LocalDateTime;
import java.util.List;

public record HistoriaClinicaResponse(
		Long mascotaId,
		String mascotaNombre,
		Long duenioId,
		String duenioNombreCompleto,
		EstadoMascota estado,
		LocalDateTime fechaEstado,
		String veterinarioEstado,
		List<AtencionClinicaResponse> atenciones,
		List<ControlMensualMascotaResponse> controlesMensuales
) {
}
