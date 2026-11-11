package com.petcare.backend.domain.service;

import com.petcare.backend.domain.repository.ClinicaRepository;
import com.petcare.backend.domain.repository.MonthlyPaymentRepository;
import com.petcare.backend.domain.repository.RolRepository;
import com.petcare.backend.domain.repository.SaasClinicAuditRepository;
import com.petcare.backend.domain.repository.UsuarioRepository;
import com.petcare.backend.persistence.entity.Clinica;
import com.petcare.backend.persistence.entity.Duenio;
import com.petcare.backend.persistence.entity.Mascota;
import com.petcare.backend.persistence.entity.MonthlyPayment;
import com.petcare.backend.persistence.entity.Rol;
import com.petcare.backend.persistence.entity.Usuario;
import com.petcare.backend.persistence.enums.PlanClinica;
import com.petcare.backend.persistence.enums.RoleName;
import com.petcare.backend.persistence.enums.SexoMascota;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:monthly-billing-integration;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;LOCK_TIMEOUT=10000",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false"
})
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@ActiveProfiles("test")
@Import(MonthlyBillingIntegrationTest.BillingClockConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Execution(ExecutionMode.SAME_THREAD)
class MonthlyBillingIntegrationTest {
    private static final String PASSWORD = "MonthlyPassword123!";
    private static final String REASON = "Confirmed monthly bank transfer";
    private static final Instant INITIAL_TIME = Instant.parse("2027-01-31T04:30:00.123456Z");
    private static final PageRequest ALL = PageRequest.of(0, 100);

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired MutableBillingClock billingClock;
    @Autowired ClinicaRepository clinics;
    @Autowired UsuarioRepository users;
    @Autowired RolRepository roles;
    @Autowired MonthlyPaymentRepository payments;
    @Autowired SaasClinicAuditRepository audits;
    @Autowired PasswordEncoder encoder;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired EntityManager em;

    private Clinic clinic;
    private Account operator;

    @BeforeEach
    void committedFixturesWithRealCredentials() throws Exception {
        billingClock.set(INITIAL_TIME);
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
        for (RoleName name : RoleName.values()) {
            if (roles.findByName(name).isEmpty()) {
                roles.saveAndFlush(Rol.builder().name(name).description("Monthly billing integration role")
                        .active(true).build());
            }
        }
        operator = account(RoleName.ROLE_SAAS_OPERATOR, null, false);
        JsonNode identity = call(GET, "/api/auth/me", operator.token(), null, 200);
        assertThat(identity.path("clinica").isNull()).isTrue();
        assertThat(identity.path("active").asBoolean()).isTrue();
        assertThat(identity.path("forcePasswordChange").asBoolean()).isFalse();
        assertThat(identity.path("roles").size()).isEqualTo(1);
        assertThat(identity.path("roles").get(0).asText()).isEqualTo("ROLE_SAAS_OPERATOR");
        clinic = registerClinic();
    }

    @Test
    void userListAndDetailMapMonthlyStateWithoutOpenInView() throws Exception {
        JsonNode listed = call(GET, "/api/usuarios", clinic.token(), null, 200);
        assertThat(listed.size()).isEqualTo(1);
        long userId = listed.get(0).path("id").asLong();
        assertThat(listed.get(0).path("clinica").path("planStatus").asText()).isEqualTo("TRIAL");

        JsonNode paid = pay(clinic, payload("CONSULTORIO", "ACTIVATE", reference(), null), 200).path("plan");
        JsonNode detail = call(GET, "/api/usuarios/" + userId, clinic.token(), null, 200).path("clinica");
        assertThat(detail.path("planStatus").asText()).isEqualTo("ACTIVE");
        assertThat(detail.path("subscriptionEndsAt")).isEqualTo(paid.path("subscriptionEndsAt"));

        billingClock.set(Instant.parse(paid.path("subscriptionEndsAt").asText()));
        JsonNode expired = call(GET, "/api/usuarios", clinic.token(), null, 200).get(0).path("clinica");
        assertThat(expired.path("planStatus").asText()).isEqualTo("SUBSCRIPTION_EXPIRED");
        assertThat(expired.path("readOnly").asBoolean()).isTrue();
    }

    @ParameterizedTest
    @CsvSource({"CONSULTORIO,59.00,2,500", "PRO,129.00,5,2500"})
    void activationCommitsServerPricedPaymentAuditAndClinic(String target, String price, int staff, int pets) throws Exception {
        String reference = reference();
        JsonNode response = pay(clinic, payload(target, "ACTIVATE", reference, null), 200);
        JsonNode plan = response.path("plan");
        assertThat(response.path("id").asLong()).isEqualTo(clinic.id());
        assertThat(response.path("estado").asText()).isEqualTo("ACTIVA");
        assertThat(plan.path("plan").asText()).isEqualTo(target);
        assertPlanState(plan, "ACTIVE", false, false, true);
        assertMoney(plan, "monthlyPrice", price);
        assertThat(plan.path("staffLimit").asInt()).isEqualTo(staff);
        assertThat(plan.path("petsLimit").asInt()).isEqualTo(pets);
        assertThat(plan.path("staffUsed").asInt()).isEqualTo(1);
        assertThat(plan.path("petsUsed").asInt()).isZero();
        assertThat(plan.path("subscriptionDaysRemaining").asInt()).isEqualTo(29);
        MonthlyPayment payment = payments.findByPaymentReference(reference).orElseThrow();
        Clinica stored = stored(clinic);
        assertThat(stored.getPlan()).isEqualTo(PlanClinica.valueOf(target));
        assertThat(stored.getLastPaymentId()).isEqualTo(payment.getId());
        assertThat(stored.getSubscriptionStartedAt()).isEqualTo(INITIAL_TIME);
        assertThat(stored.getSubscriptionEndsAt()).isEqualTo(Instant.parse("2027-03-01T04:30:00.123456Z"));
        assertThat(stored.getSubscriptionAnchorDay()).isEqualTo(30);
        assertThat(payment.getAmount()).isEqualByComparingTo(price);
        assertThat(payment.getCurrency()).isEqualTo("PEN");
        assertThat(payment.getBillingCycle()).isEqualTo("MONTHLY");
        assertThat(payment.getConfirmedAt()).isEqualTo(INITIAL_TIME);
        assertThat(payment.getActorId()).isEqualTo(operator.id());
        assertThat(payment.getActorEmail()).isEqualTo(operator.email());
        assertThat(payment.getPreviousPaymentId()).isNull();
        assertThat(payment.getReason()).isEqualTo(REASON);
        var audit = audits.findByClinicId(clinic.id(), ALL).getContent().getFirst();
        assertThat(audit.getId()).isEqualTo(payment.getAuditId());
        assertThat(audit.getAction()).isEqualTo("MONTHLY_ACTIVATED");
        assertThat(audit.getPreviousPlan()).isEqualTo(PlanClinica.TRIAL);
        assertThat(audit.getNewPlan()).isEqualTo(PlanClinica.valueOf(target));
        assertThat(audit.getActorId()).isEqualTo(operator.id());
        assertThat(audit.getReason()).isEqualTo(REASON);
        assertCounts(clinic, 1, 1);
        assertThat(myPlan(clinic)).isEqualTo(plan);
    }

    @ParameterizedTest
    @CsvSource({
            "2027-02-01T04:30:00.123456Z,2027-03-01T04:30:00.123456Z,2027-04-01T04:30:00.123456Z,31",
            "2028-02-01T04:30:00.123456Z,2028-03-01T04:30:00.123456Z,2028-04-01T04:30:00.123456Z,31",
            "2027-02-28T17:00:00Z,2027-03-28T17:00:00Z,2027-04-28T17:00:00Z,28",
            "2028-02-29T17:00:00Z,2028-03-29T17:00:00Z,2028-04-29T17:00:00Z,29",
            "2027-12-31T17:00:00Z,2028-01-31T17:00:00Z,2028-02-29T17:00:00Z,31"
    })
    void limaCalendarRetainsOriginalAnchorAcrossClampingAndYearRollover(
            String now, String firstEnd, String secondEnd, int anchor) throws Exception {
        billingClock.set(Instant.parse(now));
        JsonNode first = pay(clinic, payload("PRO", "ACTIVATE", reference(), null), 200).path("plan");
        assertThat(Instant.parse(first.path("subscriptionEndsAt").asText())).isEqualTo(Instant.parse(firstEnd));
        Long firstId = first.path("lastPaymentId").asLong();
        billingClock.set(Instant.parse(firstEnd).minusSeconds(1));
        pay(clinic, payload("PRO", "RENEW", reference(), firstId), 200);
        Clinica stored = stored(clinic);
        assertThat(stored.getSubscriptionStartedAt()).isEqualTo(Instant.parse(now));
        assertThat(stored.getSubscriptionEndsAt()).isEqualTo(Instant.parse(secondEnd));
        assertThat(stored.getSubscriptionAnchorDay()).isEqualTo(anchor);
        assertCounts(clinic, 2, 2);
    }

    @Test
    void earlyRenewalAppendsFromOldEndAndKeepsHistoricalCoverageStart() throws Exception {
        JsonNode first = activate("PRO");
        MonthlyPayment original = payment(first);
        JsonNode originalSnapshot = mapper.valueToTree(original);
        billingClock.set(INITIAL_TIME.plus(Duration.ofDays(3)));
        JsonNode renewed = pay(clinic, payload("PRO", "RENEW", reference(), original.getId()), 200).path("plan");
        MonthlyPayment next = payment(renewed);
        assertThat(next.getPeriodStartedAt()).isEqualTo(original.getPeriodEndsAt());
        assertThat(next.getPeriodEndsAt()).isEqualTo(Instant.parse("2027-03-31T04:30:00.123456Z"));
        assertThat(next.getPreviousPaymentId()).isEqualTo(original.getId());
        assertThat(stored(clinic).getSubscriptionStartedAt()).isEqualTo(original.getPeriodStartedAt());
        assertThat(mapper.<JsonNode>valueToTree(payments.findByPaymentReference(original.getPaymentReference()).orElseThrow()))
                .isEqualTo(originalSnapshot);
        assertThat(audits.findByClinicId(clinic.id(), ALL).getContent()).anySatisfy(audit -> {
            assertThat(audit.getId()).isEqualTo(next.getAuditId());
            assertThat(audit.getAction()).isEqualTo("MONTHLY_RENEWED");
            assertThat(audit.getPreviousPlan()).isEqualTo(PlanClinica.PRO);
        });
        assertPlanState(renewed, "ACTIVE", false, false, true);
        assertCounts(clinic, 2, 2);
    }

    @Test
    void expiredRenewalStartsNowWithoutBackfillAndResetsAnchor() throws Exception {
        MonthlyPayment first = payment(activate("PRO"));
        Instant restart = Instant.parse("2027-04-12T20:00:00.654321Z");
        billingClock.set(restart);
        assertPlanState(myPlan(clinic), "SUBSCRIPTION_EXPIRED", true, true, true);
        JsonNode renewed = pay(clinic, payload("PRO", "RENEW", reference(), first.getId()), 200).path("plan");
        MonthlyPayment second = payment(renewed);
        assertThat(second.getPeriodStartedAt()).isEqualTo(restart).isAfter(first.getPeriodEndsAt());
        assertThat(second.getPeriodEndsAt()).isEqualTo(Instant.parse("2027-05-12T20:00:00.654321Z"));
        assertThat(second.getAnchorDay()).isEqualTo(12);
        assertThat(stored(clinic).getSubscriptionStartedAt()).isEqualTo(restart);
        assertThat(stored(clinic).getSubscriptionAnchorDay()).isEqualTo(12);
        assertCounts(clinic, 2, 2);
    }

    @ParameterizedTest
    @CsvSource({"PRO,ACTIVATE", "CONSULTORIO,ACTIVATE", "CONSULTORIO,RENEW"})
    void activeCoverageRejectsRestartAndPlanChanges(String target, String operation) throws Exception {
        long id = activate("PRO").path("lastPaymentId").asLong();
        JsonNode before = snapshot(clinic);
        assertCode(pay(clinic, payload(target, operation, reference(), id), 409), "SUBSCRIPTION_ACTIVE");
        assertThat(snapshot(clinic)).isEqualTo(before);
    }

    @Test
    void exactEndAllowsPlanChangeAndStartsNewCoverageImmediately() throws Exception {
        MonthlyPayment first = payment(activate("PRO"));
        billingClock.set(first.getPeriodEndsAt());
        assertPlanState(myPlan(clinic), "SUBSCRIPTION_EXPIRED", true, true, true);
        JsonNode changed = pay(clinic, payload("CONSULTORIO", "ACTIVATE", reference(), first.getId()), 200).path("plan");
        assertPlanState(changed, "ACTIVE", false, false, true);
        assertThat(stored(clinic).getPlan()).isEqualTo(PlanClinica.CONSULTORIO);
        assertThat(payment(changed).getPeriodStartedAt()).isEqualTo(first.getPeriodEndsAt());
        assertThat(stored(clinic).getSubscriptionStartedAt()).isEqualTo(first.getPeriodEndsAt());
        assertMoney(changed, "monthlyPrice", "59.00");
        assertCounts(clinic, 2, 2);
    }

    @Test
    void planChangeAtExpiryStillChecksActiveStaffAndLeavesNoFailedPayment() throws Exception {
        MonthlyPayment first = payment(activate("PRO"));
        account(RoleName.ROLE_ASISTENTE, clinic.id(), false);
        Account excess = account(RoleName.ROLE_VETERINARIO, clinic.id(), false);
        billingClock.set(first.getPeriodEndsAt());
        JsonNode before = snapshot(clinic);
        Map<String, Object> request = payload("CONSULTORIO", "ACTIVATE", reference(), first.getId());
        assertCode(pay(clinic, request, 409), "STAFF_LIMIT_EXCEEDED");
        assertThat(snapshot(clinic)).isEqualTo(before);
        transactions().executeWithoutResult(tx -> users.findById(excess.id()).orElseThrow().setActive(false));
        JsonNode changed = pay(clinic, request, 200).path("plan");
        assertThat(changed.path("staffUsed").asInt()).isEqualTo(2);
        assertThat(changed.path("staffLimit").asInt()).isEqualTo(2);
        assertCounts(clinic, 2, 2);
    }

    @Test
    void planChangeAtExpiryStillChecksPetQuotaWithoutDeletingRecords() throws Exception {
        MonthlyPayment first = payment(activate("PRO"));
        long owner = registerOwner().path("user").path("id").asLong();
        transactions().executeWithoutResult(tx -> {
            Duenio record = em.createQuery("select d from Duenio d where d.usuario.id = :id", Duenio.class)
                    .setParameter("id", owner).getSingleResult();
            Clinica tenant = em.find(Clinica.class, clinic.id());
            LocalDateTime now = LocalDateTime.ofInstant(billingClock.instant(), ZoneOffset.UTC);
            for (int i = 0; i < 501; i++) {
                em.persist(Mascota.builder().duenio(record).clinica(tenant).nombre("Quota pet " + i)
                        .especie("Canino").raza("Mestizo").sexo(SexoMascota.MACHO)
                        .fechaNacimiento(LocalDate.of(2020, 1, 1)).active(true).createdAt(now).updatedAt(now).build());
            }
        });
        billingClock.set(first.getPeriodEndsAt());
        JsonNode before = snapshot(clinic);
        assertCode(pay(clinic, payload("CONSULTORIO", "ACTIVATE", reference(), first.getId()), 409), "PET_LIMIT_EXCEEDED");
        assertThat(snapshot(clinic)).isEqualTo(before);
        assertThat(myPlan(clinic).path("petsUsed").asInt()).isEqualTo(501);
    }

    @Test
    void suspensionHasPriorityAndPaymentNeverUnsuspendsOrPausesCoverage() throws Exception {
        changeEstado("SUSPENDIDA");
        JsonNode activated = activate("PRO");
        MonthlyPayment first = payment(activated);
        assertPlanState(activated, "SUSPENDED", true, false, true);
        assertThat(stored(clinic).getEstado().name()).isEqualTo("SUSPENDIDA");
        JsonNode renewed = pay(clinic, payload("PRO", "RENEW", reference(), first.getId()), 200).path("plan");
        Instant end = payment(renewed).getPeriodEndsAt();
        assertPlanState(renewed, "SUSPENDED", true, false, true);
        assertThat(stored(clinic).getSubscriptionStartedAt()).isEqualTo(INITIAL_TIME);
        billingClock.set(end);
        assertThat(myPlan(clinic).path("status").asText()).isEqualTo("SUSPENDED");
        JsonNode restored = changeEstado("ACTIVA").path("plan");
        assertPlanState(restored, "SUBSCRIPTION_EXPIRED", true, true, true);
        assertThat(stored(clinic).getSubscriptionEndsAt()).isEqualTo(end);
        assertThat(stored(clinic).getSubscriptionStartedAt()).isEqualTo(INITIAL_TIME);
        assertCounts(clinic, 2, 4);
    }

    @ParameterizedTest
    @CsvSource({"-1,ACTIVE,false", "0,SUBSCRIPTION_EXPIRED,true", "1,SUBSCRIPTION_EXPIRED,true"})
    void paidCoverageEndIsHalfOpenToMicrosecondPrecision(long micros, String status, boolean readOnly) throws Exception {
        MonthlyPayment first = payment(activate("PRO"));
        billingClock.set(first.getPeriodEndsAt().plus(micros, ChronoUnit.MICROS));
        JsonNode plan = myPlan(clinic);
        assertPlanState(plan, status, readOnly, readOnly, true);
        JsonNode session = call(GET, "/api/auth/me", clinic.token(), null, 200).path("clinica");
        assertThat(session.path("planStatus").asText()).isEqualTo(status);
        assertThat(session.path("readOnly").asBoolean()).isEqualTo(readOnly);
        assertThat(session.path("subscriptionStartedAt")).isEqualTo(plan.path("subscriptionStartedAt"));
        assertThat(session.path("subscriptionEndsAt")).isEqualTo(plan.path("subscriptionEndsAt"));
        assertThat(plan.path("subscriptionDaysRemaining").asInt()).isEqualTo(readOnly ? 0 : 1);
        Reply write = exchange(POST, "/api/servicios", clinic.token(), serviceBody());
        assertThat(write.status()).isEqualTo(readOnly ? 403 : 201);
        if (readOnly) assertCode(write.body(), "SUBSCRIPTION_EXPIRED");
        assertCounts(clinic, 1, 1);
    }

    @Test
    void futurePaidCoverageIsPendingWithBothActionsDisabledUntilInclusiveStart() throws Exception {
        MonthlyPayment first = payment(activate("PRO"));
        billingClock.set(first.getPeriodStartedAt().minus(1, ChronoUnit.MICROS));
        JsonNode plan = myPlan(clinic);
        assertPlanState(plan, "PAYMENT_PENDING", true, false, false);
        JsonNode session = call(GET, "/api/auth/me", clinic.token(), null, 200).path("clinica");
        assertThat(session.path("planStatus").asText()).isEqualTo("PAYMENT_PENDING");
        assertThat(session.path("readOnly").asBoolean()).isTrue();
        JsonNode before = snapshot(clinic);
        for (String operation : List.of("ACTIVATE", "RENEW")) {
            assertCode(pay(clinic, payload("PRO", operation, reference(), first.getId()), 409), "SUBSCRIPTION_NOT_STARTED");
            assertThat(snapshot(clinic)).isEqualTo(before);
        }
        assertCode(call(POST, "/api/servicios", clinic.token(), serviceBody(), 403), "PAYMENT_REQUIRED");
        billingClock.set(first.getPeriodStartedAt());
        assertPlanState(myPlan(clinic), "ACTIVE", false, false, true);
        call(POST, "/api/servicios", clinic.token(), serviceBody(), 201);
    }

    @ParameterizedTest
    @ValueSource(strings = {"PRO", "CONSULTORIO"})
    void legacyPaidPlanWithoutCoverageRemainsPendingAndCannotFakeRenewal(String target) throws Exception {
        transactions().executeWithoutResult(tx -> clinics.findById(clinic.id()).orElseThrow().setPlan(PlanClinica.valueOf(target)));
        JsonNode before = snapshot(clinic);
        assertPlanState(myPlan(clinic), "PAYMENT_PENDING", true, true, false);
        assertCode(pay(clinic, payload(target, "RENEW", reference(), null), 409), "PAYMENT_REQUIRED");
        assertCode(call(POST, "/api/servicios", clinic.token(), serviceBody(), 403), "PAYMENT_REQUIRED");
        JsonNode session = login(clinic.email()).path("user").path("clinica");
        assertThat(session.path("planStatus").asText()).isEqualTo("PAYMENT_PENDING");
        assertThat(session.path("readOnly").asBoolean()).isTrue();
        assertThat(session.path("subscriptionStartedAt").isNull()).isTrue();
        assertThat(session.path("subscriptionEndsAt").isNull()).isTrue();
        assertThat(snapshot(clinic)).isEqualTo(before);
        pay(clinic, payload(target, "ACTIVATE", reference(), null), 200);
        assertCounts(clinic, 1, 1);
    }

    @Test
    void monthlyPaymentsPreserveFourteenDayTrialOwnerRecordsAndUsageCounts() throws Exception {
        Clinica original = stored(clinic);
        assertThat(Duration.between(original.getTrialStartedAt(), original.getTrialEndsAt())).isEqualTo(Duration.ofDays(14));
        JsonNode owner = registerOwner();
        String ownerToken = owner.path("accessToken").asText();
        JsonNode ownerRecord = call(GET, "/api/duenios/me", ownerToken, null, 200);
        call(POST, "/api/mascotas", ownerToken, Map.of("duenioId", ownerRecord.path("id").asLong(),
                "nombre", "Monthly pet", "especie", "Canino", "raza", "Mestizo", "sexo", "MACHO",
                "fechaNacimiento", "2020-01-01", "pesoKg", 10), 201);
        call(POST, "/api/servicios", clinic.token(), serviceBody(), 201);
        JsonNode owners = call(GET, "/api/duenios", clinic.token(), null, 200);
        JsonNode pets = call(GET, "/api/mascotas", clinic.token(), null, 200);
        JsonNode services = call(GET, "/api/servicios", clinic.token(), null, 200);
        JsonNode accounts = call(GET, "/api/usuarios", clinic.token(), null, 200);
        assertThat(myPlan(clinic).path("staffUsed").asInt()).isEqualTo(1);
        long id = activate("CONSULTORIO").path("lastPaymentId").asLong();
        pay(clinic, payload("CONSULTORIO", "RENEW", reference(), id), 200);
        assertThat(stored(clinic).getTrialStartedAt()).isEqualTo(original.getTrialStartedAt());
        assertThat(stored(clinic).getTrialEndsAt()).isEqualTo(original.getTrialEndsAt());
        assertThat(call(GET, "/api/duenios", clinic.token(), null, 200)).isEqualTo(owners);
        assertThat(call(GET, "/api/mascotas", clinic.token(), null, 200)).isEqualTo(pets);
        assertThat(call(GET, "/api/servicios", clinic.token(), null, 200)).isEqualTo(services);
        JsonNode currentAccounts = call(GET, "/api/usuarios", clinic.token(), null, 200);
        JsonNode currentClinic = call(GET, "/api/clinicas/me", clinic.token(), null, 200);
        assertThat(currentAccounts.size()).isEqualTo(accounts.size());
        for (int i = 0; i < accounts.size(); i++) {
            for (String field : List.of("id", "fullName", "email", "telefono", "active", "forcePasswordChange", "roles")) {
                assertThat(currentAccounts.get(i).path(field)).as("unchanged user %s", field).isEqualTo(accounts.get(i).path(field));
            }
            assertThat(currentAccounts.get(i).path("clinica")).isEqualTo(currentClinic);
        }
        assertThat(currentClinic.path("plan").asText()).isEqualTo("CONSULTORIO");
        assertThat(currentClinic.path("planStatus").asText()).isEqualTo("ACTIVE");
        assertThat(currentClinic.path("readOnly").asBoolean()).isFalse();
        assertThat(call(GET, "/api/duenios/me", ownerToken, null, 200)).isEqualTo(ownerRecord);
        assertThat(myPlan(clinic).path("staffUsed").asInt()).isEqualTo(1);
        assertThat(myPlan(clinic).path("petsUsed").asInt()).isEqualTo(1);
        assertCounts(clinic, 2, 2);
    }

    @Test
    void normalizedReplayIsIdempotentEvenAfterLaterPaymentsAndExpiry() throws Exception {
        String normalized = "BANK " + reference() + " / 01";
        Map<String, Object> original = payload("PRO", "ACTIVATE", "  " + normalized.toLowerCase(Locale.ROOT).replace(" ", "   ") + "  ", null);
        original.put("reason", "  " + REASON + "  ");
        JsonNode first = pay(clinic, original, 200).path("plan");
        long firstId = first.path("lastPaymentId").asLong();
        JsonNode firstSnapshot = snapshot(clinic);
        assertThat(pay(clinic, payload("PRO", "ACTIVATE", normalized, null), 200).path("plan")).isEqualTo(first);
        assertThat(snapshot(clinic)).isEqualTo(firstSnapshot);
        assertThat(payment(first).getPaymentReference()).isEqualTo(normalized);
        Map<String, Object> renewal = payload("PRO", "RENEW", reference(), firstId);
        JsonNode second = pay(clinic, renewal, 200).path("plan");
        JsonNode third = pay(clinic, payload("PRO", "RENEW", reference(), second.path("lastPaymentId").asLong()), 200).path("plan");
        billingClock.set(payment(third).getPeriodEndsAt().plusSeconds(1));
        JsonNode beforeReplay = snapshot(clinic);
        for (Map<String, Object> replay : List.of(original, renewal)) {
            JsonNode response = pay(clinic, replay, 200).path("plan");
            assertThat(response.path("lastPaymentId")).isEqualTo(third.path("lastPaymentId"));
            assertThat(response.path("subscriptionEndsAt")).isEqualTo(third.path("subscriptionEndsAt"));
            assertThat(response.path("status").asText()).isEqualTo("SUBSCRIPTION_EXPIRED");
            assertThat(snapshot(clinic)).isEqualTo(beforeReplay);
        }
        assertCounts(clinic, 3, 3);
    }

    @ParameterizedTest
    @ValueSource(strings = {"reason", "operation", "plan", "expectedPaymentId"})
    void existingReferenceWithChangedPayloadConflictsBeforeAnyMutation(String field) throws Exception {
        String reference = reference();
        Map<String, Object> original = payload("PRO", "ACTIVATE", reference, null);
        long id = pay(clinic, original, 200).path("plan").path("lastPaymentId").asLong();
        JsonNode before = snapshot(clinic);
        Map<String, Object> changed = new LinkedHashMap<>(original);
        changed.put(field, switch (field) {
            case "reason" -> "Different confirmed transfer reason";
            case "operation" -> "RENEW";
            case "plan" -> "CONSULTORIO";
            default -> id;
        });
        assertCode(pay(clinic, changed, 409), "PAYMENT_REFERENCE_USED");
        assertThat(snapshot(clinic)).isEqualTo(before);
    }

    @Test
    void referenceCannotBeReusedByAnotherClinicEvenWithSamePayload() throws Exception {
        Clinic other = registerClinic();
        String reference = reference();
        Map<String, Object> request = payload("PRO", "ACTIVATE", reference, null);
        pay(clinic, request, 200);
        JsonNode firstBefore = snapshot(clinic);
        JsonNode otherBefore = snapshot(other);
        assertCode(pay(other, request, 409), "PAYMENT_REFERENCE_USED");
        assertThat(snapshot(clinic)).isEqualTo(firstBefore);
        assertThat(snapshot(other)).isEqualTo(otherBefore);
        assertCounts(other, 0, 0);
    }

    @Test
    void unknownReferencesRejectStaleNullAndForgedForeignExpectedPaymentIds() throws Exception {
        long first = activate("PRO").path("lastPaymentId").asLong();
        long current = pay(clinic, payload("PRO", "RENEW", reference(), first), 200).path("plan").path("lastPaymentId").asLong();
        Clinic other = registerClinic();
        long foreign = pay(other, payload("PRO", "ACTIVATE", reference(), null), 200).path("plan").path("lastPaymentId").asLong();
        JsonNode before = snapshot(clinic);
        JsonNode otherBefore = snapshot(other);
        for (Long expected : new Long[]{null, first, foreign, Long.MAX_VALUE}) {
            String reference = reference();
            assertCode(pay(clinic, payload("PRO", "RENEW", reference, expected), 409), "PAYMENT_VERSION_CONFLICT");
            assertThat(payments.findByPaymentReference(reference)).isEmpty();
            assertThat(snapshot(clinic)).isEqualTo(before);
            assertThat(snapshot(other)).isEqualTo(otherBefore);
        }
        assertThat(stored(clinic).getLastPaymentId()).isEqualTo(current);
    }

    @Test
    void renewalRequiresPreviousPaidCoverageAndNeverChangesPlansAtExpiry() throws Exception {
        JsonNode before = snapshot(clinic);
        assertCode(pay(clinic, payload("PRO", "RENEW", reference(), null), 409), "PAYMENT_REQUIRED");
        assertThat(snapshot(clinic)).isEqualTo(before);
        MonthlyPayment first = payment(activate("PRO"));
        billingClock.set(first.getPeriodEndsAt());
        JsonNode expired = snapshot(clinic);
        assertCode(pay(clinic, payload("CONSULTORIO", "RENEW", reference(), first.getId()), 409), "PLAN_MISMATCH");
        assertThat(snapshot(clinic)).isEqualTo(expired);
    }

    @ParameterizedTest(name = "invalid {0}: {1}")
    @MethodSource("invalidPayments")
    void invalidPaymentPayloadIsBadRequestWithoutAnySideEffects(String field, Object value) throws Exception {
        JsonNode before = snapshot(clinic);
        Map<String, Object> request = payload("PRO", "ACTIVATE", reference(), null);
        request.put(field, value);
        pay(clinic, request, 400);
        assertThat(snapshot(clinic)).isEqualTo(before);
        assertCounts(clinic, 0, 0);
    }

    static Stream<Arguments> invalidPayments() {
        return Stream.of(
                Arguments.of("paymentConfirmed", false), Arguments.of("paymentConfirmed", null),
                Arguments.of("reason", null), Arguments.of("reason", "    "), Arguments.of("reason", "abcd"),
                Arguments.of("reason", "x".repeat(501)), Arguments.of("plan", null),
                Arguments.of("plan", "TRIAL"), Arguments.of("plan", "FREE"), Arguments.of("plan", "pro"),
                Arguments.of("operation", null), Arguments.of("operation", "RESTART"), Arguments.of("operation", "renew"),
                Arguments.of("expectedPaymentId", 0), Arguments.of("expectedPaymentId", -1),
                Arguments.of("paymentReference", null), Arguments.of("paymentReference", "     "),
                Arguments.of("paymentReference", "ABCD"), Arguments.of("paymentReference", "X".repeat(121)),
                Arguments.of("paymentReference", "BANK\nREFERENCE"), Arguments.of("paymentReference", "BANK\rREFERENCE"),
                Arguments.of("paymentReference", "BANK\tREFERENCE"), Arguments.of("paymentReference", "BANK\u0000REFERENCE"),
                Arguments.of("paymentReference", "BANK@REFERENCE"), Arguments.of("paymentReference", "BANK\\REFERENCE"));
    }

    @Test
    void retiredPlanPatchRequiresOperatorAndNeverGrantsCoverage() throws Exception {
        JsonNode before = snapshot(clinic);
        assertCode(call(PATCH, base(clinic) + "/plan", operator.token(), Map.of("plan", "PRO", "reason", REASON), 410),
                "MONTHLY_PAYMENT_REQUIRED");
        call(PATCH, base(clinic) + "/plan", operator.token(), null, 410);
        call(PATCH, base(clinic) + "/plan", clinic.token(), Map.of("plan", "PRO", "reason", REASON), 403);
        assertThat(snapshot(clinic)).isEqualTo(before);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "DUENIO"})
    void tenantJwtCannotUseMonthlyPaymentHistoryOrAuditRoutes(String role) throws Exception {
        String token = role.equals("ADMIN") ? clinic.token() : registerOwner().path("accessToken").asText();
        JsonNode before = snapshot(clinic);
        call(POST, base(clinic) + "/mensualidades", token, payload("PRO", "ACTIVATE", reference(), null), 403);
        call(GET, base(clinic) + "/mensualidades", token, null, 403);
        call(GET, base(clinic) + "/auditoria", token, null, 403);
        assertThat(snapshot(clinic)).isEqualTo(before);
    }

    @Test
    void temporaryOperatorJwtCannotReadOrRecordPayments() throws Exception {
        Account temporary = account(RoleName.ROLE_SAAS_OPERATOR, null, true);
        JsonNode me = call(GET, "/api/auth/me", temporary.token(), null, 200);
        assertThat(me.path("forcePasswordChange").asBoolean()).isTrue();
        JsonNode before = snapshot(clinic);
        call(POST, base(clinic) + "/mensualidades", temporary.token(), payload("PRO", "ACTIVATE", reference(), null), 403);
        call(GET, base(clinic) + "/mensualidades", temporary.token(), null, 403);
        call(GET, base(clinic) + "/auditoria", temporary.token(), null, 403);
        call(PATCH, base(clinic) + "/plan", temporary.token(), Map.of("plan", "PRO", "reason", REASON), 403);
        assertThat(snapshot(clinic)).isEqualTo(before);
    }

    @Test
    void expiredClinicCanLoginRefreshAndReadButOperationalWritesAreForbidden() throws Exception {
        call(POST, "/api/servicios", clinic.token(), serviceBody(), 201);
        JsonNode services = call(GET, "/api/servicios", clinic.token(), null, 200);
        MonthlyPayment first = payment(activate("CONSULTORIO"));
        billingClock.set(first.getPeriodEndsAt());
        JsonNode login = login(clinic.email());
        JsonNode session = login.path("user").path("clinica");
        assertThat(session.path("planStatus").asText()).isEqualTo("SUBSCRIPTION_EXPIRED");
        assertThat(session.path("readOnly").asBoolean()).isTrue();
        String token = login.path("accessToken").asText();
        assertThat(call(GET, "/api/servicios", token, null, 200)).isEqualTo(services);
        assertThat(call(GET, "/api/auth/me", token, null, 200).path("clinica")).isEqualTo(session);
        JsonNode refreshed = call(POST, "/api/auth/refresh", null, Map.of("refreshToken", login.path("refreshToken").asText()), 200);
        assertThat(refreshed.path("user").path("clinica")).isEqualTo(session);
        JsonNode before = snapshot(clinic);
        assertCode(call(POST, "/api/servicios", token, serviceBody(), 403), "SUBSCRIPTION_EXPIRED");
        String email = email();
        assertCode(call(POST, "/api/usuarios/internal", token,
                Map.of("nombres", "Expired", "apellidos", "Staff", "email", email, "rol", "ASISTENTE"), 403), "SUBSCRIPTION_EXPIRED");
        assertThat(users.existsByEmail(email)).isFalse();
        assertThat(snapshot(clinic)).isEqualTo(before);
        assertThat(call(GET, "/api/servicios", token, null, 200)).isEqualTo(services);
        pay(clinic, payload("CONSULTORIO", "RENEW", reference(), first.getId()), 200);
        JsonNode active = call(GET, "/api/auth/me", token, null, 200).path("clinica");
        assertThat(active.path("planStatus").asText()).isEqualTo("ACTIVE");
        assertThat(active.path("readOnly").asBoolean()).isFalse();
        call(POST, "/api/servicios", token, serviceBody(), 201);
    }

    @Test
    void historyPaginationAndAuditExposeImmutableMonthlyPaymentDetails() throws Exception {
        String ref = reference();
        JsonNode first = pay(clinic, payload("PRO", "ACTIVATE", ref, null), 200).path("plan");
        JsonNode firstHistory = call(GET, base(clinic) + "/mensualidades", operator.token(), null, 200).path("items").get(0);
        billingClock.set(INITIAL_TIME.plusSeconds(1));
        long firstId = first.path("lastPaymentId").asLong();
        JsonNode second = pay(clinic, payload("PRO", "RENEW", reference(), firstId), 200).path("plan");
        JsonNode page0 = call(GET, base(clinic) + "/mensualidades?page=0&size=1", operator.token(), null, 200);
        JsonNode page1 = call(GET, base(clinic) + "/mensualidades?page=1&size=1", operator.token(), null, 200);
        assertThat(page0.path("page").asInt()).isZero();
        assertThat(page0.path("totalPages").asInt()).isEqualTo(2);
        assertThat(page0.path("totalElements").asInt()).isEqualTo(2);
        assertThat(page0.path("items").size()).isEqualTo(1);
        assertThat(page0.path("items").get(0).path("id")).isEqualTo(second.path("lastPaymentId"));
        assertThat(page0.path("items").get(0).path("previousPaymentId").asLong()).isEqualTo(firstId);
        assertThat(page1.path("page").asInt()).isEqualTo(1);
        assertThat(page1.path("items").get(0)).isEqualTo(firstHistory);
        assertThat(firstHistory.path("actorEmail").asText()).isEqualTo(operator.email());
        assertThat(firstHistory.path("paymentReference").asText()).isEqualTo(ref);
        assertMoney(firstHistory, "amount", "129.00");
        assertThat(firstHistory.path("currency").asText()).isEqualTo("PEN");
        assertThat(firstHistory.path("billingCycle").asText()).isEqualTo("MONTHLY");
        JsonNode auditItems = call(GET, base(clinic) + "/auditoria", operator.token(), null, 200).path("items");
        assertThat(auditItems.size()).isEqualTo(2);
        assertThat(auditItems.get(0).path("monthlyPayment")).isEqualTo(page0.path("items").get(0));
        assertThat(auditItems.get(1).path("monthlyPayment")).isEqualTo(firstHistory);
        assertThat(auditItems.get(1).path("id")).isEqualTo(firstHistory.path("auditId"));
        assertThat(call(GET, base(clinic) + "/mensualidades?page=2&size=1", operator.token(), null, 200)
                .path("items").isEmpty()).isTrue();
        Clinic empty = registerClinic();
        assertThat(call(GET, base(empty) + "/mensualidades", operator.token(), null, 200).path("totalElements").asInt()).isZero();
    }

    @Test
    void nonexistentClinicsAndInvalidHistoryPaginationDoNotMutateBilling() throws Exception {
        JsonNode before = snapshot(clinic);
        String missing = "/api/saas/clinicas/" + Long.MAX_VALUE;
        call(POST, missing + "/mensualidades", operator.token(), payload("PRO", "ACTIVATE", reference(), null), 404);
        call(GET, missing + "/mensualidades", operator.token(), null, 404);
        call(GET, missing + "/auditoria", operator.token(), null, 404);
        for (String query : List.of("page=-1", "size=0", "size=101")) {
            call(GET, base(clinic) + "/mensualidades?" + query, operator.token(), null, 400);
            call(GET, base(clinic) + "/auditoria?" + query, operator.token(), null, 400);
        }
        assertThat(snapshot(clinic)).isEqualTo(before);
    }

    @Test
    void concurrentSamePayloadInSeparateCommittedTransactionsCreatesOnlyOnePaymentAndAudit() throws Exception {
        Map<String, Object> request = payload("PRO", "ACTIVATE", reference(), null);
        List<Reply> results = race(
                () -> exchange(POST, base(clinic) + "/mensualidades", operator.token(), request),
                () -> exchange(POST, base(clinic) + "/mensualidades", operator.token(), request));
        assertThat(results).extracting(Reply::status).containsExactly(200, 200);
        long id = results.getFirst().body().path("plan").path("lastPaymentId").asLong();
        assertThat(id).isPositive();
        assertThat(results.get(1).body().path("plan").path("lastPaymentId").asLong()).isEqualTo(id);
        assertThat(stored(clinic).getLastPaymentId()).isEqualTo(id);
        assertCounts(clinic, 1, 1);
    }

    @Test
    void concurrentDifferentReferencesWithSameExpectedVersionHaveExactlyOneWinner() throws Exception {
        long expected = activate("PRO").path("lastPaymentId").asLong();
        Map<String, Object> first = payload("PRO", "RENEW", reference(), expected);
        Map<String, Object> second = payload("PRO", "RENEW", reference(), expected);
        List<Reply> results = race(
                () -> exchange(POST, base(clinic) + "/mensualidades", operator.token(), first),
                () -> exchange(POST, base(clinic) + "/mensualidades", operator.token(), second));
        assertThat(results).extracting(Reply::status).containsExactlyInAnyOrder(200, 409);
        Reply loser = results.stream().filter(reply -> reply.status() == 409).findFirst().orElseThrow();
        assertCode(loser.body(), "PAYMENT_VERSION_CONFLICT");
        Reply winner = results.stream().filter(reply -> reply.status() == 200).findFirst().orElseThrow();
        assertThat(stored(clinic).getLastPaymentId()).isEqualTo(winner.body().path("plan").path("lastPaymentId").asLong());
        long matchingReferences = Stream.of(first, second)
                .filter(body -> payments.findByPaymentReference((String) body.get("paymentReference")).isPresent()).count();
        assertThat(matchingReferences).isEqualTo(1);
        assertCounts(clinic, 2, 2);
    }

    @Test
    void crossClinicReferenceRaceTranslatesUniqueViolationAfterRollbackInto409Not500() throws Exception {
        Clinic other = registerClinic();
        String ref = reference();
        Map<String, Object> request = payload("PRO", "ACTIVATE", ref, null);
        JsonNode otherBefore = snapshot(other);
        CountDownLatch inserted = new CountDownLatch(1);
        CountDownLatch releaseCommit = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> transactions().execute(tx -> {
                try {
                    Reply reply = exchange(POST, base(clinic) + "/mensualidades", operator.token(), request);
                    assertThat(reply.status()).isEqualTo(200);
                    em.flush();
                    inserted.countDown();
                    await(releaseCommit);
                    return reply;
                } catch (Exception exception) {
                    throw new AssertionError(exception);
                }
            }));
            try {
                await(inserted);
                // The first insert is real but uncommitted. The other clinic has a different row lock.
                var second = executor.submit(() -> {
                    secondStarted.countDown();
                    return exchange(POST, base(other) + "/mensualidades", operator.token(), request);
                });
                await(secondStarted);
                assertThatThrownBy(() -> second.get(500, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
                releaseCommit.countDown();
                assertThat(first.get(15, TimeUnit.SECONDS).status()).isEqualTo(200);
                Reply rejected = second.get(15, TimeUnit.SECONDS);
                assertThat(rejected.status()).isEqualTo(409);
                assertCode(rejected.body(), "PAYMENT_REFERENCE_USED");
            } finally {
                releaseCommit.countDown();
            }
        }
        assertCounts(clinic, 1, 1);
        assertCounts(other, 0, 0);
        assertThat(snapshot(other)).isEqualTo(otherBefore);
        assertThat(payments.findByPaymentReference(ref).orElseThrow().getClinicId()).isEqualTo(clinic.id());
        // A fresh transaction after the rejected insert must still be usable.
        pay(other, payload("PRO", "ACTIVATE", reference(), null), 200);
        assertCounts(other, 1, 1);
    }

    @Test
    void forcedOuterTransactionFailureRollsBackPaymentAuditAndClinicTogether() throws Exception {
        JsonNode before = snapshot(clinic);
        String reference = reference();
        assertThatThrownBy(() -> transactions().executeWithoutResult(tx -> {
            try {
                pay(clinic, payload("PRO", "ACTIVATE", reference, null), 200);
                em.flush();
                assertThat(payments.findByPaymentReference(reference)).isPresent();
                assertCounts(clinic, 1, 1);
                assertThat(clinics.findById(clinic.id()).orElseThrow().getLastPaymentId()).isNotNull();
            } catch (Exception exception) {
                throw new AssertionError(exception);
            }
            throw new IllegalStateException("forced billing transaction failure");
        })).isInstanceOf(IllegalStateException.class).hasMessage("forced billing transaction failure");
        assertThat(payments.findByPaymentReference(reference)).isEmpty();
        assertThat(snapshot(clinic)).isEqualTo(before);
        assertCounts(clinic, 0, 0);
        pay(clinic, payload("PRO", "ACTIVATE", reference, null), 200);
        assertCounts(clinic, 1, 1);
    }

    @Test
    void persistedPaymentSnapshotsCannotBeDirtyCheckedIntoDifferentFinancialHistory() throws Exception {
        MonthlyPayment original = payment(activate("PRO"));
        JsonNode before = mapper.valueToTree(original);
        transactions().executeWithoutResult(tx -> {
            MonthlyPayment managed = em.find(MonthlyPayment.class, original.getId());
            ReflectionTestUtils.setField(managed, "amount", new BigDecimal("0.01"));
            ReflectionTestUtils.setField(managed, "reason", "Tampered financial history");
            ReflectionTestUtils.setField(managed, "currency", "USD");
            ReflectionTestUtils.setField(managed, "periodEndsAt", INITIAL_TIME.plusSeconds(1));
            em.flush();
            em.clear();
        });
        assertThat(mapper.<JsonNode>valueToTree(payments.findByPaymentReference(original.getPaymentReference()).orElseThrow()))
                .isEqualTo(before);
        assertCounts(clinic, 1, 1);
    }

    @ParameterizedTest
    @CsvSource({"CONSULTORIO,59.00", "PRO,129.00"})
    void clientFinancialFieldsCannotOverrideServerMonthlyPriceOrCurrency(String plan, String price) throws Exception {
        String ref = reference();
        Map<String, Object> body = payload(plan, "ACTIVATE", ref, null);
        body.put("amount", new BigDecimal("0.01"));
        body.put("currency", "USD");
        body.put("billingCycle", "ANNUAL");
        body.put("annualPrice", 1);
        JsonNode response = pay(clinic, body, 200);
        assertThat(response.toString()).doesNotContain("annualPrice");
        assertMoney(response.path("plan"), "monthlyPrice", price);
        MonthlyPayment payment = payments.findByPaymentReference(ref).orElseThrow();
        assertThat(payment.getAmount()).isEqualByComparingTo(price);
        assertThat(payment.getCurrency()).isEqualTo("PEN");
        assertThat(payment.getBillingCycle()).isEqualTo("MONTHLY");
        assertThat(call(GET, base(clinic) + "/mensualidades", operator.token(), null, 200).toString()).doesNotContain("annualPrice");
    }

    private Clinic registerClinic() throws Exception {
        String email = email();
        JsonNode response = call(POST, "/api/auth/register-clinic", null, Map.of(
                "fullName", "Monthly Administrator", "email", email, "password", PASSWORD,
                "telefono", "+51987654321", "clinicaNombre", "Monthly " + UUID.randomUUID()), 201);
        JsonNode tenant = response.path("user").path("clinica");
        return new Clinic(tenant.path("id").asLong(), tenant.path("slug").asText(), email,
                login(email).path("accessToken").asText());
    }

    private Account account(RoleName role, Long clinicId, boolean temporary) throws Exception {
        String email = email();
        Usuario user = users.saveAndFlush(Usuario.builder().fullName("Monthly " + role.name()).email(email)
                .password(encoder.encode(PASSWORD)).active(true).forcePasswordChange(temporary)
                .clinica(clinicId == null ? null : clinics.findById(clinicId).orElseThrow())
                .roles(Set.of(roles.findByName(role).orElseThrow()))
                .createdAt(LocalDateTime.ofInstant(billingClock.instant(), ZoneOffset.UTC)).build());
        return new Account(user.getId(), email, login(email).path("accessToken").asText());
    }

    private JsonNode registerOwner() throws Exception {
        return call(POST, "/api/auth/register", null, Map.of("fullName", "Monthly Owner", "email", email(),
                "password", PASSWORD, "telefono", "+51987654321", "clinicaSlug", clinic.slug()), 201);
    }

    private JsonNode login(String email) throws Exception {
        return call(POST, "/api/auth/login", null, Map.of("email", email, "password", PASSWORD), 200);
    }

    private JsonNode activate(String plan) throws Exception {
        return pay(clinic, payload(plan, "ACTIVATE", reference(), null), 200).path("plan");
    }

    private JsonNode pay(Clinic tenant, Map<String, Object> body, int expectedStatus) throws Exception {
        return call(POST, base(tenant) + "/mensualidades", operator.token(), body, expectedStatus);
    }

    private JsonNode myPlan(Clinic tenant) throws Exception {
        return call(GET, "/api/clinicas/me/plan", tenant.token(), null, 200);
    }

    private JsonNode changeEstado(String state) throws Exception {
        return call(PATCH, base(clinic) + "/estado", operator.token(), Map.of("estado", state, "reason", REASON), 200);
    }

    private MonthlyPayment payment(JsonNode plan) {
        return payments.findByClinicId(clinic.id(), ALL).getContent().stream()
                .filter(payment -> payment.getId().equals(plan.path("lastPaymentId").asLong())).findFirst().orElseThrow();
    }

    private Clinica stored(Clinic tenant) {
        return clinics.findById(tenant.id()).orElseThrow();
    }

    private JsonNode snapshot(Clinic tenant) {
        return mapper.valueToTree(Map.of("clinic", stored(tenant),
                "payments", payments.findByClinicId(tenant.id(), ALL).getContent(),
                "audits", audits.findByClinicId(tenant.id(), ALL).getContent()));
    }

    private void assertCounts(Clinic tenant, long expectedPayments, long expectedAudits) {
        assertThat(payments.findByClinicId(tenant.id(), ALL).getTotalElements()).isEqualTo(expectedPayments);
        assertThat(audits.findByClinicId(tenant.id(), ALL).getTotalElements()).isEqualTo(expectedAudits);
    }

    private void assertPlanState(JsonNode plan, String status, boolean readOnly, boolean canActivate, boolean canRenew) {
        assertThat(plan.path("status").asText()).isEqualTo(status);
        assertThat(plan.path("readOnly").asBoolean()).isEqualTo(readOnly);
        assertThat(plan.path("canActivate").asBoolean()).isEqualTo(canActivate);
        assertThat(plan.path("canRenew").asBoolean()).isEqualTo(canRenew);
        assertThat(plan.path("currency").asText()).isEqualTo("PEN");
        assertThat(plan.path("billingCycle").asText()).isEqualTo("MONTHLY");
        assertThat(plan.has("annualPrice")).isFalse();
    }

    private void assertMoney(JsonNode node, String field, String expected) {
        assertThat(new BigDecimal(node.path(field).asText())).isEqualByComparingTo(expected);
    }

    private void assertCode(JsonNode body, String expected) {
        assertThat(body.path("code").asText()).isEqualTo(expected);
    }

    private JsonNode call(HttpMethod method, String path, String token, Object body, int expectedStatus) throws Exception {
        Reply response = exchange(method, path, token, body);
        assertThat(response.status()).as("%s %s: %s", method, path, response.body()).isEqualTo(expectedStatus);
        return response.body();
    }

    private Reply exchange(HttpMethod method, String path, String token, Object body) throws Exception {
        var builder = request(method, path).accept(MediaType.APPLICATION_JSON);
        if (token != null) builder.header("Authorization", "Bearer " + token);
        if (body != null) builder.contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsBytes(body));
        var response = mvc.perform(builder).andReturn().getResponse();
        String content = response.getContentAsString();
        return new Reply(response.getStatus(), content.isBlank() ? mapper.nullNode() : mapper.readTree(content));
    }

    private List<Reply> race(Callable<Reply> first, Callable<Reply> second) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var a = executor.submit(() -> {
                assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
                ready.countDown();
                await(start);
                return first.call();
            });
            var b = executor.submit(() -> {
                assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
                ready.countDown();
                await(start);
                return second.call();
            });
            try {
                await(ready);
            } finally {
                start.countDown();
            }
            return List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
        }
    }

    private static void await(CountDownLatch latch) throws InterruptedException {
        assertThat(latch.await(10, TimeUnit.SECONDS)).as("concurrent billing transaction synchronization").isTrue();
    }

    private TransactionTemplate transactions() {
        return new TransactionTemplate(transactionManager);
    }

    private Map<String, Object> payload(String plan, String operation, String reference, Long expected) {
        Map<String, Object> request = new LinkedHashMap<>(Map.of("plan", plan, "operation", operation,
                "paymentReference", reference, "reason", REASON, "paymentConfirmed", true));
        request.put("expectedPaymentId", expected);
        return request;
    }

    private Map<String, Object> serviceBody() {
        return Map.of("nombre", "Monthly service " + UUID.randomUUID(), "descripcion", "Billing gate check", "costoBase", 20);
    }

    private String reference() {
        return "MONTHLY-" + UUID.randomUUID().toString().toUpperCase(Locale.ROOT);
    }

    private String email() {
        return "monthly-" + UUID.randomUUID() + "@billing.example";
    }

    private String base(Clinic tenant) {
        return "/api/saas/clinicas/" + tenant.id();
    }

    private record Clinic(long id, String slug, String email, String token) {}
    private record Account(long id, String email, String token) {}
    private record Reply(int status, JsonNode body) {}

    @TestConfiguration(proxyBeanMethods = false)
    static class BillingClockConfiguration {
        @Bean
        @Primary
        MutableBillingClock monthlyBillingClock() {
            return new MutableBillingClock(new AtomicReference<>(INITIAL_TIME), ZoneOffset.UTC);
        }
    }

    static final class MutableBillingClock extends Clock {
        private final AtomicReference<Instant> instant;
        private final ZoneId zone;

        private MutableBillingClock(AtomicReference<Instant> instant, ZoneId zone) {
            this.instant = instant;
            this.zone = zone;
        }

        void set(Instant value) {
            instant.set(value.truncatedTo(ChronoUnit.MICROS));
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId requestedZone) {
            return new MutableBillingClock(instant, requestedZone);
        }

        @Override
        public Instant instant() {
            return instant.get();
        }
    }
}
