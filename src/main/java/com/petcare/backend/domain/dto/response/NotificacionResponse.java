package com.petcare.backend.domain.dto.response;

public record NotificacionResponse(
    Long id,
    String tipo,
    String mensaje,
    String fecha,
    String ruta,
    String icono,
    boolean leida
) {}
