package com.petcare.backend.domain.service;

import com.petcare.backend.domain.dto.request.LoginRequest;
import com.petcare.backend.domain.dto.request.RegisterRequest;
import com.petcare.backend.domain.dto.response.AuthResponse;
import com.petcare.backend.domain.dto.response.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AuthServiceTest {

	@Autowired
	private AuthService authService;

	@Test
	void registerFirstUserAsAdminAndLogin() {
		RegisterRequest registerRequest = new RegisterRequest(
				"Administrador PetCare",
				"admin@petcare.test",
				"admin123"
		);

		AuthResponse registerResponse = authService.register(registerRequest);
		AuthResponse loginResponse = authService.login(new LoginRequest("admin@petcare.test", "admin123"));
		UserResponse currentUser = authService.me("admin@petcare.test");

		assertThat(registerResponse.accessToken()).isNotBlank();
		assertThat(registerResponse.user().roles()).contains("ROLE_ADMIN");
		assertThat(loginResponse.accessToken()).isNotBlank();
		assertThat(currentUser.email()).isEqualTo("admin@petcare.test");
		assertThat(currentUser.roles()).contains("ROLE_ADMIN");
	}
}
