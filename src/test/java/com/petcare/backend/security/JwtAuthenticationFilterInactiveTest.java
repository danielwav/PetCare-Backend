package com.petcare.backend.security;

import com.petcare.backend.domain.repository.UsuarioRepository;
import com.petcare.backend.persistence.entity.Rol;
import com.petcare.backend.persistence.entity.Usuario;
import com.petcare.backend.persistence.enums.RoleName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterInactiveTest {
    private final UsuarioRepository users = mock(UsuarioRepository.class);
    private final JwtService jwt = new JwtService(new JwtProperties(
            "UGV0Q2FyZS1EZXZlbG9wbWVudC1KMFQtU2VjcmV0LTIwMjYtU3VwZXItU2VndXJhLUNhbWJpYXJFbkVudg==", 60000, 120000));

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void disabledSubjectCannotAuthenticateOrFallBack() throws Exception {
        Usuario user = account(7L, false);
        when(users.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(users.findById(7L)).thenReturn(Optional.of(user));
        check(jwt.generateAccessToken("user@test.com", 7L), false);
    }

    @Test
    void disabledIdFallbackCannotAuthenticate() throws Exception {
        when(users.findById(7L)).thenReturn(Optional.of(account(7L, false)));
        check(jwt.generateAccessToken("old@test.com", 7L), false);
    }

    @Test
    void enabledSubjectAuthenticatesWithDatabaseRoles() throws Exception {
        Usuario user = account(7L, true);
        when(users.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(users.findById(7L)).thenReturn(Optional.of(user));
        check(jwt.generateAccessToken("user@test.com", 7L), true);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority").containsExactly("ROLE_ASISTENTE");
    }

    @Test
    void enabledIdFallbackAuthenticatesWithCurrentEmail() throws Exception {
        when(users.findById(7L)).thenReturn(Optional.of(account(7L, true)));
        check(jwt.generateAccessToken("old@test.com", 7L), true);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("user@test.com");
    }

    @Test
    void existingSubjectWithDifferentIdCannotAuthenticate() throws Exception {
        when(users.findByEmail("user@test.com")).thenReturn(Optional.of(account(8L, true)));
        check(jwt.generateAccessToken("user@test.com", 7L), false);
        verify(users, never()).findById(anyLong());
    }

    @Test
    void deletedIdDoesNotFallBackToExistingEmail() throws Exception {
        when(users.findByEmail("user@test.com")).thenReturn(Optional.of(account(7L, true)));
        check(jwt.generateAccessToken("user@test.com", 7L), false);
    }

    @Test
    void subjectOnlyTenantTokenCannotSelectARecycledEmailIdentity() throws Exception {
        check(jwt.generateToken("user@test.com"), false);
        verifyNoInteractions(users);
    }

    @Test
    void disabledTenantRoleCannotAuthenticate() throws Exception {
        Usuario user = account(7L, true);
        user.getRoles().iterator().next().setActive(false);
        when(users.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(users.findById(7L)).thenReturn(Optional.of(user));
        check(jwt.generateAccessToken("user@test.com", 7L), false);
    }

    @Test
    void disabledAuthorityIsNotGrantedAlongsideActiveRole() throws Exception {
        Usuario user = account(7L, true);
        user.setRoles(Set.of(user.getRoles().iterator().next(), Rol.builder().name(RoleName.ROLE_ADMIN).active(false).build()));
        when(users.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(users.findById(7L)).thenReturn(Optional.of(user));
        check(jwt.generateAccessToken("user@test.com", 7L), true);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority").containsExactly("ROLE_ASISTENTE");
    }

    @Test
    void refreshTokenCannotAuthenticateAsAccess() throws Exception {
        check(jwt.generateRefreshToken("user@test.com", 7L), false);
        verifyNoInteractions(users);
    }

    private void check(String token, boolean enabled) throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        var filter = new JwtAuthenticationFilter(jwt, new JwtIdentityService(jwt, users));
        filter.doFilter(request, new MockHttpServletResponse(), (req, res) ->
                assertThat(SecurityContextHolder.getContext().getAuthentication() != null).isEqualTo(enabled));
        assertThat(SecurityContextHolder.getContext().getAuthentication() != null).isEqualTo(enabled);
    }

    private Usuario account(Long id, boolean active) {
        return Usuario.builder().id(id).email("user@test.com").password("encoded").active(active)
                .roles(Set.of(Rol.builder().name(RoleName.ROLE_ASISTENTE).active(true).build())).build();
    }
}
