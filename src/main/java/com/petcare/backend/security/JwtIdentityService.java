package com.petcare.backend.security;

import com.petcare.backend.domain.repository.UsuarioRepository;
import com.petcare.backend.persistence.entity.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class JwtIdentityService {
    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public Usuario resolve(String token) {
        String subject;
        Long id;
        try {
            subject = jwtService.extractSubject(token);
            id = jwtService.extractUserId(token);
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException exception) {
            throw new BadCredentialsException("Identidad de token invalida.", exception);
        }
        if (subject == null || subject.isBlank() || id == null || id <= 0) {
            throw new BadCredentialsException("Identidad de token invalida.");
        }
        var subjectUser = usuarioRepository.findByEmail(subject.toLowerCase(Locale.ROOT));
        // Keep email-change support, but never let a recycled email select another identity.
        if (subjectUser.isPresent() && !id.equals(subjectUser.get().getId())) {
            throw new BadCredentialsException("La identidad del token no coincide.");
        }
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new BadCredentialsException("Usuario no encontrado."));
        if (!Boolean.TRUE.equals(usuario.getActive())) {
            throw new BadCredentialsException("Usuario inactivo.");
        }
        SaasOperatorPolicy.assertValidAccount(usuario);
        if (usuario.getRoles().stream().noneMatch(role -> Boolean.TRUE.equals(role.getActive()))) {
            throw new BadCredentialsException("Usuario sin roles activos.");
        }
        return usuario;
    }
}
