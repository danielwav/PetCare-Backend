package com.petcare.backend.domain.service;

import com.petcare.backend.domain.dto.request.DuenioRequest;
import com.petcare.backend.domain.dto.request.RegisterRequest;
import com.petcare.backend.domain.dto.response.AuthResponse;
import com.petcare.backend.domain.dto.response.DuenioResponse;
import com.petcare.backend.domain.repository.DuenioRepository;
import com.petcare.backend.domain.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class DuenioServiceTest {

	@Autowired
	private DuenioService duenioService;

	@Autowired
	private AuthService authService;

	@Autowired
	private ClinicaService clinicaService;

	@Autowired
	private DuenioRepository duenioRepository;

	@Autowired
	private UsuarioRepository usuarioRepository;

	private Long clinicaId() {
		return clinicaService.getOrCreateDefaultClinic().getId();
	}

	@Test
	void createFindUpdateAndDeactivateDuenio() {
		DuenioResponse created = duenioService.create(new DuenioRequest(
				null,
				"Maria",
				"Lopez",
				"DNI",
				"70000001",
				"999111222",
				"maria.lopez@test.com",
				"Av. PetCare 123"
		), clinicaId());

		DuenioResponse found = duenioService.findById(created.id(), clinicaId());
		DuenioResponse updated = duenioService.update(created.id(), new DuenioRequest(
				null,
				"Maria Fernanda",
				"Lopez",
				"DNI",
				"70000001",
				"999111333",
				"maria.fernanda@test.com",
				"Av. PetCare 456"
		), clinicaId());

		duenioService.deactivate(created.id(), clinicaId());
		DuenioResponse inactive = duenioService.findById(created.id(), clinicaId());

		assertThat(found.email()).isEqualTo("maria.lopez@test.com");
		assertThat(updated.nombres()).isEqualTo("Maria Fernanda");
		assertThat(updated.telefono()).isEqualTo("999111333");
		assertThat(inactive.active()).isFalse();
	}

	@Test
	void searchDueniosByTextAndActiveStatus() {
		duenioService.create(new DuenioRequest(
				null,
				"Carlos",
				"Paredes",
				"DNI",
				"70000002",
				"999222333",
				"carlos.paredes@test.com",
				null
		), clinicaId());
		DuenioResponse inactive = duenioService.create(new DuenioRequest(
				null,
				"Lucia",
				"Ramos",
				"DNI",
				"70000003",
				"999333444",
				"lucia.ramos@test.com",
				null
		), clinicaId());
		duenioService.deactivate(inactive.id(), clinicaId());

		List<DuenioResponse> activeResults = duenioService.findAll("paredes", true, clinicaId());
		List<DuenioResponse> inactiveResults = duenioService.findAll(null, false, clinicaId());

		assertThat(activeResults).hasSize(1);
		assertThat(activeResults.getFirst().email()).isEqualTo("carlos.paredes@test.com");
		assertThat(inactiveResults).hasSize(1);
		assertThat(inactiveResults.getFirst().email()).isEqualTo("lucia.ramos@test.com");
	}

	@Test
	void rejectDuplicatedEmailAndDocument() {
		duenioService.create(new DuenioRequest(
				null,
				"Ana",
				"Torres",
				"DNI",
				"70000004",
				"999444555",
				"ana.torres@test.com",
				null
		), clinicaId());

		assertThatThrownBy(() -> duenioService.create(new DuenioRequest(
				null,
				"Ana",
				"Duplicada",
				"DNI",
				"70000005",
				"999444556",
				"ana.torres@test.com",
				null
		), clinicaId())).isInstanceOf(IllegalArgumentException.class);

		assertThatThrownBy(() -> duenioService.create(new DuenioRequest(
				null,
				"Documento",
				"Duplicado",
				"DNI",
				"70000004",
				"999444557",
				"documento.duplicado@test.com",
				null
		), clinicaId())).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void createDuenioLinksExistingDuenioUserByEmail() {
		AuthResponse ownerUser = authService.register(new RegisterRequest("Owner", "owner.link@test.com", "secret123", "000000000"));
		duenioRepository.deleteById(duenioService.findOwn(ownerUser.user().email()).id());

		DuenioResponse owner = duenioService.create(new DuenioRequest(
				null,
				"Owner",
				"Linked",
				"DNI",
				"70000008",
				"999444562",
				"owner.link@test.com",
				null
		), clinicaId());

		assertThat(owner.usuarioId()).isEqualTo(ownerUser.user().id());
		assertThat(duenioService.findOwn(ownerUser.user().email()).id()).isEqualTo(owner.id());
	}

	@Test
	void duenioCanOnlyReadAndUpdateOwnProfile() {
		AuthResponse ownerUser = authService.register(new RegisterRequest("Owner", "owner.duenio@test.com", "secret123", "000000000"));
		AuthResponse otherUser = authService.register(new RegisterRequest("Other", "other.duenio@test.com", "secret123", "000000000"));

		DuenioResponse owner = duenioService.update(duenioService.findOwn(ownerUser.user().email()).id(), new DuenioRequest(
				ownerUser.user().id(),
				"Owner",
				"Principal",
				"DNI",
				"70000006",
				"999444558",
				"owner.profile@test.com",
				null
		), clinicaId());
		DuenioResponse other = duenioService.update(duenioService.findOwn(otherUser.user().email()).id(), new DuenioRequest(
				otherUser.user().id(),
				"Other",
				"Owner",
				"DNI",
				"70000007",
				"999444559",
				"other.profile@test.com",
				null
		), clinicaId());

		DuenioResponse ownProfile = duenioService.findOwn(ownerUser.user().email());
		DuenioResponse updated = duenioService.updateOwn(owner.id(), new DuenioRequest(
				null,
				"Owner Editado",
				"Principal",
				"DNI",
				"70000006",
				"999444560",
				"owner.edited@test.com",
				"Av. Propia 123"
		), ownerUser.user().email());

		assertThat(ownProfile.id()).isEqualTo(owner.id());
		assertThat(updated.nombres()).isEqualTo("Owner Editado");
		assertThatThrownBy(() -> duenioService.findOwnById(other.id(), updated.email()))
				.isInstanceOf(AccessDeniedException.class);
		assertThatThrownBy(() -> duenioService.updateOwn(owner.id(), new DuenioRequest(
				otherUser.user().id(),
				"Owner",
				"Principal",
				"DNI",
				"70000006",
				"999444561",
				"owner.fail@test.com",
				null
		), updated.email())).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void emailAndDocumentAreUniqueOnlyWithinClinicForCreateAndUpdate() {
		Long otherClinicId = clinicaService.createClinic("Other", "other-owner").getId();
		DuenioRequest shared = new DuenioRequest(null, "Shared", "Owner", "DNI",
				"70000901", "999444555", "shared@test.com", null);
		DuenioResponse first = duenioService.create(shared, clinicaId());
		DuenioResponse second = duenioService.create(shared, otherClinicId);
		assertThat(second.id()).isNotEqualTo(first.id());
		assertThat(duenioService.update(second.id(), shared, otherClinicId).email()).isEqualTo(shared.email());
		DuenioResponse third = duenioService.create(new DuenioRequest(null, "Third", "Owner", "DNI",
				"70000902", "999444555", "third@test.com", null), otherClinicId);
		assertThatThrownBy(() -> duenioService.update(third.id(), new DuenioRequest(null, "Third", "Owner", "DNI",
				"70000902", "999444555", " SHARED@test.com ", null), otherClinicId))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("correo");
		assertThatThrownBy(() -> duenioService.update(third.id(), new DuenioRequest(null, "Third", "Owner", "DNI",
				" 70000901 ", "999444555", "third@test.com", null), otherClinicId))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("documento");
	}

	@Test
	void createDoesNotAutoLinkUserFromAnotherClinicAndRejectsExplicitCrossClinicLink() {
		AuthResponse user = authService.register(new RegisterRequest("Owner", "cross-owner@test.com", "secret123", "000000000"));
		Long otherClinicId = clinicaService.createClinic("Other", "cross-owner").getId();
		DuenioResponse owner = duenioService.create(new DuenioRequest(null, "Other", "Owner", "DNI",
				"70000903", "999444555", user.user().email(), null), otherClinicId);
		assertThat(owner.usuarioId()).isNull();
		assertThat(duenioService.findOwn(user.user().email()).id()).isNotEqualTo(owner.id());
		assertThatThrownBy(() -> duenioService.update(owner.id(), new DuenioRequest(user.user().id(), "Other", "Owner", "DNI",
				"70000903", "999444555", user.user().email(), null), otherClinicId))
				.isInstanceOf(AccessDeniedException.class);
		assertThatThrownBy(() -> duenioService.create(new DuenioRequest(user.user().id(), "Other", "Owner", "DNI",
				"70000904", "999444555", "explicit@test.com", null), otherClinicId))
				.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void ownUpdateCannotMutateAccountSelectedBySubmittedEmail() {
		AuthResponse owner = authService.register(new RegisterRequest("Owner Original", "safe-owner@test.com", "secret123", "000000000"));
		String otherSlug = clinicaService.createClinic("Other", "identity-other").getSlug();
		AuthResponse victim = authService.register(new RegisterRequest("Victim Original", "victim@test.com", "secret123", "111111111", otherSlug));
		DuenioResponse profile = duenioService.findOwn(owner.user().email());
		DuenioRequest malicious = new DuenioRequest(null, "Changed", "Name", "DNI", "70000905",
				"999444555", victim.user().email(), null);
		assertThatThrownBy(() -> duenioService.updateOwn(profile.id(), malicious, owner.user().email()))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("otro usuario");
		assertThat(authService.me(victim.user().email()).fullName()).isEqualTo("Victim Original");
		assertThat(authService.me(owner.user().email()).fullName()).isEqualTo("Owner Original");
		assertThat(duenioService.findOwn(owner.user().email()).email()).isEqualTo(owner.user().email());

		DuenioResponse unlinked = duenioService.create(new DuenioRequest(null, "Unlinked", "Owner", "DNI",
				"70000906", "999444555", "unlinked@test.com", null), clinicaId());
		var account = usuarioRepository.findById(owner.user().id()).orElseThrow();
		account.setEmail(unlinked.email());
		usuarioRepository.save(account);
		duenioRepository.deleteById(profile.id());
		assertThatThrownBy(() -> duenioService.updateOwn(unlinked.id(), malicious, unlinked.email()))
				.isInstanceOf(AccessDeniedException.class);
		assertThat(authService.me(victim.user().email()).fullName()).isEqualTo("Victim Original");
	}
}
