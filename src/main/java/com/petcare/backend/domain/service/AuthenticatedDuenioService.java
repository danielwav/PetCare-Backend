package com.petcare.backend.domain.service;

import com.petcare.backend.domain.repository.DuenioRepository;
import com.petcare.backend.persistence.entity.Duenio;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticatedDuenioService {

	private final DuenioRepository duenioRepository;

	@Transactional(readOnly = true)
	public Duenio findByAuthenticatedEmail(String email) {
		return duenioRepository.findByUsuarioEmail(normalizeEmail(email))
				.orElseThrow(() -> new AccessDeniedException("El usuario autenticado no tiene un perfil de duenio vinculado."));
	}

	@Transactional(readOnly = true)
	public Duenio validateOwnDuenio(String email, Long duenioId) {
		Duenio duenio = findByAuthenticatedEmail(email);
		if (!duenio.getId().equals(duenioId)) {
			throw new AccessDeniedException("No tienes permiso para consultar datos de otro duenio.");
		}
		return duenio;
	}

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase();
	}
}
