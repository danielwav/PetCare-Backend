package com.petcare.backend.domain.service;

import com.petcare.backend.domain.repository.DuenioRepository;
import com.petcare.backend.persistence.entity.Duenio;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthenticatedDuenioService {

	private final DuenioRepository duenioRepository;

	@Transactional(readOnly = true)
	public Duenio findByAuthenticatedEmail(String email) {
		String normalized = normalizeEmail(email);
		return duenioRepository.findByUsuarioEmail(normalized)
				.filter(duenio -> duenio.getClinica() != null
						&& duenio.getUsuario().getClinica() != null
						&& duenio.getClinica().getId().equals(duenio.getUsuario().getClinica().getId()))
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
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
