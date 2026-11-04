package com.petcare.backend.security;

import com.petcare.backend.persistence.entity.Usuario;
import com.petcare.backend.persistence.enums.RoleName;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

public final class SaasOperatorPolicy {
    private SaasOperatorPolicy() {}

    public static boolean hasOperatorRole(Usuario usuario) {
        return usuario.getRoles().stream().anyMatch(role -> role.getName() == RoleName.ROLE_SAAS_OPERATOR);
    }

    public static void assertValidAccount(Usuario usuario) {
        if (hasOperatorRole(usuario) && (usuario.getClinica() != null || usuario.getRoles().size() != 1
                || usuario.getRoles().stream().anyMatch(role -> !Boolean.TRUE.equals(role.getActive())))) {
            throw new AccessDeniedException("El operador SaaS debe tener un unico rol y no pertenecer a una clinica.");
        }
    }

    public static boolean isOperator(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals(RoleName.ROLE_SAAS_OPERATOR.name()));
    }

    public static boolean isSaasPath(String path) {
        return path.equals("/api/saas") || path.startsWith("/api/saas/");
    }

    public static boolean allows(Authentication authentication, String method, String path) {
        if (!authentication.isAuthenticated() || authentication.getAuthorities().size() != 1) return false;
        if (isSaasPath(path)) {
            return authentication.getPrincipal() instanceof UserDetails details && details.isCredentialsNonExpired();
        }
        return (method.equals("GET") && path.equals("/api/auth/me"))
                || (method.equals("POST") && java.util.Set.of("/api/auth/login", "/api/auth/refresh",
                "/api/auth/change-password", "/api/auth/logout").contains(path));
    }
}
