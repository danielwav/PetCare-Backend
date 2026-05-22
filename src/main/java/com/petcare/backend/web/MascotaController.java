package com.petcare.backend.web;

import com.petcare.backend.domain.dto.request.MascotaRequest;
import com.petcare.backend.domain.dto.response.MascotaResponse;
import com.petcare.backend.domain.service.MascotaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MascotaController {

	private final MascotaService mascotaService;

	@PostMapping("/api/mascotas")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAnyRole('ADMIN', 'ASISTENTE')")
	public MascotaResponse create(@Valid @RequestBody MascotaRequest request) {
		return mascotaService.create(request);
	}

	@GetMapping("/api/mascotas")
	@PreAuthorize("hasAnyRole('ADMIN', 'ASISTENTE', 'VETERINARIO')")
	public List<MascotaResponse> findAll(
			@RequestParam(required = false) String search,
			@RequestParam(required = false) Long duenioId,
			@RequestParam(required = false) Boolean active
	) {
		return mascotaService.findAll(search, duenioId, active);
	}

	@GetMapping("/api/duenios/{duenioId}/mascotas")
	@PreAuthorize("hasAnyRole('ADMIN', 'ASISTENTE', 'VETERINARIO')")
	public List<MascotaResponse> findByDuenio(@PathVariable Long duenioId) {
		return mascotaService.findByDuenio(duenioId);
	}

	@GetMapping("/api/mascotas/{id}")
	@PreAuthorize("hasAnyRole('ADMIN', 'ASISTENTE', 'VETERINARIO')")
	public MascotaResponse findById(@PathVariable Long id) {
		return mascotaService.findById(id);
	}

	@PutMapping("/api/mascotas/{id}")
	@PreAuthorize("hasAnyRole('ADMIN', 'ASISTENTE')")
	public MascotaResponse update(@PathVariable Long id, @Valid @RequestBody MascotaRequest request) {
		return mascotaService.update(id, request);
	}

	@DeleteMapping("/api/mascotas/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("hasAnyRole('ADMIN', 'ASISTENTE')")
	public void deactivate(@PathVariable Long id) {
		mascotaService.deactivate(id);
	}
}
