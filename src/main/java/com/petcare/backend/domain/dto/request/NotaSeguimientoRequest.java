package com.petcare.backend.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NotaSeguimientoRequest(
    @NotNull Long citaId,
    @NotBlank @Size(max = 500) String observacion
) {}
