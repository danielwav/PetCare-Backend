package com.petcare.backend.config;

import com.petcare.backend.domain.repository.RolRepository;
import com.petcare.backend.domain.repository.ServicioRepository;
import com.petcare.backend.domain.repository.UsuarioRepository;
import com.petcare.backend.domain.repository.VacunaRepository;
import com.petcare.backend.persistence.entity.Rol;
import com.petcare.backend.persistence.entity.Servicio;
import com.petcare.backend.persistence.entity.Usuario;
import com.petcare.backend.persistence.entity.Vacuna;
import com.petcare.backend.persistence.enums.RoleName;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final ServicioRepository servicioRepository;
    private final VacunaRepository vacunaRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        initRoles();
        initAdminUser();
        initServicios();
        initVacunas();
    }

    private void initRoles() {
        for (var roleName : RoleName.values()) {
            rolRepository.findByName(roleName).orElseGet(() -> {
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
            });
        }
    }

    private void initAdminUser() {
        var adminEmail = "admin@petcare.com";
        if (usuarioRepository.findByEmail(adminEmail).isEmpty()) {
            var adminRole = rolRepository.findByName(RoleName.ROLE_ADMIN)
                    .orElseThrow(() -> new IllegalStateException("Rol ROLE_ADMIN no encontrado"));
            usuarioRepository.save(Usuario.builder()
                    .fullName("Admin Sistema")
                    .email(adminEmail)
                    .password(passwordEncoder.encode("admin123"))
                    .active(true)
                    .createdAt(LocalDateTime.now())
                    .roles(Set.of(adminRole))
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
}
