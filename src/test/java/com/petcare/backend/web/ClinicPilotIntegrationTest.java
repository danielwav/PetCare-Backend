package com.petcare.backend.web;

import com.petcare.backend.domain.repository.RolRepository;
import com.petcare.backend.domain.repository.UsuarioRepository;
import com.petcare.backend.persistence.entity.Cita;
import com.petcare.backend.persistence.entity.Rol;
import com.petcare.backend.persistence.entity.Usuario;
import com.petcare.backend.persistence.enums.RoleName;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@ActiveProfiles("test")
@Transactional
class ClinicPilotIntegrationTest {
    private static final String PASSWORD = "PilotPassword123!";
    private static final String PHONE = "+51987654321";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired EntityManager em;
    @Autowired RolRepository roles;
    @Autowired UsuarioRepository users;
    @Autowired PasswordEncoder encoder;

    private Clinic first;
    private Clinic second;

    @BeforeEach
    void registerIndependentClinics() throws Exception {
        for (RoleName name : RoleName.values()) {
            if (roles.findByName(name).isEmpty()) {
                roles.saveAndFlush(Rol.builder().name(name).description("Pilot integration role").active(true).build());
            }
        }
        first = registerClinic();
        second = registerClinic();
        assertThat(first.id()).isNotEqualTo(second.id());
        assertThat(first.slug()).isNotEqualTo(second.slug());
    }

    @Test
    void fullClinicalPipelineUsesRealLoginAndPreservesOwnerHistoryAndCostSnapshots() throws Exception {
        Pipeline p = pipeline(first);
        Account assistant = staff(first, "ASISTENTE");
        JsonNode assistantProfile = call(POST, "/api/asistentes", first.token(), Map.of(
                "usuarioId", assistant.id(), "tipoDocumento", "DNI", "numeroDocumento", unique(),
                "telefono", "987654321", "email", assistant.email(), "funciones", "Recepcion y seguimiento"), 201);
        assertThat(assistantProfile.path("usuarioId").asLong()).isEqualTo(assistant.id());

        JsonNode calculation = call(POST, "/api/servicios/calcular-costo", assistant.token(), Map.of(
                "servicios", services(p.service()), "descuento", 10), 200);
        assertMoney(calculation, "subtotal", "150.00");
        assertMoney(calculation, "total", "140.00");
        JsonNode confirmed = call(PATCH, "/api/citas/" + p.appointment() + "/confirmar", p.owner().token(), null, 200);
        assertThat(confirmed.path("estado").asText()).isEqualTo("CONFIRMADA");
        assertThat(confirmed.path("confirmadaPor").asText()).isEqualTo(p.owner().email());

        call(POST, "/api/citas/" + p.appointment() + "/atencion", p.vet().token(), attention(), 400);
        assertThat(call(GET, "/api/citas/" + p.appointment(), first.token(), null, 200)
                .path("estado").asText()).isEqualTo("CONFIRMADA");
        assertThat(call(GET, "/api/mascotas/" + p.pet() + "/historia-clinica", first.token(), null, 200)
                .path("atenciones").size()).isZero();

        moveAppointmentIntoPastForTestOnly(p.appointment());
        JsonNode attention = call(POST, "/api/citas/" + p.appointment() + "/atencion", p.vet().token(), attention(), 201);
        long attentionId = attention.path("id").asLong();
        assertThat(attention.path("notasInternas").asText()).isEqualTo("Staff-only pilot note");
        call(POST, "/api/citas/" + p.appointment() + "/atencion", p.vet().token(), attention(), 400);
        call(POST, "/api/notas-seguimiento", assistant.token(), Map.of(
                "citaId", p.appointment(), "observacion", "Seguimiento telefonico completado"), 201);
        assertThat(call(GET, "/api/notas-seguimiento/" + p.appointment(), assistant.token(), null, 200).size()).isEqualTo(1);

        long vaccine = call(POST, "/api/vacunas", first.token(), Map.of(
                "nombre", "Rabia " + unique(), "descripcion", "Vacuna piloto", "intervaloProximaDosisDias", 30), 201)
                .path("id").asLong();
        JsonNode vaccination = call(POST, "/api/mascotas/" + p.pet() + "/vacunas", p.vet().token(), Map.of(
                "vacunaId", vaccine, "veterinarioId", p.veterinarian(), "citaId", p.appointment(),
                "fechaAplicacion", LocalDate.now(), "lote", "PILOT-LOT"), 201);
        assertThat(vaccination.path("fechaProximaDosis").asText()).isEqualTo(LocalDate.now().plusDays(30).toString());

        // Re-read persisted state rather than relying on managed entities or POST responses.
        em.flush();
        em.clear();
        assertThat(call(GET, "/api/citas/" + p.appointment(), p.owner().token(), null, 200)
                .path("estado").asText()).isEqualTo("ATENDIDA");
        JsonNode staffHistory = call(GET, "/api/mascotas/" + p.pet() + "/historia-clinica", p.vet().token(), null, 200);
        assertThat(staffHistory.path("estado").asText()).isEqualTo("SALUDABLE");
        assertThat(staffHistory.path("atenciones").size()).isEqualTo(1);
        assertThat(staffHistory.path("atenciones").get(0).path("id").asLong()).isEqualTo(attentionId);
        assertThat(call(GET, "/api/atenciones/" + attentionId, p.owner().token(), null, 200)
                .path("notasInternas").isNull()).isTrue();
        JsonNode ownerHistory = call(GET, "/api/mascotas/" + p.pet() + "/historia-clinica", p.owner().token(), null, 200);
        assertThat(ownerHistory.path("atenciones").get(0).path("diagnostico").asText()).isEqualTo("Paciente saludable");
        assertThat(ownerHistory.toString()).doesNotContain("Staff-only pilot note");
        JsonNode vaccineReadback = call(GET, "/api/mascotas/" + p.pet() + "/vacunas", p.owner().token(), null, 200);
        assertThat(vaccineReadback.size()).isEqualTo(1);
        assertThat(vaccineReadback.get(0).path("id").asLong()).isEqualTo(vaccination.path("id").asLong());

        call(PUT, "/api/servicios/" + p.service(), first.token(), Map.of(
                "nombre", "Consulta actualizada " + unique(), "descripcion", "Nuevo precio", "costoBase", 99), 200);
        JsonNode costReport = call(GET, "/api/reportes/citas/" + p.appointment() + "/costos", assistant.token(), null, 200);
        assertMoney(costReport, "subtotal", "150.00");
        assertMoney(costReport, "descuento", "10.00");
        assertMoney(costReport, "total", "140.00");
        JsonNode report = call(GET, "/api/reportes/citas?estado=ATENDIDA&mascotaId=" + p.pet(), first.token(), null, 200);
        assertThat(report.size()).isEqualTo(1);
        assertThat(report.get(0).path("id").asLong()).isEqualTo(p.appointment());
        JsonNode historyReport = call(GET, "/api/reportes/mascotas/" + p.pet() + "/historia-clinica", first.token(), null, 200);
        assertThat(historyReport.path("atenciones").get(0).path("id").asLong()).isEqualTo(attentionId);
        assertThat(call(GET, "/api/reportes/vacunas-proximas", first.token(), null, 200).size()).isEqualTo(1);
        assertThat(call(GET, "/api/reportes/citas", second.token(), null, 200).size()).isZero();
    }

    @Test
    void assistantProfilePersistsInternationalPhoneAcceptedByRequestValidation() throws Exception {
        Account assistant = staff(first, "ASISTENTE");
        JsonNode profile = call(POST, "/api/asistentes", first.token(), Map.of(
                "usuarioId", assistant.id(), "tipoDocumento", "DNI", "numeroDocumento", unique(),
                "telefono", PHONE, "email", assistant.email(), "funciones", "Recepcion"), 201);
        assertThat(profile.path("telefono").asText()).isEqualTo(PHONE);
    }

    @Test
    void temporaryStaffCredentialsCannotReadClinicalDataBeforePasswordChange() throws Exception {
        String email = email("temporary-staff");
        JsonNode created = call(POST, "/api/usuarios/internal", first.token(), internalUser(email, "VETERINARIO"), 201);
        JsonNode login = login(email, created.path("temporaryPassword").asText());
        assertThat(login.path("user").path("forcePasswordChange").asBoolean()).isTrue();
        call(GET, "/api/mascotas", login.path("accessToken").asText(), null, 403);
        String token = login.path("accessToken").asText();
        call(GET, "/api/auth/me", token, null, 200);
        call(POST, "/api/auth/refresh", null, Map.of("refreshToken", login.path("refreshToken").asText()), 200);
        call(POST, "/api/auth/change-password", token, Map.of(
                "currentPassword", created.path("temporaryPassword").asText(), "newPassword", PASSWORD), 200);
        call(GET, "/api/mascotas", token, null, 200);
    }

    @Test
    void existingShortPasswordCanBeReplacedWithAStrongPassword() throws Exception {
        String email = email("legacy-password");
        JsonNode registered = call(POST, "/api/auth/register-clinic", null, Map.of(
                "fullName", "Legacy Admin", "email", email, "password", "legacy",
                "telefono", PHONE, "clinicaNombre", "Legacy " + unique()), 201);
        call(POST, "/api/auth/change-password", registered.path("accessToken").asText(), Map.of(
                "currentPassword", "legacy", "newPassword", PASSWORD), 200);
        assertThat(login(email, PASSWORD).path("user").path("id")).isEqualTo(registered.path("user").path("id"));
    }

    @Test
    void trialAllowsFiveStaffWhileOwnerRegistrationConsumesNoStaffSeat() throws Exception {
        assertThat(plan(first).path("staffUsed").asInt()).isEqualTo(1);
        for (int i = 0; i < 4; i++) {
            staff(first, i == 0 ? "VETERINARIO" : "ASISTENTE");
        }
        JsonNode full = plan(first);
        assertThat(full.path("staffUsed").asInt()).isEqualTo(5);
        assertThat(full.path("staffLimit").asInt()).isEqualTo(5);
        JsonNode usersBefore = call(GET, "/api/usuarios", first.token(), null, 200);
        String rejectedEmail = email("over-quota");
        JsonNode rejected = call(POST, "/api/usuarios/internal", first.token(), internalUser(rejectedEmail, "ASISTENTE"), 409);
        assertThat(rejected.path("code").asText()).isEqualTo("STAFF_LIMIT_REACHED");
        assertThat(users.existsByEmail(rejectedEmail)).isFalse();
        assertThat(call(GET, "/api/usuarios", first.token(), null, 200)).isEqualTo(usersBefore);

        registerOwner(first);
        assertThat(plan(first).path("staffUsed").asInt()).isEqualTo(5);
        assertThat(call(GET, "/api/usuarios", first.token(), null, 200).size()).isEqualTo(6);
        staff(second, "VETERINARIO");
        assertThat(plan(second).path("staffUsed").asInt()).isEqualTo(2);
    }

    @Test
    void foreignDirectReadsAndForgedRelationsAreForbiddenWithoutChangingEitherClinic() throws Exception {
        Pipeline a = pipeline(first);
        Pipeline b = pipeline(second);
        Map<String, JsonNode> before = snapshot(first, second);
        for (String path : List.of("/api/duenios/" + a.ownerId(), "/api/mascotas/" + a.pet(),
                "/api/veterinarios/" + a.veterinarian(), "/api/servicios/" + a.service(),
                "/api/citas/" + a.appointment(), "/api/usuarios/" + a.vet().id(),
                "/api/mascotas/" + a.pet() + "/historia-clinica", "/api/mascotas/" + a.pet() + "/vacunas",
                "/api/reportes/citas/" + a.appointment() + "/costos")) {
            call(GET, path, second.token(), null, 403);
        }
        call(POST, "/api/mascotas", second.token(), petRequest(a.ownerId()), 403);
        call(PUT, "/api/mascotas/" + b.pet(), second.token(), petRequest(a.ownerId()), 403);
        call(PUT, "/api/duenios/" + b.ownerId(), second.token(), Map.of(
                "usuarioId", a.owner().id(), "nombres", "Forged", "apellidos", "Owner",
                "tipoDocumento", "DNI", "numeroDocumento", unique(), "telefono", PHONE, "email", b.owner().email()), 403);
        call(POST, "/api/veterinarios", second.token(), vetRequest(a.vet()), 403);
        for (String field : List.of("duenioId", "mascotaId", "veterinarioId", "servicios")) {
            Map<String, Object> forged = new LinkedHashMap<>(appointmentRequest(b));
            forged.put(field, switch (field) {
                case "duenioId" -> a.ownerId();
                case "mascotaId" -> a.pet();
                case "veterinarioId" -> a.veterinarian();
                default -> services(a.service());
            });
            // A different time avoids confusing relation validation with an existing-slot conflict.
            forged.put("horaInicio", "13:00");
            call(POST, "/api/citas", second.token(), forged, 403);
        }
        call(PATCH, "/api/citas/" + a.appointment() + "/confirmar", second.token(), null, 403);
        call(PATCH, "/api/citas/" + a.appointment() + "/cancelar", second.token(), null, 403);
        call(POST, "/api/citas/" + a.appointment() + "/atencion", b.vet().token(), attention(), 403);
        call(POST, "/api/notas-seguimiento", second.token(), Map.of(
                "citaId", a.appointment(), "observacion", "Must not be stored"), 403);
        call(GET, "/api/citas/" + a.appointment(), b.owner().token(), null, 403);
        call(POST, "/api/citas", b.owner().token(), appointmentRequest(a), 403);
        em.flush();
        em.clear();
        assertThat(snapshot(first, second)).isEqualTo(before);
        assertThat(call(GET, "/api/notas-seguimiento/" + a.appointment(), first.token(), null, 200).size()).isZero();
    }

    @Test
    void separatelySeededCliniclessOperatorLogsInButCannotAccessClinicalDocuments() throws Exception {
        Account operator = operator();
        JsonNode me = call(GET, "/api/auth/me", operator.token(), null, 200);
        assertThat(me.path("clinica").isNull()).isTrue();
        assertThat(me.path("forcePasswordChange").asBoolean()).isFalse();
        assertThat(me.path("roles").size()).isEqualTo(1);
        assertThat(me.path("roles").get(0).asText()).isEqualTo("ROLE_SAAS_OPERATOR");
        JsonNode listing = call(GET, "/api/saas/clinicas?q=" + first.slug(), operator.token(), null, 200);
        assertThat(listing.path("items").size()).isEqualTo(1);
        JsonNode clinic = listing.path("items").get(0);
        assertThat(clinic.path("id").asLong()).isEqualTo(first.id());
        assertThat(clinic.path("plan").path("plan").asText()).isEqualTo("TRIAL");
        assertThat(clinic.toString()).doesNotContain("password", "numeroDocumento", "diagnostico", "notasInternas");
        for (String path : List.of("/api/mascotas", "/api/duenios", "/api/citas", "/api/usuarios",
                "/api/reportes/citas", "/api/clinicas/me", "/api/servicios")) {
            call(GET, path, operator.token(), null, 403);
        }
        call(POST, "/api/mascotas", operator.token(), petRequest(1), 403);
        call(GET, "/api/saas/clinicas", first.token(), null, 403);
        call(GET, "/api/saas/clinicas/" + first.id() + "/auditoria", first.token(), null, 403);
        call(PATCH, "/api/saas/clinicas/" + first.id() + "/plan", first.token(), Map.of(
                "plan", "PRO", "reason", "Tenant must not change plan"), 403);
        call(PATCH, "/api/saas/clinicas/" + first.id() + "/estado", first.token(), Map.of(
                "estado", "SUSPENDIDA", "reason", "Tenant must not suspend"), 403);
        assertThat(plan(first).path("plan").asText()).isEqualTo("TRIAL");
        assertThat(call(GET, "/api/saas/clinicas/" + first.id() + "/auditoria", operator.token(), null, 200)
                .path("items").size()).isZero();
    }

    @Test
    void downgradeConflictAndSuspensionPreserveDataTrialClocksAndImmutableAuditSnapshots() throws Exception {
        Account operator = operator();
        Pipeline p = pipeline(first);
        staff(first, "ASISTENTE");
        JsonNode originalPlan = plan(first);
        Map<String, JsonNode> originalData = snapshot(first, second);
        String saasPath = "/api/saas/clinicas/" + first.id();
        JsonNode downgrade = call(PATCH, saasPath + "/plan", operator.token(), Map.of(
                "plan", "CONSULTORIO", "reason", "Too many active staff"), 409);
        assertThat(downgrade.path("code").asText()).isEqualTo("STAFF_LIMIT_EXCEEDED");
        assertThat(plan(first)).isEqualTo(originalPlan);
        assertThat(call(GET, saasPath + "/auditoria", operator.token(), null, 200).path("items").size()).isZero();

        JsonNode suspended = call(PATCH, saasPath + "/estado", operator.token(), Map.of(
                "estado", "SUSPENDIDA", "reason", "Pilot billing suspension"), 200);
        assertThat(suspended.path("estado").asText()).isEqualTo("SUSPENDIDA");
        assertThat(suspended.path("plan").path("readOnly").asBoolean()).isTrue();
        assertThat(suspended.path("plan").path("status").asText()).isEqualTo("SUSPENDED");
        call(GET, "/api/usuarios", first.token(), null, 200);
        call(GET, "/api/citas/" + p.appointment(), p.owner().token(), null, 200);
        call(GET, "/api/mascotas/" + p.pet(), p.owner().token(), null, 200);
        call(POST, "/api/mascotas", p.owner().token(), petRequest(p.ownerId()), 403);
        String blockedEmail = email("suspended");
        JsonNode blocked = call(POST, "/api/usuarios/internal", first.token(), internalUser(blockedEmail, "ASISTENTE"), 403);
        assertThat(blocked.path("code").asText()).isEqualTo("CLINIC_READ_ONLY");
        call(POST, "/api/servicios", first.token(), Map.of(
                "nombre", "Blocked service", "descripcion", "Must not persist", "costoBase", 10), 403);
        assertThat(users.existsByEmail(blockedEmail)).isFalse();
        JsonNode suspensionAudit = call(GET, saasPath + "/auditoria", operator.token(), null, 200).path("items").get(0);
        assertThat(suspensionAudit.path("actorId").asLong()).isEqualTo(operator.id());
        assertThat(suspensionAudit.path("actorEmail").asText()).isEqualTo(operator.email());
        assertThat(suspensionAudit.path("action").asText()).isEqualTo("ESTADO_CHANGED");
        assertThat(suspensionAudit.path("previousPlan").asText()).isEqualTo("TRIAL");
        assertThat(suspensionAudit.path("newPlan").asText()).isEqualTo("TRIAL");
        assertThat(suspensionAudit.path("previousEstado").asText()).isEqualTo("ACTIVA");
        assertThat(suspensionAudit.path("newEstado").asText()).isEqualTo("SUSPENDIDA");
        assertThat(suspensionAudit.path("reason").asText()).isEqualTo("Pilot billing suspension");
        assertThat(suspensionAudit.path("createdAt").asText()).isNotBlank();

        JsonNode reactivated = call(PATCH, saasPath + "/estado", operator.token(), Map.of(
                "estado", "ACTIVA", "reason", "Pilot billing restored"), 200);
        assertThat(reactivated.path("plan").path("status").asText()).isEqualTo("TRIAL");
        assertThat(reactivated.path("plan").path("readOnly").asBoolean()).isFalse();
        for (String clock : List.of("trialStartedAt", "trialEndsAt")) {
            assertThat(reactivated.path("plan").path(clock)).isEqualTo(originalPlan.path(clock));
        }
        em.flush();
        em.clear();
        assertThat(snapshot(first, second)).isEqualTo(originalData);
        JsonNode upgraded = call(PATCH, saasPath + "/plan", operator.token(), Map.of(
                "plan", "PRO", "reason", "Pilot paid plan upgrade"), 200);
        assertThat(upgraded.path("plan").path("plan").asText()).isEqualTo("PRO");
        assertThat(upgraded.path("plan").path("status").asText()).isEqualTo("ACTIVE");
        em.flush();
        em.clear();
        JsonNode audit = call(GET, saasPath + "/auditoria", operator.token(), null, 200).path("items");
        assertThat(audit.size()).isEqualTo(3);
        assertThat(audit.get(2)).isEqualTo(suspensionAudit);
        assertThat(audit.get(1).path("previousEstado").asText()).isEqualTo("SUSPENDIDA");
        assertThat(audit.get(1).path("newEstado").asText()).isEqualTo("ACTIVA");
        assertThat(audit.get(0).path("action").asText()).isEqualTo("PLAN_CHANGED");
        assertThat(audit.get(0).path("previousPlan").asText()).isEqualTo("TRIAL");
        assertThat(audit.get(0).path("newPlan").asText()).isEqualTo("PRO");
        staff(first, "ASISTENTE");
        assertThat(plan(first).path("staffUsed").asInt()).isEqualTo(4);
        assertThat(plan(second).path("plan").asText()).isEqualTo("TRIAL");
    }

    private Clinic registerClinic() throws Exception {
        String email = email("admin");
        JsonNode registered = call(POST, "/api/auth/register-clinic", null, Map.of(
                "fullName", "Pilot Administrator", "email", email, "password", PASSWORD,
                "telefono", PHONE, "clinicaNombre", "Pilot " + unique()), 201);
        JsonNode clinic = registered.path("user").path("clinica");
        return new Clinic(clinic.path("id").asLong(), clinic.path("slug").asText(), login(email, PASSWORD).path("accessToken").asText());
    }

    private Account staff(Clinic clinic, String role) throws Exception {
        String email = email(role.toLowerCase());
        JsonNode created = call(POST, "/api/usuarios/internal", clinic.token(), internalUser(email, role), 201);
        String temporary = created.path("temporaryPassword").asText();
        assertThat(temporary).isNotBlank();
        JsonNode login = login(email, temporary);
        assertThat(login.path("user").path("forcePasswordChange").asBoolean()).isTrue();
        String token = login.path("accessToken").asText();
        call(POST, "/api/auth/change-password", token, Map.of("currentPassword", temporary, "newPassword", PASSWORD), 200);
        JsonNode changed = login(email, PASSWORD);
        assertThat(changed.path("user").path("forcePasswordChange").asBoolean()).isFalse();
        assertThat(changed.path("user").path("clinica").path("id").asLong()).isEqualTo(clinic.id());
        return new Account(changed.path("user").path("id").asLong(), email, changed.path("accessToken").asText());
    }

    private Account registerOwner(Clinic clinic) throws Exception {
        String email = email("owner");
        long ownerId = call(POST, "/api/duenios", clinic.token(), Map.of(
                "nombres", "Pilot", "apellidos", "Owner", "tipoDocumento", "DNI", "numeroDocumento", unique(),
                "telefono", PHONE, "email", email, "direccion", "Pilot Street 123"), 201).path("id").asLong();
        JsonNode registered = call(POST, "/api/auth/register", null, Map.of(
                "fullName", "Pilot Owner", "email", email, "password", PASSWORD, "telefono", PHONE,
                "clinicaSlug", clinic.slug()), 201);
        JsonNode login = login(email, PASSWORD);
        Account owner = new Account(login.path("user").path("id").asLong(), email, login.path("accessToken").asText());
        assertThat(registered.path("user").path("id").asLong()).isEqualTo(owner.id());
        JsonNode ownRecord = call(GET, "/api/duenios/me", owner.token(), null, 200);
        assertThat(ownRecord.path("id").asLong()).isEqualTo(ownerId);
        assertThat(ownRecord.path("usuarioId").asLong()).isEqualTo(owner.id());
        assertThat(call(GET, "/api/duenios", clinic.token(), null, 200).size()).isEqualTo(1);
        return owner;
    }

    private Pipeline pipeline(Clinic clinic) throws Exception {
        Account vet = staff(clinic, "VETERINARIO");
        long veterinarian = call(POST, "/api/veterinarios", clinic.token(), vetRequest(vet), 201).path("id").asLong();
        Account owner = registerOwner(clinic);
        long ownerId = call(GET, "/api/duenios/me", owner.token(), null, 200).path("id").asLong();
        long pet = call(POST, "/api/mascotas", owner.token(), petRequest(ownerId), 201).path("id").asLong();
        long service = call(POST, "/api/servicios", clinic.token(), Map.of(
                "nombre", "Consulta " + unique(), "descripcion", "Consulta piloto", "costoBase", 75), 201).path("id").asLong();
        Pipeline pending = new Pipeline(vet, veterinarian, owner, ownerId, pet, service, 0);
        JsonNode appointment = call(POST, "/api/citas", owner.token(), appointmentRequest(pending), 201);
        assertThat(appointment.path("estado").asText()).isEqualTo("PROGRAMADA");
        assertMoney(appointment, "total", "140.00");
        return new Pipeline(vet, veterinarian, owner, ownerId, pet, service, appointment.path("id").asLong());
    }

    private Account operator() throws Exception {
        String email = email("operator");
        Rol role = roles.findByName(RoleName.ROLE_SAAS_OPERATOR).orElseThrow();
        assertThat(role.getActive()).isTrue();
        Usuario seeded = users.saveAndFlush(Usuario.builder().fullName("Pilot SaaS Operator").email(email)
                .password(encoder.encode(PASSWORD)).active(true).forcePasswordChange(false).clinica(null)
                .roles(Set.of(role)).createdAt(LocalDateTime.now()).build());
        JsonNode login = login(email, PASSWORD);
        assertThat(login.path("user").path("id").asLong()).isEqualTo(seeded.getId());
        return new Account(seeded.getId(), email, login.path("accessToken").asText());
    }

    private void moveAppointmentIntoPastForTestOnly(long appointmentId) {
        // Test database fixture only: the API correctly rejects past appointments and pre-start attention.
        Cita appointment = em.find(Cita.class, appointmentId);
        appointment.setFecha(LocalDate.now().minusDays(1));
        appointment.setHoraInicio(LocalTime.NOON);
        appointment.setHoraFin(LocalTime.NOON.plusMinutes(30));
        em.flush();
        em.clear();
    }

    private JsonNode login(String email, String password) throws Exception {
        return call(POST, "/api/auth/login", null, Map.of("email", email, "password", password), 200);
    }

    private JsonNode plan(Clinic clinic) throws Exception {
        return call(GET, "/api/clinicas/me/plan", clinic.token(), null, 200);
    }

    private Map<String, JsonNode> snapshot(Clinic... clinics) throws Exception {
        Map<String, JsonNode> result = new LinkedHashMap<>();
        for (Clinic clinic : clinics) {
            for (String resource : List.of("usuarios", "duenios", "mascotas", "veterinarios", "servicios", "citas")) {
                result.put(clinic.id() + "/" + resource, call(GET, "/api/" + resource, clinic.token(), null, 200));
            }
        }
        return result;
    }

    private JsonNode call(HttpMethod method, String path, String token, Object body, int expected) throws Exception {
        var builder = request(method, path).accept(MediaType.APPLICATION_JSON);
        if (token != null) builder.header("Authorization", "Bearer " + token);
        if (body != null) builder.contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsBytes(body));
        String response = mvc.perform(builder).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
        return mapper.readTree(response);
    }

    private Map<String, Object> vetRequest(Account vet) {
        List<Map<String, Object>> schedule = Arrays.stream(DayOfWeek.values()).map(day -> Map.<String, Object>of(
                "diaSemana", day, "horaInicio", "09:00", "horaFin", "17:00", "duracionBloqueMinutos", 30)).toList();
        return Map.of("usuarioId", vet.id(), "numeroColegiatura", unique(), "especialidad", "General",
                "telefono", PHONE, "email", vet.email(), "horarios", schedule);
    }

    private Map<String, Object> petRequest(long ownerId) {
        return Map.of("duenioId", ownerId, "nombre", "Pilot Pet", "especie", "Canino", "raza", "Mestizo",
                "sexo", "MACHO", "fechaNacimiento", LocalDate.now().minusYears(2), "pesoKg", 12.5);
    }

    private Map<String, Object> appointmentRequest(Pipeline p) {
        return Map.of("duenioId", p.ownerId(), "mascotaId", p.pet(), "veterinarioId", p.veterinarian(),
                "fecha", LocalDate.now().plusDays(7), "horaInicio", "12:00", "duracionMinutos", 30,
                "motivo", "Consulta piloto", "servicios", services(p.service()), "descuento", 10);
    }

    private List<Map<String, Object>> services(long service) {
        return List.of(Map.of("servicioId", service, "cantidad", 2));
    }

    private Map<String, Object> attention() {
        return Map.of("motivo", "Consulta piloto", "diagnostico", "Paciente saludable", "tratamiento", "Control preventivo",
                "recomendaciones", "Volver en un mes", "notasInternas", "Staff-only pilot note", "estadoMascota", "SALUDABLE");
    }

    private Map<String, Object> internalUser(String email, String role) {
        return Map.of("nombres", "Pilot", "apellidos", "Staff", "email", email, "rol", role);
    }

    private void assertMoney(JsonNode response, String field, String expected) {
        assertThat(new BigDecimal(response.path(field).asText())).isEqualByComparingTo(expected);
    }

    private String email(String label) {
        return label + "-" + unique() + "@pilot.example";
    }

    private String unique() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private record Clinic(long id, String slug, String token) {}
    private record Account(long id, String email, String token) {}
    private record Pipeline(Account vet, long veterinarian, Account owner, long ownerId, long pet, long service, long appointment) {}
}
