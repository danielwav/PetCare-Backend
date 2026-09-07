package com.petcare.backend.web;

import com.petcare.backend.domain.dto.request.CalculoCostoCitaRequest;
import com.petcare.backend.domain.dto.request.ServicioRequest;
import com.petcare.backend.domain.dto.response.CalculoCostoCitaResponse;
import com.petcare.backend.domain.dto.response.ServicioResponse;
import com.petcare.backend.domain.service.ClinicaService;
import com.petcare.backend.domain.service.ServicioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
@RequestMapping("/api/servicios")
@RequiredArgsConstructor
public class ServicioController {

	private final ServicioService servicioService;
	private final ClinicaService clinicaService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ServicioResponse create(@Valid @RequestBody ServicioRequest request, Authentication authentication) {
		return servicioService.create(request, clinicaService.resolveClinicaId(authentication.getName()));
	}

	@GetMapping
	public List<ServicioResponse> findAll(
			@RequestParam(required = false) String search,
			@RequestParam(required = false) Boolean active,
			Authentication authentication
	) {
		return servicioService.findAll(search, active, clinicaService.resolveClinicaId(authentication.getName()));
	}

	@GetMapping("/{id}")
	public ServicioResponse findById(@PathVariable Long id, Authentication authentication) {
		return servicioService.findById(id, clinicaService.resolveClinicaId(authentication.getName()));
	}

	@PutMapping("/{id}")
	public ServicioResponse update(@PathVariable Long id, @Valid @RequestBody ServicioRequest request, Authentication authentication) {
		return servicioService.update(id, request, clinicaService.resolveClinicaId(authentication.getName()));
	}

	@PatchMapping("/{id}/activar")
	public ServicioResponse activate(@PathVariable Long id, Authentication authentication) {
		return servicioService.activate(id, clinicaService.resolveClinicaId(authentication.getName()));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deactivate(@PathVariable Long id, Authentication authentication) {
		servicioService.deactivate(id, clinicaService.resolveClinicaId(authentication.getName()));
	}

	@PostMapping("/calcular-costo")
	public CalculoCostoCitaResponse calculateCost(@Valid @RequestBody CalculoCostoCitaRequest request, Authentication authentication) {
		return servicioService.calculateCost(request, clinicaService.resolveClinicaId(authentication.getName()));
	}
}
