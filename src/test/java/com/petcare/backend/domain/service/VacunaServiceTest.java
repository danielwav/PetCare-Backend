package com.petcare.backend.domain.service;

import com.petcare.backend.domain.dto.request.CitaRequest;
import com.petcare.backend.domain.dto.request.CostoCitaServicioRequest;
import com.petcare.backend.domain.dto.request.DuenioRequest;
import com.petcare.backend.domain.dto.request.HorarioVeterinarioRequest;
import com.petcare.backend.domain.dto.request.MascotaRequest;
import com.petcare.backend.domain.dto.request.RegisterRequest;
import com.petcare.backend.domain.dto.request.ServicioRequest;
import com.petcare.backend.domain.dto.request.VacunaMascotaRequest;
import com.petcare.backend.domain.dto.request.VacunaRequest;
import com.petcare.backend.domain.dto.request.VeterinarioRequest;
import com.petcare.backend.domain.dto.response.AuthResponse;
import com.petcare.backend.domain.dto.response.CitaResponse;
import com.petcare.backend.domain.dto.response.DuenioResponse;
import com.petcare.backend.domain.dto.response.MascotaResponse;
import com.petcare.backend.domain.dto.response.ServicioResponse;
import com.petcare.backend.domain.dto.response.VacunaMascotaResponse;
import com.petcare.backend.domain.dto.response.VacunaResponse;
import com.petcare.backend.domain.dto.response.VeterinarioResponse;
import com.petcare.backend.domain.repository.ClinicaRepository;
import com.petcare.backend.domain.repository.MascotaRepository;
import com.petcare.backend.domain.repository.RolRepository;
import com.petcare.backend.domain.repository.UsuarioRepository;
import com.petcare.backend.domain.repository.VacunaRepository;
import com.petcare.backend.persistence.entity.Clinica;
import com.petcare.backend.persistence.entity.Usuario;
import com.petcare.backend.persistence.entity.Vacuna;
import com.petcare.backend.persistence.enums.SexoMascota;
import com.petcare.backend.persistence.enums.RoleName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class VacunaServiceTest {

	@Autowired
	private VacunaService vacunaService;

	@Autowired
	private AuthService authService;

	@Autowired
	private DuenioService duenioService;

	@Autowired
	private MascotaService mascotaService;

	@Autowired
	private VeterinarioService veterinarioService;

	@Autowired
	private ServicioService servicioService;

	@Autowired
	private CitaService citaService;

	@Autowired
	private ClinicaService clinicaService;

	@Autowired
	private ClinicaRepository clinicaRepository;

	@Autowired
	private VacunaRepository vacunaRepository;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private RolRepository rolRepository;

	@Autowired
	private MascotaRepository mascotaRepository;

	private Long clinicaId() {
		return clinicaService.getOrCreateDefaultClinic().getId();
	}

	@Test
	void createUpdateAndDeactivateVaccineCatalog() {
		VacunaResponse created = vacunaService.create(baseVacunaRequest("Rabia", 365), clinicaId());
		VacunaResponse updated = vacunaService.update(created.id(), new VacunaRequest(
				"Rabia anual",
				"Proteccion antirrabica anual.",
				365
		), clinicaId());
		List<VacunaResponse> results = vacunaService.findAll("rabia", null, clinicaId());

		vacunaService.deactivate(created.id(), clinicaId());
		VacunaResponse inactive = vacunaService.findById(created.id(), clinicaId());

		assertThat(updated.nombre()).isEqualTo("Rabia anual");
		assertThat(results).hasSize(1);
		assertThat(inactive.active()).isFalse();
		assertThatThrownBy(() -> vacunaService.create(baseVacunaRequest("rabia anual", 365), clinicaId()))
				.isInstanceOf(IllegalArgumentException.class);
		assertThat(vacunaService.activate(created.id(), clinicaId()).active()).isTrue();
	}

	@Test
	void registerVaccineForPetAndCalculateNextDose() {
		TestData data = createBaseData();
		VacunaResponse vacuna = vacunaService.create(baseVacunaRequest("Triple felina", 180), clinicaId());
		CitaResponse cita = citaService.create(baseCitaRequest(data), clinicaId());

		VacunaMascotaResponse applied = vacunaService.registerForMascota(data.mascota().id(), new VacunaMascotaRequest(
				vacuna.id(),
				data.veterinario().id(),
				cita.id(),
				LocalDate.now(),
				"LOTE-001",
				null,
				"Primera dosis aplicada sin reacciones."
		), data.veterinario().id(), clinicaId());
		List<VacunaMascotaResponse> petVaccines = vacunaService.findByMascota(data.mascota().id(), clinicaId());

		assertThat(applied.fechaProximaDosis()).isEqualTo(LocalDate.now().plusDays(180));
		assertThat(applied.citaId()).isEqualTo(cita.id());
		assertThat(applied.estadoAlerta()).isEqualTo("PROGRAMADA");
		assertThat(petVaccines).extracting(VacunaMascotaResponse::id).contains(applied.id());
	}

	@Test
	void findUpcomingAndAlerts() {
		TestData data = createBaseData();
		VacunaResponse vacuna = vacunaService.create(baseVacunaRequest("Parvovirus", null), clinicaId());
		VacunaMascotaResponse applied = vacunaService.registerForMascota(data.mascota().id(), new VacunaMascotaRequest(
				vacuna.id(),
				data.veterinario().id(),
				null,
				LocalDate.now(),
				null,
				LocalDate.now().plusDays(15),
				"Proxima dosis indicada manualmente."
		), data.veterinario().id(), clinicaId());

		List<VacunaMascotaResponse> upcoming = vacunaService.findUpcoming(20, clinicaId());
		List<VacunaMascotaResponse> alerts = vacunaService.findAlerts(null, clinicaId());

		assertThat(upcoming).extracting(VacunaMascotaResponse::id).contains(applied.id());
		assertThat(alerts).extracting(VacunaMascotaResponse::id).contains(applied.id());
		assertThat(alerts.getFirst().estadoAlerta()).isEqualTo("PROXIMA");
	}

	@Test
	void rejectInactiveVaccineOrMismatchedAppointment() {
		TestData data = createBaseData();
		VacunaResponse vacuna = vacunaService.create(baseVacunaRequest("Moquillo", 365), clinicaId());
		vacunaService.deactivate(vacuna.id(), clinicaId());

		assertThatThrownBy(() -> vacunaService.registerForMascota(data.mascota().id(), new VacunaMascotaRequest(
				vacuna.id(),
				data.veterinario().id(),
				null,
				LocalDate.now(),
				null,
				null,
				null
		), data.veterinario().id(), clinicaId())).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void duenioCanOnlyConsultOwnVaccineHistory() {
		AuthResponse ownerUser = authService.register(new RegisterRequest("Owner", "owner.vacuna@test.com", "secret123", "000000000"));
		AuthResponse otherUser = authService.register(new RegisterRequest("Other", "other.vacuna@test.com", "secret123", "000000000"));
		TestData ownerData = createBaseData("12345670", "owner.vacuna.profile@test.com", ownerUser.user().id(), "Lola", "CMVP-010", "vet.vacuna10@test.com", "Consulta vacuna owner");
		TestData otherData = createBaseData("12345671", "other.vacuna.profile@test.com", otherUser.user().id(), "Toby", "CMVP-011", "vet.vacuna11@test.com", "Consulta vacuna other");
		VacunaResponse ownerVaccine = vacunaService.create(baseVacunaRequest("Bordetella", 365), clinicaId());
		VacunaResponse otherVaccine = vacunaService.create(baseVacunaRequest("Leptospira", 365), clinicaId());
		VacunaMascotaResponse ownerHistory = vacunaService.registerForMascota(ownerData.mascota().id(), new VacunaMascotaRequest(
				ownerVaccine.id(),
				ownerData.veterinario().id(),
				null,
				LocalDate.now(),
				"LOTE-OWNER",
				null,
				"Aplicada a mascota propia."
		), ownerData.veterinario().id(), clinicaId());
		vacunaService.registerForMascota(otherData.mascota().id(), new VacunaMascotaRequest(
				otherVaccine.id(),
				otherData.veterinario().id(),
				null,
				LocalDate.now(),
				"LOTE-OTHER",
				null,
				"Aplicada a mascota ajena."
		), otherData.veterinario().id(), clinicaId());

		List<VacunaMascotaResponse> ownHistory = vacunaService.findByMascotaForDuenio(
				ownerData.mascota().id(),
				ownerUser.user().email()
		);

		assertThat(ownHistory).extracting(VacunaMascotaResponse::id).containsExactly(ownerHistory.id());
		assertThatThrownBy(() -> vacunaService.findByMascotaForDuenio(
				otherData.mascota().id(),
				ownerUser.user().email()
		)).isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void catalogsAreIndependentAndRejectCrossClinicOperations() {
		Long clinic = clinicaId();
		Long otherClinic = createOtherClinic();
		VacunaResponse own = vacunaService.create(baseVacunaRequest("Rabia", 365), clinic);
		VacunaResponse other = vacunaService.create(baseVacunaRequest("Rabia", 180), otherClinic);

		assertThat(vacunaRepository.findById(own.id()).orElseThrow().getClinica().getId()).isEqualTo(clinic);
		assertThat(vacunaService.findAll(null, null, clinic)).extracting(VacunaResponse::id).containsExactly(own.id());
		assertThat(vacunaService.findAll("  rabia  ", true, otherClinic)).extracting(VacunaResponse::id).containsExactly(other.id());
		assertThat(vacunaService.findAll("Descripcion", true, clinic)).extracting(VacunaResponse::id).containsExactly(own.id());
		assertThat(vacunaService.update(own.id(), baseVacunaRequest("Rabia", 300), clinic).intervaloProximaDosisDias()).isEqualTo(300);
		VacunaResponse second = vacunaService.create(baseVacunaRequest("Moquillo", 365), clinic);
		assertThatThrownBy(() -> vacunaService.update(second.id(), baseVacunaRequest("  RABIA  ", 365), clinic))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> vacunaService.findById(other.id(), clinic)).isInstanceOf(AccessDeniedException.class);
		assertThatThrownBy(() -> vacunaService.update(other.id(), baseVacunaRequest("Otra", 365), clinic)).isInstanceOf(AccessDeniedException.class);
		assertThatThrownBy(() -> vacunaService.deactivate(other.id(), clinic)).isInstanceOf(AccessDeniedException.class);
		assertThatThrownBy(() -> vacunaService.activate(other.id(), clinic)).isInstanceOf(AccessDeniedException.class);
		vacunaService.deactivate(own.id(), clinic);
		assertThat(vacunaService.findAll("Rabia", null, clinic)).isEmpty();
		assertThat(vacunaService.findAll("Rabia", false, clinic)).extracting(VacunaResponse::id).containsExactly(own.id());
		assertThat(vacunaService.findById(other.id(), otherClinic).active()).isTrue();
	}

	@Test
	void rejectApplicationOfVaccineFromAnotherClinic() {
		TestData data = createBaseData();
		VacunaResponse other = vacunaService.create(baseVacunaRequest("Rabia", 365), createOtherClinic());
		assertThatThrownBy(() -> vacunaService.registerForMascota(data.mascota().id(), new VacunaMascotaRequest(
				other.id(), data.veterinario().id(), null, LocalDate.now(), null, null, null
		), data.veterinario().id(), clinicaId())).isInstanceOf(AccessDeniedException.class);
		assertThat(vacunaService.findByMascota(data.mascota().id(), clinicaId())).isEmpty();
	}

	@Test
	void legacyUnassignedVaccineIsNotAvailableForCatalogOrApplication() {
		TestData data = createBaseData();
		LocalDateTime now = LocalDateTime.now();
		Vacuna legacy = vacunaRepository.save(Vacuna.builder()
				.nombre("Legacy").descripcion("Vacuna sin clinica").active(true)
				.createdAt(now).updatedAt(now).build());
		assertThat(vacunaService.findAll("Legacy", true, clinicaId())).isEmpty();
		assertThatThrownBy(() -> vacunaService.findById(legacy.getId(), clinicaId())).isInstanceOf(AccessDeniedException.class);
		assertThatThrownBy(() -> vacunaService.registerForMascota(data.mascota().id(), new VacunaMascotaRequest(
				legacy.getId(), data.veterinario().id(), null, LocalDate.now(), null, null, null
		), data.veterinario().id(), clinicaId())).isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void excludeLegacyPetWithOwnerFromAnotherClinic() {
		AuthResponse owner = authService.register(new RegisterRequest(
				"Owner", "legacy.owner@test.com", "secret123", "000000000"));
		TestData data = createBaseData("12345670", "legacy.owner@test.com", owner.user().id(),
				"Lola", "CMVP-010", "legacy.vet@test.com", "Consulta");
		VacunaResponse vaccine = vacunaService.create(baseVacunaRequest("Rabia", 30), clinicaId());
		vacunaService.registerForMascota(data.mascota().id(), new VacunaMascotaRequest(
				vaccine.id(), data.veterinario().id(), null, LocalDate.now(), null, null, null
		), data.veterinario().id(), clinicaId());
		var pet = mascotaRepository.findById(data.mascota().id()).orElseThrow();
		pet.setClinica(clinicaRepository.findById(createOtherClinic()).orElseThrow());
		mascotaRepository.save(pet);

		assertThatThrownBy(() -> vacunaService.findByMascotaForDuenio(pet.getId(), owner.user().email()))
				.isInstanceOf(AccessDeniedException.class);
		assertThat(vacunaService.findUpcomingForDuenio(60, owner.user().email())).isEmpty();
		assertThat(vacunaService.findAlertsForDuenio(60, owner.user().email())).isEmpty();
	}

	private Long createOtherClinic() {
		Clinica defaultClinic = clinicaService.getOrCreateDefaultClinic();
		LocalDateTime now = LocalDateTime.now();
		return clinicaRepository.save(Clinica.builder()
				.nombre("Otra clinica").slug("otra-clinica")
				.plan(defaultClinic.getPlan()).estado(defaultClinic.getEstado())
				.createdAt(now).updatedAt(now).build()).getId();
	}

	private TestData createBaseData() {
		return createBaseData("12345678", "daniel@test.com", null, "Firulais", "CMVP-001", "ana.vet@test.com", "Consulta general");
	}

	private TestData createBaseData(
			String documento,
			String duenioEmail,
			Long usuarioId,
			String mascotaNombre,
			String numeroColegiatura,
			String veterinarioEmail,
			String servicioNombre
	) {
		DuenioResponse duenio = usuarioId != null
				? duenioService.findOwn(usuarioRepository.findById(usuarioId).orElseThrow().getEmail())
				: duenioService.create(new DuenioRequest(
				usuarioId,
				"Daniel",
				"Torres",
				"DNI",
				documento,
				"999888777",
				duenioEmail,
				"Av. Siempre Viva 123"
		), clinicaId());
		MascotaResponse mascota = mascotaService.create(new MascotaRequest(
				duenio.id(),
				mascotaNombre,
				"Perro",
				"Mestizo",
				SexoMascota.MACHO,
				LocalDate.now().minusYears(2),
				"Marron",
				new BigDecimal("12.50"),
				"Sin observaciones",
				null
		), clinicaId());
		Usuario veterinarioUser = usuarioRepository.save(Usuario.builder()
				.fullName("Ana Salas").email(veterinarioEmail).password("test-password")
				.active(true).createdAt(LocalDateTime.now())
				.clinica(clinicaRepository.findById(clinicaId()).orElseThrow())
				.roles(Set.of(rolRepository.findByName(RoleName.ROLE_VETERINARIO).orElseThrow()))
				.build());
		VeterinarioResponse veterinario = veterinarioService.create(new VeterinarioRequest(
				veterinarioUser.getId(),
				"Ana",
				"Salas",
				numeroColegiatura,
				"Medicina general",
				"999777666",
				veterinarioEmail,
				List.of(new HorarioVeterinarioRequest(
						DayOfWeek.MONDAY,
						LocalTime.of(9, 0),
						LocalTime.of(11, 0),
						30
				))
		), clinicaId());
		ServicioResponse consulta = servicioService.create(new ServicioRequest(
				servicioNombre,
				"Evaluacion clinica basica.",
				new BigDecimal("50.00")
		), clinicaId());

		return new TestData(duenio, mascota, veterinario, consulta);
	}

	private CitaRequest baseCitaRequest(TestData data) {
		return new CitaRequest(
				data.duenio().id(),
				data.mascota().id(),
				data.veterinario().id(),
				nextDate(DayOfWeek.MONDAY),
				LocalTime.of(9, 0),
				30,
				"Consulta preventiva",
				List.of(new CostoCitaServicioRequest(data.consulta().id(), 1)),
				BigDecimal.ZERO
		);
	}

	private VacunaRequest baseVacunaRequest(String nombre, Integer intervaloDias) {
		return new VacunaRequest(
				nombre,
				"Descripcion de vacuna " + nombre,
				intervaloDias
		);
	}

	private LocalDate nextDate(DayOfWeek dayOfWeek) {
		LocalDate date = LocalDate.now().plusDays(1);
		while (date.getDayOfWeek() != dayOfWeek) {
			date = date.plusDays(1);
		}
		return date;
	}

	private record TestData(
			DuenioResponse duenio,
			MascotaResponse mascota,
			VeterinarioResponse veterinario,
			ServicioResponse consulta
	) {
	}
}
