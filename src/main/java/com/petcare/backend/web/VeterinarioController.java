package com.petcare.backend.web;

import com.petcare.backend.domain.dto.request.VeterinarioRequest;
import com.petcare.backend.domain.dto.response.DisponibilidadVeterinarioResponse;
import com.petcare.backend.domain.dto.response.VeterinarioResponse;
import com.petcare.backend.domain.service.VeterinarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
public class VeterinarioController {

	private final VeterinarioService veterinarioService;

	@PostMapping("/api/veterinarios")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('ADMIN')")
	public VeterinarioResponse create(@Valid @RequestBody VeterinarioRequest request) {
		return veterinarioService.create(request);
	}

	@GetMapping("/api/veterinarios")
	public List<VeterinarioResponse> findAll(
			@RequestParam(required = false) String search,
			@RequestParam(required = false) Boolean active
	) {
		return veterinarioService.findAll(search, active);
	}

	@GetMapping("/api/veterinarios/{id}")
	@PreAuthorize("hasAnyRole('ADMIN', 'ASISTENTE', 'VETERINARIO')")
	public VeterinarioResponse findById(@PathVariable Long id) {
		return veterinarioService.findById(id);
	}

	@GetMapping("/api/veterinarios/{id}/disponibilidad")
	@PreAuthorize("hasAnyRole('ADMIN', 'ASISTENTE', 'VETERINARIO')")
	public DisponibilidadVeterinarioResponse findDisponibilidad(
			@PathVariable Long id,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
			@RequestParam(required = false) Integer duracionMinutos
	) {
		return veterinarioService.findDisponibilidad(id, fecha, duracionMinutos);
	}

	@PutMapping("/api/veterinarios/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public VeterinarioResponse update(@PathVariable Long id, @Valid @RequestBody VeterinarioRequest request) {
		return veterinarioService.update(id, request);
	}

	@DeleteMapping("/api/veterinarios/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("hasRole('ADMIN')")
	public void deactivate(@PathVariable Long id) {
		veterinarioService.deactivate(id);
	}
}
