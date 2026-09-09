package com.petcare.backend.web;

import com.petcare.backend.domain.dto.request.HorarioSemanalRequest;
import com.petcare.backend.domain.dto.response.HorarioSemanalResponse;
import com.petcare.backend.domain.service.ClinicaService;
import com.petcare.backend.domain.service.HorarioSemanalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class HorarioSemanalController {

    private final HorarioSemanalService service;
    private final ClinicaService clinicaService;

    @GetMapping("/api/horarios-semanales")
    public List<HorarioSemanalResponse> findBySemana(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate semana,
            Authentication authentication) {
        return service.findBySemana(semana, clinicaService.resolveClinicaId(authentication.getName()));
    }

    @PostMapping("/api/horarios-semanales")
    @ResponseStatus(HttpStatus.OK)
    public List<HorarioSemanalResponse> saveWeek(@Valid @RequestBody HorarioSemanalRequest request, Authentication authentication) {
        return service.saveWeek(request, clinicaService.resolveClinicaId(authentication.getName()));
    }

    @DeleteMapping("/api/horarios-semanales/{usuarioId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteByUsuario(
            @PathVariable Long usuarioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate semana,
            Authentication authentication) {
        service.deleteByUsuarioAndSemana(usuarioId, semana, clinicaService.resolveClinicaId(authentication.getName()));
    }
}
