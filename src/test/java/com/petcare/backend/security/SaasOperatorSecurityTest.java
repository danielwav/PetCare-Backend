package com.petcare.backend.security;

import com.petcare.backend.domain.dto.request.CreateInternalUserRequest;
import com.petcare.backend.domain.dto.request.LoginRequest;
import com.petcare.backend.domain.dto.request.RefreshTokenRequest;
import com.petcare.backend.domain.dto.request.UpdateUserRequest;
import com.petcare.backend.domain.dto.request.UpdateUserRolesRequest;
import com.petcare.backend.domain.repository.RolRepository;
import com.petcare.backend.domain.repository.UsuarioRepository;
import com.petcare.backend.domain.service.AuthService;
import com.petcare.backend.domain.service.UsuarioService;
import com.petcare.backend.persistence.entity.Clinica;
import com.petcare.backend.persistence.entity.Rol;
import com.petcare.backend.persistence.entity.Usuario;
import com.petcare.backend.persistence.enums.EstadoClinica;
import com.petcare.backend.persistence.enums.PlanClinica;
import com.petcare.backend.persistence.enums.RoleName;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SaasOperatorSecurityTest {
    @Autowired MockMvc mvc;
    @Autowired EntityManager em;
    @Autowired RolRepository roles;
    @Autowired UsuarioRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtService jwt;
    @Autowired AuthService auth;
    @Autowired UsuarioService usuarios;

    @Test
    void everyTenantRoleIsForbiddenFromAllSaasOperations() throws Exception {
        for (String role : new String[]{"ADMIN", "VETERINARIO", "ASISTENTE", "DUENIO"}) {
            mvc.perform(get("/api/saas/clinicas").with(user("tenant@test.com").roles(role)))
                    .andExpect(status().isForbidden());
            mvc.perform(get("/api/saas/clinicas/1/auditoria").with(user("tenant@test.com").roles(role)))
                    .andExpect(status().isForbidden());
            for (String action : new String[]{"plan", "estado"}) {
                mvc.perform(patch("/api/saas/clinicas/1/" + action).with(user("tenant@test.com").roles(role))
                        .contentType("application/json").content("{}"))
                        .andExpect(status().isForbidden());
            }
        }
        mvc.perform(get("/api/saas/clinicas")).andExpect(status().isForbidden());
    }

    @Test
    void dedicatedOperatorCanLoginRefreshReadAndChangePasswordWithoutClinic() throws Exception {
        Usuario operator = account("operator@test.com", null, RoleName.ROLE_SAAS_OPERATOR);
        operator.setPassword(encoder.encode("Password123!"));
        var login = auth.login(new LoginRequest(operator.getEmail(), "Password123!"));
        assertThat(login.user().clinica()).isNull();
        assertThat(login.user().roles()).containsExactly("ROLE_SAAS_OPERATOR");
        assertThat(jwt.extractUserId(login.accessToken())).isEqualTo(operator.getId());
        var refreshed = auth.refresh(new RefreshTokenRequest(login.refreshToken()));
        assertThat(refreshed.user().id()).isEqualTo(operator.getId());
        mvc.perform(get("/api/saas/clinicas").header("Authorization", "Bearer " + refreshed.accessToken()))
                .andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + login.accessToken()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.clinica").isEmpty());
        mvc.perform(post("/api/auth/change-password").header("Authorization", "Bearer " + login.accessToken())
                .contentType("application/json")
                .content("{\"currentPassword\":\"Password123!\",\"newPassword\":\"Changed123!\"}"))
                .andExpect(status().isOk());
        assertThat(encoder.matches("Changed123!", operator.getPassword())).isTrue();
    }

    @Test
    void cliniclessOperatorCanWriteOnlyDedicatedSaasHandlerEvenForReadOnlyClinic() throws Exception {
        Clinica clinic = clinic("expired", PlanClinica.FREE);
        Usuario operator = account("writer@test.com", null, RoleName.ROLE_SAAS_OPERATOR);
        String token = jwt.generateAccessToken(operator.getEmail(), operator.getId());
        mvc.perform(patch("/api/saas/clinicas/" + clinic.getId() + "/estado")
                .header("Authorization", "Bearer " + token).contentType("application/json")
                .content("{\"estado\":\"SUSPENDIDA\",\"reason\":\"Security test suspension\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("SUSPENDIDA"));
        mvc.perform(post("/api/mascotas").header("Authorization", "Bearer " + token)
                .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        Usuario tenant = account("tenant@test.com", clinic, RoleName.ROLE_ADMIN);
        mvc.perform(post("/api/mascotas").with(user(tenant.getEmail()).roles("ADMIN")))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CLINIC_READ_ONLY"));
    }

    @Test
    void operatorIsGloballyDeniedClinicPublicRegistrationAndLookalikePaths() throws Exception {
        Usuario operator = account("isolated@test.com", null, RoleName.ROLE_SAAS_OPERATOR);
        String token = jwt.generateAccessToken(operator.getEmail(), operator.getId());
        for (String path : new String[]{"/api/mascotas", "/api/duenios", "/api/citas", "/api/clinicas/me",
                "/api/usuarios", "/api/servicios", "/api/public/clinicas", "/api/health", "/api/saas-escape",
                "/api/auth/activate-account", "/api/auth/me/extra", "/api/auth/future-business-action"}) {
            mvc.perform(get(path).header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
        }
        for (String path : new String[]{"/api/auth/register", "/api/auth/register-clinic", "/api/auth/set-password",
                "/api/auth/activate/token", "/api/servicios/calcular-costo", "/api/saas-escape"}) {
            mvc.perform(post(path).header("Authorization", "Bearer " + token).contentType("application/json").content("{}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void mixedAuthoritiesFailClosedEvenWhereAdminOrPublicWouldNormallyPass() throws Exception {
        for (String path : new String[]{"/api/saas/clinicas", "/api/usuarios", "/api/mascotas", "/api/auth/me", "/api/health"}) {
            mvc.perform(get(path).with(user("mixed@test.com").roles("SAAS_OPERATOR", "ADMIN")))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(post("/api/auth/register").with(user("mixed@test.com").roles("SAAS_OPERATOR", "ADMIN"))
                .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidOperatorAccountsCannotLoginRefreshOrUseTokensIncludingPublicRoutes() throws Exception {
        Clinica clinic = clinic("invalid-operator", PlanClinica.PRO);
        Usuario operator = account("invalid@test.com", clinic, RoleName.ROLE_SAAS_OPERATOR);
        String token = jwt.generateAccessToken(operator.getEmail(), operator.getId());
        for (boolean mixed : new boolean[]{false, true}) {
            if (mixed) {
                operator.setClinica(null);
                operator.setRoles(roleSet(RoleName.ROLE_SAAS_OPERATOR, RoleName.ROLE_ADMIN));
            }
            assertThatThrownBy(() -> auth.login(new LoginRequest(operator.getEmail(), "password123")))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> auth.refresh(new RefreshTokenRequest(jwt.generateRefreshToken(operator.getEmail(), operator.getId()))))
                    .isInstanceOf(AccessDeniedException.class);
            mvc.perform(get("/api/saas/clinicas").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
            mvc.perform(post("/api/auth/register").header("Authorization", "Bearer " + token)
                    .contentType("application/json").content("{}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void disabledOperatorAndIdentityMismatchCannotUseAccessOrRefresh() throws Exception {
        Usuario operator = account("binding@test.com", null, RoleName.ROLE_SAAS_OPERATOR);
        String token = jwt.generateAccessToken(operator.getEmail(), operator.getId());
        operator.setActive(false);
        mvc.perform(get("/api/saas/clinicas").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/refresh").contentType("application/json").content("{\"refreshToken\":\""
                + jwt.generateRefreshToken(operator.getEmail(), operator.getId()) + "\"}"))
                .andExpect(status().isUnauthorized());
        assertThatThrownBy(() -> auth.refresh(new RefreshTokenRequest(jwt.generateRefreshToken(operator.getEmail(), operator.getId()))))
                .isInstanceOf(BadCredentialsException.class);
        operator.setActive(true);
        Usuario other = account("other@test.com", clinic("binding", PlanClinica.PRO), RoleName.ROLE_ADMIN);
        for (Long id : new Long[]{other.getId(), Long.MAX_VALUE}) {
            mvc.perform(get("/api/saas/clinicas").header("Authorization", "Bearer " + jwt.generateAccessToken(operator.getEmail(), id)))
                    .andExpect(status().isForbidden());
            assertThatThrownBy(() -> auth.refresh(new RefreshTokenRequest(jwt.generateRefreshToken(operator.getEmail(), id))))
                    .isInstanceOf(BadCredentialsException.class);
            mvc.perform(post("/api/auth/refresh").contentType("application/json").content("{\"refreshToken\":\""
                    + jwt.generateRefreshToken(operator.getEmail(), id) + "\"}"))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(get("/api/saas/clinicas").header("Authorization", "Bearer " + jwt.generateToken(operator.getEmail())))
                .andExpect(status().isForbidden());
        assertThatThrownBy(() -> auth.refresh(new RefreshTokenRequest(jwt.generateRefreshToken(operator.getEmail()))))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void forcedPasswordChangeBlocksSaasReadsAndWritesUntilPasswordChanges() throws Exception {
        Clinica clinic = clinic("forced-change", PlanClinica.FREE);
        Usuario operator = account("temporary@test.com", null, RoleName.ROLE_SAAS_OPERATOR);
        operator.setPassword(encoder.encode("Temporary123!"));
        operator.setForcePasswordChange(true);
        var login = auth.login(new LoginRequest(operator.getEmail(), "Temporary123!"));
        String token = login.accessToken();
        assertThat(login.user().forcePasswordChange()).isTrue();
        mvc.perform(get("/api/saas/clinicas").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/saas/clinicas/" + clinic.getId() + "/estado")
                .header("Authorization", "Bearer " + token).contentType("application/json")
                .content("{\"estado\":\"SUSPENDIDA\",\"reason\":\"Must not execute\"}"))
                .andExpect(status().isForbidden());
        assertThat(clinic.getEstado()).isEqualTo(EstadoClinica.ACTIVA);
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.forcePasswordChange").value(true));
        var refresh = auth.refresh(new RefreshTokenRequest(login.refreshToken()));
        assertThat(refresh.user().forcePasswordChange()).isTrue();
        mvc.perform(get("/api/saas/clinicas").header("Authorization", "Bearer " + refresh.accessToken()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/change-password").header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("{\"currentPassword\":\"Temporary123!\",\"newPassword\":\"Changed123!\"}"))
                .andExpect(status().isOk());
        assertThat(operator.getForcePasswordChange()).isFalse();
        mvc.perform(get("/api/saas/clinicas").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void disabledOperatorRoleCannotLoginRefreshOrUseAccessEvenOnPublicRoutes() throws Exception {
        Usuario operator = account("disabled-role@test.com", null, RoleName.ROLE_SAAS_OPERATOR);
        String access = jwt.generateAccessToken(operator.getEmail(), operator.getId());
        String refresh = jwt.generateRefreshToken(operator.getEmail(), operator.getId());
        operator.getRoles().iterator().next().setActive(false);
        em.flush();
        assertThatThrownBy(() -> auth.login(new LoginRequest(operator.getEmail(), "password123")))
                .isInstanceOf(org.springframework.security.core.AuthenticationException.class);
        assertThatThrownBy(() -> auth.refresh(new RefreshTokenRequest(refresh)))
                .isInstanceOf(AccessDeniedException.class);
        mvc.perform(get("/api/saas/clinicas").header("Authorization", "Bearer " + access))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/register").header("Authorization", "Bearer " + access)
                .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/refresh").contentType("application/json")
                .content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void subjectOnlyTenantTokensCannotBindToReusedEmailAndDisabledTenantRolesCannotAuthenticate() throws Exception {
        Usuario tenant = account("legacy@test.com", clinic("legacy-identity", PlanClinica.PRO), RoleName.ROLE_ADMIN);
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + jwt.generateToken(tenant.getEmail())))
                .andExpect(status().isForbidden());
        assertThatThrownBy(() -> auth.refresh(new RefreshTokenRequest(jwt.generateRefreshToken(tenant.getEmail()))))
                .isInstanceOf(BadCredentialsException.class);
        tenant.getRoles().iterator().next().setActive(false);
        em.flush();
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + jwt.generateAccessToken(tenant.getEmail(), tenant.getId())))
                .andExpect(status().isForbidden());
        assertThatThrownBy(() -> auth.refresh(new RefreshTokenRequest(jwt.generateRefreshToken(tenant.getEmail(), tenant.getId()))))
                .isInstanceOf(BadCredentialsException.class);
        assertThatThrownBy(() -> auth.login(new LoginRequest(tenant.getEmail(), "password123")))
                .isInstanceOf(org.springframework.security.core.AuthenticationException.class);
    }

    @Test
    void reusedAccessTokenUsesCurrentDatabaseRolesRatherThanIssuedPrivileges() throws Exception {
        Usuario operator = account("revoked@test.com", null, RoleName.ROLE_SAAS_OPERATOR);
        String token = jwt.generateAccessToken(operator.getEmail(), operator.getId());
        mvc.perform(get("/api/saas/clinicas").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        operator.setRoles(roleSet(RoleName.ROLE_DUENIO));
        em.flush();
        mvc.perform(get("/api/saas/clinicas").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.roles[0]").value("ROLE_DUENIO"));
    }

    @Test
    void refreshPreservesEmailChangesWithoutTrustingRecycledSubjectAndUsesCurrentDatabaseRoles() {
        Usuario tenant = account("old@test.com", clinic("email-change", PlanClinica.PRO), RoleName.ROLE_ADMIN);
        String refresh = jwt.generateRefreshToken(tenant.getEmail(), tenant.getId());
        tenant.setEmail("new@test.com");
        tenant.setRoles(roleSet(RoleName.ROLE_DUENIO));
        em.flush();
        var response = auth.refresh(new RefreshTokenRequest(refresh));
        assertThat(response.user().email()).isEqualTo("new@test.com");
        assertThat(response.user().roles()).containsExactly("ROLE_DUENIO");
        account("old@test.com", tenant.getClinica(), RoleName.ROLE_ADMIN);
        assertThatThrownBy(() -> auth.refresh(new RefreshTokenRequest(refresh))).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void tenantCreationAndRoleUpdateCannotGrantOperatorButOriginalAllowlistsStillWork() {
        Clinica clinic = clinic("tenant-management", PlanClinica.PRO);
        long before = users.count();
        for (String role : new String[]{"SAAS_OPERATOR", "ADMIN", "ROLE_SAAS_OPERATOR", "UNKNOWN"}) {
            assertThatThrownBy(() -> auth.createInternalUser(new CreateInternalUserRequest("New", "User", "new@test.com", role), clinic.getId()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertThat(users.count()).isEqualTo(before);
        Usuario target = account("target@test.com", clinic, RoleName.ROLE_DUENIO);
        assertThatThrownBy(() -> usuarios.updateRoles(target.getId(), new UpdateUserRolesRequest(Set.of("ROLE_SAAS_OPERATOR")), clinic.getId()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> usuarios.updateRoles(target.getId(), new UpdateUserRolesRequest(Set.of("ROLE_ADMIN", "ROLE_SAAS_OPERATOR")), clinic.getId()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(target.getRoles()).extracting(Rol::getName).containsExactly(RoleName.ROLE_DUENIO);
        for (String role : new String[]{"VETERINARIO", "ASISTENTE", "DUENIO"}) {
            auth.createInternalUser(new CreateInternalUserRequest("New", "User", role.toLowerCase() + "@test.com", role), clinic.getId());
        }
        var updated = usuarios.updateRoles(target.getId(), new UpdateUserRolesRequest(Set.of(
                "ROLE_ADMIN", "ROLE_VETERINARIO", "ROLE_ASISTENTE", "ROLE_DUENIO")), clinic.getId());
        assertThat(updated.roles()).hasSize(4).doesNotContain("ROLE_SAAS_OPERATOR");
    }

    @Test
    void tenantManagementCannotReadModifyActivateDeleteOrDemoteOperatorTargets() {
        Clinica clinic = clinic("operator-target", PlanClinica.PRO);
        Usuario target = account("protected@test.com", clinic, RoleName.ROLE_SAAS_OPERATOR, RoleName.ROLE_ADMIN);
        Long id = target.getId();
        assertThat(usuarios.findAll(clinic.getId())).isEmpty();
        assertThatThrownBy(() -> usuarios.findById(id, clinic.getId())).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> usuarios.update(id, new UpdateUserRequest("Changed", "changed@test.com", "123"), clinic.getId()))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> usuarios.updateRoles(id, new UpdateUserRolesRequest(Set.of("ROLE_DUENIO")), clinic.getId()))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> usuarios.toggleActive(id, clinic.getId())).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> usuarios.hardDelete(id, clinic.getId())).isInstanceOf(AccessDeniedException.class);
        assertThat(target.getEmail()).isEqualTo("protected@test.com");
        assertThat(target.getActive()).isTrue();
    }

    @Test
    void operatorCannotBeActivatedThroughPublicTenantActivation() {
        Usuario operator = account("activation@test.com", null, RoleName.ROLE_SAAS_OPERATOR);
        operator.setActive(false);
        operator.setActivationToken("operator-activation");
        operator.setTokenExpiry(LocalDateTime.now().plusHours(1));
        assertThat(auth.validateActivationToken("operator-activation")).containsEntry("valid", false);
        assertThatThrownBy(() -> auth.activateWithToken("operator-activation", "changed123"))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(operator.getActive()).isFalse();
    }

    private Clinica clinic(String slug, PlanClinica plan) {
        var now = LocalDateTime.now();
        Clinica clinic = Clinica.builder().nombre(slug).slug(slug).plan(plan).estado(EstadoClinica.ACTIVA)
                .createdAt(now).updatedAt(now).build();
        em.persist(clinic);
        return clinic;
    }

    private Usuario account(String email, Clinica clinic, RoleName... names) {
        return users.saveAndFlush(Usuario.builder().fullName("Security Test").email(email)
                .password(encoder.encode("password123")).active(true).createdAt(LocalDateTime.now())
                .roles(roleSet(names)).clinica(clinic).build());
    }

    private Set<Rol> roleSet(RoleName... names) {
        return Arrays.stream(names).map(name -> roles.findByName(name).orElseThrow()).collect(Collectors.toSet());
    }
}
