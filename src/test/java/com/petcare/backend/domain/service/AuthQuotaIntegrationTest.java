package com.petcare.backend.domain.service;

import com.petcare.backend.domain.dto.request.CreateInternalUserRequest;
import com.petcare.backend.domain.dto.request.LoginRequest;
import com.petcare.backend.domain.dto.request.RefreshTokenRequest;
import com.petcare.backend.domain.dto.request.RegisterRequest;
import com.petcare.backend.domain.repository.ClinicaRepository;
import com.petcare.backend.domain.repository.UsuarioRepository;
import com.petcare.backend.persistence.enums.PlanClinica;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AuthQuotaIntegrationTest {
    @Autowired AuthService auth;
    @Autowired UsuarioService users;
    @Autowired ClinicaService clinicService;
    @Autowired ClinicaRepository clinics;
    @Autowired UsuarioRepository userRepository;
    @Autowired PlanService plans;
    @Autowired PlatformTransactionManager transactionManager;

    @Test
    void concurrentInactiveOwnerActivationAndPromotionCannotExceedFullQuota() throws Exception {
        var clinic = clinicService.createClinic("Owner race", "owner-race");
        clinic.setPlan(PlanClinica.CONSULTORIO);
        clinics.save(clinic);
        var owner = auth.register(new RegisterRequest("Owner", "owner@test.com", "password123", "123", clinic.getSlug()));
        users.toggleActive(owner.user().id(), clinic.getId());
        auth.createInternalUser(staffRequest("first@test.com"), clinic.getId());
        auth.createInternalUser(staffRequest("second@test.com"), clinic.getId());

        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var activation = executor.submit(() -> changeConcurrent(
                    () -> users.toggleActive(owner.user().id(), clinic.getId()), ready, start));
            var promotion = executor.submit(() -> changeConcurrent(
                    () -> users.updateRoles(owner.user().id(),
                            new com.petcare.backend.domain.dto.request.UpdateUserRolesRequest(
                                    java.util.Set.of("ROLE_ASISTENTE")), clinic.getId()), ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                plans.lockForWrite(clinic.getId());
                start.countDown();
                // Both seat-neutral decisions must wait for the same real database row lock.
                assertThatThrownBy(() -> activation.get(250, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
                assertThatThrownBy(() -> promotion.get(250, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            });
            assertThat(activation.get(10, TimeUnit.SECONDS) + promotion.get(10, TimeUnit.SECONDS)).isEqualTo(1);
        }
        var plan = plans.getPlan(clinic.getId());
        assertThat(plan.staffUsed()).isEqualTo(2).isLessThanOrEqualTo(plan.staffLimit());
        var finalOwner = auth.meById(owner.user().id());
        assertThat(finalOwner.active() && finalOwner.roles().contains("ROLE_ASISTENTE")).isFalse();
    }

    @Test
    void concurrentInternalCreationCannotConsumeSameLastSeat() throws Exception {
        var clinic = clinicService.createClinic("Concurrent", "concurrent-quota");
        clinic.setPlan(PlanClinica.CONSULTORIO);
        clinics.save(clinic);
        auth.createInternalUser(staffRequest("first@test.com"), clinic.getId());
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> createConcurrent("second@test.com", clinic.getId(), ready, start));
            var second = executor.submit(() -> createConcurrent("third@test.com", clinic.getId(), ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(first.get(10, TimeUnit.SECONDS) + second.get(10, TimeUnit.SECONDS)).isEqualTo(1);
        }
        assertThat(userRepository.findAllByClinicaId(clinic.getId())).hasSize(2);
    }

    @Test
    void rolePromotionAndActivationRespectActualCapacityAndRollbackState() {
        var clinic = clinicService.createClinic("Capacity", "actual-capacity");
        clinic.setPlan(PlanClinica.CONSULTORIO);
        clinics.save(clinic);
        var owner = auth.register(new RegisterRequest("Owner", "owner@test.com", "password123", "123", clinic.getSlug()));
        var promoted = users.updateRoles(owner.user().id(),
                new com.petcare.backend.domain.dto.request.UpdateUserRolesRequest(
                        java.util.Set.of("ROLE_ADMIN", "ROLE_ASISTENTE")), clinic.getId());
        assertThat(promoted.roles()).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_ASISTENTE");
        auth.createInternalUser(staffRequest("second@test.com"), clinic.getId());
        users.toggleActive(owner.user().id(), clinic.getId());
        auth.createInternalUser(staffRequest("third@test.com"), clinic.getId());
        assertThatThrownBy(() -> users.toggleActive(owner.user().id(), clinic.getId()))
                .isInstanceOf(PlanException.class).hasMessageContaining("Limite");
        assertThat(userRepository.findById(owner.user().id()).orElseThrow().getActive()).isFalse();
    }

    @Test
    void expiredTrialBlocksRegistrationButAllowsLoginRefreshAndPasswordChange() {
        var clinic = clinicService.createClinic("Expired", "expired-quota");
        var registered = auth.register(new RegisterRequest("Owner", "owner@test.com", "password123", "123", clinic.getSlug()));
        clinic.setTrialEndsAt(LocalDateTime.now().minusDays(1));
        clinics.save(clinic);
        assertThatThrownBy(() -> auth.register(new RegisterRequest("New", "new@test.com", "password123", "123", clinic.getSlug())))
                .isInstanceOf(PlanException.class);
        assertThat(auth.login(new LoginRequest("owner@test.com", "password123")).accessToken()).isNotBlank();
        assertThat(auth.refresh(new RefreshTokenRequest(registered.refreshToken())).accessToken()).isNotBlank();
        auth.changePassword("owner@test.com", "password123", "changed123");
        assertThat(auth.login(new LoginRequest("owner@test.com", "changed123")).accessToken()).isNotBlank();
        assertThat(userRepository.existsByEmail("new@test.com")).isFalse();
    }

    private int createConcurrent(String email, Long clinicId, CountDownLatch ready, CountDownLatch start) throws Exception {
        return changeConcurrent(() -> auth.createInternalUser(staffRequest(email), clinicId), ready, start);
    }

    private int changeConcurrent(Runnable change, CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Start timed out");
        try {
            change.run();
            return 1;
        } catch (PlanException expected) {
            assertThat(expected.getMessage()).contains("Limite");
            return 0;
        }
    }

    private CreateInternalUserRequest staffRequest(String email) {
        return new CreateInternalUserRequest("Staff", "User", email, "ASISTENTE");
    }
}
