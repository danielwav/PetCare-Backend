package com.petcare.backend.web;

import com.petcare.backend.domain.dto.request.CalculoCostoCitaRequest;
import com.petcare.backend.domain.dto.request.ServicioRequest;
import com.petcare.backend.domain.dto.response.CalculoCostoCitaResponse;
import com.petcare.backend.domain.dto.response.ServicioResponse;
import com.petcare.backend.domain.service.ServicioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ServicioController {

	private final ServicioService servicioService;

	@PostMapping("/api/servicios")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('ADMIN')")
	public ServicioResponse create(@Valid @RequestBody ServicioRequest request) {
		return servicioService.create(request);
	}

	@GetMapping("/api/servicios")
	public List<ServicioResponse> findAll(
			@RequestParam(required = false) String search,
			@RequestParam(required = false) Boolean active
	) {
		return servicioService.findAll(search, active);
	}

	@GetMapping("/api/servicios/{id}")
	@PreAuthorize("hasAnyRole('ADMIN', 'ASISTENTE', 'VETERINARIO')")
	public ServicioResponse findById(@PathVariable Long id) {
		return servicioService.findById(id);
	}

	@PutMapping("/api/servicios/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ServicioResponse update(@PathVariable Long id, @Valid @RequestBody ServicioRequest request) {
		return servicioService.update(id, request);
	}

	@PatchMapping("/api/servicios/{id}/activar")
	@PreAuthorize("hasRole('ADMIN')")
	public ServicioResponse activate(@PathVariable Long id) {
		return servicioService.activate(id);
	}

	@DeleteMapping("/api/servicios/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("hasRole('ADMIN')")
	public void deactivate(@PathVariable Long id) {
		servicioService.deactivate(id);
	}

	@PostMapping("/api/servicios/calcular-costo")
	@PreAuthorize("hasAnyRole('ADMIN', 'ASISTENTE', 'VETERINARIO')")
	public CalculoCostoCitaResponse calculateCost(@Valid @RequestBody CalculoCostoCitaRequest request) {
		return servicioService.calculateCost(request);
	}
}
