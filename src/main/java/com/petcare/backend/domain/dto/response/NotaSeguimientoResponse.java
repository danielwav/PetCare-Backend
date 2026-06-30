package com.petcare.backend.domain.dto.response;

import java.time.LocalDateTime;

public record NotaSeguimientoResponse(
    Long id,
    Long citaId,
    String observacion,
    String registradoPor,
    LocalDateTime createdAt
) {}
