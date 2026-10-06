package com.petcare.backend.domain.service;

import com.petcare.backend.domain.dto.request.CreateInternalUserRequest;
import com.petcare.backend.domain.repository.UsuarioRepository;

final class ServiceTestFixtures {

	private ServiceTestFixtures() {
	}

	static Long veterinarioUser(AuthService authService, UsuarioRepository usuarioRepository,
			Long clinicaId, String email) {
		return usuarioRepository.findByEmail(email)
				.map(usuario -> usuario.getId())
				.orElseGet(() -> {
					authService.createInternalUser(new CreateInternalUserRequest(
							"Ana", "Salas", email, "VETERINARIO"), clinicaId);
					return authService.me(email).id();
				});
	}
}
