package com.petcare.backend.config;

import com.petcare.backend.domain.repository.*;
import com.petcare.backend.persistence.entity.*;
import com.petcare.backend.persistence.enums.EstadoClinica;
import com.petcare.backend.persistence.enums.PlanClinica;
import com.petcare.backend.persistence.enums.RoleName;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect")
@ActiveProfiles("test")
@Transactional
class DataInitializerTest {

    @Autowired private AutowireCapableBeanFactory beanFactory;
    @Autowired private ApplicationContext context;
    @Autowired private EntityManager entityManager;
    @Autowired private ClinicaRepository clinicaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private DuenioRepository duenioRepository;
    @Autowired private VeterinarioRepository veterinarioRepository;
    @Autowired private AsistenteRepository asistenteRepository;
    @Autowired private ServicioRepository servicioRepository;
    @Autowired private VacunaRepository vacunaRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void startupSeedIsIdempotentAfterAnotherClinicAddsMatchingContactsAndCatalogNames() {
        assertThat(context.getBeansOfType(DataInitializer.class)).isEmpty();
        var initializer = beanFactory.createBean(DataInitializer.class);
        initializer.run();
        var initialCounts = counts();
        assertThat(initialCounts).containsEntry("Usuario", 15L).containsEntry("Duenio", 10L)
                .containsEntry("Servicio", 20L).containsEntry("Vacuna", 12L)
                .containsEntry("Mascota", 22L).containsEntry("Cita", 36L);

        var demo = clinicaRepository.findBySlug("demo").orElseThrow();
        var demoOwner = duenioRepository.findByEmailAndClinicaId("duenio@petcare.com", demo.getId()).orElseThrow();
        assertThat(demoOwner.getUsuario().getId())
                .isEqualTo(usuarioRepository.findByEmail("duenio@petcare.com").orElseThrow().getId());
        var foreign = clinic("independent");
        var now = LocalDateTime.now();
        var foreignOwner = duenioRepository.save(Duenio.builder().clinica(foreign)
                .nombres("Juan").apellidos("Perez").email(demoOwner.getEmail())
                .tipoDocumento("DNI").numeroDocumento(demoOwner.getNumeroDocumento()).telefono("111222333")
                .active(false).createdAt(now).updatedAt(now).build());
        var foreignService = servicioRepository.save(Servicio.builder().clinica(foreign)
                .nombre("Consulta General").descripcion("Independent service").costoBase(new BigDecimal("999.00"))
                .active(false).createdAt(now).updatedAt(now).build());
        var foreignVaccine = vacunaRepository.save(Vacuna.builder().clinica(foreign)
                .nombre("Rabia Canina").descripcion("Independent vaccine").intervaloProximaDosisDias(90)
                .active(false).createdAt(now).updatedAt(now).build());
        var expectedCounts = counts();

        initializer.run();
        assertThat(counts()).isEqualTo(expectedCounts);
        initializer.run();
        assertThat(counts()).isEqualTo(expectedCounts);
        var owner = duenioRepository.findById(foreignOwner.getId()).orElseThrow();
        assertThat(owner.getClinica().getId()).isEqualTo(foreign.getId());
        assertThat(owner.getUsuario()).isNull();
        assertThat(owner.getActive()).isFalse();
        assertThat(owner.getTelefono()).isEqualTo("111222333");
        var service = servicioRepository.findById(foreignService.getId()).orElseThrow();
        assertThat(service.getClinica().getId()).isEqualTo(foreign.getId());
        assertThat(service.getCostoBase()).isEqualByComparingTo("999.00");
        assertThat(service.getDescripcion()).isEqualTo("Independent service");
        assertThat(service.getActive()).isFalse();
        var vaccine = vacunaRepository.findById(foreignVaccine.getId()).orElseThrow();
        assertThat(vaccine.getClinica().getId()).isEqualTo(foreign.getId());
        assertThat(vaccine.getDescripcion()).isEqualTo("Independent vaccine");
        assertThat(vaccine.getIntervaloProximaDosisDias()).isEqualTo(90);
        assertThat(vaccine.getActive()).isFalse();
        assertThat(duenioRepository.findByEmailAndClinicaId(owner.getEmail(), demo.getId()).orElseThrow().getId())
                .isEqualTo(demoOwner.getId());
    }

    @Test
    void startupAndRepeatedSeedLeaveForeignAccountsUntouchedAndUnlinkedFromDemoContacts() {
        var foreign = clinic("foreign-accounts");
        var now = LocalDateTime.now().withNano(0);
        var role = rolRepository.findByName(RoleName.ROLE_ADMIN).orElseThrow();
        var emails = List.of("admin@petcare.com", "laura.admin@petcare.com", "vet@petcare.com",
                "miguel.alvarez@petcare.com", "patricia.h@petcare.com", "ricardo.g@petcare.com",
                "asistente@petcare.com", "sofia.reyes@petcare.com", "diego.c@petcare.com",
                "duenio@petcare.com", "ana.gomez@email.com", "pedro.s@email.com",
                "carmen.t@email.com", "luis.f@email.com", "supervisor@petcare.com");
        var password = passwordEncoder.encode("foreign-secret");
        for (var email : emails) {
            usuarioRepository.save(Usuario.builder().clinica(foreign).email(email)
                    .fullName("Foreign " + email).password(password).roles(Set.of(role))
                    .active(false).telefono("111222333").forcePasswordChange(true)
                    .activationToken("foreign-token").tokenExpiry(now.plusDays(1)).createdAt(now).build());
        }
        var foreignOwner = duenioRepository.save(Duenio.builder().clinica(foreign)
                .usuario(usuarioRepository.findByEmail("duenio@petcare.com").orElseThrow())
                .nombres("Juan").apellidos("Perez").email("duenio@petcare.com")
                .tipoDocumento("DNI").numeroDocumento("12345678").telefono("111222333")
                .active(false).createdAt(now).updatedAt(now).build());
        var initializer = beanFactory.createBean(DataInitializer.class);
        initializer.run();
        var expectedCounts = counts();
        initializer.run();
        assertThat(counts()).isEqualTo(expectedCounts);
        var demo = clinicaRepository.findBySlug("demo").orElseThrow();

        for (var email : emails) {
            var account = usuarioRepository.findByEmail(email).orElseThrow();
            assertThat(account.getClinica().getId()).isEqualTo(foreign.getId());
            assertThat(account.getFullName()).isEqualTo("Foreign " + email);
            assertThat(account.getPassword()).isEqualTo(password);
            assertThat(account.getRoles()).extracting(Rol::getName).containsExactly(RoleName.ROLE_ADMIN);
            assertThat(account.getActive()).isFalse();
            assertThat(account.getTelefono()).isEqualTo("111222333");
            assertThat(account.getForcePasswordChange()).isTrue();
            assertThat(account.getActivationToken()).isEqualTo("foreign-token");
            assertThat(account.getTokenExpiry()).isEqualTo(now.plusDays(1));
            assertThat(account.getCreatedAt()).isEqualTo(now);
        }
        assertThat(duenioRepository.search(demo.getId(), null, null)).hasSize(10)
                .allSatisfy(owner -> assertThat(owner.getUsuario()).isNull());
        assertThat(veterinarioRepository.search(demo.getId(), null, null)).hasSize(8)
                .allSatisfy(vet -> assertThat(vet.getUsuario()).isNull());
        assertThat(asistenteRepository.search(demo.getId(), null, null)).hasSize(5)
                .allSatisfy(assistant -> assertThat(assistant.getUsuario()).isNull());
        assertThat(duenioRepository.findById(foreignOwner.getId()).orElseThrow().getUsuario().getId())
                .isEqualTo(usuarioRepository.findByEmail("duenio@petcare.com").orElseThrow().getId());
    }

    private Clinica clinic(String slug) {
        var now = LocalDateTime.now();
        return clinicaRepository.save(Clinica.builder().nombre(slug).slug(slug)
                .plan(PlanClinica.TRIAL).estado(EstadoClinica.ACTIVA).createdAt(now).updatedAt(now).build());
    }

    private Map<String, Long> counts() {
        entityManager.flush();
        entityManager.clear();
        var counts = new HashMap<String, Long>();
        for (var entity : entityManager.getMetamodel().getEntities()) {
            counts.put(entity.getName(), entityManager.createQuery(
                    "select count(e) from " + entity.getName() + " e", Long.class).getSingleResult());
        }
        return counts;
    }
}
