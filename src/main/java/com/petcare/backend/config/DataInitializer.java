package com.petcare.backend.config;

import com.petcare.backend.domain.repository.*;
import com.petcare.backend.persistence.entity.*;
import com.petcare.backend.persistence.enums.EstadoCita;
import com.petcare.backend.persistence.enums.RoleName;
import com.petcare.backend.persistence.enums.SexoMascota;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

//@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final DuenioRepository duenioRepository;
    private final VeterinarioRepository veterinarioRepository;
    private final AsistenteRepository asistenteRepository;
    private final MascotaRepository mascotaRepository;
    private final ServicioRepository servicioRepository;
    private final VacunaRepository vacunaRepository;
    private final CitaRepository citaRepository;
    private final AtencionClinicaRepository atencionClinicaRepository;
    private final InasistenciaRepository inasistenciaRepository;
    private final DetalleCostoCitaRepository detalleCostoCitaRepository;
    private final VacunaMascotaRepository vacunaMascotaRepository;
    private final ControlMensualMascotaRepository controlMensualMascotaRepository;
    private final HorarioVeterinarioRepository horarioVeterinarioRepository;

    @Override
    public void run(String... args) {
        var roles = initRoles();
        initUsuarios(roles);
        initServicios();
        initVacunas();
        initVeterinarios();
        initAsistentes();
        initDuenios();
        initMascotas();
        initCitas();
    }

    private Map<RoleName, Rol> initRoles() {
        var roles = new HashMap<RoleName, Rol>();
        for (var roleName : RoleName.values()) {
            roles.put(roleName, rolRepository.findByName(roleName).orElseGet(() -> {
                var descripcion = switch (roleName) {
                    case ROLE_ADMIN -> "Administrador general del sistema.";
                    case ROLE_VETERINARIO -> "Personal medico veterinario.";
                    case ROLE_ASISTENTE -> "Personal operativo de recepcion y agenda.";
                    case ROLE_DUENIO -> "Cliente o propietario de mascota.";
                };
                return rolRepository.save(Rol.builder()
                        .name(roleName)
                        .description(descripcion)
                        .active(true)
                        .build());
            }));
        }
        return roles;
    }

    private void initUsuarios(Map<RoleName, Rol> roles) {
        var users = List.of(
                Map.of("name", "Admin Sistema", "email", "admin@petcare.com", "pass", "admin123", "roles", List.of(roles.get(RoleName.ROLE_ADMIN))),
                Map.of("name", "Laura Mendoza", "email", "laura.admin@petcare.com", "pass", "admin123", "roles", List.of(roles.get(RoleName.ROLE_ADMIN))),
                Map.of("name", "Dr. Carlos Lopez", "email", "vet@petcare.com", "pass", "vet123", "roles", List.of(roles.get(RoleName.ROLE_VETERINARIO))),
                Map.of("name", "Dr. Miguel Alvarez", "email", "miguel.alvarez@petcare.com", "pass", "123456", "roles", List.of(roles.get(RoleName.ROLE_VETERINARIO))),
                Map.of("name", "Dra. Patricia Huaman", "email", "patricia.h@petcare.com", "pass", "123456", "roles", List.of(roles.get(RoleName.ROLE_VETERINARIO))),
                Map.of("name", "Dr. Ricardo Gutierrez", "email", "ricardo.g@petcare.com", "pass", "123456", "roles", List.of(roles.get(RoleName.ROLE_VETERINARIO))),
                Map.of("name", "Maria Garcia", "email", "asistente@petcare.com", "pass", "asistente123", "roles", List.of(roles.get(RoleName.ROLE_ASISTENTE))),
                Map.of("name", "Sofia Reyes", "email", "sofia.reyes@petcare.com", "pass", "123456", "roles", List.of(roles.get(RoleName.ROLE_ASISTENTE))),
                Map.of("name", "Diego Castillo", "email", "diego.c@petcare.com", "pass", "123456", "roles", List.of(roles.get(RoleName.ROLE_ASISTENTE))),
                Map.of("name", "Juan Perez", "email", "duenio@petcare.com", "pass", "duenio123", "roles", List.of(roles.get(RoleName.ROLE_DUENIO))),
                Map.of("name", "Ana Gomez", "email", "ana.gomez@email.com", "pass", "123456", "roles", List.of(roles.get(RoleName.ROLE_DUENIO))),
                Map.of("name", "Pedro Sanchez", "email", "pedro.s@email.com", "pass", "123456", "roles", List.of(roles.get(RoleName.ROLE_DUENIO))),
                Map.of("name", "Carmen Torres", "email", "carmen.t@email.com", "pass", "123456", "roles", List.of(roles.get(RoleName.ROLE_DUENIO))),
                Map.of("name", "Luis Fernandez", "email", "luis.f@email.com", "pass", "123456", "roles", List.of(roles.get(RoleName.ROLE_DUENIO))),
                Map.of("name", "Dr. Supervisor", "email", "supervisor@petcare.com", "pass", "123456", "roles", List.of(roles.get(RoleName.ROLE_ADMIN), roles.get(RoleName.ROLE_VETERINARIO)))
        );
        for (var u : users) {
            var email = (String) u.get("email");
            if (usuarioRepository.findByEmail(email).isPresent()) continue;
            @SuppressWarnings("unchecked")
            var roleSet = new HashSet<>((List<Rol>) u.get("roles"));
            usuarioRepository.save(Usuario.builder()
                    .fullName((String) u.get("name"))
                    .email(email)
                    .password(passwordEncoder.encode((String) u.get("pass")))
                    .active(true)
                    .createdAt(LocalDateTime.now())
                    .roles(roleSet)
                    .build());
        }
    }

    private void initServicios() {
        var servicios = List.of(
                new Object[]{"Consulta General", "Atencion medica general para mascotas", new BigDecimal("60.00")},
                new Object[]{"Consulta Especializada", "Atencion con especialista en areas especificas", new BigDecimal("90.00")},
                new Object[]{"Vacunacion Completa", "Aplicacion de vacunas segun calendario", new BigDecimal("120.00")},
                new Object[]{"Vacunacion Antirrabica", "Vacuna contra la rabia", new BigDecimal("45.00")},
                new Object[]{"Cirugia Menor", "Procedimientos quirurgicos de baja complejidad", new BigDecimal("250.00")},
                new Object[]{"Esterilizacion", "Cirugia de esterilizacion para perros y gatos", new BigDecimal("350.00")},
                new Object[]{"Analisis de Sangre", "Perfil bioquimico completo", new BigDecimal("80.00")},
                new Object[]{"Ecografia", "Diagnostico por imagenes ecograficas", new BigDecimal("150.00")},
                new Object[]{"Radiografia", "Estudio radiologico digital", new BigDecimal("100.00")},
                new Object[]{"Peluqueria Canina", "Bano, corte y cepillado", new BigDecimal("55.00")},
                new Object[]{"Desparasitacion", "Desparasitacion interna y externa", new BigDecimal("40.00")},
                new Object[]{"Limpieza Dental", "Profilaxis dental con sedacion", new BigDecimal("180.00")},
                new Object[]{"Hospitalizacion", "Hospitalizacion con cuidados intensivos", new BigDecimal("200.00")},
                new Object[]{"Terapia Fisica", "Rehabilitacion y fisioterapia", new BigDecimal("70.00")},
                new Object[]{"Examen de Heces", "Analisis coproparasitologico", new BigDecimal("35.00")},
                new Object[]{"Prueba de Alergias", "Test de alergias intraepidermico", new BigDecimal("120.00")},
                new Object[]{"Electrocardiograma", "Estudio electrico del corazon", new BigDecimal("130.00")},
                new Object[]{"Transfusion Sanguinea", "Transfusion de sangre completa o plasma", new BigDecimal("400.00")},
                new Object[]{"Endoscopia", "Examen endoscopico digestivo", new BigDecimal("280.00")},
                new Object[]{"Ultrasonido Abdominal", "Ultrasonido de abdomen completo", new BigDecimal("170.00")}
        );
        for (var s : servicios) {
            var nombre = (String) s[0];
            if (servicioRepository.findByNombreIgnoreCase(nombre).isPresent()) continue;
            servicioRepository.save(Servicio.builder()
                    .nombre(nombre)
                    .descripcion((String) s[1])
                    .costoBase((BigDecimal) s[2])
                    .active(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build());
        }
    }

    private void initVacunas() {
        var vacunas = List.of(
                new Object[]{"Rabia Canina", "Vacuna antirrabica para caninos, dosis unica anual", 365},
                new Object[]{"Multiple Canina (Sextuple)", "Protege contra moquillo, hepatitis, parvovirus, parainfluenza, leptospira y coronavirus", 365},
                new Object[]{"Moquillo Canino", "Vacuna contra el moquillo en perros", 365},
                new Object[]{"Triple Felina", "Protege contra panleucopenia, calicivirus y rinotraqueitis", 365},
                new Object[]{"Leucemia Felina", "Vacuna contra el virus de la leucemia felina", 365},
                new Object[]{"Parvovirus Canino", "Vacuna especifica contra parvovirus", 365},
                new Object[]{"Bordetella (Tos de las perreras)", "Vacuna contra la bordetella bronchiseptica", 180},
                new Object[]{"Leptospirosis", "Vacuna contra la leptospirosis canina", 365},
                new Object[]{"Coronavirus Canino", "Vacuna contra el coronavirus enterico canino", 365},
                new Object[]{"Influenza Canina", "Vacuna contra la gripe canina (H3N8 y H3N2)", 365},
                new Object[]{"Polivalente Felina", "Vacuna tetravalente para felinos", 365},
                new Object[]{"Giardiasis Canina", "Vacuna contra la giardia en caninos", 180}
        );
        for (var v : vacunas) {
            var nombre = (String) v[0];
            if (vacunaRepository.findByNombreIgnoreCase(nombre).isPresent()) continue;
            vacunaRepository.save(Vacuna.builder()
                    .nombre(nombre)
                    .descripcion((String) v[1])
                    .intervaloProximaDosisDias((Integer) v[2])
                    .active(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build());
        }
    }

    private void initVeterinarios() {
        var vetUser1 = usuarioRepository.findByEmail("vet@petcare.com").orElse(null);
        var vetUser2 = usuarioRepository.findByEmail("miguel.alvarez@petcare.com").orElse(null);
        var vetUser3 = usuarioRepository.findByEmail("patricia.h@petcare.com").orElse(null);
        var vetUser4 = usuarioRepository.findByEmail("ricardo.g@petcare.com").orElse(null);

        if (veterinarioRepository.findByEmail("vet@petcare.com").isEmpty()) {
            var vet1 = Veterinario.builder()
                    .usuario(vetUser1)
                    .nombres("Carlos").apellidos("Lopez")
                    .numeroColegiatura("CMP-12345").especialidad("Medicina General")
                    .telefono("999111222").email("vet@petcare.com")
                    .active(true).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .build();
            vet1.setHorarios(List.of(
                horario(vet1, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(17, 0), 30),
                horario(vet1, DayOfWeek.TUESDAY, LocalTime.of(8, 0), LocalTime.of(17, 0), 30),
                horario(vet1, DayOfWeek.WEDNESDAY, LocalTime.of(8, 0), LocalTime.of(17, 0), 30),
                horario(vet1, DayOfWeek.THURSDAY, LocalTime.of(8, 0), LocalTime.of(17, 0), 30),
                horario(vet1, DayOfWeek.FRIDAY, LocalTime.of(8, 0), LocalTime.of(17, 0), 30)
            ));
            veterinarioRepository.save(vet1);
        }

        if (veterinarioRepository.findByEmail("miguel.alvarez@petcare.com").isEmpty()) {
            var vet2 = Veterinario.builder()
                    .usuario(vetUser2)
                    .nombres("Miguel").apellidos("Alvarez")
                    .numeroColegiatura("CMP-56789").especialidad("Medicina General")
                    .telefono("999888999").email("miguel.alvarez@petcare.com")
                    .active(true).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .build();
            vet2.setHorarios(List.of(
                horario(vet2, DayOfWeek.MONDAY, LocalTime.of(7, 0), LocalTime.of(14, 0), 30),
                horario(vet2, DayOfWeek.TUESDAY, LocalTime.of(7, 0), LocalTime.of(14, 0), 30),
                horario(vet2, DayOfWeek.WEDNESDAY, LocalTime.of(7, 0), LocalTime.of(14, 0), 30),
                horario(vet2, DayOfWeek.THURSDAY, LocalTime.of(7, 0), LocalTime.of(14, 0), 30),
                horario(vet2, DayOfWeek.FRIDAY, LocalTime.of(7, 0), LocalTime.of(14, 0), 30)
            ));
            veterinarioRepository.save(vet2);
        }

        if (veterinarioRepository.findByEmail("patricia.h@petcare.com").isEmpty()) {
            var vet3 = Veterinario.builder()
                    .usuario(vetUser3)
                    .nombres("Patricia").apellidos("Huaman")
                    .numeroColegiatura("CMP-45678").especialidad("Medicina Felina")
                    .telefono("998111222").email("patricia.h@petcare.com")
                    .active(true).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .build();
            vet3.setHorarios(List.of(
                horario(vet3, DayOfWeek.TUESDAY, LocalTime.of(8, 0), LocalTime.of(15, 0), 30),
                horario(vet3, DayOfWeek.WEDNESDAY, LocalTime.of(8, 0), LocalTime.of(15, 0), 30),
                horario(vet3, DayOfWeek.THURSDAY, LocalTime.of(8, 0), LocalTime.of(15, 0), 30),
                horario(vet3, DayOfWeek.FRIDAY, LocalTime.of(8, 0), LocalTime.of(15, 0), 30),
                horario(vet3, DayOfWeek.SATURDAY, LocalTime.of(9, 0), LocalTime.of(13, 0), 30)
            ));
            veterinarioRepository.save(vet3);
        }

        if (veterinarioRepository.findByEmail("ricardo.g@petcare.com").isEmpty()) {
            var vet4 = Veterinario.builder()
                    .usuario(vetUser4)
                    .nombres("Ricardo").apellidos("Gutierrez")
                    .numeroColegiatura("CMP-78901").especialidad("Cirugia")
                    .telefono("998333444").email("ricardo.g@petcare.com")
                    .active(true).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .build();
            vet4.setHorarios(List.of(
                horario(vet4, DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(17, 0), 45),
                horario(vet4, DayOfWeek.WEDNESDAY, LocalTime.of(10, 0), LocalTime.of(17, 0), 45),
                horario(vet4, DayOfWeek.FRIDAY, LocalTime.of(10, 0), LocalTime.of(17, 0), 45)
            ));
            veterinarioRepository.save(vet4);
        }

        if (veterinarioRepository.findByEmail("maria.fernandez@petcare.com").isEmpty()) {
            var vet5 = Veterinario.builder()
                    .usuario(null)
                    .nombres("Maria").apellidos("Fernandez")
                    .numeroColegiatura("CMP-23456").especialidad("Cirugia Veterinaria")
                    .telefono("999222333").email("maria.fernandez@petcare.com")
                    .active(true).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .build();
            vet5.setHorarios(List.of(
                horario(vet5, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(18, 0), 30),
                horario(vet5, DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(18, 0), 30),
                horario(vet5, DayOfWeek.WEDNESDAY, LocalTime.of(9, 0), LocalTime.of(18, 0), 30),
                horario(vet5, DayOfWeek.THURSDAY, LocalTime.of(9, 0), LocalTime.of(18, 0), 30)
            ));
            veterinarioRepository.save(vet5);
        }

        if (veterinarioRepository.findByEmail("jose.ramirez@petcare.com").isEmpty()) {
            var vet6 = Veterinario.builder()
                    .usuario(null)
                    .nombres("Jose").apellidos("Ramirez")
                    .numeroColegiatura("CMP-34567").especialidad("Dermatologia")
                    .telefono("999444555").email("jose.ramirez@petcare.com")
                    .active(true).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .build();
            vet6.setHorarios(List.of(
                horario(vet6, DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(16, 0), 45),
                horario(vet6, DayOfWeek.WEDNESDAY, LocalTime.of(10, 0), LocalTime.of(16, 0), 45),
                horario(vet6, DayOfWeek.FRIDAY, LocalTime.of(10, 0), LocalTime.of(16, 0), 45)
            ));
            veterinarioRepository.save(vet6);
        }

        if (veterinarioRepository.findByEmail("diana.torres@petcare.com").isEmpty()) {
            var vet7 = Veterinario.builder()
                    .usuario(null)
                    .nombres("Diana").apellidos("Torres")
                    .numeroColegiatura("CMP-45679").especialidad("Medicina Felina")
                    .telefono("999666777").email("diana.torres@petcare.com")
                    .active(true).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .build();
            vet7.setHorarios(List.of(
                horario(vet7, DayOfWeek.TUESDAY, LocalTime.of(8, 0), LocalTime.of(15, 0), 30),
                horario(vet7, DayOfWeek.WEDNESDAY, LocalTime.of(8, 0), LocalTime.of(15, 0), 30),
                horario(vet7, DayOfWeek.THURSDAY, LocalTime.of(8, 0), LocalTime.of(15, 0), 30),
                horario(vet7, DayOfWeek.FRIDAY, LocalTime.of(8, 0), LocalTime.of(15, 0), 30),
                horario(vet7, DayOfWeek.SATURDAY, LocalTime.of(9, 0), LocalTime.of(13, 0), 30)
            ));
            veterinarioRepository.save(vet7);
        }

        if (veterinarioRepository.findByEmail("andrea.castillo@petcare.com").isEmpty()) {
            var vet8 = Veterinario.builder()
                    .usuario(null)
                    .nombres("Andrea").apellidos("Castillo")
                    .numeroColegiatura("CMP-67890").especialidad("Cardiologia")
                    .telefono("998111222").email("andrea.castillo@petcare.com")
                    .active(true).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .build();
            vet8.setHorarios(List.of(
                horario(vet8, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(16, 0), 30),
                horario(vet8, DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(16, 0), 30),
                horario(vet8, DayOfWeek.WEDNESDAY, LocalTime.of(9, 0), LocalTime.of(16, 0), 30),
                horario(vet8, DayOfWeek.THURSDAY, LocalTime.of(9, 0), LocalTime.of(16, 0), 30),
                horario(vet8, DayOfWeek.FRIDAY, LocalTime.of(9, 0), LocalTime.of(16, 0), 30)
            ));
            veterinarioRepository.save(vet8);
        }
    }

    private HorarioVeterinario horario(Veterinario vet, DayOfWeek dia, LocalTime inicio, LocalTime fin, int duracion) {
        return HorarioVeterinario.builder()
                .veterinario(vet)
                .diaSemana(dia)
                .horaInicio(inicio)
                .horaFin(fin)
                .duracionBloqueMinutos(duracion)
                .active(true)
                .build();
    }

    private void initAsistentes() {
        var asisUser1 = usuarioRepository.findByEmail("asistente@petcare.com").orElse(null);
        var asisUser2 = usuarioRepository.findByEmail("sofia.reyes@petcare.com").orElse(null);
        var asisUser3 = usuarioRepository.findByEmail("diego.c@petcare.com").orElse(null);

        if (asistenteRepository.findByEmail("asistente@petcare.com").isEmpty()) {
            asistenteRepository.save(Asistente.builder()
                    .usuario(asisUser1).nombres("Maria").apellidos("Garcia")
                    .tipoDocumento("DNI").numeroDocumento("87654321")
                    .telefono("999333444").email("asistente@petcare.com")
                    .funciones("Recepcion, agenda, facturacion")
                    .active(true).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .build());
        }
        if (asistenteRepository.findByEmail("sofia.reyes@petcare.com").isEmpty()) {
            asistenteRepository.save(Asistente.builder()
                    .usuario(asisUser2).nombres("Sofia").apellidos("Reyes")
                    .tipoDocumento("DNI").numeroDocumento("11223344")
                    .telefono("999777333").email("sofia.reyes@petcare.com")
                    .funciones("Recepcion y atencion al cliente")
                    .active(true).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .build());
        }
        if (asistenteRepository.findByEmail("diego.c@petcare.com").isEmpty()) {
            asistenteRepository.save(Asistente.builder()
                    .usuario(asisUser3).nombres("Diego").apellidos("Castillo")
                    .tipoDocumento("DNI").numeroDocumento("22334455")
                    .telefono("998222444").email("diego.c@petcare.com")
                    .funciones("Farmacia e inventario")
                    .active(true).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .build());
        }
        if (asistenteRepository.findByEmail("luis.torres@petcare.com").isEmpty()) {
            asistenteRepository.save(Asistente.builder()
                    .usuario(null).nombres("Luis").apellidos("Torres")
                    .tipoDocumento("DNI").numeroDocumento("98765432")
                    .telefono("999555111").email("luis.torres@petcare.com")
                    .funciones("Apoyo en consultas y farmacia")
                    .active(true).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .build());
        }
        if (asistenteRepository.findByEmail("elena.vargas@petcare.com").isEmpty()) {
            asistenteRepository.save(Asistente.builder()
                    .usuario(null).nombres("Elena").apellidos("Vargas")
                    .tipoDocumento("CE").numeroDocumento("CE-007654")
                    .telefono("998666888").email("elena.vargas@petcare.com")
                    .funciones("Apoyo quirurgico y hospitalizacion")
                    .active(true).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .build());
        }
    }

    private void initDuenios() {
        var duenioUser1 = usuarioRepository.findByEmail("duenio@petcare.com").orElse(null);
        var duenioUser2 = usuarioRepository.findByEmail("ana.gomez@email.com").orElse(null);
        var duenioUser3 = usuarioRepository.findByEmail("pedro.s@email.com").orElse(null);
        var duenioUser4 = usuarioRepository.findByEmail("carmen.t@email.com").orElse(null);
        var duenioUser5 = usuarioRepository.findByEmail("luis.f@email.com").orElse(null);

        var duenios = List.of(
                new Object[]{duenioUser1, "Juan", "Perez", "DNI", "12345678", "999888777", "duenio@petcare.com", "Av. Siempre Viva 123, Lima"},
                new Object[]{duenioUser2, "Ana", "Gomez", "DNI", "23456789", "999111222", "ana.gomez@email.com", "Jr. Las Flores 456, Lima"},
                new Object[]{null, "Pedro", "Martinez", "CE", "CE-001234", "999333444", "pedro.m@email.com", "Calle Los Olivos 789, Lima"},
                new Object[]{null, "Carmen", "Lopez", "DNI", "34567890", "999555666", "carmen.lopez@email.com", "Av. Primavera 321, Lima"},
                new Object[]{null, "Roberto", "Sanchez", "DNI", "45678901", "999777888", "roberto.s@email.com", "Urb. El Sol 654, Lima"},
                new Object[]{null, "Laura", "Diaz", "CE", "CE-005678", "999999000", "laura.diaz@email.com", "Pasaje La Paz 987, Lima"},
                new Object[]{null, "Diego", "Herrera", "DNI", "56789012", "998111333", "diego.h@email.com", "Av. Los Pinos 159, Lima"},
                new Object[]{null, "Valeria", "Rojas", "CE", "CE-009876", "998444555", "valeria.r@email.com", "Jr. Las Dalias 753, Lima"},
                new Object[]{null, "Fernando", "Mendoza", "DNI", "67890123", "997666222", "fernando.m@email.com", "Calle Real 852, Lima"},
                new Object[]{null, "Gabriela", "Torres", "DNI", "78901234", "997888111", "gabriela.t@email.com", "Urb. Los Jazmines 456, Lima"}
        );
        for (var d : duenios) {
            var email = (String) d[6];
            if (duenioRepository.findByEmail(email).isPresent()) continue;
            duenioRepository.save(Duenio.builder()
                    .usuario((Usuario) d[0])
                    .nombres((String) d[1]).apellidos((String) d[2])
                    .tipoDocumento((String) d[3]).numeroDocumento((String) d[4])
                    .telefono((String) d[5]).email(email)
                    .direccion((String) d[7])
                    .active(true).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .build());
        }
    }

    private void initMascotas() {
        var d1 = duenioRepository.findByEmail("duenio@petcare.com");
        var d2 = duenioRepository.findByEmail("ana.gomez@email.com");
        var d3 = duenioRepository.findByEmail("pedro.m@email.com");
        var d4 = duenioRepository.findByEmail("carmen.lopez@email.com");
        var d5 = duenioRepository.findByEmail("roberto.s@email.com");
        var d6 = duenioRepository.findByEmail("laura.diaz@email.com");
        var d7 = duenioRepository.findByEmail("diego.h@email.com");
        var d8 = duenioRepository.findByEmail("valeria.r@email.com");
        var d9 = duenioRepository.findByEmail("fernando.m@email.com");
        var d10 = duenioRepository.findByEmail("gabriela.t@email.com");

        if (d1.isPresent() && mascotaRepository.findByDuenioIdOrderByNombreAsc(d1.get().getId()).isEmpty()) {
            var duenio = d1.get();
            saveMascota(duenio, "Max", "Canino", "Golden Retriever", SexoMascota.MACHO, LocalDate.of(2021, 3, 15), "Dorado", new BigDecimal("28.50"), "Alergico a las pulgas");
            saveMascota(duenio, "Luna", "Felino", "Siamés", SexoMascota.HEMBRA, LocalDate.of(2022, 7, 8), "Crema", new BigDecimal("4.20"), "Dieta especial por sobrepeso");
            saveMascota(duenio, "Rocky", "Canino", "Bulldog Frances", SexoMascota.MACHO, LocalDate.of(2023, 1, 20), "Atigrado", new BigDecimal("12.00"), null);
            saveMascota(duenio, "Coco", "Canino", "Chihuahua", SexoMascota.MACHO, LocalDate.of(2024, 6, 1), "Café", new BigDecimal("2.80"), null);
        }
        if (d2.isPresent() && mascotaRepository.findByDuenioIdOrderByNombreAsc(d2.get().getId()).isEmpty()) {
            var duenio = d2.get();
            saveMascota(duenio, "Bella", "Canino", "Labrador", SexoMascota.HEMBRA, LocalDate.of(2020, 5, 10), "Amarillo", new BigDecimal("32.00"), "Vacunas al dia");
            saveMascota(duenio, "Mimi", "Felino", "Persa", SexoMascota.HEMBRA, LocalDate.of(2021, 11, 22), "Blanco", new BigDecimal("3.80"), null);
        }
        if (d3.isPresent() && mascotaRepository.findByDuenioIdOrderByNombreAsc(d3.get().getId()).isEmpty()) {
            var duenio = d3.get();
            saveMascota(duenio, "Toby", "Canino", "Poodle", SexoMascota.MACHO, LocalDate.of(2022, 9, 5), "Blanco", new BigDecimal("6.50"), "Tiene displasia de cadera");
            saveMascota(duenio, "Pelusa", "Felino", "Angora", SexoMascota.HEMBRA, LocalDate.of(2023, 4, 18), "Gris", new BigDecimal("3.20"), null);
        }
        if (d4.isPresent() && mascotaRepository.findByDuenioIdOrderByNombreAsc(d4.get().getId()).isEmpty()) {
            var duenio = d4.get();
            saveMascota(duenio, "Thor", "Canino", "Pastor Aleman", SexoMascota.MACHO, LocalDate.of(2019, 8, 12), "Negro", new BigDecimal("38.00"), "Requiere paseos diarios");
            saveMascota(duenio, "Canela", "Felino", "Naranja", SexoMascota.HEMBRA, LocalDate.of(2020, 12, 30), "Naranja", new BigDecimal("4.50"), null);
        }
        if (d5.isPresent() && mascotaRepository.findByDuenioIdOrderByNombreAsc(d5.get().getId()).isEmpty()) {
            var duenio = d5.get();
            saveMascota(duenio, "Zeus", "Canino", "Husky", SexoMascota.MACHO, LocalDate.of(2021, 6, 1), "Gris/Blanco", new BigDecimal("25.00"), "Se escapa seguido");
            saveMascota(duenio, "Thor 2", "Canino", "Rottweiler", SexoMascota.MACHO, LocalDate.of(2022, 3, 10), "Negro/Café", new BigDecimal("42.00"), "Necesita socializacion");
        }
        if (d6.isPresent() && mascotaRepository.findByDuenioIdOrderByNombreAsc(d6.get().getId()).isEmpty()) {
            var duenio = d6.get();
            saveMascota(duenio, "Nala", "Felino", "Bengali", SexoMascota.HEMBRA, LocalDate.of(2022, 2, 14), "Moteado", new BigDecimal("3.90"), "Muy activa");
            saveMascota(duenio, "Simba", "Canino", "Beagle", SexoMascota.MACHO, LocalDate.of(2023, 10, 10), "Marron/Blanco", new BigDecimal("14.00"), "Olfato muy desarrollado");
        }
        if (d7.isPresent() && mascotaRepository.findByDuenioIdOrderByNombreAsc(d7.get().getId()).isEmpty()) {
            var duenio = d7.get();
            saveMascota(duenio, "Princesa", "Canino", "Cocker Spaniel", SexoMascota.HEMBRA, LocalDate.of(2020, 8, 20), "Dorado", new BigDecimal("15.00"), "Propensa a infecciones de oido");
            saveMascota(duenio, "Garfiel", "Felino", "Naranja", SexoMascota.MACHO, LocalDate.of(2021, 12, 5), "Anaranjado", new BigDecimal("5.20"), "Ronca al dormir");
        }
        if (d8.isPresent() && mascotaRepository.findByDuenioIdOrderByNombreAsc(d8.get().getId()).isEmpty()) {
            var duenio = d8.get();
            saveMascota(duenio, "Kiara", "Felino", "Sagrado de Birmania", SexoMascota.HEMBRA, LocalDate.of(2023, 5, 15), "Crema", new BigDecimal("3.50"), null);
            saveMascota(duenio, "Bruno", "Canino", "San Bernardo", SexoMascota.MACHO, LocalDate.of(2018, 11, 30), "Blanco/Rojo", new BigDecimal("55.00"), "Excelente con ninos");
        }
        if (d9.isPresent() && mascotaRepository.findByDuenioIdOrderByNombreAsc(d9.get().getId()).isEmpty()) {
            var duenio = d9.get();
            saveMascota(duenio, "Duke", "Canino", "Doberman", SexoMascota.MACHO, LocalDate.of(2021, 9, 15), "Negro/Café", new BigDecimal("35.00"), "Usar bozal en consulta");
            saveMascota(duenio, "Lola", "Felino", "Esfinge", SexoMascota.HEMBRA, LocalDate.of(2023, 3, 22), "Gris", new BigDecimal("2.90"), "Piel sensible al sol");
        }
        if (d10.isPresent() && mascotaRepository.findByDuenioIdOrderByNombreAsc(d10.get().getId()).isEmpty()) {
            var duenio = d10.get();
            saveMascota(duenio, "Paco", "Canino", "Fox Terrier", SexoMascota.MACHO, LocalDate.of(2022, 7, 1), "Blanco/Negro", new BigDecimal("8.00"), null);
            saveMascota(duenio, "Mia", "Felino", "Azul Ruso", SexoMascota.HEMBRA, LocalDate.of(2023, 11, 10), "Gris", new BigDecimal("3.10"), "Timida con extranos");
        }
    }

    private void saveMascota(Duenio duenio, String nombre, String especie, String raza, SexoMascota sexo, LocalDate fechaNac, String color, BigDecimal peso, String observaciones) {
        mascotaRepository.save(Mascota.builder()
                .duenio(duenio).nombre(nombre).especie(especie).raza(raza).sexo(sexo)
                .fechaNacimiento(fechaNac).color(color).pesoKg(peso).observaciones(observaciones)
                .active(true).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build());
    }

    private void initCitas() {
        var hoy = LocalDate.now();

        var servicios = servicioRepository.findAll();
        var consulGral = servicios.stream().filter(s -> s.getNombre().equals("Consulta General")).findFirst().orElse(null);
        var consulEsp = servicios.stream().filter(s -> s.getNombre().equals("Consulta Especializada")).findFirst().orElse(null);
        var vacCompleta = servicios.stream().filter(s -> s.getNombre().equals("Vacunacion Completa")).findFirst().orElse(null);
        var vacAntirrab = servicios.stream().filter(s -> s.getNombre().equals("Vacunacion Antirrabica")).findFirst().orElse(null);
        var desparasit = servicios.stream().filter(s -> s.getNombre().equals("Desparasitacion")).findFirst().orElse(null);
        var limpDental = servicios.stream().filter(s -> s.getNombre().equals("Limpieza Dental")).findFirst().orElse(null);
        var ecografia = servicios.stream().filter(s -> s.getNombre().equals("Ecografia")).findFirst().orElse(null);
        var radiografia = servicios.stream().filter(s -> s.getNombre().equals("Radiografia")).findFirst().orElse(null);
        var electro = servicios.stream().filter(s -> s.getNombre().equals("Electrocardiograma")).findFirst().orElse(null);

        if (citaRepository.count() > 0) return;

        var duenios = duenioRepository.findAll();
        var duenioMap = new HashMap<String, Duenio>();
        for (var d : duenios) duenioMap.put(d.getEmail(), d);

        var d1 = duenioMap.get("duenio@petcare.com");
        var d2 = duenioMap.get("ana.gomez@email.com");
        var d3 = duenioMap.get("pedro.m@email.com");
        var d4 = duenioMap.get("carmen.lopez@email.com");
        var d5 = duenioMap.get("roberto.s@email.com");
        var d6 = duenioMap.get("laura.diaz@email.com");
        var d7 = duenioMap.get("diego.h@email.com");
        var d8 = duenioMap.get("valeria.r@email.com");
        var d9 = duenioMap.get("fernando.m@email.com");
        var d10 = duenioMap.get("gabriela.t@email.com");

        var mascotas = mascotaRepository.findAll();
        var m1 = findMascota(mascotas, "Max", d1);
        var m2 = findMascota(mascotas, "Luna", d1);
        var m3 = findMascota(mascotas, "Rocky", d1);
        var m4 = findMascota(mascotas, "Coco", d1);
        var m5 = findMascota(mascotas, "Bella", d2);
        var m6 = findMascota(mascotas, "Mimi", d2);
        var m7 = findMascota(mascotas, "Toby", d3);
        var m8 = findMascota(mascotas, "Pelusa", d3);
        var m9 = findMascota(mascotas, "Thor", d4);
        var m10 = findMascota(mascotas, "Canela", d4);
        var m11 = findMascota(mascotas, "Zeus", d5);
        var m12 = findMascota(mascotas, "Thor 2", d5);
        var m13 = findMascota(mascotas, "Nala", d6);
        var m14 = findMascota(mascotas, "Simba", d6);
        var m15 = findMascota(mascotas, "Princesa", d7);
        var m16 = findMascota(mascotas, "Garfiel", d7);
        var m17 = findMascota(mascotas, "Kiara", d8);
        var m18 = findMascota(mascotas, "Bruno", d8);
        var m19 = findMascota(mascotas, "Duke", d9);
        var m20 = findMascota(mascotas, "Lola", d9);
        var m21 = findMascota(mascotas, "Paco", d10);
        var m22 = findMascota(mascotas, "Mia", d10);

        var veterinarios = veterinarioRepository.findAll();
        var vetMap = new HashMap<String, Veterinario>();
        for (var v : veterinarios) vetMap.put(v.getEmail(), v);
        var v1 = vetMap.get("vet@petcare.com");
        var v2 = vetMap.get("miguel.alvarez@petcare.com");
        var v3 = vetMap.get("patricia.h@petcare.com");
        var v4 = vetMap.get("ricardo.g@petcare.com");
        var v5 = vetMap.get("maria.fernandez@petcare.com");
        var v6 = vetMap.get("jose.ramirez@petcare.com");

        if (v1 == null || v2 == null) return;

        var now = LocalDateTime.now();
        var citas = new ArrayList<Cita>();

        var citaData = List.of(
                new CitaData(d1, m1, v1, hoy, LocalTime.of(8, 0), LocalTime.of(8, 30), 30, "Control anual de rutina", EstadoCita.CONFIRMADA, bd("60.00"), bd("0"), bd("60.00"), true, now, now),
                new CitaData(d1, m2, v2, hoy, LocalTime.of(9, 0), LocalTime.of(9, 30), 30, "Revision por perdida de peso", EstadoCita.CONFIRMADA, bd("60.00"), bd("0"), bd("60.00"), true, now, now),
                new CitaData(d2, m5, v1, hoy, LocalTime.of(9, 30), LocalTime.of(10, 0), 30, "Vacunacion antirrabica anual", EstadoCita.CONFIRMADA, bd("105.00"), bd("0"), bd("105.00"), true, now, now),
                new CitaData(d3, m7, v3, hoy, LocalTime.of(10, 0), LocalTime.of(10, 45), 45, "Consulta por dermatitis", EstadoCita.PROGRAMADA, bd("90.00"), bd("0"), bd("90.00"), false, now, now),
                new CitaData(d4, m9, v4, hoy, LocalTime.of(11, 0), LocalTime.of(11, 30), 30, "Revision de cadera", EstadoCita.PROGRAMADA, bd("60.00"), bd("0"), bd("60.00"), false, now, now),
                new CitaData(d5, m11, v2, hoy, LocalTime.of(11, 30), LocalTime.of(12, 0), 30, "Control de peso", EstadoCita.PROGRAMADA, bd("60.00"), bd("0"), bd("60.00"), false, now, now),
                new CitaData(d6, m13, v4, hoy, LocalTime.of(14, 0), LocalTime.of(14, 30), 30, "Revision general por letargo", EstadoCita.PROGRAMADA, bd("60.00"), bd("0"), bd("60.00"), false, now, now),
                new CitaData(d5, m11, v2, hoy.plusDays(1), LocalTime.of(8, 0), LocalTime.of(8, 30), 30, "Vacunacion multiple", EstadoCita.PROGRAMADA, bd("165.00"), bd("0"), bd("165.00"), true, now, now),
                new CitaData(d7, m15, v4, hoy.plusDays(1), LocalTime.of(9, 0), LocalTime.of(9, 30), 30, "Control mensual", EstadoCita.PROGRAMADA, bd("60.00"), bd("0"), bd("60.00"), false, now, now),
                new CitaData(d1, m3, v2, hoy.plusDays(1), LocalTime.of(10, 0), LocalTime.of(10, 30), 30, "Revision de alergias", EstadoCita.CONFIRMADA, bd("60.00"), bd("0"), bd("60.00"), true, now, now),
                new CitaData(d2, m6, v1, hoy.plusDays(1), LocalTime.of(11, 30), LocalTime.of(12, 0), 30, "Vacunacion triple felina", EstadoCita.PROGRAMADA, bd("165.00"), bd("0"), bd("165.00"), false, now, now),
                new CitaData(d10, m21, v2, hoy.plusDays(1), LocalTime.of(14, 0), LocalTime.of(14, 30), 30, "Consulta por vomitos", EstadoCita.PROGRAMADA, bd("60.00"), bd("0"), bd("60.00"), false, now, now),
                new CitaData(d1, m1, v1, hoy.minusDays(7), LocalTime.of(9, 0), LocalTime.of(9, 30), 30, "Consulta general", EstadoCita.ATENDIDA, bd("60.00"), bd("0"), bd("60.00"), true, now.minusDays(7), now.minusDays(7)),
                new CitaData(d2, m5, v1, hoy.minusDays(6), LocalTime.of(10, 0), LocalTime.of(10, 30), 30, "Desparasitacion", EstadoCita.ATENDIDA, bd("40.00"), bd("0"), bd("40.00"), true, now.minusDays(6), now.minusDays(6)),
                new CitaData(d3, m8, v3, hoy.minusDays(5), LocalTime.of(11, 0), LocalTime.of(11, 45), 45, "Revision de alergias cutaneas", EstadoCita.ATENDIDA, bd("90.00"), bd("0"), bd("90.00"), true, now.minusDays(5), now.minusDays(5)),
                new CitaData(d5, m12, v2, hoy.minusDays(4), LocalTime.of(14, 0), LocalTime.of(14, 30), 30, "Control de socializacion", EstadoCita.ATENDIDA, bd("60.00"), bd("0"), bd("60.00"), false, now.minusDays(4), now.minusDays(4)),
                new CitaData(d7, m15, v4, hoy.minusDays(3), LocalTime.of(15, 0), LocalTime.of(15, 30), 30, "Revision de oidos", EstadoCita.ATENDIDA, bd("60.00"), bd("0"), bd("60.00"), false, now.minusDays(3), now.minusDays(3)),
                new CitaData(d8, m18, v1, hoy.minusDays(2), LocalTime.of(8, 0), LocalTime.of(8, 30), 30, "Control de peso - obesidad", EstadoCita.ATENDIDA, bd("60.00"), bd("0"), bd("60.00"), true, now.minusDays(2), now.minusDays(2)),
                new CitaData(d9, m19, v6, hoy.minusDays(1), LocalTime.of(9, 0), LocalTime.of(9, 30), 30, "Evaluacion cardiaca de rutina", EstadoCita.ATENDIDA, bd("130.00"), bd("0"), bd("130.00"), false, now.minusDays(1), now.minusDays(1)),
                new CitaData(d4, m10, v4, hoy.minusDays(1), LocalTime.of(10, 0), LocalTime.of(10, 30), 30, "Control felino trimestral", EstadoCita.ATENDIDA, bd("60.00"), bd("0"), bd("60.00"), false, now.minusDays(1), now.minusDays(1)),
                new CitaData(d4, m9, v4, hoy.minusDays(8), LocalTime.of(14, 0), LocalTime.of(14, 30), 30, "Revision general", EstadoCita.CANCELADA, bd("60.00"), bd("0"), bd("60.00"), false, now.minusDays(8), now.minusDays(8)),
                new CitaData(d6, m14, v2, hoy.minusDays(6), LocalTime.of(15, 0), LocalTime.of(15, 30), 30, "Vacunacion de refuerzo", EstadoCita.CANCELADA, bd("45.00"), bd("0"), bd("45.00"), false, now.minusDays(6), now.minusDays(6)),
                new CitaData(d10, m22, v4, hoy.minusDays(3), LocalTime.of(16, 0), LocalTime.of(16, 30), 30, "Primera consulta gatita nueva", EstadoCita.CANCELADA, bd("60.00"), bd("0"), bd("60.00"), false, now.minusDays(3), now.minusDays(3)),
                new CitaData(d3, m7, v3, hoy.minusDays(10), LocalTime.of(15, 0), LocalTime.of(15, 45), 45, "Consulta por vomitos", EstadoCita.NO_ASISTIO, bd("90.00"), bd("0"), bd("90.00"), false, now.minusDays(10), now.minusDays(10)),
                new CitaData(d7, m16, v1, hoy.minusDays(5), LocalTime.of(10, 0), LocalTime.of(10, 30), 30, "Revision de oidos - recurrencia", EstadoCita.NO_ASISTIO, bd("60.00"), bd("0"), bd("60.00"), false, now.minusDays(5), now.minusDays(5)),
                new CitaData(d2, m6, v2, hoy.minusDays(3), LocalTime.of(11, 0), LocalTime.of(11, 30), 30, "Vacunacion anual", EstadoCita.NO_ASISTIO, bd("45.00"), bd("0"), bd("45.00"), false, now.minusDays(3), now.minusDays(3)),
                new CitaData(d9, m20, v3, hoy.minusDays(1), LocalTime.of(14, 0), LocalTime.of(14, 45), 45, "Consulta dermatologica", EstadoCita.NO_ASISTIO, bd("90.00"), bd("0"), bd("90.00"), false, now.minusDays(1), now.minusDays(1)),
                new CitaData(d1, m1, v1, hoy.plusDays(7), LocalTime.of(9, 0), LocalTime.of(9, 30), 30, "Vacunacion multiple", EstadoCita.PROGRAMADA, bd("120.00"), bd("0"), bd("120.00"), true, now, now),
                new CitaData(d1, m2, v2, hoy.plusDays(7), LocalTime.of(10, 0), LocalTime.of(10, 30), 30, "Limpieza dental", EstadoCita.PROGRAMADA, bd("180.00"), bd("0"), bd("180.00"), false, now, now),
                new CitaData(d6, m14, v2, hoy.plusDays(8), LocalTime.of(8, 0), LocalTime.of(8, 30), 30, "Primera consulta", EstadoCita.PROGRAMADA, bd("60.00"), bd("0"), bd("60.00"), true, now, now),
                new CitaData(d5, m11, v2, hoy.plusDays(10), LocalTime.of(11, 0), LocalTime.of(11, 30), 30, "Radiografia de cadera", EstadoCita.CONFIRMADA, bd("100.00"), bd("10.00"), bd("90.00"), true, now, now),
                new CitaData(d8, m17, v4, hoy.plusDays(10), LocalTime.of(14, 0), LocalTime.of(14, 30), 30, "Vacunacion felina completa", EstadoCita.PROGRAMADA, bd("165.00"), bd("0"), bd("165.00"), false, now, now),
                new CitaData(d3, m8, v3, hoy.plusDays(12), LocalTime.of(9, 0), LocalTime.of(9, 45), 45, "Revision de dermatitis - control", EstadoCita.PROGRAMADA, bd("90.00"), bd("0"), bd("90.00"), true, now, now),
                new CitaData(d10, m22, v6, hoy.plusDays(14), LocalTime.of(10, 0), LocalTime.of(10, 30), 30, "Electrocardiograma preventivo", EstadoCita.PROGRAMADA, bd("130.00"), bd("0"), bd("130.00"), false, now, now),
                new CitaData(d9, m19, v2, hoy.plusDays(14), LocalTime.of(11, 0), LocalTime.of(11, 30), 30, "Revision de conducta", EstadoCita.PROGRAMADA, bd("60.00"), bd("0"), bd("60.00"), false, now, now),
                new CitaData(d7, m15, v6, hoy.plusDays(15), LocalTime.of(9, 0), LocalTime.of(9, 30), 30, "Ecografia abdominal de control", EstadoCita.PROGRAMADA, bd("150.00"), bd("0"), bd("150.00"), true, now, now)
        );

        for (var cd : citaData) {
            var saved = citaRepository.save(Cita.builder()
                    .duenio(cd.duenio).mascota(cd.mascota).veterinario(cd.veterinario)
                    .fecha(cd.fecha).horaInicio(cd.horaInicio).horaFin(cd.horaFin)
                    .duracionMinutos(cd.duracionMinutos).motivo(cd.motivo).estado(cd.estado)
                    .subtotal(cd.subtotal).descuento(cd.descuento).total(cd.total)
                    .requiereConfirmacion(cd.requiereConfirmacion)
                    .createdAt(cd.createdAt).updatedAt(cd.updatedAt)
                    .build());
            citas.add(saved);
        }

        if (citas.size() < 36) return;

        // === DETALLES DE COSTO ===
        saveDetalle(citas.get(0), consulGral, 1, bd("60.00"));
        saveDetalle(citas.get(2), consulGral, 1, bd("60.00"));
        saveDetalle(citas.get(2), vacAntirrab, 1, bd("45.00"));
        saveDetalle(citas.get(7), consulGral, 1, bd("60.00"));
        saveDetalle(citas.get(7), vacCompleta, 1, bd("120.00"));
        saveDetalle(citas.get(10), consulGral, 1, bd("60.00"));
        saveDetalle(citas.get(10), vacAntirrab, 1, bd("45.00"));
        saveDetalle(citas.get(12), consulGral, 1, bd("60.00"));
        saveDetalle(citas.get(13), desparasit, 1, bd("40.00"));
        saveDetalle(citas.get(14), consulEsp, 1, bd("90.00"));
        saveDetalle(citas.get(15), consulGral, 1, bd("60.00"));
        saveDetalle(citas.get(16), consulGral, 1, bd("60.00"));
        saveDetalle(citas.get(17), consulGral, 1, bd("60.00"));
        saveDetalle(citas.get(18), electro, 1, bd("130.00"));
        saveDetalle(citas.get(19), consulGral, 1, bd("60.00"));
        saveDetalle(citas.get(27), consulGral, 1, bd("60.00"));
        saveDetalle(citas.get(27), vacCompleta, 1, bd("120.00"));
        saveDetalle(citas.get(28), limpDental, 1, bd("180.00"));
        saveDetalle(citas.get(29), consulGral, 1, bd("60.00"));
        saveDetalle(citas.get(30), radiografia, 1, bd("100.00"));
        saveDetalle(citas.get(31), consulGral, 1, bd("60.00"));
        saveDetalle(citas.get(31), vacCompleta, 1, bd("120.00"));
        saveDetalle(citas.get(32), consulEsp, 1, bd("90.00"));
        saveDetalle(citas.get(33), electro, 1, bd("130.00"));
        saveDetalle(citas.get(34), consulGral, 1, bd("60.00"));
        saveDetalle(citas.get(35), ecografia, 1, bd("150.00"));

        // === ATENCIONES CLINICAS (citas 12-19, indices 12-19) ===
        var atenciones = List.of(
                new AtencionData(citas.get(12), m1, v1, "Consulta general",
                        "Paciente en buen estado general. Peso adecuado. Vacunas al dia. Sin hallazgos anormales en exploracion fisica.",
                        "No requiere tratamiento. Continuar con dieta balanceada y ejercicio diario.",
                        "Regresar en 6 meses para control anual. Mantener pauta de desparasitacion trimestral.",
                        "Paciente tranquilo y cooperador durante la revision. Constantes vitales normales.",
                        "Dueno puntual. Mascota bien cuidada. Sin novedades.",
                        now.minusDays(7)),
                new AtencionData(citas.get(13), m5, v1, "Desparasitacion",
                        "Se aplico desparasitante oral Febendazol 100mg. Sin signos clinicos de parasitos visibles en heces ni examen fisico.",
                        "Aplicacion unica de Febendazol 100mg. Repetir en 15 dias con segunda dosis.",
                        "Mantener ambiente limpio. Recoger y revisar heces en 2 semanas. Traer muestra en proximo control.",
                        "Se administro el medicamento sin problemas. Paciente tolero bien la via oral.",
                        "Dueno siguio indicaciones previas de desparasitacion. Buena predisposicion.",
                        now.minusDays(6)),
                new AtencionData(citas.get(14), m8, v3, "Revision de alergias cutaneas",
                        "Dermatitis alergica por pulgas confirmada. Presencia de pulgas en zona lumbar y base de cola. Eritema y prurito moderado.",
                        "Aplicacion de pipeta antipulgas (Frontline). Prednisolona 5mg cada 12h por 5 dias. Champu medicado con avena coloidal.",
                        "Mantener ambiente libre de pulgas. Tratar a todas las mascotas del hogar. Repetir bano medicado cada 7 dias por 3 semanas. Control en 2 semanas.",
                        "Paciente con prurito moderado. Zonas afectadas: lomo, base de cola y abdomen. Sin sobreinfeccion bacteriana.",
                        "Se explico la importancia del control ambiental de pulgas. Dueno comprometido con el tratamiento.",
                        now.minusDays(5)),
                new AtencionData(citas.get(15), m12, v2, "Control de socializacion",
                        "Paciente Rottweiler de 4 anos con antecedentes de agresividad. Progreso favorable en terapia conductual. Responde mejor a comandos basicos.",
                        "Continuar con sesiones de refuerzo positivo. Uso de feromonas apaciguadoras (Adaptil). Paseos diarios de 30 min minimo.",
                        "Mantener rutina de ejercicios. Evitar situaciones de estres. Continuar con etologo cada 15 dias.",
                        "Paciente mas tolerante a la manipulacion. Permite revision de orejas y boca sin mostrar agresividad.",
                        "Dueno siguiendo recomendaciones al pie de la letra. Buen progreso general.",
                        now.minusDays(4)),
                new AtencionData(citas.get(16), m15, v4, "Revision de oidos",
                        "Otitis externa bilateral por Malassezia confirmada por citologia. Eritema y exudado ceruminoso marron oscuro. Dolor a la palpacion.",
                        "Limpieza otica con solucion limpiadora. Clotrimazol topico 2 gotas en cada oido cada 12h por 10 dias. AINE por 3 dias.",
                        "Mantener oidos secos. Evitar banos por 2 semanas. Control en 10 dias o antes si empeora.",
                        "Se tomo muestra para citologia. Paciente sensible a la manipulacion de orejas. Se trabajo con paciencia y refuerzo positivo.",
                        "Dueno preocupada pero colaboradora. Se explico tecnica de limpieza otica en casa.",
                        now.minusDays(3)),
                new AtencionData(citas.get(17), m18, v1, "Control de peso - obesidad",
                        "San Bernardo de 7 anos con obesidad (55kg, BCS 9/9). Dificultad para levantarse. Sin signos de displasia en radiografias.",
                        "Dieta hipocalorica recetada (Hill's Metabolic 4 tazas/dia dividido en 2 tomas). Ejercicio acuatico 2 veces/semana.",
                        "Control en 30 dias con pesaje. Meta: perder 3-4 kg por mes. Evaluar funcion tiroidea si no responde.",
                        "Paciente obeso pero alerta. Dificultad leve para caminar. Constantes vitales dentro de parametros.",
                        "Se explico la dieta a la dueno. Se programo control mensual. Compromiso del dueno.",
                        now.minusDays(2)),
                new AtencionData(citas.get(18), m19, v6, "Evaluacion cardiaca de rutina",
                        "Doberman de 5 anos. ECG: ritmo sinusal normal, sin arritmias. Ecocardiograma: fraccion de acortamiento 32% (normal). Sin cardiomegalia.",
                        "No requiere tratamiento cardiaco. Continuar con prevencion de cardiopatia dilatada con suplemento de taurina y L-carnitina.",
                        "Control cardiologico anual. Monitorear signos de alerta: tos, disnea, intolerancia al ejercicio. Mantener peso ideal.",
                        "Paciente tranquilo durante ECG y ecocardiograma. Buen temperamento.",
                        "Raza predispuesta a cardiopatia dilatada. Dueno informado sobre signos de alerta.",
                        now.minusDays(1)),
                new AtencionData(citas.get(19), m10, v4, "Control felino trimestral",
                        "Felino naranja de 5 anos en excelente condicion general. Peso 4.5kg estable. Vacunas al dia. Sin hallazgos patologicos.",
                        "Ninguno. Continuar con manejo actual. Mantener alimentacion con premium balanceado.",
                        "Proximo control en 3 meses. Vacunacion antirrabica pendiente para el proximo mes.",
                        "Paciente sociable y relajado. Se realizo cepillado dental con pasta enzimatica.",
                        "Dueno cumple con controles. Mascota bien cuidada.",
                        now.minusDays(1))
        );
        for (var a : atenciones) {
            atencionClinicaRepository.save(AtencionClinica.builder()
                    .cita(a.cita).mascota(a.mascota).veterinario(a.veterinario)
                    .motivo(a.motivo).diagnostico(a.diagnostico).tratamiento(a.tratamiento)
                    .recomendaciones(a.recomendaciones).observacionesClinicas(a.observacionesClinicas)
                    .notasInternas(a.notasInternas).fechaRegistro(a.fechaRegistro)
                    .build());
        }

        // === INASISTENCIAS (citas 23-26, indices 23-26) ===
        var inasistencias = List.of(
                new InasistenciaData(citas.get(23), d3, m7, "El dueno no asistio ni llamo para cancelar. Se intento contactar sin exito.", "Sistema", now.minusDays(10)),
                new InasistenciaData(citas.get(24), d7, m16, "La dueno llamo 2 horas despues de la cita. Se reprogramo para la proxima semana.", "Maria Garcia", now.minusDays(5)),
                new InasistenciaData(citas.get(25), d2, m6, "No asistio. Se dejo mensaje en contestador. Segunda inasistencia registrada.", "Sistema", now.minusDays(3)),
                new InasistenciaData(citas.get(26), d9, m20, "El dueno olvido la cita. Llamo al dia siguiente para disculparse y solicito nueva cita.", "Luis Torres", now.minusDays(1))
        );
        for (var i : inasistencias) {
            inasistenciaRepository.save(Inasistencia.builder()
                    .cita(i.cita).duenio(i.duenio).mascota(i.mascota)
                    .observacion(i.observacion).registradoPor(i.registradoPor)
                    .fechaRegistro(i.fechaRegistro)
                    .build());
        }

        // === VACUNAS MASCOTA ===
        var vacunasCatalogo = vacunaRepository.findAll();
        var vacMap = new HashMap<String, Vacuna>();
        for (var v : vacunasCatalogo) vacMap.put(v.getNombre(), v);
        var rCanina = vacMap.get("Rabia Canina");
        var mCanina = vacMap.get("Multiple Canina (Sextuple)");
        var moquillo = vacMap.get("Moquillo Canino");
        var tFelina = vacMap.get("Triple Felina");
        var lFelina = vacMap.get("Leucemia Felina");
        var pCanino = vacMap.get("Parvovirus Canino");
        var bordetella = vacMap.get("Bordetella (Tos de las perreras)");
        var leptospi = vacMap.get("Leptospirosis");
        var influenza = vacMap.get("Influenza Canina");
        var polivalF = vacMap.get("Polivalente Felina");

        var vacMascotaData = List.of(
                new VacMascotaData(m1, rCanina, v1, citas.get(12), hoy.minusDays(7), "LOTE-R01", hoy.plusDays(358), "Sin reacciones adversas"),
                new VacMascotaData(m1, mCanina, v1, null, hoy.minusDays(180), "LOTE-M02", hoy.plusDays(185), "Refuerzo anual aplicado"),
                new VacMascotaData(m5, rCanina, v1, citas.get(13), hoy.minusDays(6), "LOTE-R02", hoy.plusDays(359), "Aplicacion sin novedad"),
                new VacMascotaData(m5, mCanina, v1, null, hoy.minusDays(30), "LOTE-M03", hoy.plusDays(335), null),
                new VacMascotaData(m7, moquillo, v3, null, hoy.minusDays(90), "LOTE-M04", hoy.plusDays(275), null),
                new VacMascotaData(m2, tFelina, v2, null, hoy.minusDays(45), "LOTE-T01", hoy.plusDays(320), "Vacuna triple felina anual"),
                new VacMascotaData(m2, lFelina, v2, null, hoy.minusDays(45), "LOTE-L01", hoy.plusDays(320), "Leucemia felina sin reacciones"),
                new VacMascotaData(m3, bordetella, v2, null, hoy.minusDays(60), "LOTE-B01", hoy.plusDays(120), "Tos de las perreras - refuerzo anual"),
                new VacMascotaData(m3, pCanino, v2, null, hoy.minusDays(180), "LOTE-P01", hoy.plusDays(185), null),
                new VacMascotaData(m6, polivalF, v4, null, hoy.minusDays(20), "LOTE-PF01", hoy.plusDays(345), "Primera dosis. Aplicar refuerzo en 21 dias."),
                new VacMascotaData(m9, rCanina, v4, null, hoy.minusDays(15), "LOTE-R03", hoy.plusDays(350), null),
                new VacMascotaData(m9, leptospi, v4, null, hoy.minusDays(15), "LOTE-LP01", hoy.plusDays(350), "Leptospirosis - dosis unica anual"),
                new VacMascotaData(m11, influenza, v2, null, hoy.minusDays(10), "LOTE-IC01", hoy.plusDays(355), "Vacuna anual contra influenza canina"),
                new VacMascotaData(m13, tFelina, v4, null, hoy.minusDays(5), "LOTE-T02", hoy.plusDays(360), null)
        );
        for (var vm : vacMascotaData) {
            vacunaMascotaRepository.save(VacunaMascota.builder()
                    .mascota(vm.mascota).vacuna(vm.vacuna).veterinario(vm.veterinario).cita(vm.cita)
                    .fechaAplicacion(vm.fechaAplicacion).lote(vm.lote)
                    .fechaProximaDosis(vm.fechaProximaDosis).observaciones(vm.observaciones)
                    .createdAt(now)
                    .build());
        }

        // === CONTROLES MENSUALES ===
        var controlData = List.of(
                new ControlData(m1, v1, hoy.minusMonths(3), bd("29.50"), "Royal Canin 3 tazas/dia", "Peso ligeramente alto", "Reducir porcion en 1/4"),
                new ControlData(m1, v1, hoy.minusMonths(2), bd("29.00"), "Royal Canin 3 tazas/dia", "Peso estable", "Continuar igual"),
                new ControlData(m1, v1, hoy.minusMonths(1), bd("28.80"), "Royal Canin 3 tazas/dia", "Bajo 200g, buen progreso", "Reducir premios"),
                new ControlData(m1, v1, hoy, bd("28.50"), "Royal Canin 2.5 tazas/dia", "Peso ideal para su talla", "Mantener rutina"),
                new ControlData(m2, v2, hoy.minusMonths(3), bd("4.80"), "Hill's Metabolic 1/2 taza", "Sobrepeso significativo", "Iniciar dieta estricta"),
                new ControlData(m2, v2, hoy.minusMonths(2), bd("4.60"), "Hill's Metabolic 1/2 taza", "Sigue con sobrepeso", "Dieta estricta + mas ejercicio"),
                new ControlData(m2, v2, hoy.minusMonths(1), bd("4.40"), "Hill's Metabolic 1/2 taza", "Bajo 200g", "Seguir dieta, aumentar juego"),
                new ControlData(m2, v2, hoy, bd("4.20"), "Hill's Metabolic 1/3 taza", "Progreso constante", "Mantener, control en 1 mes"),
                new ControlData(m5, v1, hoy.minusMonths(2), bd("33.00"), "Eukanuba 3 tazas/dia", "Peso normal", "Continuar rutina"),
                new ControlData(m5, v1, hoy.minusMonths(1), bd("32.50"), "Eukanuba 3 tazas/dia", "Buena condicion", "Continuar rutina"),
                new ControlData(m5, v1, hoy, bd("32.00"), "Eukanuba 3 tazas/dia", "Peso ideal", "Vacunas al dia"),
                new ControlData(m9, v4, hoy.minusMonths(3), bd("38.80"), "Pro Plan 4 tazas/dia", "Displasia estable", "Continuar con condroprotectores"),
                new ControlData(m9, v4, hoy.minusMonths(2), bd("38.50"), "Pro Plan 4 tazas/dia", "Displasia estable", "Continuar con condroprotectores"),
                new ControlData(m9, v4, hoy.minusMonths(1), bd("38.20"), "Pro Plan 3.5 tazas/dia", "Bajo 300g", "Mantener tratamiento + ejercicios suaves"),
                new ControlData(m9, v4, hoy, bd("38.00"), "Pro Plan 3.5 tazas/dia", "Peso controlado", "Continuar igual. Proxima radiografia en 3 meses."),
                new ControlData(m11, v2, hoy.minusMonths(2), bd("25.50"), "Purina Pro Plan 2.5 tazas/dia", "Peso normal", "Mantener rutina"),
                new ControlData(m11, v2, hoy.minusMonths(1), bd("25.20"), "Purina Pro Plan 2.5 tazas/dia", "Estable", "Reforzar cercado del hogar"),
                new ControlData(m11, v2, hoy, bd("25.00"), "Purina Pro Plan 2.5 tazas/dia", "Peso estable", "Mantener manejo actual"),
                new ControlData(m15, v4, hoy.minusMonths(1), bd("15.50"), "Royal Canin Cocker 2 tazas/dia", "Oidos limpios. Sin signos de infeccion.", "Continuar limpieza otica semanal"),
                new ControlData(m15, v4, hoy, bd("15.00"), "Royal Canin Cocker 2 tazas/dia", "Peso ideal. Oidos sanos.", "Mantener cuidados. Control en 6 meses."),
                new ControlData(m19, v6, hoy, bd("35.00"), "Royal Canin Doberman 3 tazas/dia", "Cardiaco estable. Peso optimo.", "Continuar suplemento de taurina. ECG anual.")
        );
        for (var c : controlData) {
            controlMensualMascotaRepository.save(ControlMensualMascota.builder()
                    .mascota(c.mascota).veterinario(c.veterinario)
                    .fechaControl(c.fechaControl)
                    .anio(c.fechaControl.getYear()).mes(c.fechaControl.getMonthValue())
                    .pesoKg(c.pesoKg).alimentacion(c.alimentacion)
                    .observaciones(c.observaciones).recomendaciones(c.recomendaciones)
                    .createdAt(now).updatedAt(now)
                    .build());
        }
    }

    private Mascota findMascota(List<Mascota> mascotas, String nombre, Duenio duenio) {
        return mascotas.stream()
                .filter(m -> m.getNombre().equals(nombre) && m.getDuenio().getId().equals(duenio.getId()))
                .findFirst().orElse(null);
    }

    private void saveDetalle(Cita cita, Servicio servicio, int cantidad, BigDecimal total) {
        if (servicio == null || cita == null) return;
        detalleCostoCitaRepository.save(DetalleCostoCita.builder()
                .cita(cita).servicio(servicio).nombreServicio(servicio.getNombre())
                .costoUnitario(servicio.getCostoBase()).cantidad(cantidad)
                .subtotal(total).descuento(bd("0")).total(total)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private static BigDecimal bd(String val) { return new BigDecimal(val); }

    private record CitaData(Duenio duenio, Mascota mascota, Veterinario veterinario, LocalDate fecha,
                            LocalTime horaInicio, LocalTime horaFin, Integer duracionMinutos, String motivo,
                            EstadoCita estado, BigDecimal subtotal, BigDecimal descuento, BigDecimal total,
                            Boolean requiereConfirmacion, LocalDateTime createdAt, LocalDateTime updatedAt) {}
    private record AtencionData(Cita cita, Mascota mascota, Veterinario veterinario, String motivo,
                                String diagnostico, String tratamiento, String recomendaciones,
                                String observacionesClinicas, String notasInternas, LocalDateTime fechaRegistro) {}
    private record InasistenciaData(Cita cita, Duenio duenio, Mascota mascota, String observacion,
                                    String registradoPor, LocalDateTime fechaRegistro) {}
    private record VacMascotaData(Mascota mascota, Vacuna vacuna, Veterinario veterinario, Cita cita,
                                  LocalDate fechaAplicacion, String lote, LocalDate fechaProximaDosis,
                                  String observaciones) {}
    private record ControlData(Mascota mascota, Veterinario veterinario, LocalDate fechaControl,
                               BigDecimal pesoKg, String alimentacion, String observaciones,
                               String recomendaciones) {}
}
