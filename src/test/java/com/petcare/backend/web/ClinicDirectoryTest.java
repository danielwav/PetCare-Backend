package com.petcare.backend.web;

import com.petcare.backend.persistence.entity.Clinica;
import com.petcare.backend.persistence.entity.Servicio;
import com.petcare.backend.persistence.entity.Usuario;
import com.petcare.backend.persistence.enums.EstadoClinica;
import com.petcare.backend.persistence.enums.PlanClinica;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ClinicDirectoryTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private EntityManager entityManager;

	@Test
	void anonymousDirectoryExposesOnlyPublicFieldsAndActiveServices() throws Exception {
		Clinica active = clinic("Alpha", "alpha", EstadoClinica.ACTIVA);
		Clinica suspended = clinic("Suspended", "suspended", EstadoClinica.SUSPENDIDA);
		service(active, "Vacunacion", true);
		service(active, "Consulta", true);
		service(active, "Hidden", false);
		service(suspended, "Consulta", true);

		mvc.perform(get("/api/public/clinicas"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.*", hasSize(4)))
				.andExpect(jsonPath("$.items", hasSize(1)))
				.andExpect(jsonPath("$.items[0].*", hasSize(9)))
				.andExpect(jsonPath("$.items[0].id").value(active.getId()))
				.andExpect(jsonPath("$.items[0].nombre").value("Alpha"))
				.andExpect(jsonPath("$.items[0].slug").value("alpha"))
				.andExpect(jsonPath("$.items[0].direccion").value("Avenida Norte"))
				.andExpect(jsonPath("$.items[0].telefono").value("123456789"))
				.andExpect(jsonPath("$.items[0].horarioAtencion").value("9-18"))
				.andExpect(jsonPath("$.items[0].descripcion").value("Public description"))
				.andExpect(jsonPath("$.items[0].logoUrl").value("https://example.com/logo.png"))
				.andExpect(jsonPath("$.items[0].servicios", contains("Consulta", "Vacunacion")))
				.andExpect(jsonPath("$.items[0].plan").doesNotExist())
				.andExpect(jsonPath("$.items[0].estado").doesNotExist())
				.andExpect(jsonPath("$.items[0].users").doesNotExist())
				.andExpect(jsonPath("$.items[0].usuarios").doesNotExist())
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.totalPages").value(1))
				.andExpect(jsonPath("$.totalElements").value(1));
	}

	@Test
	void searchMatchesTrimmedCaseInsensitiveClinicNameAndAddress() throws Exception {
		clinic("Alpha Veterinary", "alpha", EstadoClinica.ACTIVA);
		clinic("Beta", "beta", EstadoClinica.ACTIVA);
		mvc.perform(get("/api/public/clinicas").param("q", "  pHa vET  "))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[*].slug", contains("alpha")));
		mvc.perform(get("/api/public/clinicas").param("q", "  nOrTe "))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(2));
	}

	@Test
	void accentedQueriesMatchUnaccentedClinicNameAddressAndServiceName() throws Exception {
		Clinica clinic = clinic("Clinica aeiouu", "unaccented", EstadoClinica.ACTIVA);
		clinic.setDireccion("Medica aeiouu");
		service(clinic, "Vacunacion aeiouu", true);
		for (String q : new String[]{" CL\u00cdNICA \u00c1\u00c9\u00cd\u00d3\u00da\u00dc ",
				"M\u00c9DICA \u00c1\u00c9\u00cd\u00d3\u00da\u00dc", "VACUNACI\u00d3N \u00c1\u00c9\u00cd\u00d3\u00da\u00dc"}) {
			mvc.perform(get("/api/public/clinicas").param("q", q))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.items[*].slug", contains("unaccented")))
					.andExpect(jsonPath("$.items[0].servicios", contains("Vacunacion aeiouu")));
		}
	}

	@Test
	void unaccentedQueriesMatchAccentedClinicNameAddressAndServiceNameWithoutChangingResponse() throws Exception {
		String vowels = "\u00c1\u00c9\u00cd\u00d3\u00da\u00dc";
		Clinica clinic = clinic("Cl\u00ednica " + vowels, "accented", EstadoClinica.ACTIVA);
		clinic.setDireccion("M\u00e9dica " + vowels);
		String serviceName = "Vacunaci\u00f3n " + vowels;
		service(clinic, serviceName, true);
		for (String q : new String[]{"clinica aeiouu", "medica aeiouu", "vacunacion aeiouu"}) {
			mvc.perform(get("/api/public/clinicas").param("q", q))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.items[*].slug", contains("accented")))
					.andExpect(jsonPath("$.items[0].nombre").value(clinic.getNombre()))
					.andExpect(jsonPath("$.items[0].direccion").value(clinic.getDireccion()))
					.andExpect(jsonPath("$.items[0].servicios", contains(serviceName)));
		}
	}

	@Test
	void accentNormalizationPreservesEnye() throws Exception {
		clinic("Pe\u00f1a", "enye", EstadoClinica.ACTIVA);
		clinic("Pena", "plain", EstadoClinica.ACTIVA);
		mvc.perform(get("/api/public/clinicas").param("q", "PE\u00d1A"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[*].slug", contains("enye")));
		mvc.perform(get("/api/public/clinicas").param("q", "pena"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[*].slug", contains("plain")));
	}

	@Test
	void serviceSearchDoesNotDuplicateClinicsOrMatchInactiveOrSuspendedOnes() throws Exception {
		Clinica active = clinic("Alpha", "alpha", EstadoClinica.ACTIVA);
		Clinica other = clinic("Beta", "beta", EstadoClinica.ACTIVA);
		Clinica suspended = clinic("Gamma", "gamma", EstadoClinica.SUSPENDIDA);
		service(active, "Consulta general", true);
		service(active, "Consulta dental", true);
		service(active, "Vacunacion", true);
		service(other, "Consulta general", false);
		service(suspended, "Consulta general", true);
		mvc.perform(get("/api/public/clinicas").param("q", "  cOnSuLtA  ").param("size", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[*].slug", contains("alpha")))
				.andExpect(jsonPath("$.items[0].servicios", contains("Consulta dental", "Consulta general", "Vacunacion")))
				.andExpect(jsonPath("$.totalElements").value(1));
	}

	@Test
	void sameServiceNamesRemainIsolatedPerClinic() throws Exception {
		Clinica alpha = clinic("Alpha", "alpha", EstadoClinica.ACTIVA);
		Clinica beta = clinic("Beta", "beta", EstadoClinica.ACTIVA);
		service(alpha, "Consulta", true);
		service(alpha, "Alpha only", true);
		service(beta, "Consulta", true);
		service(beta, "Beta only", true);
		mvc.perform(get("/api/public/clinicas").param("q", "consulta"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].servicios", contains("Alpha only", "Consulta")))
				.andExpect(jsonPath("$.items[1].servicios", contains("Beta only", "Consulta")));
	}

	@Test
	void privateStaffAndServiceDescriptionsAreNotSearchable() throws Exception {
		Clinica clinic = clinic("Alpha", "alpha", EstadoClinica.ACTIVA);
		entityManager.persist(Usuario.builder().fullName("SecretStaffName").email("secret@example.com")
				.password("private-password").active(true).createdAt(LocalDateTime.now()).clinica(clinic).build());
		service(clinic, "Consulta", true);
		for (String q : new String[]{"SecretStaffName", "secret@example.com", "Private service description", "Public description"}) {
			mvc.perform(get("/api/public/clinicas").param("q", q))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.items", hasSize(0)))
					.andExpect(jsonPath("$.totalElements").value(0));
		}
	}

	@Test
	void wildcardAndEscapeCharactersAreLiteral() throws Exception {
		clinic("Rate 100%", "percent", EstadoClinica.ACTIVA);
		clinic("Under_score", "underscore", EstadoClinica.ACTIVA);
		Clinica exclamation = clinic("Other", "other", EstadoClinica.ACTIVA);
		service(exclamation, "Care!", true);
		for (String[] query : new String[][]{{"%", "percent"}, {"_", "underscore"}, {"!", "other"}}) {
			mvc.perform(get("/api/public/clinicas").param("q", query[0]))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.items[*].slug", contains(query[1])));
		}
	}

	@Test
	void paginationIsStableByNameThenIdAndHandlesLastAndOutOfRangePages() throws Exception {
		clinic("Zulu", "zulu", EstadoClinica.ACTIVA);
		clinic("Alpha", "alpha-first", EstadoClinica.ACTIVA);
		clinic("Alpha", "alpha-second", EstadoClinica.ACTIVA);
		mvc.perform(get("/api/public/clinicas").param("size", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[*].slug", contains("alpha-first", "alpha-second")))
				.andExpect(jsonPath("$.totalPages").value(2))
				.andExpect(jsonPath("$.totalElements").value(3));
		mvc.perform(get("/api/public/clinicas").param("size", "2").param("page", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[*].slug", contains("zulu")))
				.andExpect(jsonPath("$.page").value(1));
		mvc.perform(get("/api/public/clinicas").param("size", "2").param("page", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items", hasSize(0)))
				.andExpect(jsonPath("$.page").value(2))
				.andExpect(jsonPath("$.totalPages").value(2))
				.andExpect(jsonPath("$.totalElements").value(3));
	}

	@Test
	void defaultsAndMaximumPageSizeAreApplied() throws Exception {
		for (int i = 0; i < 25; i++) {
			clinic("Clinic " + i, "clinic-" + i, EstadoClinica.ACTIVA);
		}
		mvc.perform(get("/api/public/clinicas").param("q", "   "))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items", hasSize(12)))
				.andExpect(jsonPath("$.items[0].servicios", hasSize(0)))
				.andExpect(jsonPath("$.totalPages").value(3));
		mvc.perform(get("/api/public/clinicas").param("size", "24"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items", hasSize(24)))
				.andExpect(jsonPath("$.totalPages").value(2));
	}

	@Test
	void rejectsInvalidParametersAndAcceptsQueryLengthBoundary() throws Exception {
		for (String[] invalid : new String[][]{{"page", "-1"}, {"size", "0"}, {"size", "-1"},
				{"size", "25"}, {"page", "abc"}, {"size", "1.5"}, {"page", "2147483648"}, {"q", "a".repeat(101)}}) {
			mvc.perform(get("/api/public/clinicas").param(invalid[0], invalid[1]))
					.andExpect(status().isBadRequest());
		}
		mvc.perform(get("/api/public/clinicas").param("q", "a".repeat(100)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalPages").value(0));
	}

	@Test
	void fetchesServiceNamesInOneBulkQueryWithoutNPlusOne() throws Exception {
		for (int i = 0; i < 4; i++) {
			service(clinic("Clinic " + i, "clinic-" + i, EstadoClinica.ACTIVA), "Consulta", true);
		}
		entityManager.flush();
		entityManager.clear();
		var statistics = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
		statistics.clear();
		mvc.perform(get("/api/public/clinicas").param("size", "3"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items", hasSize(3)))
				.andExpect(jsonPath("$.items[2].servicios", contains("Consulta")));
		assertThat(statistics.getPrepareStatementCount()).isEqualTo(3);
	}

	@Test
	void existingSlugRoutesKeepTheirContracts() throws Exception {
		Clinica active = clinic("Alpha", "alpha", EstadoClinica.ACTIVA);
		service(active, "Consulta", true);
		service(active, "Hidden", false);
		mvc.perform(get("/api/public/clinicas/alpha"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.slug").value("alpha"))
				.andExpect(jsonPath("$.servicios").doesNotExist());
		mvc.perform(get("/api/public/clinicas/alpha/servicios"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].nombre").value("Consulta"))
				.andExpect(jsonPath("$[0].costoBase").value(10));
	}

	private Clinica clinic(String name, String slug, EstadoClinica state) {
		LocalDateTime now = LocalDateTime.now();
		Clinica clinic = Clinica.builder().nombre(name).slug(slug).plan(PlanClinica.PRO).estado(state)
				.direccion("Avenida Norte").telefono("123456789").horarioAtencion("9-18")
				.descripcion("Public description").logoUrl("https://example.com/logo.png")
				.createdAt(now).updatedAt(now).build();
		entityManager.persist(clinic);
		return clinic;
	}

	private void service(Clinica clinic, String name, boolean active) {
		LocalDateTime now = LocalDateTime.now();
		entityManager.persist(Servicio.builder().clinica(clinic).nombre(name).active(active)
				.descripcion("Private service description").costoBase(BigDecimal.TEN)
				.createdAt(now).updatedAt(now).build());
	}
}
