package com.petcare.backend.domain.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record CitaRequest(
		@NotNull Long duenioId,
		@NotNull Long mascotaId,
		@NotNull Long veterinarioId,
		@NotNull @FutureOrPresent LocalDate fecha,
		@NotNull LocalTime horaInicio,
		@NotNull @Min(15) Integer duracionMinutos,
		@NotBlank @Size(max = 250) String motivo,
		@NotEmpty List<@Valid CostoCitaServicioRequest> servicios,
		@DecimalMin(value = "0.00") BigDecimal descuento
) {
}
