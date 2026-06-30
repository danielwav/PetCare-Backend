package com.petcare.backend.domain.dto.response;

import java.time.LocalDate;

public record HorarioSemanalResponse(
    Long id,
    Long usuarioId,
    String usuarioNombre,
    String usuarioRol,
    LocalDate fechaSemana,
    String lunes, String martes, String miercoles,
    String jueves, String viernes, String sabado, String domingo
) {}
