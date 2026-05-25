package com.petcare.backend.domain.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record VacunaMascotaRequest(
		@NotNull Long vacunaId,
		Long veterinarioId,
		Long citaId,
		@NotNull @PastOrPresent LocalDate fechaAplicacion,
		@Size(max = 80) String lote,
		LocalDate fechaProximaDosis,
		@Size(max = 500) String observaciones
) {
}
