package com.petcare.backend.config;

import com.petcare.backend.domain.service.ClinicaService;
import com.petcare.backend.domain.service.PlanService;
import com.petcare.backend.security.SaasOperatorPolicy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Set;

@Configuration
@RequiredArgsConstructor
public class ClinicWriteGateConfig implements WebMvcConfigurer, HandlerInterceptor {
	private final ClinicaService clinicaService;
	private final PlanService planService;
	private static final Set<String> AUTH_POSTS = Set.of("/api/auth/login", "/api/auth/refresh",
			"/api/auth/register-clinic", "/api/auth/register", "/api/auth/change-password", "/api/auth/set-password");

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(this).addPathPatterns("/api/**");
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		String method = request.getMethod();
		String path = request.getRequestURI().substring(request.getContextPath().length());
		var auth = SecurityContextHolder.getContext().getAuthentication();
		if (SaasOperatorPolicy.isOperator(auth)) {
			if (!SaasOperatorPolicy.allows(auth, method, path)) {
				throw new org.springframework.security.access.AccessDeniedException("El operador SaaS no puede acceder a rutas de clinica.");
			}
			if (SaasOperatorPolicy.isSaasPath(path) && !Set.of("GET", "HEAD", "OPTIONS").contains(method)) {
				if (handler instanceof HandlerMethod hm
						&& hm.getBeanType() == com.petcare.backend.web.SaasClinicController.class) return true;
				throw new org.springframework.security.access.AccessDeniedException("Handler SaaS no autorizado.");
			}
			if (method.equals("POST") && path.equals("/api/auth/logout")) return true;
		}
		if (Set.of("GET", "HEAD", "OPTIONS").contains(method)) return true;
		if (method.equals("POST") && (AUTH_POSTS.contains(path)
				|| path.equals("/api/servicios/calcular-costo")
				|| (path.matches("/api/auth/activate/[^/]+") && handler instanceof HandlerMethod hm
						&& hm.getBeanType() == com.petcare.backend.web.AuthController.class
						&& hm.getMethod().getName().equals("activateWithToken")))) return true;
		if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
			planService.assertCanWrite(clinicaService.resolveClinicaId(auth.getName()));
		}
		return true;
	}
}
