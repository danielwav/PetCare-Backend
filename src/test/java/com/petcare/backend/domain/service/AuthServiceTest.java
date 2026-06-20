package com.petcare.backend.domain.service;

import com.petcare.backend.domain.dto.request.LoginRequest;
import com.petcare.backend.domain.dto.request.DuenioRequest;
import com.petcare.backend.domain.dto.request.RefreshTokenRequest;
import com.petcare.backend.domain.dto.request.RegisterRequest;
import com.petcare.backend.domain.dto.response.AuthResponse;
import com.petcare.backend.domain.dto.response.DuenioResponse;
import com.petcare.backend.domain.dto.response.UserResponse;
import com.petcare.backend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

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

	@Test
	void registerFirstUserAsAdminAndLogin() {
		RegisterRequest registerRequest = new RegisterRequest(
				"Administrador PetCare",
				"admin@petcare.test",
				"000000000",
				"admin123"
		);

		AuthResponse registerResponse = authService.register(registerRequest);
		AuthResponse loginResponse = authService.login(new LoginRequest("admin@petcare.test", "admin123"));
		UserResponse currentUser = authService.me("admin@petcare.test");

		assertThat(registerResponse.accessToken()).isNotBlank();
		assertThat(registerResponse.refreshToken()).isNotBlank();
		assertThat(registerResponse.expiresInSeconds()).isEqualTo(3600);
		assertThat(registerResponse.user().roles()).contains("ROLE_ADMIN");
		assertThat(loginResponse.accessToken()).isNotBlank();
		assertThat(loginResponse.refreshToken()).isNotBlank();
		assertThat(currentUser.email()).isEqualTo("admin@petcare.test");
		assertThat(currentUser.roles()).contains("ROLE_ADMIN");
	}

	@Test
	void refreshTokenRenewsAccessTokenButIsNotAcceptedAsAccessToken() {
		AuthResponse registerResponse = authService.register(new RegisterRequest(
				"Administrador PetCare",
				"admin.refresh@test.com",
				"000000000",
				"admin123"
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
		authService.register(new RegisterRequest("Administrador PetCare", "admin.link.auth@test.com", "000000000", "admin123"));
		DuenioResponse duenio = duenioService.create(new DuenioRequest(
				null,
				"Cliente",
				"Sin Cuenta",
				"DNI",
				"70010001",
				"999123456",
				"cliente.link.auth@test.com",
				null
		));

		AuthResponse ownerUser = authService.register(new RegisterRequest(
				"Cliente Sin Cuenta",
				"cliente.link.auth@test.com",
				"000000000",
				"owner123"
		));

		DuenioResponse linked = duenioService.findOwn(ownerUser.user().email());
		assertThat(ownerUser.user().roles()).contains("ROLE_DUENIO");
		assertThat(linked.id()).isEqualTo(duenio.id());
		assertThat(linked.usuarioId()).isEqualTo(ownerUser.user().id());
	}
}
