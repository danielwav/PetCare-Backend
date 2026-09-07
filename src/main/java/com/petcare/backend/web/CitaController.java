package com.petcare.backend.web;

import com.petcare.backend.domain.dto.request.CitaRequest;
import com.petcare.backend.domain.dto.response.CitaResponse;
import com.petcare.backend.domain.service.CitaService;
import com.petcare.backend.domain.service.ClinicaService;
import com.petcare.backend.persistence.enums.EstadoCita;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class CitaController {

	private final CitaService citaService;
	private final ClinicaService clinicaService;

	@PostMapping("/api/citas")
	@ResponseStatus(HttpStatus.CREATED)
	public CitaResponse create(@Valid @RequestBody CitaRequest request, Authentication authentication) {
		if (isDuenioOnly(authentication)) {
			return citaService.createAsDuenio(request, authentication.getName());
		}
		return citaService.create(request, clinicaService.resolveClinicaId(authentication.getName()));
	}

	@GetMapping("/api/citas")
	public List<CitaResponse> findAll(
			@RequestParam(required = false) EstadoCita estado,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
			@RequestParam(required = false) Long duenioId,
			@RequestParam(required = false) Long mascotaId,
			@RequestParam(required = false) Long veterinarioId,
			Authentication authentication
	) {
		if (isDuenioOnly(authentication)) {
			return citaService.findAllForDuenio(authentication.getName(), estado, fecha, mascotaId, veterinarioId);
		}
		return citaService.findAll(estado, fecha, duenioId, mascotaId, veterinarioId, clinicaService.resolveClinicaId(authentication.getName()));
	}

	@GetMapping("/api/citas/{id}")
	public CitaResponse findById(@PathVariable Long id, Authentication authentication) {
		if (isDuenioOnly(authentication)) {
			return citaService.findByIdForDuenio(id, authentication.getName());
		}
		return citaService.findById(id, clinicaService.resolveClinicaId(authentication.getName()));
	}

	@PutMapping("/api/citas/{id}")
	public CitaResponse update(@PathVariable Long id, @Valid @RequestBody CitaRequest request, Authentication authentication) {
		return citaService.update(id, request, clinicaService.resolveClinicaId(authentication.getName()));
	}

	@PatchMapping("/api/citas/{id}/cancelar")
	public CitaResponse cancel(@PathVariable Long id, Authentication authentication) {
		if (isDuenioOnly(authentication)) {
			return citaService.cancelAsDuenio(id, authentication.getName());
		}
		return citaService.cancel(id, clinicaService.resolveClinicaId(authentication.getName()));
	}

	@PatchMapping("/api/citas/{id}/confirmar")
	public CitaResponse confirm(@PathVariable Long id, Authentication authentication) {
		String confirmedBy = authentication == null ? "sistema" : authentication.getName();
		if (isDuenioOnly(authentication)) {
			return citaService.confirmAsDuenio(id, confirmedBy);
		}
		return citaService.confirm(id, confirmedBy, clinicaService.resolveClinicaId(authentication.getName()));
	}

	@GetMapping("/api/citas/alertas-confirmacion")
	public List<CitaResponse> findConfirmationAlerts(@RequestParam(required = false) Integer horas, Authentication authentication) {
		return citaService.findConfirmationAlerts(horas, clinicaService.resolveClinicaId(authentication.getName()));
	}

	private boolean isDuenioOnly(Authentication authentication) {
		return hasRole(authentication, "ROLE_DUENIO")
				&& !hasRole(authentication, "ROLE_ADMIN")
				&& !hasRole(authentication, "ROLE_ASISTENTE")
				&& !hasRole(authentication, "ROLE_VETERINARIO");
	}

	private boolean hasRole(Authentication authentication, String role) {
		return authentication != null && authentication.getAuthorities().stream()
				.anyMatch(authority -> authority.getAuthority().equals(role));
	}
}
