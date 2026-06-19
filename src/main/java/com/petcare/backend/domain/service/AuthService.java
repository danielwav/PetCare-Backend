package com.petcare.backend.domain.service;

import com.petcare.backend.domain.dto.request.CreateInternalUserRequest;
import com.petcare.backend.domain.dto.request.LoginRequest;
import com.petcare.backend.domain.dto.request.RefreshTokenRequest;
import com.petcare.backend.domain.dto.request.RegisterRequest;
import com.petcare.backend.domain.dto.response.AuthResponse;
import com.petcare.backend.domain.dto.response.UserResponse;
import com.petcare.backend.domain.repository.DuenioRepository;
import com.petcare.backend.domain.repository.RolRepository;
import com.petcare.backend.domain.repository.UsuarioRepository;
import com.petcare.backend.persistence.entity.Duenio;
import com.petcare.backend.persistence.entity.Rol;
import com.petcare.backend.persistence.entity.Usuario;
import com.petcare.backend.persistence.enums.RoleName;
import com.petcare.backend.security.JwtProperties;
import com.petcare.backend.security.JwtService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;
	private final PasswordEncoder passwordEncoder;
	private final RolRepository rolRepository;
	private final UsuarioRepository usuarioRepository;
	private final DuenioRepository duenioRepository;
	private final JwtProperties jwtProperties;
	private final EmailService emailService;

	@Transactional
	public AuthResponse register(RegisterRequest request) {
		String email = request.email().toLowerCase();
		if (usuarioRepository.existsByEmail(email)) {
			throw new IllegalArgumentException("El correo ya esta registrado.");
		}

		RoleName roleName = RoleName.ROLE_DUENIO;
		Rol role = rolRepository.findByName(roleName)
				.orElseThrow(() -> new IllegalStateException("Rol base no encontrado: " + roleName));

		Usuario usuario = Usuario.builder()
				.fullName(request.fullName())
				.email(email)
				.password(passwordEncoder.encode(request.password()))
				.active(true)
				.createdAt(LocalDateTime.now())
				.roles(Set.of(role))
				.build();

		Usuario savedUser = usuarioRepository.save(usuario);
		linkExistingDuenioIfNeeded(savedUser, roleName);
		return buildAuthResponse(savedUser);
	}

	public AuthResponse login(LoginRequest request) {
		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(request.email().toLowerCase(), request.password())
		);

		String email = authentication.getName();
		Usuario usuario = findByEmail(email);
		return buildAuthResponse(usuario);
	}

	public AuthResponse refresh(RefreshTokenRequest request) {
		if (!jwtService.isValidRefreshToken(request.refreshToken())) {
			throw new IllegalArgumentException("Refresh token invalido o expirado.");
		}
		Usuario usuario = findByEmail(jwtService.extractSubject(request.refreshToken()));
		if (!usuario.getActive()) {
			throw new UsernameNotFoundException("Usuario inactivo.");
		}
		return buildAuthResponse(usuario);
	}

	public UserResponse me(String email) {
		return toUserResponse(findByEmail(email));
	}

	@Transactional
	public String createInternalUser(CreateInternalUserRequest request) {
		String email = request.email().toLowerCase();
		if (usuarioRepository.existsByEmail(email)) {
			throw new IllegalArgumentException("El correo ya esta registrado.");
		}

		String roleStr = request.rol().toUpperCase();
		RoleName roleName;
		try {
			roleName = RoleName.valueOf("ROLE_" + roleStr);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Rol invalido. Use VETERINARIO o ASISTENTE.");
		}

		if (roleName == RoleName.ROLE_ADMIN || roleName == RoleName.ROLE_DUENIO) {
			throw new IllegalArgumentException("No puedes asignar este rol desde aqui.");
		}

		Rol role = rolRepository.findByName(roleName)
				.orElseThrow(() -> new IllegalStateException("Rol no encontrado: " + roleName));

		String token = UUID.randomUUID().toString();
		String fullName = request.nombres() + " " + request.apellidos();

		Usuario usuario = Usuario.builder()
				.fullName(fullName)
				.email(email)
				.password(passwordEncoder.encode(token))
				.active(false)
				.activationToken(token)
				.tokenExpiry(LocalDateTime.now().plusDays(7))
				.createdAt(LocalDateTime.now())
				.roles(Set.of(role))
				.build();

		usuarioRepository.save(usuario);
		emailService.sendActivationEmail(email, fullName, token);
		return token;
	}

	@Transactional(readOnly = true)
	public Map<String, Object> validateActivationToken(String token) {
		Usuario usuario = usuarioRepository.findByActivationToken(token)
				.orElse(null);

		if (usuario == null) {
			return Map.of("valid", false, "reason", "INVALID_TOKEN");
		}

		if (usuario.getTokenExpiry() == null || usuario.getTokenExpiry().isBefore(LocalDateTime.now())) {
			return Map.of("valid", false, "reason", "EXPIRED_TOKEN");
		}

		if (usuario.getActive()) {
			return Map.of("valid", false, "reason", "ALREADY_ACTIVE");
		}

		return Map.of("valid", true, "email", usuario.getEmail(), "nombre", usuario.getFullName());
	}

	@Transactional
	public void activateWithToken(String token, String password) {
		Usuario usuario = usuarioRepository.findByActivationToken(token)
				.orElseThrow(() -> new IllegalArgumentException("Token de activacion invalido."));

		if (usuario.getTokenExpiry() == null || usuario.getTokenExpiry().isBefore(LocalDateTime.now())) {
			throw new IllegalArgumentException("El token de activacion ha expirado.");
		}

		if (usuario.getActive()) {
			throw new IllegalArgumentException("La cuenta ya esta activa.");
		}

		if (password == null || password.length() < 8) {
			throw new IllegalArgumentException("La contrasena debe tener al menos 8 caracteres.");
		}

		usuario.setPassword(passwordEncoder.encode(password));
		usuario.setActive(true);
		usuario.setActivationToken(null);
		usuario.setTokenExpiry(null);
		usuarioRepository.save(usuario);
	}

	private Usuario findByEmail(String email) {
		return usuarioRepository.findByEmail(email.toLowerCase())
				.orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado."));
	}

	private void linkExistingDuenioIfNeeded(Usuario usuario, RoleName roleName) {
		if (roleName != RoleName.ROLE_DUENIO) {
			return;
		}

		duenioRepository.findByEmail(usuario.getEmail())
				.ifPresentOrElse(
						duenio -> linkDuenioToUser(duenio, usuario),
						() -> createDuenioForUser(usuario)
				);
	}

	@Transactional
	protected void createDuenioForUser(Usuario usuario) {
		String fullName = usuario.getFullName();
		String[] parts = fullName.trim().split("\\s+", 2);
		String nombres = parts[0];
		String apellidos = parts.length > 1 ? parts[1] : "";

		Duenio duenio = Duenio.builder()
				.usuario(usuario)
				.nombres(nombres)
				.apellidos(apellidos)
				.tipoDocumento("PENDIENTE")
				.numeroDocumento("PENDIENTE-" + usuario.getId())
				.telefono("000000000")
				.email(usuario.getEmail())
				.active(true)
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.build();
		duenioRepository.save(duenio);
	}

	private void linkDuenioToUser(Duenio duenio, Usuario usuario) {
		if (duenio.getUsuario() != null) {
			throw new IllegalArgumentException("El duenio ya esta relacionado a otro usuario.");
		}
		duenio.setUsuario(usuario);
		duenio.setUpdatedAt(LocalDateTime.now());
		duenioRepository.save(duenio);
	}

	private UserResponse toUserResponse(Usuario usuario) {
		Set<String> roles = usuario.getRoles().stream()
				.map(role -> role.getName().name())
				.collect(Collectors.toSet());

		return new UserResponse(
				usuario.getId(),
				usuario.getFullName(),
				usuario.getEmail(),
				usuario.getActive(),
				roles
		);
	}

	private AuthResponse buildAuthResponse(Usuario usuario) {
		String accessToken = jwtService.generateAccessToken(usuario.getEmail());
		String refreshToken = jwtService.generateRefreshToken(usuario.getEmail());
		return new AuthResponse(
				accessToken,
				refreshToken,
				jwtProperties.accessExpirationMs() / 1000,
				toUserResponse(usuario)
		);
	}
}
