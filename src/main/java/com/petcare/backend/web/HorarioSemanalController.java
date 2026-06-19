package com.petcare.backend.web;

import com.petcare.backend.domain.dto.request.HorarioSemanalRequest;
import com.petcare.backend.domain.dto.response.HorarioSemanalResponse;
import com.petcare.backend.domain.service.HorarioSemanalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class HorarioSemanalController {

    private final HorarioSemanalService service;

    @GetMapping("/api/horarios-semanales")
    @PreAuthorize("hasAnyRole('ADMIN', 'ASISTENTE', 'VETERINARIO')")
    public List<HorarioSemanalResponse> findBySemana(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate semana) {
        return service.findBySemana(semana);
    }

    @PostMapping("/api/horarios-semanales")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    public List<HorarioSemanalResponse> saveWeek(@Valid @RequestBody HorarioSemanalRequest request) {
        return service.saveWeek(request);
    }

    @DeleteMapping("/api/horarios-semanales/{usuarioId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteByUsuario(
            @PathVariable Long usuarioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate semana) {
        service.deleteByUsuarioAndSemana(usuarioId, semana);
    }
}
