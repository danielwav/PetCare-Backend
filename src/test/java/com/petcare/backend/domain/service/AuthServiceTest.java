package com.petcare.backend.domain.service;

import com.petcare.backend.domain.dto.request.LoginRequest;
import com.petcare.backend.domain.dto.request.DuenioRequest;
import com.petcare.backend.domain.dto.request.RefreshTokenRequest;
import com.petcare.backend.domain.dto.request.RegisterRequest;
import com.petcare.backend.domain.dto.response.AuthResponse;
import com.petcare.backend.domain.dto.response.DuenioResponse;
import com.petcare.backend.domain.dto.response.UserResponse;
import com.petcare.backend.domain.repository.ClinicaRepository;
import com.petcare.backend.domain.repository.DuenioRepository;
import com.petcare.backend.persistence.enums.EstadoClinica;
import com.petcare.backend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AuthServiceTest {

	@Autowired
	private AuthService authService;

	@Autowired
	private DuenioService duenioService;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private ClinicaService clinicaService;

	@Autowired
	private ClinicaRepository clinicaRepository;

	@Autowired
	private DuenioRepository duenioRepository;

	private Long clinicaId() {
		return clinicaService.getOrCreateDefaultClinic().getId();
	}

	@Test
	void registerFirstUserAsDuenioAndLogin() {
		RegisterRequest registerRequest = new RegisterRequest(
				"Administrador PetCare",
				"admin@petcare.test",
				"admin123",
				"000000000"
		);

		AuthResponse registerResponse = authService.register(registerRequest);
		AuthResponse loginResponse = authService.login(new LoginRequest("admin@petcare.test", "admin123"));
		UserResponse currentUser = authService.me("admin@petcare.test");

		assertThat(registerResponse.accessToken()).isNotBlank();
		assertThat(registerResponse.refreshToken()).isNotBlank();
		assertThat(registerResponse.expiresInSeconds()).isEqualTo(3600);
		assertThat(registerResponse.user().roles()).containsExactly("ROLE_DUENIO");
		assertThat(loginResponse.accessToken()).isNotBlank();
		assertThat(loginResponse.refreshToken()).isNotBlank();
		assertThat(currentUser.email()).isEqualTo("admin@petcare.test");
		assertThat(currentUser.roles()).containsExactly("ROLE_DUENIO");
	}

	@Test
	void refreshTokenRenewsAccessTokenButIsNotAcceptedAsAccessToken() {
		AuthResponse registerResponse = authService.register(new RegisterRequest(
				"Administrador PetCare",
				"admin.refresh@test.com",
				"admin123",
				"000000000"
		));

		AuthResponse refreshResponse = authService.refresh(new RefreshTokenRequest(registerResponse.refreshToken()));

		assertThat(refreshResponse.accessToken()).isNotBlank();
		assertThat(refreshResponse.refreshToken()).isNotBlank();
		assertThat(refreshResponse.expiresInSeconds()).isEqualTo(3600);
		assertThat(refreshResponse.user().email()).isEqualTo("admin.refresh@test.com");
		assertThat(jwtService.isValidRefreshToken(registerResponse.refreshToken())).isTrue();
		assertThat(jwtService.isValidAccessToken(registerResponse.refreshToken())).isFalse();
	}

	@Test
	void registerDuenioUserLinksExistingDuenioByEmail() {
		DuenioResponse duenio = duenioService.create(new DuenioRequest(
				null,
				"Cliente",
				"Sin Cuenta",
				"DNI",
				"70010001",
				"999123456",
				"cliente.link.auth@test.com",
				null
		), clinicaId());

		AuthResponse ownerUser = authService.register(new RegisterRequest(
				"Cliente Sin Cuenta",
				"cliente.link.auth@test.com",
				"owner123",
				"000000000"
		));

		DuenioResponse linked = duenioService.findOwn(ownerUser.user().email());
		assertThat(ownerUser.user().roles()).contains("ROLE_DUENIO");
		assertThat(linked.id()).isEqualTo(duenio.id());
		assertThat(linked.usuarioId()).isEqualTo(ownerUser.user().id());
	}

	@Test
	void registrationCreatesLocalOwnerWhenSameEmailExistsInAnotherClinic() {
		var otherClinic = clinicaService.createClinic("Other", "auth-other");
		DuenioResponse other = duenioService.create(new DuenioRequest(null, "Other", "Owner", "DNI",
				"70010002", "999123456", "shared.auth@test.com", null), otherClinic.getId());
		AuthResponse registered = authService.register(new RegisterRequest("Local Owner", other.email(), "secret123", "000000000"));
		DuenioResponse local = duenioService.findOwn(registered.user().email());
		assertThat(local.id()).isNotEqualTo(other.id());
		assertThat(local.usuarioId()).isEqualTo(registered.user().id());
		assertThat(duenioService.findById(other.id(), otherClinic.getId()).usuarioId()).isNull();
		assertThat(duenioRepository.findById(local.id()).orElseThrow().getClinica().getId()).isEqualTo(clinicaId());
		assertThatThrownBy(() -> authService.register(new RegisterRequest("Duplicate", other.email(), "secret123", "000000000", otherClinic.getSlug())))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("correo");
	}

	@Test
	void registrationLinksOnlyOwnerInSelectedClinic() {
		var clinic = clinicaService.createClinic("Selected", "selected-auth");
		DuenioRequest request = new DuenioRequest(null, "Shared", "Owner", "DNI",
				"70010003", "999123456", "local-link@test.com", null);
		DuenioResponse other = duenioService.create(request, clinicaId());
		DuenioResponse selected = duenioService.create(request, clinic.getId());
		AuthResponse registered = authService.register(new RegisterRequest("Shared Owner", request.email(), "secret123", "000000000", " SELECTED-AUTH "));
		assertThat(duenioService.findOwn(registered.user().email()).id()).isEqualTo(selected.id());
		assertThat(duenioService.findById(other.id(), clinicaId()).usuarioId()).isNull();
	}

	@Test
	void registrationRejectsInactiveDemoAndExplicitClinic() {
		var demo = clinicaService.getOrCreateDefaultClinic();
		demo.setEstado(EstadoClinica.SUSPENDIDA);
		clinicaRepository.save(demo);
		for (String slug : new String[]{null, " ", "demo"}) {
			assertThatThrownBy(() -> authService.register(new RegisterRequest("Owner", "inactive@test.com", "secret123", "000000000", slug)))
					.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("no esta activa");
		}
	}
}
