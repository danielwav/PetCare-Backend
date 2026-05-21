package com.petcare.backend.domain.service;

import com.petcare.backend.domain.dto.request.LoginRequest;
import com.petcare.backend.domain.dto.request.RegisterRequest;
import com.petcare.backend.domain.dto.response.AuthResponse;
import com.petcare.backend.domain.dto.response.UserResponse;
import com.petcare.backend.domain.repository.RolRepository;
import com.petcare.backend.domain.repository.UsuarioRepository;
import com.petcare.backend.persistence.entity.Rol;
import com.petcare.backend.persistence.entity.Usuario;
import com.petcare.backend.persistence.enums.RoleName;
import com.petcare.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;
	private final PasswordEncoder passwordEncoder;
	private final RolRepository rolRepository;
	private final UsuarioRepository usuarioRepository;

	@Transactional
	public AuthResponse register(RegisterRequest request) {
		if (usuarioRepository.existsByEmail(request.email())) {
			throw new IllegalArgumentException("El correo ya esta registrado.");
		}

		RoleName roleName = usuarioRepository.count() == 0 ? RoleName.ROLE_ADMIN : RoleName.ROLE_DUENIO;
		Rol role = rolRepository.findByName(roleName)
				.orElseThrow(() -> new IllegalStateException("Rol base no encontrado: " + roleName));

		Usuario usuario = Usuario.builder()
				.fullName(request.fullName())
				.email(request.email().toLowerCase())
				.password(passwordEncoder.encode(request.password()))
				.active(true)
				.createdAt(LocalDateTime.now())
				.roles(Set.of(role))
				.build();

		Usuario savedUser = usuarioRepository.save(usuario);
		String token = jwtService.generateToken(savedUser.getEmail());
		return new AuthResponse(token, toUserResponse(savedUser));
	}

	public AuthResponse login(LoginRequest request) {
		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(request.email().toLowerCase(), request.password())
		);

		String email = authentication.getName();
		Usuario usuario = findByEmail(email);
		String token = jwtService.generateToken(email);
		return new AuthResponse(token, toUserResponse(usuario));
	}

	public UserResponse me(String email) {
		return toUserResponse(findByEmail(email));
	}

	private Usuario findByEmail(String email) {
		return usuarioRepository.findByEmail(email.toLowerCase())
				.orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado."));
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
}
