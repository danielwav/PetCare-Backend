package com.petcare.backend.web;

import com.petcare.backend.domain.dto.request.SaasClinicEstadoRequest;
import com.petcare.backend.domain.dto.request.SaasClinicPlanRequest;
import com.petcare.backend.domain.dto.request.MonthlyPaymentRequest;
import com.petcare.backend.domain.dto.response.MonthlyPaymentResponse;
import com.petcare.backend.domain.dto.response.SaasClinicAuditResponse;
import com.petcare.backend.domain.dto.response.SaasClinicResponse;
import com.petcare.backend.domain.dto.response.SaasPageResponse;
import com.petcare.backend.domain.service.SaasClinicService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/saas/clinicas")
@RequiredArgsConstructor
public class SaasClinicController {
	private final SaasClinicService service;

	@GetMapping
	public SaasPageResponse<SaasClinicResponse> list(Authentication authentication,
			@RequestParam(required = false) String q, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return service.list(authentication, q, page, size);
	}

	@PatchMapping("/{id}/plan")
	public SaasClinicResponse changePlan(Authentication authentication, @PathVariable Long id,
			@RequestBody(required = false) SaasClinicPlanRequest request) {
		return service.changePlan(authentication, id, request);
	}

	@PostMapping("/{id}/mensualidades")
	public SaasClinicResponse monthlyPayment(Authentication authentication, @PathVariable Long id,
			@Valid @RequestBody MonthlyPaymentRequest request) {
		return service.monthlyPayment(authentication, id, request);
	}

	@GetMapping("/{id}/mensualidades")
	public SaasPageResponse<MonthlyPaymentResponse> monthlyPayments(Authentication authentication, @PathVariable Long id,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return service.monthlyPayments(authentication, id, page, size);
	}

	@PatchMapping("/{id}/estado")
	public SaasClinicResponse changeEstado(Authentication authentication, @PathVariable Long id,
			@Valid @RequestBody SaasClinicEstadoRequest request) {
		return service.changeEstado(authentication, id, request);
	}

	@GetMapping("/{id}/auditoria")
	public SaasPageResponse<SaasClinicAuditResponse> audit(Authentication authentication, @PathVariable Long id,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return service.audit(authentication, id, page, size);
	}
}
