package com.petcare.backend.web;

import com.petcare.backend.domain.dto.request.CitaRequest;
import com.petcare.backend.domain.dto.response.CitaResponse;
import com.petcare.backend.domain.service.CitaService;
import com.petcare.backend.persistence.enums.EstadoCita;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
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

	@PostMapping("/api/citas")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAnyRole('ADMIN', 'ASISTENTE')")
	public CitaResponse create(@Valid @RequestBody CitaRequest request) {
		return citaService.create(request);
	}

	@GetMapping("/api/citas")
	@PreAuthorize("hasAnyRole('ADMIN', 'ASISTENTE', 'VETERINARIO')")
	public List<CitaResponse> findAll(
			@RequestParam(required = false) EstadoCita estado,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
			@RequestParam(required = false) Long duenioId,
			@RequestParam(required = false) Long mascotaId,
			@RequestParam(required = false) Long veterinarioId
	) {
		return citaService.findAll(estado, fecha, duenioId, mascotaId, veterinarioId);
	}

	@GetMapping("/api/citas/{id}")
	@PreAuthorize("hasAnyRole('ADMIN', 'ASISTENTE', 'VETERINARIO')")
	public CitaResponse findById(@PathVariable Long id) {
		return citaService.findById(id);
	}

	@PutMapping("/api/citas/{id}")
	@PreAuthorize("hasAnyRole('ADMIN', 'ASISTENTE')")
	public CitaResponse update(@PathVariable Long id, @Valid @RequestBody CitaRequest request) {
		return citaService.update(id, request);
	}

	@PatchMapping("/api/citas/{id}/cancelar")
	@PreAuthorize("hasAnyRole('ADMIN', 'ASISTENTE')")
	public CitaResponse cancel(@PathVariable Long id) {
		return citaService.cancel(id);
	}

	@PatchMapping("/api/citas/{id}/confirmar")
	@PreAuthorize("hasAnyRole('ADMIN', 'ASISTENTE', 'DUENIO')")
	public CitaResponse confirm(@PathVariable Long id, Authentication authentication) {
		String confirmedBy = authentication == null ? "sistema" : authentication.getName();
		return citaService.confirm(id, confirmedBy);
	}

	@GetMapping("/api/citas/alertas-confirmacion")
	@PreAuthorize("hasAnyRole('ADMIN', 'ASISTENTE')")
	public List<CitaResponse> findConfirmationAlerts(@RequestParam(required = false) Integer horas) {
		return citaService.findConfirmationAlerts(horas);
	}
}
