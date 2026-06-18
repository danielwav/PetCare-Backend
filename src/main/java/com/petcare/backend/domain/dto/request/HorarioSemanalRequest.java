package com.petcare.backend.domain.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record HorarioSemanalRequest(
    @NotNull LocalDate semana,
    @NotNull List<AsignacionRequest> asignaciones
) {
    public record AsignacionRequest(
        @NotNull Long usuarioId,
        String lunes, String martes, String miercoles,
        String jueves, String viernes, String sabado, String domingo
    ) {}
}
