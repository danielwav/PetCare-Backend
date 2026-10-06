package com.petcare.backend.security;

import com.petcare.backend.domain.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterInactiveTest {
    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void disabledSubjectCannotAuthenticateOrFallBack() throws Exception {
        checkAuthentication(false, false);
    }

    @Test
    void disabledIdFallbackCannotAuthenticate() throws Exception {
        checkAuthentication(true, false);
    }

    @Test
    void enabledSubjectAuthenticatesWithoutClinicPlanCheck() throws Exception {
        checkAuthentication(false, true);
    }

    @Test
    void enabledIdFallbackAuthenticates() throws Exception {
        checkAuthentication(true, true);
    }

    private void checkAuthentication(boolean fallback, boolean enabled) throws Exception {
        JwtService jwt = mock(JwtService.class);
        CustomUserDetailsService users = mock(CustomUserDetailsService.class);
        var details = User.withUsername("user@test.com").password("encoded")
                .roles("ASISTENTE").disabled(!enabled).build();
        when(jwt.isValidAccessToken("token")).thenReturn(true);
        when(jwt.extractSubject("token")).thenReturn("old@test.com");
        if (fallback) {
            when(users.loadUserByUsername("old@test.com")).thenThrow(new UsernameNotFoundException("changed"));
            when(jwt.extractUserId("token")).thenReturn(7L);
            when(users.loadUserById(7L)).thenReturn(details);
        } else {
            when(users.loadUserByUsername("old@test.com")).thenReturn(details);
        }
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        var filter = new JwtAuthenticationFilter(jwt, users, mock(UsuarioRepository.class));
        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {
            assertThat(SecurityContextHolder.getContext().getAuthentication() != null).isEqualTo(enabled);
        });
        if (!fallback) {
            verify(users, never()).loadUserById(anyLong());
        }
    }
}
