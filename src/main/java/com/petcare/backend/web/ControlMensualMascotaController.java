package com.petcare.backend.web;

import com.petcare.backend.domain.dto.request.ControlMensualMascotaRequest;
import com.petcare.backend.domain.dto.response.ControlMensualMascotaResponse;
import com.petcare.backend.domain.service.ClinicaService;
import com.petcare.backend.domain.service.ControlMensualMascotaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ControlMensualMascotaController {

	private final ControlMensualMascotaService controlMensualMascotaService;
	private final ClinicaService clinicaService;

	@PostMapping("/api/mascotas/{id}/controles-mensuales")
	@ResponseStatus(HttpStatus.CREATED)
	public ControlMensualMascotaResponse create(
			@PathVariable Long id,
			@Valid @RequestBody ControlMensualMascotaRequest request,
			Authentication authentication
	) {
		return controlMensualMascotaService.create(id, request, clinicaService.resolveClinicaId(authentication.getName()));
	}

	@GetMapping("/api/mascotas/{id}/controles-mensuales")
	public List<ControlMensualMascotaResponse> findByMascota(@PathVariable Long id, Authentication authentication) {
		return controlMensualMascotaService.findByMascota(id, clinicaService.resolveClinicaId(authentication.getName()));
	}

	@GetMapping("/api/controles-mensuales/{id}")
	public ControlMensualMascotaResponse findById(@PathVariable Long id, Authentication authentication) {
		return controlMensualMascotaService.findById(id, clinicaService.resolveClinicaId(authentication.getName()));
	}

	@PutMapping("/api/controles-mensuales/{id}")
	public ControlMensualMascotaResponse update(
			@PathVariable Long id,
			@Valid @RequestBody ControlMensualMascotaRequest request,
			Authentication authentication
	) {
		return controlMensualMascotaService.update(id, request, clinicaService.resolveClinicaId(authentication.getName()));
	}
}
