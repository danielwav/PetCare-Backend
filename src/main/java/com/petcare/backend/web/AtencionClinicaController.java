package com.petcare.backend.web;

import com.petcare.backend.domain.dto.request.AtencionClinicaRequest;
import com.petcare.backend.domain.dto.response.AtencionClinicaResponse;
import com.petcare.backend.domain.dto.response.HistoriaClinicaResponse;
import com.petcare.backend.domain.service.AtencionClinicaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AtencionClinicaController {

	private final AtencionClinicaService atencionClinicaService;

	@PostMapping("/api/citas/{id}/atencion")
	@ResponseStatus(HttpStatus.CREATED)
	public AtencionClinicaResponse register(
			@PathVariable Long id,
			@Valid @RequestBody AtencionClinicaRequest request
	) {
		return atencionClinicaService.register(id, request);
	}

	@GetMapping("/api/mascotas/{id}/historia-clinica")
	public HistoriaClinicaResponse findHistoriaClinicaByMascota(@PathVariable Long id) {
		return atencionClinicaService.findHistoriaClinicaByMascota(id);
	}

	@GetMapping("/api/atenciones/{id}")
	public AtencionClinicaResponse findById(@PathVariable Long id) {
		return atencionClinicaService.findById(id);
	}
}
