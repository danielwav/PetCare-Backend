package com.petcare.backend.domain.service;

import com.petcare.backend.domain.dto.request.*;
import com.petcare.backend.domain.repository.*;
import com.petcare.backend.persistence.entity.*;
import com.petcare.backend.persistence.enums.RoleName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuotaEntrypointTest {
    @Mock PlanService plans;
    @Mock UsuarioRepository users;
    @Mock RolRepository roles;
    @Mock ClinicaRepository clinics;
    @Mock AsistenteRepository assistants;
    @Mock DuenioRepository owners;
    @Mock MascotaRepository pets;
    @Mock AuthenticatedDuenioService authenticatedOwner;
    @Mock PasswordEncoder encoder;
    @Mock ClinicaService clinicService;
    @Mock jakarta.persistence.EntityManager entityManager;
    @InjectMocks UsuarioService usuarioService;
    @InjectMocks AuthService authService;
    @InjectMocks AsistenteService asistenteService;
    @InjectMocks MascotaService mascotaService;

    private Clinica clinic;
    private Usuario user;

    @BeforeEach
    void setup() {
        clinic = Clinica.builder().id(42L)
                .plan(com.petcare.backend.persistence.enums.PlanClinica.PRO)
                .estado(com.petcare.backend.persistence.enums.EstadoClinica.ACTIVA).build();
        user = Usuario.builder().id(7L).clinica(clinic).email("user@test.com")
                .active(true).roles(new HashSet<>(Set.of(role(RoleName.ROLE_DUENIO)))).build();
    }

    @Test
    void ownerToMultipleStaffRolesChecksCapacityBeforeRolesChange() {
        when(users.findById(7L)).thenReturn(Optional.of(user));
        resolveRoles(RoleName.ROLE_ADMIN, RoleName.ROLE_ASISTENTE);
        denySeat();
        assertThatThrownBy(() -> usuarioService.updateRoles(7L,
                new UpdateUserRolesRequest(Set.of("ROLE_ADMIN", "ROLE_ASISTENTE")), 42L))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(user.getRoles()).extracting(Rol::getName).containsExactly(RoleName.ROLE_DUENIO);
        verify(users, never()).save(any());
    }

    @Test
    void staffToStaffAndInactiveOwnerRoleChangesDoNotAddSeats() {
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(users.save(user)).thenReturn(user);
        resolveRoles(RoleName.ROLE_ADMIN, RoleName.ROLE_VETERINARIO);
        user.setRoles(new HashSet<>(Set.of(role(RoleName.ROLE_ADMIN), role(RoleName.ROLE_ASISTENTE))));
        usuarioService.updateRoles(7L, new UpdateUserRolesRequest(Set.of("ROLE_VETERINARIO")), 42L);
        user.setActive(false);
        user.setRoles(new HashSet<>(Set.of(role(RoleName.ROLE_DUENIO))));
        usuarioService.updateRoles(7L, new UpdateUserRolesRequest(Set.of("ROLE_ADMIN")), 42L);
        verify(plans, never()).assertStaffCapacity(any());
        verify(plans, times(2)).lockForWrite(42L);
    }

    @Test
    void inactiveMultiroleStaffToggleChecksBeforeActivation() {
        user.setActive(false);
        user.getRoles().add(role(RoleName.ROLE_VETERINARIO));
        when(users.findById(7L)).thenReturn(Optional.of(user));
        denySeat();
        assertThatThrownBy(() -> usuarioService.toggleActive(7L, 42L)).isInstanceOf(AccessDeniedException.class);
        assertThat(user.getActive()).isFalse();
    }

    @Test
    void tokenActivationUsesTokenUsersClinicAndPreservesTokenOnDenial() {
        user.setActive(false);
        user.getRoles().add(role(RoleName.ROLE_ADMIN));
        user.setActivationToken("activation");
        user.setTokenExpiry(LocalDateTime.now().plusHours(1));
        when(users.findByActivationToken("activation")).thenReturn(Optional.of(user));
        denySeat();
        assertThatThrownBy(() -> authService.activateWithToken("activation", "password123"))
                .isInstanceOf(AccessDeniedException.class);
        var order = inOrder(plans, entityManager);
        order.verify(plans).lockForWrite(42L);
        order.verify(entityManager).refresh(user);
        order.verify(plans).assertStaffCapacity(42L);
        assertThat(user.getActive()).isFalse();
        assertThat(user.getActivationToken()).isEqualTo("activation");
        verifyNoInteractions(encoder);
    }

    @Test
    void internalStaffCreationChecksBeforeUserWrite() {
        resolveRoles(RoleName.ROLE_VETERINARIO);
        when(clinics.findById(42L)).thenReturn(Optional.of(clinic));
        denySeat();
        assertThatThrownBy(() -> authService.createInternalUser(
                new CreateInternalUserRequest("Vet", "One", "vet@test.com", "VETERINARIO"), 42L))
                .isInstanceOf(AccessDeniedException.class);
        verify(plans).assertCanWrite(42L);
        verify(users, never()).save(any());
    }

    @Test
    void tokenActivationUsesStaffRolesRefreshedUnderLock() {
        user.setActive(false);
        user.setActivationToken("activation");
        user.setTokenExpiry(LocalDateTime.now().plusHours(1));
        when(users.findByActivationToken("activation")).thenReturn(Optional.of(user));
        doAnswer(invocation -> {
            user.getRoles().add(role(RoleName.ROLE_ASISTENTE));
            return null;
        }).when(entityManager).refresh(user);
        denySeat();
        assertThatThrownBy(() -> authService.activateWithToken("activation", "password123"))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(user.getActive()).isFalse();
        verify(users, never()).save(any());
    }

    @Test
    void tokenActivationRejectsTokenConsumedWhileWaitingForLock() {
        user.setActive(false);
        user.setActivationToken("activation");
        user.setTokenExpiry(LocalDateTime.now().plusHours(1));
        when(users.findByActivationToken("activation")).thenReturn(Optional.of(user));
        doAnswer(invocation -> {
            user.setActivationToken(null);
            user.setActive(true);
            return null;
        }).when(entityManager).refresh(user);
        assertThatThrownBy(() -> authService.activateWithToken("activation", "password123"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("invalido");
        verify(plans, never()).assertStaffCapacity(any());
        verify(users, never()).save(any());
    }

    @Test
    void tokenActivationRechecksExpiryAfterRefresh() {
        user.setActive(false);
        user.setActivationToken("activation");
        user.setTokenExpiry(LocalDateTime.now().plusHours(1));
        when(users.findByActivationToken("activation")).thenReturn(Optional.of(user));
        doAnswer(invocation -> {
            user.setTokenExpiry(LocalDateTime.now().minusHours(1));
            return null;
        }).when(entityManager).refresh(user);
        assertThatThrownBy(() -> authService.activateWithToken("activation", "password123"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("expirado");
        verify(users, never()).save(any());
    }

    @Test
    void publicOwnerRegistrationGuardsTargetClinicBeforeAnyWrite() {
        clinic.setEstado(com.petcare.backend.persistence.enums.EstadoClinica.ACTIVA);
        when(clinics.findBySlug("target")).thenReturn(Optional.of(clinic));
        resolveRoles(RoleName.ROLE_DUENIO);
        doThrow(new AccessDeniedException("read only")).when(plans).assertCanWrite(42L);
        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("Owner", "owner@test.com", "password123", "123", "target")))
                .isInstanceOf(AccessDeniedException.class);
        verify(users, never()).save(any());
        verifyNoInteractions(owners);
    }

    @Test
    void assistantLinkingActiveOwnerChecksBeforeRoleMutation() {
        when(users.findById(7L)).thenReturn(Optional.of(user));
        resolveRoles(RoleName.ROLE_ASISTENTE);
        denySeat();
        assertThatThrownBy(() -> asistenteService.create(assistantRequest(7L), 42L))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(user.getRoles()).extracting(Rol::getName).containsExactly(RoleName.ROLE_DUENIO);
        verify(users, never()).save(any());
    }

    @Test
    void assistantUpdateChecksBeforeMutatingLinkedOwnerDetails() {
        var assistant = Asistente.builder().id(3L).clinica(clinic).usuario(user).build();
        when(assistants.findById(3L)).thenReturn(Optional.of(assistant));
        resolveRoles(RoleName.ROLE_ASISTENTE);
        denySeat();
        assertThatThrownBy(() -> asistenteService.update(3L, assistantRequest(null), 42L))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(user.getEmail()).isEqualTo("user@test.com");
        assertThat(user.getFullName()).isNull();
        assertThat(user.getRoles()).extracting(Rol::getName).containsExactly(RoleName.ROLE_DUENIO);
    }

    @Test
    void assistantNewUserChecksCapacityBeforeSave() {
        when(clinics.findById(42L)).thenReturn(Optional.of(clinic));
        resolveRoles(RoleName.ROLE_ASISTENTE);
        denySeat();
        assertThatThrownBy(() -> asistenteService.create(assistantRequest(null), 42L))
                .isInstanceOf(AccessDeniedException.class);
        verify(users, never()).save(any());
    }

    @Test
    void assistantLinkingAlreadyCountedMultiroleUserDoesNotAddSeat() {
        user.getRoles().add(role(RoleName.ROLE_ADMIN));
        user.getRoles().add(role(RoleName.ROLE_VETERINARIO));
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(users.save(user)).thenReturn(user);
        when(clinics.findById(42L)).thenReturn(Optional.of(clinic));
        when(assistants.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        resolveRoles(RoleName.ROLE_ASISTENTE);
        asistenteService.create(assistantRequest(7L), 42L);
        verify(plans, never()).assertStaffCapacity(any());
    }

    @Test
    void assistantActivationChecksLinkedInactiveStaffBeforeEitherMutation() {
        user.setActive(false);
        user.getRoles().add(role(RoleName.ROLE_ASISTENTE));
        var assistant = Asistente.builder().id(3L).clinica(clinic).usuario(user).active(false).build();
        when(assistants.findById(3L)).thenReturn(Optional.of(assistant));
        denySeat();
        assertThatThrownBy(() -> asistenteService.activate(3L, 42L)).isInstanceOf(AccessDeniedException.class);
        assertThat(user.getActive()).isFalse();
        assertThat(assistant.getActive()).isFalse();
    }

    @Test
    void ownerPetDelegateCannotBypassCapacity() {
        var owner = Duenio.builder().id(9L).clinica(clinic).active(true).build();
        when(authenticatedOwner.findByAuthenticatedEmail("owner@test.com")).thenReturn(owner);
        when(owners.findById(9L)).thenReturn(Optional.of(owner));
        doThrow(new AccessDeniedException("full")).when(plans).assertPetCapacity(42L);
        var request = new MascotaRequest(999L, "Pet", "Dog", "Breed", null, null, null, null, null, null);
        assertThatThrownBy(() -> mascotaService.createForDuenio("owner@test.com", request))
                .isInstanceOf(AccessDeniedException.class);
        verify(plans).assertCanWrite(42L);
        verify(pets, never()).save(any());
    }

    private void denySeat() {
        doThrow(new AccessDeniedException("full")).when(plans).assertStaffCapacity(42L);
    }

    private void resolveRoles(RoleName... names) {
        for (RoleName name : names) {
            when(roles.findByName(name)).thenReturn(Optional.of(role(name)));
        }
    }

    private Rol role(RoleName name) {
        return Rol.builder().name(name).build();
    }

    private AsistenteRequest assistantRequest(Long userId) {
        return new AsistenteRequest(userId, "Changed", "Name", "DNI", "123", "123",
                "changed@test.com", "Reception", "password123");
    }
}
