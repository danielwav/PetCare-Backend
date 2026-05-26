package com.petcare.backend.config;

import com.petcare.backend.domain.repository.*;
import com.petcare.backend.persistence.entity.*;
import com.petcare.backend.persistence.enums.EstadoCita;
import com.petcare.backend.persistence.enums.RoleName;
import com.petcare.backend.persistence.enums.SexoMascota;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final ServicioRepository servicioRepository;
    private final VacunaRepository vacunaRepository;
    private final DuenioRepository duenioRepository;
    private final VeterinarioRepository veterinarioRepository;
    private final AsistenteRepository asistenteRepository;
    private final MascotaRepository mascotaRepository;

    @Value("${app.seed-data.enabled:true}")
    private boolean seedDataEnabled;

    @Override
    public void run(String... args) {
        createRoleIfMissing(RoleName.ROLE_ADMIN, "Administrador general del sistema.");
        createRoleIfMissing(RoleName.ROLE_VETERINARIO, "Personal medico veterinario.");
        createRoleIfMissing(RoleName.ROLE_ASISTENTE, "Personal operativo de recepcion y agenda.");
        createRoleIfMissing(RoleName.ROLE_DUENIO, "Cliente o propietario de mascota.");

        if (!seedDataEnabled) {
            return;
        }

        if (usuarioRepository.findByEmail("admin@petcare.com").isPresent()) {
            return;
        }

        var rolAdmin = rolRepository.findByName(RoleName.ROLE_ADMIN).orElseThrow();
        var rolVet = rolRepository.findByName(RoleName.ROLE_VETERINARIO).orElseThrow();
        var rolAsistente = rolRepository.findByName(RoleName.ROLE_ASISTENTE).orElseThrow();
        var rolDuenio = rolRepository.findByName(RoleName.ROLE_DUENIO).orElseThrow();

        // Admins
        var adminUser = createUser("Admin Sistema", "admin@petcare.com", "admin123", rolAdmin);
        createUser("Laura Mendoza", "laura.admin@petcare.com", "admin123", rolAdmin);

        // Veterinarios
        var vetUser = createUser("Dr. Carlos López", "vet@petcare.com", "vet123", rolVet);
        createUser("Dr. Miguel Álvarez", "miguel.alvarez@petcare.com", "123456", rolVet);
        createUser("Dra. Patricia Huamán", "patricia.h@petcare.com", "123456", rolVet);
        createUser("Dr. Ricardo Gutiérrez", "ricardo.g@petcare.com", "123456", rolVet);

        // Asistentes
        var asisUser = createUser("María García", "asistente@petcare.com", "asistente123", rolAsistente);
        createUser("Sofía Reyes", "sofia.reyes@petcare.com", "123456", rolAsistente);
        createUser("Diego Castillo", "diego.c@petcare.com", "123456", rolAsistente);

        // Dueños
        var duenioUser = createUser("Juan Pérez", "duenio@petcare.com", "duenio123", rolDuenio);
        createUser("Ana Gómez", "ana.gomez@email.com", "123456", rolDuenio);
        createUser("Pedro Sánchez", "pedro.s@email.com", "123456", rolDuenio);
        createUser("Carmen Torres", "carmen.t@email.com", "123456", rolDuenio);
        createUser("Luis Fernández", "luis.f@email.com", "123456", rolDuenio);

        // Multi-role user (admin + vet)
        var multiUser = Usuario.builder()
                .fullName("Dr. Supervisor")
                .email("supervisor@petcare.com")
                .password(passwordEncoder.encode("123456"))
                .active(true)
                .createdAt(LocalDateTime.now())
                .roles(new java.util.HashSet<>(Set.of(rolAdmin, rolVet)))
                .build();
        usuarioRepository.save(multiUser);

        var servicios = createServicios();
        createVacunas();

        var vet = createVeterinario(vetUser);
        createAsistente(asisUser);
        var duenio = createDuenio(duenioUser);
        createMascotas(duenio);

        System.out.println("Seed data created successfully.");
        System.out.println("  Admin:       admin@petcare.com / admin123");
        System.out.println("  Veterinario: vet@petcare.com / vet123");
        System.out.println("  Asistente:   asistente@petcare.com / asistente123");
        System.out.println("  Dueño:       duenio@petcare.com / duenio123");
        System.out.println("  Supervisor:  supervisor@petcare.com / 123456 (ADMIN + VETERINARIO)");
    }

    private Usuario createUser(String fullName, String email, String password, Rol role) {
        var user = Usuario.builder()
                .fullName(fullName)
                .email(email)
                .password(passwordEncoder.encode(password))
                .active(true)
                .createdAt(LocalDateTime.now())
                .roles(new java.util.HashSet<>(Set.of(role)))
                .build();
        return usuarioRepository.save(user);
    }

    private Duenio createDuenio(Usuario user) {
        var duenio = Duenio.builder()
                .usuario(user)
                .nombres("Juan")
                .apellidos("Pérez")
                .tipoDocumento("DNI")
                .numeroDocumento("12345678")
                .telefono("999888777")
                .email(user.getEmail())
                .direccion("Av. Siempre Viva 123, Lima")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return duenioRepository.save(duenio);
    }

    private Veterinario createVeterinario(Usuario user) {
        var vet = Veterinario.builder()
                .usuario(user)
                .nombres("Carlos")
                .apellidos("López")
                .numeroColegiatura("CMP-12345")
                .especialidad("Medicina General")
                .telefono("999111222")
                .email(user.getEmail())
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        var horarios = List.of(
                HorarioVeterinario.builder().veterinario(vet).diaSemana(DayOfWeek.MONDAY).horaInicio(LocalTime.of(8, 0)).horaFin(LocalTime.of(17, 0)).duracionBloqueMinutos(30).active(true).build(),
                HorarioVeterinario.builder().veterinario(vet).diaSemana(DayOfWeek.TUESDAY).horaInicio(LocalTime.of(8, 0)).horaFin(LocalTime.of(17, 0)).duracionBloqueMinutos(30).active(true).build(),
                HorarioVeterinario.builder().veterinario(vet).diaSemana(DayOfWeek.WEDNESDAY).horaInicio(LocalTime.of(8, 0)).horaFin(LocalTime.of(17, 0)).duracionBloqueMinutos(30).active(true).build(),
                HorarioVeterinario.builder().veterinario(vet).diaSemana(DayOfWeek.THURSDAY).horaInicio(LocalTime.of(8, 0)).horaFin(LocalTime.of(17, 0)).duracionBloqueMinutos(30).active(true).build(),
                HorarioVeterinario.builder().veterinario(vet).diaSemana(DayOfWeek.FRIDAY).horaInicio(LocalTime.of(8, 0)).horaFin(LocalTime.of(17, 0)).duracionBloqueMinutos(30).active(true).build()
        );
        vet.setHorarios(horarios);
        return veterinarioRepository.save(vet);
    }

    private void createAsistente(Usuario user) {
        var asis = Asistente.builder()
                .usuario(user)
                .nombres("María")
                .apellidos("García")
                .tipoDocumento("DNI")
                .numeroDocumento("87654321")
                .telefono("999333444")
                .email(user.getEmail())
                .funciones("Recepción, agenda, facturación")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        asistenteRepository.save(asis);
    }

    private List<Servicio> createServicios() {
        var servicios = List.of(
                Servicio.builder().nombre("Consulta General").descripcion("Atención médica general para mascotas").costoBase(new BigDecimal("60.00")).active(true).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build(),
                Servicio.builder().nombre("Consulta Especializada").descripcion("Atención con especialista en áreas específicas").costoBase(new BigDecimal("90.00")).active(true).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build(),
                Servicio.builder().nombre("Vacunación Completa").descripcion("Aplicación de vacunas según calendario").costoBase(new BigDecimal("120.00")).active(true).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build(),
                Servicio.builder().nombre("Vacunación Antirrábica").descripcion("Vacuna contra la rabia").costoBase(new BigDecimal("45.00")).active(true).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build(),
                Servicio.builder().nombre("Cirugía Menor").descripcion("Procedimientos quirúrgicos de baja complejidad").costoBase(new BigDecimal("250.00")).active(true).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build(),
                Servicio.builder().nombre("Esterilización").descripcion("Cirugía de esterilización para perros y gatos").costoBase(new BigDecimal("350.00")).active(true).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build(),
                Servicio.builder().nombre("Análisis de Sangre").descripcion("Perfil bioquímico completo").costoBase(new BigDecimal("80.00")).active(true).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build(),
                Servicio.builder().nombre("Ecografía").descripcion("Diagnóstico por imágenes ecográficas").costoBase(new BigDecimal("150.00")).active(true).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build(),
                Servicio.builder().nombre("Radiografía").descripcion("Estudio radiológico digital").costoBase(new BigDecimal("100.00")).active(true).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build(),
                Servicio.builder().nombre("Peluquería Canina").descripcion("Baño, corte y cepillado").costoBase(new BigDecimal("55.00")).active(true).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build(),
                Servicio.builder().nombre("Desparasitación").descripcion("Desparasitación interna y externa").costoBase(new BigDecimal("40.00")).active(true).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build(),
                Servicio.builder().nombre("Limpieza Dental").descripcion("Profilaxis dental con sedación").costoBase(new BigDecimal("180.00")).active(true).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build()
        );
        return servicioRepository.saveAll(servicios);
    }

    private void createVacunas() {
        var vacunas = List.of(
                Vacuna.builder().nombre("Rabia Canina").descripcion("Vacuna antirrábica para caninos, dosis única anual").intervaloProximaDosisDias(365).active(true).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build(),
                Vacuna.builder().nombre("Múltiple Canina (Séxtuple)").descripcion("Protege contra moquillo, hepatitis, parvovirus, parainfluenza, leptospira y coronavirus").intervaloProximaDosisDias(365).active(true).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build(),
                Vacuna.builder().nombre("Moquillo Canino").descripcion("Vacuna contra el moquillo en perros").intervaloProximaDosisDias(365).active(true).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build(),
                Vacuna.builder().nombre("Triple Felina").descripcion("Protege contra panleucopenia, calicivirus y rinotraqueitis").intervaloProximaDosisDias(365).active(true).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build(),
                Vacuna.builder().nombre("Leucemia Felina").descripcion("Vacuna contra el virus de la leucemia felina").intervaloProximaDosisDias(365).active(true).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build(),
                Vacuna.builder().nombre("Parvovirus Canino").descripcion("Vacuna específica contra parvovirus").intervaloProximaDosisDias(365).active(true).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build()
        );
        vacunaRepository.saveAll(vacunas);
    }

    private void createMascotas(Duenio duenio) {
        var mascotas = List.of(
                Mascota.builder().duenio(duenio).nombre("Max").especie("Canino").raza("Golden Retriever").sexo(SexoMascota.MACHO).fechaNacimiento(LocalDate.of(2021, 3, 15)).color("Dorado").pesoKg(new BigDecimal("28.50")).observaciones("Alérgico a las pulgas").active(true).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build(),
                Mascota.builder().duenio(duenio).nombre("Luna").especie("Felino").raza("Siamés").sexo(SexoMascota.HEMBRA).fechaNacimiento(LocalDate.of(2022, 7, 8)).color("Crema").pesoKg(new BigDecimal("4.20")).observaciones("Dieta especial por sobrepeso").active(true).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build(),
                Mascota.builder().duenio(duenio).nombre("Rocky").especie("Canino").raza("Bulldog Francés").sexo(SexoMascota.MACHO).fechaNacimiento(LocalDate.of(2023, 1, 20)).color("Atigrado").pesoKg(new BigDecimal("12.00")).observaciones(null).active(true).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build()
        );
        mascotaRepository.saveAll(mascotas);
    }

    private void createRoleIfMissing(RoleName name, String description) {
        if (rolRepository.existsByName(name)) {
            return;
        }
        rolRepository.save(Rol.builder()
                .name(name)
                .description(description)
                .active(true)
                .build());
    }
}
