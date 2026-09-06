package com.petcare.backend.web;

import com.petcare.backend.domain.dto.request.UpdateClinicaRequest;
import com.petcare.backend.domain.dto.response.ClinicaResponse;
import com.petcare.backend.domain.service.ClinicaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clinicas")
@RequiredArgsConstructor
public class ClinicaController {

	private final ClinicaService clinicaService;

	@GetMapping("/me")
	public ClinicaResponse me(Authentication authentication) {
		return clinicaService.getMyClinic(authentication.getName());
	}

	@PutMapping("/me")
	public ClinicaResponse update(Authentication authentication, @Valid @RequestBody UpdateClinicaRequest request) {
		return clinicaService.updateMyClinic(authentication.getName(), request);
	}
}