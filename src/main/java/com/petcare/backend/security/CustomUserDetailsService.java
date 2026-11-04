package com.petcare.backend.security;

import com.petcare.backend.domain.repository.UsuarioRepository;
import com.petcare.backend.persistence.entity.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

	private final UsuarioRepository usuarioRepository;

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		Usuario usuario = usuarioRepository.findByEmail(email.toLowerCase())
				.orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado."));

		return toUserDetails(usuario);
	}

	public UserDetails loadUserById(Long id) throws UsernameNotFoundException {
		Usuario usuario = usuarioRepository.findById(id)
				.orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con id: " + id));

		return toUserDetails(usuario);
	}

	private UserDetails toUserDetails(Usuario usuario) {
		return User.builder()
				.username(usuario.getEmail())
				.password(usuario.getPassword())
				.disabled(!Boolean.TRUE.equals(usuario.getActive())
						|| usuario.getRoles().stream().noneMatch(role -> Boolean.TRUE.equals(role.getActive())))
				.authorities(usuario.getRoles().stream()
						.filter(role -> Boolean.TRUE.equals(role.getActive()))
						.map(role -> new SimpleGrantedAuthority(role.getName().name()))
						.toList())
				.build();
	}
}
