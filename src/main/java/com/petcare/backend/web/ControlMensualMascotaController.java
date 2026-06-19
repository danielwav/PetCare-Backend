package com.petcare.backend.web;

import com.petcare.backend.domain.dto.request.ControlMensualMascotaRequest;
import com.petcare.backend.domain.dto.response.ControlMensualMascotaResponse;
import com.petcare.backend.domain.service.ControlMensualMascotaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

	@PostMapping("/api/mascotas/{id}/controles-mensuales")
	@ResponseStatus(HttpStatus.CREATED)
	public ControlMensualMascotaResponse create(
			@PathVariable Long id,
			@Valid @RequestBody ControlMensualMascotaRequest request
	) {
		return controlMensualMascotaService.create(id, request);
	}

	@GetMapping("/api/mascotas/{id}/controles-mensuales")
	public List<ControlMensualMascotaResponse> findByMascota(@PathVariable Long id) {
		return controlMensualMascotaService.findByMascota(id);
	}

	@GetMapping("/api/controles-mensuales/{id}")
	public ControlMensualMascotaResponse findById(@PathVariable Long id) {
		return controlMensualMascotaService.findById(id);
	}

	@PutMapping("/api/controles-mensuales/{id}")
	public ControlMensualMascotaResponse update(
			@PathVariable Long id,
			@Valid @RequestBody ControlMensualMascotaRequest request
	) {
		return controlMensualMascotaService.update(id, request);
	}
}
