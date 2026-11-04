package com.petcare.backend.security;

import com.petcare.backend.config.ClinicWriteGateConfig;
import com.petcare.backend.domain.dto.request.SaasClinicEstadoRequest;
import com.petcare.backend.domain.service.ClinicaService;
import com.petcare.backend.domain.service.PlanService;
import com.petcare.backend.web.SaasClinicController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.method.HandlerMethod;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SaasWriteGateTest {
    private final ClinicaService clinics = mock(ClinicaService.class);
    private final PlanService plans = mock(PlanService.class);
    private final ClinicWriteGateConfig gate = new ClinicWriteGateConfig(clinics, plans);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void exceptionRequiresOperatorAndDedicatedHandlerAndHonorsContextPath() throws Exception {
        authenticate("SAAS_OPERATOR");
        var request = new MockHttpServletRequest("PATCH", "/petcare/api/saas/clinicas/1/estado");
        request.setContextPath("/petcare");
        assertThat(gate.preHandle(request, new MockHttpServletResponse(), handler())).isTrue();
        verifyNoInteractions(clinics, plans);
    }

    @Test
    void lookalikeClinicalPathsAndMixedRolesHaveNoWriteException() throws Exception {
        authenticate("SAAS_OPERATOR");
        for (String path : new String[]{"/api/saas-escape", "/api/saasclinicas/1", "/api/mascotas", "/api/usuarios"}) {
            assertThatThrownBy(() -> gate.preHandle(new MockHttpServletRequest("PATCH", path),
                    new MockHttpServletResponse(), handler())).isInstanceOf(AccessDeniedException.class);
        }
        authenticate("SAAS_OPERATOR", "ADMIN");
        assertThatThrownBy(() -> gate.preHandle(new MockHttpServletRequest("PATCH", "/api/saas/clinicas/1/estado"),
                new MockHttpServletResponse(), handler())).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(clinics, plans);
    }

    @Test
    void unknownSaasHandlerFailsClosedRatherThanResolvingNullTenant() {
        authenticate("SAAS_OPERATOR");
        assertThatThrownBy(() -> gate.preHandle(new MockHttpServletRequest("PATCH", "/api/saas/clinicas/1/estado"),
                new MockHttpServletResponse(), new Object())).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(clinics, plans);
    }

    @Test
    void tenantAuthoritiesCannotObtainSaasWriteGateException() throws Exception {
        authenticate("ADMIN");
        when(clinics.resolveClinicaId("test@test.com")).thenReturn(42L);
        doThrow(new AccessDeniedException("read only")).when(plans).assertCanWrite(42L);
        assertThatThrownBy(() -> gate.preHandle(new MockHttpServletRequest("PATCH", "/api/saas/clinicas/1/estado"),
                new MockHttpServletResponse(), handler())).isInstanceOf(AccessDeniedException.class);
        verify(plans).assertCanWrite(42L);
    }

    private void authenticate(String... roles) {
        var details = User.withUsername("test@test.com").password("encoded").roles(roles).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }

    private HandlerMethod handler() throws NoSuchMethodException {
        return new HandlerMethod(new SaasClinicController(null), SaasClinicController.class.getMethod(
                "changeEstado", Authentication.class, Long.class, SaasClinicEstadoRequest.class));
    }
}
