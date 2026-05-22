package com.petcare.backend.domain.dto.response;

import com.petcare.backend.persistence.enums.SexoMascota;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record MascotaResponse(
		Long id,
		Long duenioId,
		String duenioNombreCompleto,
		String nombre,
		String especie,
		String raza,
		SexoMascota sexo,
		LocalDate fechaNacimiento,
		String color,
		BigDecimal pesoKg,
		String observaciones,
		Boolean active,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}
