package com.petcare.backend.web;

import com.petcare.backend.domain.dto.request.InasistenciaRequest;
import com.petcare.backend.domain.dto.response.InasistenciaResponse;
import com.petcare.backend.domain.service.ClinicaService;
import com.petcare.backend.domain.service.InasistenciaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class InasistenciaController {

	private final InasistenciaService inasistenciaService;
	private final ClinicaService clinicaService;

	@PatchMapping("/api/citas/{id}/inasistencia")
	public InasistenciaResponse register(
			@PathVariable Long id,
			@Valid @RequestBody InasistenciaRequest request,
			Authentication authentication
	) {
		String registeredBy = authentication == null ? "sistema" : authentication.getName();
		return inasistenciaService.register(id, request, registeredBy, clinicaService.resolveClinicaId(authentication.getName()));
	}

	@GetMapping("/api/inasistencias")
	public List<InasistenciaResponse> findAll(
			@RequestParam(required = false) Long duenioId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
			Authentication authentication
	) {
		return inasistenciaService.findAll(duenioId, fechaInicio, fechaFin, clinicaService.resolveClinicaId(authentication.getName()));
	}

	@GetMapping("/api/inasistencias/{id}")
	public InasistenciaResponse findById(@PathVariable Long id, Authentication authentication) {
		return inasistenciaService.findById(id, clinicaService.resolveClinicaId(authentication.getName()));
	}
}
