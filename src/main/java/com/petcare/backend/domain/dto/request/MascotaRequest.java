package com.petcare.backend.domain.dto.request;

import com.petcare.backend.persistence.enums.SexoMascota;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MascotaRequest(
		Long duenioId,
		@NotBlank @Size(max = 80) String nombre,
		@NotBlank @Size(max = 60) String especie,
		@NotBlank @Size(max = 80) String raza,
		@NotNull SexoMascota sexo,
		@NotNull @PastOrPresent LocalDate fechaNacimiento,
		@Size(max = 50) String color,
		@DecimalMin(value = "0.01") BigDecimal pesoKg,
		@Size(max = 300) String observaciones,
		String fotoUrl
) {
}
