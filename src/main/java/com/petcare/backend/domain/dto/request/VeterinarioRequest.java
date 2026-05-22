package com.petcare.backend.domain.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record VeterinarioRequest(
		Long usuarioId,
		@NotBlank @Size(max = 80) String nombres,
		@NotBlank @Size(max = 80) String apellidos,
		@NotBlank @Size(max = 30) String numeroColegiatura,
		@NotBlank @Size(max = 100) String especialidad,
		@NotBlank @Pattern(regexp = "^\\d{9}$") String telefono,
		@NotBlank @Email @Size(max = 120) String email,
		List<@Valid HorarioVeterinarioRequest> horarios
) {
}
