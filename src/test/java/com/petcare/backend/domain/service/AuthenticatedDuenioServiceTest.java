package com.petcare.backend.domain.service;

import com.petcare.backend.domain.repository.DuenioRepository;
import com.petcare.backend.persistence.entity.Clinica;
import com.petcare.backend.persistence.entity.Duenio;
import com.petcare.backend.persistence.entity.Usuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticatedDuenioServiceTest {

	@Mock
	private DuenioRepository duenioRepository;

	@InjectMocks
	private AuthenticatedDuenioService service;

	@Test
	void resolvesLinkedIdentityRatherThanOwnerContactEmail() {
		Clinica clinic = Clinica.builder().id(1L).build();
		Duenio owner = Duenio.builder().id(10L).clinica(clinic).email("contact@test.com")
				.usuario(Usuario.builder().id(20L).email("login@test.com").clinica(clinic).build()).build();
		when(duenioRepository.findByUsuarioEmail("login@test.com")).thenReturn(Optional.of(owner));
		assertThat(service.findByAuthenticatedEmail(" LOGIN@test.com ")).isSameAs(owner);
		assertThatThrownBy(() -> service.validateOwnDuenio("login@test.com", 11L))
				.isInstanceOf(AccessDeniedException.class);
		verifyNoMoreInteractions(duenioRepository);
	}

	@Test
	void missingLinkDoesNotFallBackToGlobalOrLocalOwnerEmail() {
		when(duenioRepository.findByUsuarioEmail("login@test.com")).thenReturn(Optional.empty());
		assertThatThrownBy(() -> service.findByAuthenticatedEmail("login@test.com"))
				.isInstanceOf(AccessDeniedException.class);
		verifyNoMoreInteractions(duenioRepository);
	}

	@Test
	void rejectsCrossClinicLinkAndMissingClinic() {
		Usuario user = Usuario.builder().id(20L).clinica(Clinica.builder().id(1L).build()).build();
		Duenio owner = Duenio.builder().id(10L).usuario(user).clinica(Clinica.builder().id(2L).build()).build();
		when(duenioRepository.findByUsuarioEmail("login@test.com")).thenReturn(Optional.of(owner));
		assertThatThrownBy(() -> service.findByAuthenticatedEmail("login@test.com"))
				.isInstanceOf(AccessDeniedException.class);
		owner.setClinica(null);
		assertThatThrownBy(() -> service.findByAuthenticatedEmail("login@test.com"))
				.isInstanceOf(AccessDeniedException.class);
		owner.setClinica(Clinica.builder().id(1L).build());
		user.setClinica(null);
		assertThatThrownBy(() -> service.findByAuthenticatedEmail("login@test.com"))
				.isInstanceOf(AccessDeniedException.class);
	}
}
