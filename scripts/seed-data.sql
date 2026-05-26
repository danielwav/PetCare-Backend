-- ============================================================
-- PetCare - Seed Data Completo (v2)
-- ============================================================
-- USO: mysql -u root -p petcare < scripts/seed-data.sql
-- O pégalo en tu cliente SQL favorito (DBeaver, MySQL Workbench, etc.)
--
-- CREDENCIALES DE ACCESO (contraseñas BCrypt):
--   admin@petcare.com       / admin123        (ROLE_ADMIN + ROLE_DUENIO)
--   vet@petcare.com         / vet123          (ROLE_VETERINARIO)
--   asistente@petcare.com   / asistente123    (ROLE_ASISTENTE)
--   duenio@petcare.com      / duenio123       (ROLE_DUENIO)
--   laura.admin@petcare.com / admin123        (ROLE_ADMIN)
--   miguel.alvarez@petcare.com / 123456       (ROLE_VETERINARIO)
--   patricia.h@petcare.com  / 123456          (ROLE_VETERINARIO)
--   ricardo.g@petcare.com   / 123456          (ROLE_VETERINARIO)
--   sofia.reyes@petcare.com / 123456          (ROLE_ASISTENTE)
--   diego.c@petcare.com     / 123456          (ROLE_ASISTENTE)
--   ana.gomez@email.com     / 123456          (ROLE_DUENIO)
--   pedro.s@email.com       / 123456          (ROLE_DUENIO)
--   carmen.t@email.com      / 123456          (ROLE_DUENIO)
--   luis.f@email.com        / 123456          (ROLE_DUENIO)
--   supervisor@petcare.com  / 123456          (ROLE_ADMIN + ROLE_VETERINARIO)
-- ============================================================

-- 1. ROLES
INSERT INTO roles (name, description, active) VALUES
('ROLE_ADMIN', 'Administrador general del sistema.', 1),
('ROLE_VETERINARIO', 'Personal medico veterinario.', 1),
('ROLE_ASISTENTE', 'Personal operativo de recepcion y agenda.', 1),
('ROLE_DUENIO', 'Cliente o propietario de mascota.', 1)
ON DUPLICATE KEY UPDATE name = name;

-- ============================================================
-- 2. USUARIOS
-- Passwords en texto plano (BCrypt se genera al registrar desde la app).
-- Los registros con usuario_id NULL se crean sin login de sistema.
-- ============================================================
INSERT INTO usuarios (full_name, email, password, active, created_at) VALUES
('Admin Sistema',       'admin@petcare.com',    '$2b$10$nX7cyAaLKSGclyUrVhOyte1fGpvwRSlVnjyHkpaGP7HTErJPGUjvW', 1, NOW()),
('Dr. Carlos López',    'vet@petcare.com',      '$2b$10$G918SoeqyZIHSZUMNqo/M.t0U4vzDvYxywYl8eMPNtecUwBf1pNGi', 1, NOW()),
('María García',        'asistente@petcare.com','$2b$10$r2xrD0Hdg2zyQ43VDPcLVOqbXtyc1VGB3eV3V7dl2baNWFh47fjW2', 1, NOW()),
('Juan Pérez',          'duenio@petcare.com',   '$2b$10$OMeKSw6z9A6wibOXJNA18uT48WfztGJdNkwwjNKlTnXiXQATo/Tce', 1, NOW()),
('Dr. Miguel Álvarez',  'miguel.alvarez@petcare.com', '$2b$10$ZcLnJ8Ztg1zPrn3bQBuHze0pu7XI8XUhxCp1IwfwxZg/dArjARUte', 1, NOW()),
('Sofía Reyes',         'sofia.reyes@petcare.com',     '$2b$10$ZcLnJ8Ztg1zPrn3bQBuHze0pu7XI8XUhxCp1IwfwxZg/dArjARUte', 1, NOW()),
('Ana Gómez',           'ana.gomez@email.com',          '$2b$10$ZcLnJ8Ztg1zPrn3bQBuHze0pu7XI8XUhxCp1IwfwxZg/dArjARUte', 1, NOW()),
('Laura Mendoza',       'laura.admin@petcare.com',      '$2b$10$ZcLnJ8Ztg1zPrn3bQBuHze0pu7XI8XUhxCp1IwfwxZg/dArjARUte', 1, NOW()),
('Dra. Patricia Huamán','patricia.h@petcare.com',       '$2b$10$ZcLnJ8Ztg1zPrn3bQBuHze0pu7XI8XUhxCp1IwfwxZg/dArjARUte', 1, NOW()),
('Dr. Ricardo Gutiérrez','ricardo.g@petcare.com',       '$2b$10$ZcLnJ8Ztg1zPrn3bQBuHze0pu7XI8XUhxCp1IwfwxZg/dArjARUte', 1, NOW()),
('Diego Castillo',      'diego.c@petcare.com',          '$2b$10$ZcLnJ8Ztg1zPrn3bQBuHze0pu7XI8XUhxCp1IwfwxZg/dArjARUte', 1, NOW()),
('Pedro Sánchez',       'pedro.s@email.com',            '$2b$10$ZcLnJ8Ztg1zPrn3bQBuHze0pu7XI8XUhxCp1IwfwxZg/dArjARUte', 1, NOW()),
('Carmen Torres',       'carmen.t@email.com',           '$2b$10$ZcLnJ8Ztg1zPrn3bQBuHze0pu7XI8XUhxCp1IwfwxZg/dArjARUte', 1, NOW()),
('Luis Fernández',      'luis.f@email.com',             '$2b$10$ZcLnJ8Ztg1zPrn3bQBuHze0pu7XI8XUhxCp1IwfwxZg/dArjARUte', 1, NOW()),
('Dr. Supervisor',      'supervisor@petcare.com',       '$2b$10$ZcLnJ8Ztg1zPrn3bQBuHze0pu7XI8XUhxCp1IwfwxZg/dArjARUte', 1, NOW())
ON DUPLICATE KEY UPDATE email = email;

-- Asignar roles a usuarios
INSERT INTO usuarios_roles (usuario_id, rol_id) VALUES
(1, 1), (1, 4),   -- Admin -> ROLE_ADMIN + ROLE_DUENIO
(2, 2),            -- Vet   -> ROLE_VETERINARIO
(3, 3),            -- Asis  -> ROLE_ASISTENTE
(4, 4),            -- Dueño -> ROLE_DUENIO
(5, 2),            -- Miguel -> ROLE_VETERINARIO
(6, 3),            -- Sofía  -> ROLE_ASISTENTE
(7, 4),            -- Ana    -> ROLE_DUENIO
(8, 1),            -- Laura  -> ROLE_ADMIN
(9, 2),            -- Patricia -> ROLE_VETERINARIO
(10, 2),           -- Ricardo -> ROLE_VETERINARIO
(11, 3),           -- Diego  -> ROLE_ASISTENTE
(12, 4),           -- Pedro  -> ROLE_DUENIO
(13, 4),           -- Carmen -> ROLE_DUENIO
(14, 4),           -- Luis   -> ROLE_DUENIO
(15, 1), (15, 2)   -- Supervisor -> ROLE_ADMIN + ROLE_VETERINARIO
ON DUPLICATE KEY UPDATE usuario_id = usuario_id;

-- ============================================================
-- 3. DUENIOS (10 dueños)
-- ============================================================
INSERT INTO duenios (usuario_id, nombres, apellidos, tipo_documento, numero_documento, telefono, email, direccion, active, created_at, updated_at) VALUES
(4, 'Juan',      'Pérez',      'DNI', '12345678', '999888777',  'duenio@petcare.com',     'Av. Siempre Viva 123, Lima',                     1, NOW(), NOW()),
(7, 'Ana',       'Gómez',      'DNI', '23456789', '999111222',  'ana.gomez@email.com',   'Jr. Las Flores 456, Lima',                        1, NOW(), NOW()),
(NULL, 'Pedro',   'Martínez',   'CE',  'CE-001234', '999333444', 'pedro.m@email.com',     'Calle Los Olivos 789, Lima',                      1, NOW(), NOW()),
(NULL, 'Carmen',  'López',      'DNI', '34567890', '999555666',  'carmen.lopez@email.com','Av. Primavera 321, Lima',                         1, NOW(), NOW()),
(NULL, 'Roberto', 'Sánchez',    'DNI', '45678901', '999777888',  'roberto.s@email.com',   'Urb. El Sol 654, Lima',                           1, NOW(), NOW()),
(NULL, 'Laura',   'Díaz',       'CE',  'CE-005678', '999999000', 'laura.diaz@email.com',  'Pasaje La Paz 987, Lima',                         1, NOW(), NOW()),
(NULL, 'Diego',   'Herrera',    'DNI', '56789012', '998111333',  'diego.h@email.com',     'Av. Los Pinos 159, Lima',                         1, NOW(), NOW()),
(NULL, 'Valeria', 'Rojas',      'CE',  'CE-009876', '998444555', 'valeria.r@email.com',   'Jr. Las Dalias 753, Lima',                        1, NOW(), NOW()),
(NULL, 'Fernando','Mendoza',    'DNI', '67890123', '997666222',  'fernando.m@email.com',  'Calle Real 852, Lima',                            1, NOW(), NOW()),
(NULL, 'Gabriela','Torres',     'DNI', '78901234', '997888111',  'gabriela.t@email.com',  'Urb. Los Jazmines 456, Lima',                     1, NOW(), NOW())
ON DUPLICATE KEY UPDATE email = email;

-- ============================================================
-- 4. VETERINARIOS (8 veterinarios)
-- ============================================================
INSERT INTO veterinarios (usuario_id, nombres, apellidos, numero_colegiatura, especialidad, telefono, email, active, created_at, updated_at) VALUES
(2, 'Carlos',  'López',     'CMP-12345', 'Medicina General',      '999111222', 'vet@petcare.com',              1, NOW(), NOW()),
(NULL, 'María',   'Fernández', 'CMP-23456', 'Cirugía Veterinaria',   '999222333', 'maria.fernandez@petcare.com', 1, NOW(), NOW()),
(NULL, 'José',    'Ramírez',   'CMP-34567', 'Dermatología',          '999444555', 'jose.ramirez@petcare.com',    1, NOW(), NOW()),
(NULL, 'Diana',   'Torres',    'CMP-45678', 'Medicina Felina',       '999666777', 'diana.torres@petcare.com',    1, NOW(), NOW()),
(5, 'Miguel',  'Álvarez',   'CMP-56789', 'Medicina General',      '999888999', 'miguel.alvarez@petcare.com',  1, NOW(), NOW()),
(NULL, 'Andrea',  'Castillo',  'CMP-67890', 'Cardiología Veterinaria','998111222', 'andrea.castillo@petcare.com',1, NOW(), NOW()),
(NULL, 'Ricardo', 'Navarro',   'CMP-78901', 'Neurología Veterinaria', '998333444', 'ricardo.navarro@petcare.com',1, NOW(), NOW()),
(NULL, 'Patricia','Vega',      'CMP-89012', 'Medicina General',      '997555666', 'patricia.vega@petcare.com',   1, NOW(), NOW())
ON DUPLICATE KEY UPDATE email = email;

-- ============================================================
-- 5. HORARIOS VETERINARIOS
-- ============================================================
INSERT INTO horarios_veterinarios (veterinario_id, dia_semana, hora_inicio, hora_fin, duracion_bloque_minutos, active) VALUES
-- Carlos López (vet 1) - Lu-Vie 8-17
(1, 'MONDAY',    '08:00', '17:00', 30, 1), (1, 'TUESDAY',   '08:00', '17:00', 30, 1),
(1, 'WEDNESDAY', '08:00', '17:00', 30, 1), (1, 'THURSDAY',  '08:00', '17:00', 30, 1), (1, 'FRIDAY', '08:00', '17:00', 30, 1),
-- María Fernández (vet 2) - Lu-Ju 9-18
(2, 'MONDAY',    '09:00', '18:00', 30, 1), (2, 'TUESDAY',   '09:00', '18:00', 30, 1),
(2, 'WEDNESDAY', '09:00', '18:00', 30, 1), (2, 'THURSDAY',  '09:00', '18:00', 30, 1),
-- José Ramírez (vet 3) - Lu, Mie, Vie 10-16
(3, 'MONDAY',    '10:00', '16:00', 45, 1), (3, 'WEDNESDAY', '10:00', '16:00', 45, 1), (3, 'FRIDAY', '10:00', '16:00', 45, 1),
-- Diana Torres (vet 4) - Ma-Sab 8-13
(4, 'TUESDAY',   '08:00', '15:00', 30, 1), (4, 'WEDNESDAY', '08:00', '15:00', 30, 1),
(4, 'THURSDAY',  '08:00', '15:00', 30, 1), (4, 'FRIDAY',    '08:00', '15:00', 30, 1), (4, 'SATURDAY', '09:00', '13:00', 30, 1),
-- Miguel Álvarez (vet 5) - Lu-Vie 7-14
(5, 'MONDAY',    '07:00', '14:00', 30, 1), (5, 'TUESDAY',   '07:00', '14:00', 30, 1),
(5, 'WEDNESDAY', '07:00', '14:00', 30, 1), (5, 'THURSDAY',  '07:00', '14:00', 30, 1), (5, 'FRIDAY', '07:00', '14:00', 30, 1),
-- Andrea Castillo (vet 6) - Lu-Vie 9-16
(6, 'MONDAY',    '09:00', '16:00', 30, 1), (6, 'TUESDAY',   '09:00', '16:00', 30, 1),
(6, 'WEDNESDAY', '09:00', '16:00', 30, 1), (6, 'THURSDAY',  '09:00', '16:00', 30, 1), (6, 'FRIDAY', '09:00', '16:00', 30, 1),
-- Ricardo Navarro (vet 7) - Lu, Mie, Vie 10-17
(7, 'MONDAY',    '10:00', '17:00', 45, 1), (7, 'WEDNESDAY', '10:00', '17:00', 45, 1), (7, 'FRIDAY', '10:00', '17:00', 45, 1),
-- Patricia Vega (vet 8) - Ma-Ju 8-15, Sab 9-13
(8, 'TUESDAY',   '08:00', '15:00', 30, 1), (8, 'WEDNESDAY', '08:00', '15:00', 30, 1),
(8, 'THURSDAY',  '08:00', '15:00', 30, 1), (8, 'SATURDAY',  '09:00', '13:00', 30, 1);

-- ============================================================
-- 6. ASISTENTES (5 asistentes)
-- ============================================================
INSERT INTO asistentes (usuario_id, nombres, apellidos, tipo_documento, numero_documento, telefono, email, funciones, active, created_at, updated_at) VALUES
(3, 'María', 'García',  'DNI', '87654321', '999333444', 'asistente@petcare.com',      'Recepción, agenda, facturación',      1, NOW(), NOW()),
(NULL, 'Luis',  'Torres',  'DNI', '98765432', '999555111', 'luis.torres@petcare.com',   'Apoyo en consultas y farmacia',       1, NOW(), NOW()),
(NULL, 'Sofía', 'Reyes',   'DNI', '11223344', '999777333', 'sofia.reyes@petcare.com',   'Recepción y atención al cliente',     1, NOW(), NOW()),
(NULL, 'Carlos','Mora',    'DNI', '22334455', '998222444', 'carlos.mora@petcare.com',   'Farmacia e inventario',               1, NOW(), NOW()),
(NULL, 'Elena', 'Vargas',  'CE',  'CE-007654', '998666888','elena.vargas@petcare.com',  'Apoyo quirúrgico y hospitalización',  1, NOW(), NOW())
ON DUPLICATE KEY UPDATE email = email;

-- ============================================================
-- 7. MASCOTAS (22 mascotas)
-- ============================================================
INSERT INTO mascotas (duenio_id, nombre, especie, raza, sexo, fecha_nacimiento, color, peso_kg, observaciones, active, created_at, updated_at) VALUES
-- Dueño 1: Juan Pérez
(1, 'Max',   'Canino', 'Golden Retriever',  'MACHO', '2021-03-15', 'Dorado',  28.50, 'Alérgico a las pulgas',               1, NOW(), NOW()),
(1, 'Luna',  'Felino', 'Siamés',            'HEMBRA','2022-07-08', 'Crema',    4.20, 'Dieta especial por sobrepeso',        1, NOW(), NOW()),
(1, 'Rocky', 'Canino', 'Bulldog Francés',   'MACHO', '2023-01-20', 'Atigrado',12.00, NULL,                                   1, NOW(), NOW()),
(1, 'Coco',  'Canino', 'Chihuahua',         'MACHO', '2024-06-01', 'Café',     2.80, NULL,                                   1, NOW(), NOW()),
-- Dueño 2: Ana Gómez
(2, 'Bella', 'Canino', 'Labrador',          'HEMBRA','2020-05-10', 'Amarillo',32.00, 'Vacunas al día',                      1, NOW(), NOW()),
(2, 'Mimi',  'Felino', 'Persa',             'HEMBRA','2021-11-22', 'Blanco',   3.80, NULL,                                   1, NOW(), NOW()),
-- Dueño 3: Pedro Martínez
(3, 'Toby',  'Canino', 'Poodle',            'MACHO', '2022-09-05', 'Blanco',   6.50, 'Tiene displasia de cadera',           1, NOW(), NOW()),
(3, 'Pelusa','Felino', 'Angora',            'HEMBRA','2023-04-18', 'Gris',     3.20, NULL,                                   1, NOW(), NOW()),
-- Dueño 4: Carmen López
(4, 'Thor',  'Canino', 'Pastor Alemán',     'MACHO', '2019-08-12', 'Negro',   38.00, 'Requiere paseos diarios',             1, NOW(), NOW()),
(4, 'Canela','Felino', 'Naranja',           'HEMBRA','2020-12-30', 'Naranja',  4.50, NULL,                                   1, NOW(), NOW()),
-- Dueño 5: Roberto Sánchez
(5, 'Zeus',  'Canino', 'Husky',             'MACHO', '2021-06-01', 'Gris/Blanco', 25.00, 'Se escapa seguido',                1, NOW(), NOW()),
(5, 'Thor 2','Canino', 'Rottweiler',        'MACHO', '2022-03-10', 'Negro/Café', 42.00, 'Necesita socialización',            1, NOW(), NOW()),
-- Dueño 6: Laura Díaz
(6, 'Nala',  'Felino', 'Bengalí',           'HEMBRA','2022-02-14', 'Moteado',  3.90, 'Muy activa',                           1, NOW(), NOW()),
(6, 'Simba', 'Canino', 'Beagle',            'MACHO', '2023-10-10', 'Marrón/Blanco', 14.00, 'Olfato muy desarrollado',          1, NOW(), NOW()),
-- Dueño 7: Diego Herrera
(7, 'Princesa','Canino','Cocker Spaniel',   'HEMBRA','2020-08-20', 'Dorado',  15.00, 'Propensa a infecciones de oído',       1, NOW(), NOW()),
(7, 'Garfiel','Felino', 'Naranja',          'MACHO', '2021-12-05', 'Anaranjado', 5.20, 'Ronca al dormir',                    1, NOW(), NOW()),
-- Dueño 8: Valeria Rojas
(8, 'Kiara', 'Felino', 'Sagrado de Birmania','HEMBRA','2023-05-15','Crema',    3.50, NULL,                                   1, NOW(), NOW()),
(8, 'Bruno', 'Canino', 'San Bernardo',      'MACHO', '2018-11-30', 'Blanco/Rojo', 55.00, 'Excelente con niños',               1, NOW(), NOW()),
-- Dueño 9: Fernando Mendoza
(9, 'Duke',  'Canino', 'Dóberman',          'MACHO', '2021-09-15', 'Negro/Café', 35.00, 'Usar bozal en consulta',             1, NOW(), NOW()),
(9, 'Lola',  'Felino', 'Esfinge',           'HEMBRA','2023-03-22', 'Gris',    2.90, 'Piel sensible al sol',                 1, NOW(), NOW()),
-- Dueño 10: Gabriela Torres
(10, 'Paco', 'Canino', 'Fox Terrier',       'MACHO', '2022-07-01', 'Blanco/Negro', 8.00, NULL,                                1, NOW(), NOW()),
(10, 'Mía',  'Felino', 'Azul Ruso',         'HEMBRA','2023-11-10', 'Gris',    3.10, 'Tímida con extraños',                  1, NOW(), NOW())
ON DUPLICATE KEY UPDATE nombre = nombre;

-- ============================================================
-- 8. SERVICIOS (20 servicios)
-- ============================================================
INSERT INTO servicios (nombre, descripcion, costo_base, active, created_at, updated_at) VALUES
('Consulta General',            'Atención médica general para mascotas',                       60.00,  1, NOW(), NOW()),
('Consulta Especializada',      'Atención con especialista en áreas específicas',              90.00,  1, NOW(), NOW()),
('Vacunación Completa',         'Aplicación de vacunas según calendario',                     120.00,  1, NOW(), NOW()),
('Vacunación Antirrábica',      'Vacuna contra la rabia',                                      45.00,  1, NOW(), NOW()),
('Cirugía Menor',               'Procedimientos quirúrgicos de baja complejidad',             250.00,  1, NOW(), NOW()),
('Esterilización',              'Cirugía de esterilización para perros y gatos',              350.00,  1, NOW(), NOW()),
('Análisis de Sangre',          'Perfil bioquímico completo',                                  80.00,  1, NOW(), NOW()),
('Ecografía',                   'Diagnóstico por imágenes ecográficas',                       150.00,  1, NOW(), NOW()),
('Radiografía',                 'Estudio radiológico digital',                                100.00,  1, NOW(), NOW()),
('Peluquería Canina',           'Baño, corte y cepillado',                                     55.00,  1, NOW(), NOW()),
('Desparasitación',             'Desparasitación interna y externa',                           40.00,  1, NOW(), NOW()),
('Limpieza Dental',             'Profilaxis dental con sedación',                             180.00,  1, NOW(), NOW()),
('Hospitalización',             'Hospitalización con cuidados intensivos',                    200.00,  1, NOW(), NOW()),
('Terapia Física',              'Rehabilitación y fisioterapia',                               70.00,  1, NOW(), NOW()),
('Examen de Heces',             'Análisis coproparasitológico',                                35.00,  1, NOW(), NOW()),
('Prueba de Alergias',          'Test de alergias intraepidérmico',                           120.00,  1, NOW(), NOW()),
('Electrocardiograma',          'Estudio eléctrico del corazón',                              130.00,  1, NOW(), NOW()),
('Transfusión Sanguínea',       'Transfusión de sangre completa o plasma',                    400.00,  1, NOW(), NOW()),
('Endoscopía',                  'Examen endoscópico digestivo',                               280.00,  1, NOW(), NOW()),
('Ultrasonido Abdominal',       'Ultrasonido de abdomen completo',                            170.00,  1, NOW(), NOW())
ON DUPLICATE KEY UPDATE nombre = nombre;

-- ============================================================
-- 9. VACUNAS (catálogo - 12 vacunas)
-- ============================================================
INSERT INTO vacunas (nombre, descripcion, intervalo_proxima_dosis_dias, active, created_at, updated_at) VALUES
('Rabia Canina',                    'Vacuna antirrábica para caninos, dosis única anual',                          365, 1, NOW(), NOW()),
('Múltiple Canina (Séxtuple)',      'Protege contra moquillo, hepatitis, parvovirus, parainfluenza, leptospira y coronavirus', 365, 1, NOW(), NOW()),
('Moquillo Canino',                 'Vacuna contra el moquillo en perros',                                        365, 1, NOW(), NOW()),
('Triple Felina',                   'Protege contra panleucopenia, calicivirus y rinotraqueitis',                 365, 1, NOW(), NOW()),
('Leucemia Felina',                 'Vacuna contra el virus de la leucemia felina',                               365, 1, NOW(), NOW()),
('Parvovirus Canino',               'Vacuna específica contra parvovirus',                                        365, 1, NOW(), NOW()),
('Bordetella (Tos de las perreras)','Vacuna contra la bordetella bronchiseptica',                                180, 1, NOW(), NOW()),
('Leptospirosis',                   'Vacuna contra la leptospirosis canina',                                      365, 1, NOW(), NOW()),
('Coronavirus Canino',              'Vacuna contra el coronavirus entérico canino',                               365, 1, NOW(), NOW()),
('Influenza Canina',                'Vacuna contra la gripe canina (H3N8 y H3N2)',                               365, 1, NOW(), NOW()),
('Polivalente Felina',              'Vacuna tetravalente para felinos',                                           365, 1, NOW(), NOW()),
('Giardiasis Canina',               'Vacuna contra la giardia en caninos',                                        180, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE nombre = nombre;

-- ============================================================
-- 10. CITAS (36 citas distribuidas en el tiempo)
-- NOTA: Fechas dinámicas con DATE_ADD/DATE_SUB
-- ============================================================
INSERT INTO citas (duenio_id, mascota_id, veterinario_id, fecha, hora_inicio, hora_fin, duracion_minutos, motivo, estado, subtotal, descuento, total, requiere_confirmacion, created_at, updated_at) VALUES
-- ============================
-- CITAS DE HOY (7 citas)
-- ============================
(1, 1, 1, CURDATE(), '08:00', '08:30', 30, 'Control anual de rutina',                        'CONFIRMADA', 60.00,  0.00,  60.00,  1, NOW(), NOW()),
(1, 2, 2, CURDATE(), '09:00', '09:30', 30, 'Revisión por pérdida de peso',                   'CONFIRMADA', 60.00,  0.00,  60.00,  1, NOW(), NOW()),
(2, 5, 1, CURDATE(), '09:30', '10:00', 30, 'Vacunación antirrábica anual',                    'CONFIRMADA',105.00,  0.00, 105.00,  1, NOW(), NOW()),
(3, 7, 3, CURDATE(), '10:00', '10:45', 45, 'Consulta por dermatitis',                         'PROGRAMADA', 90.00,  0.00,  90.00,  0, NOW(), NOW()),
(4, 9, 4, CURDATE(), '11:00', '11:30', 30, 'Revisión de cadera',                              'PROGRAMADA', 60.00,  0.00,  60.00,  0, NOW(), NOW()),
(5, 11, 5, CURDATE(), '11:30', '12:00', 30, 'Control de peso',                                'PROGRAMADA', 60.00,  0.00,  60.00,  0, NOW(), NOW()),
(6, 13, 4, CURDATE(), '14:00', '14:30', 30, 'Revisión general por letargo',                   'PROGRAMADA', 60.00,  0.00,  60.00,  0, NOW(), NOW()),

-- ============================
-- CITAS DE MAÑANA (5 citas)
-- ============================
(5, 11, 5, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '08:00', '08:30', 30, 'Vacunación múltiple',                    'PROGRAMADA',165.00,  0.00, 165.00,  1, NOW(), NOW()),
(7, 15, 4, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '09:00', '09:30', 30, 'Control mensual',                        'PROGRAMADA', 60.00,  0.00,  60.00,  0, NOW(), NOW()),
(1, 3, 2, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '10:00', '10:30', 30, 'Revisión de alergias',                    'CONFIRMADA', 60.00,  0.00,  60.00,  1, NOW(), NOW()),
(2, 6, 1, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '11:30', '12:00', 30, 'Vacunación triple felina',                'PROGRAMADA',165.00,  0.00, 165.00,  0, NOW(), NOW()),
(10, 21, 2, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '14:00', '14:30', 30, 'Consulta por vómitos',                   'PROGRAMADA', 60.00,  0.00,  60.00,  0, NOW(), NOW()),

-- ============================
-- CITAS PASADAS - ATENDIDAS (8 citas)
-- ============================
(1, 1, 1, DATE_SUB(CURDATE(), INTERVAL 7 DAY), '09:00', '09:30', 30, 'Consulta general',                        'ATENDIDA',  60.00,  0.00,  60.00,  1, DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY)),
(2, 5, 1, DATE_SUB(CURDATE(), INTERVAL 6 DAY), '10:00', '10:30', 30, 'Desparasitación',                         'ATENDIDA',  40.00,  0.00,  40.00,  1, DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY)),
(3, 8, 3, DATE_SUB(CURDATE(), INTERVAL 5 DAY), '11:00', '11:45', 45, 'Revisión de alergias cutáneas',            'ATENDIDA',  90.00,  0.00,  90.00,  1, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),
(5, 12, 5, DATE_SUB(CURDATE(), INTERVAL 4 DAY), '14:00', '14:30', 30, 'Control de socialización',                'ATENDIDA',  60.00,  0.00,  60.00,  0, DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY)),
(7, 15, 4, DATE_SUB(CURDATE(), INTERVAL 3 DAY), '15:00', '15:30', 30, 'Revisión de oídos',                       'ATENDIDA',  60.00,  0.00,  60.00,  0, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
(8, 18, 1, DATE_SUB(CURDATE(), INTERVAL 2 DAY), '08:00', '08:30', 30, 'Control de peso - obesidad',              'ATENDIDA',  60.00,  0.00,  60.00,  1, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(9, 19, 6, DATE_SUB(CURDATE(), INTERVAL 1 DAY), '09:00', '09:30', 30, 'Evaluación cardíaca de rutina',            'ATENDIDA', 130.00,  0.00, 130.00,  0, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
(4, 10, 4, DATE_SUB(CURDATE(), INTERVAL 1 DAY), '10:00', '10:30', 30, 'Control felino trimestral',               'ATENDIDA',  60.00,  0.00,  60.00,  0, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),

-- ============================
-- CITAS CANCELADAS (3 citas)
-- ============================
(4, 9, 4, DATE_SUB(CURDATE(), INTERVAL 8 DAY), '14:00', '14:30', 30, 'Revisión general',                         'CANCELADA', 60.00,  0.00,  60.00,  0, DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY)),
(6, 14, 5, DATE_SUB(CURDATE(), INTERVAL 6 DAY), '15:00', '15:30', 30, 'Vacunación de refuerzo',                   'CANCELADA', 45.00,  0.00,  45.00,  0, DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY)),
(10, 22, 4, DATE_SUB(CURDATE(), INTERVAL 3 DAY), '16:00', '16:30', 30, 'Primera consulta gatita nueva',            'CANCELADA', 60.00,  0.00,  60.00,  0, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),

-- ============================
-- CITAS NO ASISTIDAS (4 citas)
-- ============================
(3, 7, 3, DATE_SUB(CURDATE(), INTERVAL 10 DAY), '15:00', '15:45', 45, 'Consulta por vómitos',                    'NO_ASISTIO', 90.00,  0.00,  90.00,  0, DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY)),
(7, 16, 1, DATE_SUB(CURDATE(), INTERVAL 5 DAY), '10:00', '10:30', 30, 'Revisión de oídos - recurrencia',        'NO_ASISTIO', 60.00,  0.00,  60.00,  0, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),
(2, 6, 2, DATE_SUB(CURDATE(), INTERVAL 3 DAY), '11:00', '11:30', 30, 'Vacunación anual',                        'NO_ASISTIO', 45.00,  0.00,  45.00,  0, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
(9, 20, 3, DATE_SUB(CURDATE(), INTERVAL 1 DAY), '14:00', '14:45', 45, 'Consulta dermatológica',                  'NO_ASISTIO', 90.00,  0.00,  90.00,  0, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),

-- ============================
-- CITAS FUTURAS (semana que viene - 9 citas)
-- ============================
(1, 1, 1, DATE_ADD(CURDATE(), INTERVAL 7 DAY),  '09:00', '09:30', 30, 'Vacunación múltiple',                     'PROGRAMADA',120.00,  0.00, 120.00,  1, NOW(), NOW()),
(1, 2, 2, DATE_ADD(CURDATE(), INTERVAL 7 DAY),  '10:00', '10:30', 30, 'Limpieza dental',                         'PROGRAMADA',180.00,  0.00, 180.00,  0, NOW(), NOW()),
(6, 14, 5, DATE_ADD(CURDATE(), INTERVAL 8 DAY), '08:00', '08:30', 30, 'Primera consulta',                        'PROGRAMADA', 60.00,  0.00,  60.00,  1, NOW(), NOW()),
(5, 11, 5, DATE_ADD(CURDATE(), INTERVAL 10 DAY),'11:00', '11:30', 30, 'Radiografía de cadera',                    'CONFIRMADA',100.00, 10.00,  90.00,  1, NOW(), NOW()),
(8, 17, 4, DATE_ADD(CURDATE(), INTERVAL 10 DAY),'14:00', '14:30', 30, 'Vacunación felina completa',               'PROGRAMADA',165.00,  0.00, 165.00,  0, NOW(), NOW()),
(3, 8, 3, DATE_ADD(CURDATE(), INTERVAL 12 DAY), '09:00', '09:45', 45, 'Revisión de dermatitis - control',         'PROGRAMADA', 90.00,  0.00,  90.00,  1, NOW(), NOW()),
(10, 22, 6, DATE_ADD(CURDATE(), INTERVAL 14 DAY),'10:00','10:30', 30, 'Electrocardiograma preventivo',            'PROGRAMADA',130.00,  0.00, 130.00,  0, NOW(), NOW()),
(9, 19, 5, DATE_ADD(CURDATE(), INTERVAL 14 DAY), '11:00', '11:30', 30, 'Revisión de conducta',                    'PROGRAMADA', 60.00,  0.00,  60.00,  0, NOW(), NOW()),
(7, 15, 6, DATE_ADD(CURDATE(), INTERVAL 15 DAY), '09:00', '09:30', 30, 'Ecografía abdominal de control',           'PROGRAMADA',150.00,  0.00, 150.00,  1, NOW(), NOW());

-- ============================================================
-- 11. ATENCIONES CLÍNICAS (para las 8 citas ATENDIDA)
-- ============================================================
INSERT INTO atenciones_clinicas (cita_id, mascota_id, veterinario_id, motivo, diagnostico, tratamiento, recomendaciones, observaciones_clinicas, notas_internas, fecha_registro) VALUES
(13, 1, 1, 'Consulta general',
    'Paciente en buen estado general. Peso adecuado. Vacunas al día. Sin hallazgos anormales en exploración física.',
    'No requiere tratamiento. Continuar con dieta balanceada y ejercicio diario.',
    'Regresar en 6 meses para control anual. Mantener pauta de desparasitación trimestral.',
    'Paciente tranquilo y cooperador durante la revisión. Constantes vitales normales.',
    'Dueño puntual. Mascota bien cuidada. Sin novedades.',
    DATE_SUB(NOW(), INTERVAL 7 DAY)),

(14, 5, 1, 'Desparasitación',
    'Se aplicó desparasitante oral Febendazol 100mg. Sin signos clínicos de parásitos visibles en heces ni examen físico.',
    'Aplicación única de Febendazol 100mg. Repetir en 15 días con segunda dosis.',
    'Mantener ambiente limpio. Recoger y revisar heces en 2 semanas. Traer muestra en próximo control.',
    'Se administró el medicamento sin problemas. Paciente toleró bien la vía oral.',
    'Dueña siguió indicaciones previas de desparasitación. Buena predisposición.',
    DATE_SUB(NOW(), INTERVAL 6 DAY)),

(15, 8, 3, 'Revisión de alergias cutáneas',
    'Dermatitis alérgica por pulgas confirmada. Presencia de pulgas en zona lumbar y base de cola. Eritema y prurito moderado.',
    'Aplicación de pipeta antipulgas (Frontline). Prednisolona 5mg cada 12h por 5 días. Champú medicado con avena coloidal.',
    'Mantener ambiente libre de pulgas. Tratar a todas las mascotas del hogar. Repetir baño medicado cada 7 días por 3 semanas. Control en 2 semanas.',
    'Paciente con prurito moderado. Zonas afectadas: lomo, base de cola y abdomen. Sin sobreinfección bacteriana.',
    'Se explicó la importancia del control ambiental de pulgas. Dueño comprometido con el tratamiento.',
    DATE_SUB(NOW(), INTERVAL 5 DAY)),

(16, 12, 5, 'Control de socialización',
    'Paciente Rottweiler de 4 años con antecedentes de agresividad. Progreso favorable en terapia conductual. Responde mejor a comandos básicos.',
    'Continuar con sesiones de refuerzo positivo. Uso de feromonas apaciguadoras (Adaptil). Paseos diarios de 30 min mínimo.',
    'Mantener rutina de ejercicios. Evitar situaciones de estrés. Continuar con etólogo cada 15 días.',
    'Paciente más tolerante a la manipulación. Permite revisión de orejas y boca sin mostrar agresividad.',
    'Dueño siguiendo recomendaciones al pie de la letra. Buen progreso general.',
    DATE_SUB(NOW(), INTERVAL 4 DAY)),

(17, 15, 4, 'Revisión de oídos',
    'Otitis externa bilateral por Malassezia confirmada por citología. Eritema y exudado ceruminoso marrón oscuro. Dolor a la palpación.',
    'Limpieza ótica con solución limpiadora. Clotrimazol tópico 2 gotas en cada oído cada 12h por 10 días. AINE por 3 días.',
    'Mantener oídos secos. Evitar baños por 2 semanas. Control en 10 días o antes si empeora.',
    'Se tomó muestra para citología. Paciente sensible a la manipulación de orejas. Se trabajó con paciencia y refuerzo positivo.',
    'Dueña preocupada pero colaboradora. Se explicó técnica de limpieza ótica en casa.',
    DATE_SUB(NOW(), INTERVAL 3 DAY)),

(18, 18, 1, 'Control de peso - obesidad',
    'San Bernardo de 7 años con obesidad (55kg, BCS 9/9). Dificultad para levantarse. Sin signos de displasia en radiografías.',
    'Dieta hipocalórica recetada (Hill\'s Metabolic 4 tazas/día dividido en 2 tomas). Ejercicio acuático 2 veces/semana.',
    'Control en 30 días con pesaje. Meta: perder 3-4 kg por mes. Evaluar función tiroidea si no responde.',
    'Paciente obeso pero alerta. Dificultad leve para caminar. Constantes vitales dentro de parámetros.',
    'Se explicó la dieta a la dueña. Se programó control mensual. Compromiso del dueño.',
    DATE_SUB(NOW(), INTERVAL 2 DAY)),

(19, 19, 6, 'Evaluación cardíaca de rutina',
    'Dóberman de 5 años. ECG: ritmo sinusal normal, sin arritmias. Ecocardiograma: fracción de acortamiento 32% (normal). Sin cardiomegalia.',
    'No requiere tratamiento cardíaco. Continuar con prevención de cardiopatía dilatada con suplemento de taurina y L-carnitina.',
    'Control cardiológico anual. Monitorear signos de alerta: tos, disnea, intolerancia al ejercicio. Mantener peso ideal.',
    'Paciente tranquilo durante ECG y ecocardiograma. Buen temperamento.',
    'Raza predispuesta a cardiopatía dilatada. Dueño informado sobre signos de alerta.',
    DATE_SUB(NOW(), INTERVAL 1 DAY)),

(20, 10, 4, 'Control felino trimestral',
    'Felino naranja de 5 años en excelente condición general. Peso 4.5kg estable. Vacunas al día. Sin hallazgos patológicos.',
    'Ninguno. Continuar con manejo actual. Mantener alimentación con premium balanceado.',
    'Próximo control en 3 meses. Vacunación antirrábica pendiente para el próximo mes.',
    'Paciente sociable y relajado. Se realizó cepillado dental con pasta enzimática.',
    'Dueña cumple con controles. Mascota bien cuidada.',
    DATE_SUB(NOW(), INTERVAL 1 DAY));

-- ============================================================
-- 12. INASISTENCIAS (para las 4 citas NO_ASISTIO + extras)
-- ============================================================
INSERT INTO inasistencias (cita_id, duenio_id, mascota_id, observacion, registrado_por, fecha_registro) VALUES
(24, 3, 7,  'El dueño no asistió ni llamó para cancelar. Se intentó contactar sin éxito.',                    'Sistema', DATE_SUB(NOW(), INTERVAL 10 DAY)),
(25, 7, 16, 'La dueña llamó 2 horas después de la cita. Se reprogramó para la próxima semana.',               'María García', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(26, 2, 6,  'No asistió. Se dejó mensaje en contestador. Segunda inasistencia registrada.',                    'Sistema', DATE_SUB(NOW(), INTERVAL 3 DAY)),
(27, 9, 20, 'El dueño olvidó la cita. Llamó al día siguiente para disculparse y solicitó nueva cita.',          'Luis Torres', DATE_SUB(NOW(), INTERVAL 1 DAY));

-- ============================================================
-- 13. DETALLES DE COSTO DE CITAS
-- ============================================================
INSERT INTO detalles_costo_cita (cita_id, servicio_id, nombre_servicio, costo_unitario, cantidad, subtotal, descuento, total, created_at) VALUES
-- Cita 1: Control anual (1 servicio)
(1, 1,  'Consulta General',       60.00,  1,  60.00,  0.00,  60.00,  NOW()),
-- Cita 3: Vacunación antirrábica (2 servicios)
(3,  1,  'Consulta General',       60.00,  1,  60.00,  0.00,  60.00,  NOW()),
(3,  4,  'Vacunación Antirrábica', 45.00,  1,  45.00,  0.00,  45.00,  NOW()),
-- Cita 8: Vacunación múltiple (2 servicios)
(8,  1,  'Consulta General',       60.00,  1,  60.00,  0.00,  60.00,  NOW()),
(8,  3,  'Vacunación Completa',    120.00, 1, 120.00,  0.00, 120.00,  NOW()),
-- Cita 10: Vacunación triple felina (1 + 1)
(10, 1,  'Consulta General',       60.00,  1,  60.00,  0.00,  60.00,  NOW()),
(10, 4,  'Vacunación Antirrábica', 45.00,  1,  45.00,  0.00,  45.00,  NOW()),
-- Cita 13: Consulta general (ya atendida)
(13, 1,  'Consulta General',       60.00,  1,  60.00,  0.00,  60.00,  DATE_SUB(NOW(), INTERVAL 7 DAY)),
-- Cita 14: Desparasitación
(14, 11, 'Desparasitación',        40.00,  1,  40.00,  0.00,  40.00,  DATE_SUB(NOW(), INTERVAL 6 DAY)),
-- Cita 15: Consulta especializada
(15, 2,  'Consulta Especializada', 90.00,  1,  90.00,  0.00,  90.00,  DATE_SUB(NOW(), INTERVAL 5 DAY)),
-- Cita 16: Control socialización
(16, 1,  'Consulta General',       60.00,  1,  60.00,  0.00,  60.00,  DATE_SUB(NOW(), INTERVAL 4 DAY)),
-- Cita 17: Revisión oídos
(17, 1,  'Consulta General',       60.00,  1,  60.00,  0.00,  60.00,  DATE_SUB(NOW(), INTERVAL 3 DAY)),
-- Cita 18: Control peso
(18, 1,  'Consulta General',       60.00,  1,  60.00,  0.00,  60.00,  DATE_SUB(NOW(), INTERVAL 2 DAY)),
-- Cita 19: Electrocardiograma
(19, 17, 'Electrocardiograma',     130.00, 1, 130.00,  0.00, 130.00,  DATE_SUB(NOW(), INTERVAL 1 DAY)),
-- Cita 20: Control felino
(20, 1,  'Consulta General',       60.00,  1,  60.00,  0.00,  60.00,  DATE_SUB(NOW(), INTERVAL 1 DAY)),
-- Cita 28: Vacunación múltiple futura
(28, 1,  'Consulta General',       60.00,  1,  60.00,  0.00,  60.00,  NOW()),
(28, 3,  'Vacunación Completa',    120.00, 1, 120.00,  0.00, 120.00,  NOW()),
-- Cita 29: Limpieza dental
(29, 12, 'Limpieza Dental',        180.00, 1, 180.00,  0.00, 180.00,  NOW()),
-- Cita 30: Primera consulta
(30, 1,  'Consulta General',       60.00,  1,  60.00,  0.00,  60.00,  NOW()),
-- Cita 31: Radiografía de cadera (con descuento)
(31, 9,  'Radiografía',            100.00, 1, 100.00,  10.00,  90.00,  NOW()),
-- Cita 32: Vacunación felina completa
(32, 1,  'Consulta General',       60.00,  1,  60.00,  0.00,  60.00,  NOW()),
(32, 3,  'Vacunación Completa',    120.00, 1, 120.00,  0.00, 120.00,  NOW()),
-- Cita 33: Dermatitis control
(33, 2,  'Consulta Especializada', 90.00,  1,  90.00,  0.00,  90.00,  NOW()),
-- Cita 34: ECG preventivo
(34, 17, 'Electrocardiograma',     130.00, 1, 130.00,  0.00, 130.00,  NOW()),
-- Cita 35: Revisión conducta
(35, 1,  'Consulta General',       60.00,  1,  60.00,  0.00,  60.00,  NOW()),
-- Cita 36: Ecografía abdominal
(36, 8,  'Ecografía',              150.00, 1, 150.00,  0.00, 150.00,  NOW());

-- ============================================================
-- 14. VACUNAS APLICADAS (historial de 14 aplicaciones)
-- ============================================================
INSERT INTO vacunas_mascota (mascota_id, vacuna_id, veterinario_id, cita_id, fecha_aplicacion, lote, fecha_proxima_dosis, observaciones, created_at) VALUES
-- Max (mascota 1) - Rabia + Múltiple
(1, 1, 1, 13, DATE_SUB(CURDATE(), INTERVAL 7 DAY),  'LOTE-R01', DATE_ADD(CURDATE(), INTERVAL 358 DAY), 'Sin reacciones adversas', NOW()),
(1, 2, 1, NULL, DATE_SUB(CURDATE(), INTERVAL 180 DAY), 'LOTE-M02', DATE_ADD(CURDATE(), INTERVAL 185 DAY), 'Refuerzo anual aplicado', NOW()),
-- Bella (mascota 5) - Rabia + Múltiple
(5, 1, 1, 14, DATE_SUB(CURDATE(), INTERVAL 6 DAY),  'LOTE-R02', DATE_ADD(CURDATE(), INTERVAL 359 DAY), 'Aplicación sin novedad', NOW()),
(5, 2, 1, NULL, DATE_SUB(CURDATE(), INTERVAL 30 DAY), 'LOTE-M03', DATE_ADD(CURDATE(), INTERVAL 335 DAY), NULL, NOW()),
-- Toby (mascota 7) - Moquillo
(7, 3, 3, NULL, DATE_SUB(CURDATE(), INTERVAL 90 DAY), 'LOTE-M04', DATE_ADD(CURDATE(), INTERVAL 275 DAY), NULL, NOW()),
-- Luna (mascota 2) - Triple Felina + Leucemia
(2, 4, 2, NULL, DATE_SUB(CURDATE(), INTERVAL 45 DAY), 'LOTE-T01', DATE_ADD(CURDATE(), INTERVAL 320 DAY), 'Vacuna triple felina anual', NOW()),
(2, 5, 2, NULL, DATE_SUB(CURDATE(), INTERVAL 45 DAY), 'LOTE-L01', DATE_ADD(CURDATE(), INTERVAL 320 DAY), 'Leucemia felina sin reacciones', NOW()),
-- Rocky (mascota 3) - Bordetella + Parvovirus
(3, 7, 2, NULL, DATE_SUB(CURDATE(), INTERVAL 60 DAY), 'LOTE-B01', DATE_ADD(CURDATE(), INTERVAL 120 DAY), 'Tos de las perreras - refuerzo anual', NOW()),
(3, 6, 2, NULL, DATE_SUB(CURDATE(), INTERVAL 180 DAY), 'LOTE-P01', DATE_ADD(CURDATE(), INTERVAL 185 DAY), NULL, NOW()),
-- Mimi (mascota 6) - Polivalente Felina
(6, 11, 4, NULL, DATE_SUB(CURDATE(), INTERVAL 20 DAY), 'LOTE-PF01', DATE_ADD(CURDATE(), INTERVAL 345 DAY), 'Primera dosis. Aplicar refuerzo en 21 días.', NOW()),
-- Thor (mascota 9) - Rabia + Leptospirosis
(9, 1, 4, NULL, DATE_SUB(CURDATE(), INTERVAL 15 DAY), 'LOTE-R03', DATE_ADD(CURDATE(), INTERVAL 350 DAY), NULL, NOW()),
(9, 8, 4, NULL, DATE_SUB(CURDATE(), INTERVAL 15 DAY), 'LOTE-LP01', DATE_ADD(CURDATE(), INTERVAL 350 DAY), 'Leptospirosis - dosis única anual', NOW()),
-- Zeus (mascota 11) - Influenza Canina
(11, 10, 5, NULL, DATE_SUB(CURDATE(), INTERVAL 10 DAY), 'LOTE-IC01', DATE_ADD(CURDATE(), INTERVAL 355 DAY), 'Vacuna anual contra influenza canina', NOW()),
-- Nala (mascota 13) - Triple Felina
(13, 4, 4, NULL, DATE_SUB(CURDATE(), INTERVAL 5 DAY),  'LOTE-T02', DATE_ADD(CURDATE(), INTERVAL 360 DAY), NULL, NOW());

-- ============================================================
-- 15. CONTROLES MENSUALES (20 registros de seguimiento)
-- ============================================================
INSERT INTO controles_mensuales_mascota (mascota_id, veterinario_id, fecha_control, anio, mes, peso_kg, alimentacion, observaciones, recomendaciones, created_at, updated_at) VALUES
-- Max (mascota 1) - 4 registros
(1, 1, DATE_SUB(CURDATE(), INTERVAL 3 MONTH), YEAR(DATE_SUB(CURDATE(), INTERVAL 3 MONTH)), MONTH(DATE_SUB(CURDATE(), INTERVAL 3 MONTH)), 29.50, 'Royal Canin 3 tazas/día', 'Peso ligeramente alto', 'Reducir porción en 1/4',       NOW(), NOW()),
(1, 1, DATE_SUB(CURDATE(), INTERVAL 2 MONTH), YEAR(DATE_SUB(CURDATE(), INTERVAL 2 MONTH)), MONTH(DATE_SUB(CURDATE(), INTERVAL 2 MONTH)), 29.00, 'Royal Canin 3 tazas/día', 'Peso estable', 'Continuar igual',                NOW(), NOW()),
(1, 1, DATE_SUB(CURDATE(), INTERVAL 1 MONTH), YEAR(DATE_SUB(CURDATE(), INTERVAL 1 MONTH)), MONTH(DATE_SUB(CURDATE(), INTERVAL 1 MONTH)), 28.80, 'Royal Canin 3 tazas/día', 'Bajó 200g, buen progreso', 'Reducir premios',              NOW(), NOW()),
(1, 1, CURDATE(), YEAR(CURDATE()), MONTH(CURDATE()), 28.50, 'Royal Canin 2.5 tazas/día', 'Peso ideal para su talla', 'Mantener rutina',                NOW(), NOW()),

-- Luna (mascota 2) - 4 registros
(2, 2, DATE_SUB(CURDATE(), INTERVAL 3 MONTH), YEAR(DATE_SUB(CURDATE(), INTERVAL 3 MONTH)), MONTH(DATE_SUB(CURDATE(), INTERVAL 3 MONTH)), 4.80, 'Hill\'s Metabolic 1/2 taza', 'Sobrepeso significativo', 'Iniciar dieta estricta',          NOW(), NOW()),
(2, 2, DATE_SUB(CURDATE(), INTERVAL 2 MONTH), YEAR(DATE_SUB(CURDATE(), INTERVAL 2 MONTH)), MONTH(DATE_SUB(CURDATE(), INTERVAL 2 MONTH)), 4.60, 'Hill\'s Metabolic 1/2 taza', 'Sigue con sobrepeso', 'Dieta estricta + más ejercicio',  NOW(), NOW()),
(2, 2, DATE_SUB(CURDATE(), INTERVAL 1 MONTH), YEAR(DATE_SUB(CURDATE(), INTERVAL 1 MONTH)), MONTH(DATE_SUB(CURDATE(), INTERVAL 1 MONTH)), 4.40, 'Hill\'s Metabolic 1/2 taza', 'Bajó 200g', 'Seguir dieta, aumentar juego',    NOW(), NOW()),
(2, 2, CURDATE(), YEAR(CURDATE()), MONTH(CURDATE()), 4.20, 'Hill\'s Metabolic 1/3 taza', 'Progreso constante', 'Mantener, control en 1 mes',       NOW(), NOW()),

-- Bella (mascota 5)
(5, 1, DATE_SUB(CURDATE(), INTERVAL 2 MONTH), YEAR(DATE_SUB(CURDATE(), INTERVAL 2 MONTH)), MONTH(DATE_SUB(CURDATE(), INTERVAL 2 MONTH)), 33.00, 'Eukanuba 3 tazas/día', 'Peso normal', 'Continuar rutina',               NOW(), NOW()),
(5, 1, DATE_SUB(CURDATE(), INTERVAL 1 MONTH), YEAR(DATE_SUB(CURDATE(), INTERVAL 1 MONTH)), MONTH(DATE_SUB(CURDATE(), INTERVAL 1 MONTH)), 32.50, 'Eukanuba 3 tazas/día', 'Buena condición', 'Continuar rutina',              NOW(), NOW()),
(5, 1, CURDATE(), YEAR(CURDATE()), MONTH(CURDATE()), 32.00, 'Eukanuba 3 tazas/día', 'Peso ideal', 'Vacunas al día',                          NOW(), NOW()),

-- Thor (mascota 9) - displasia
(9, 4, DATE_SUB(CURDATE(), INTERVAL 3 MONTH), YEAR(DATE_SUB(CURDATE(), INTERVAL 3 MONTH)), MONTH(DATE_SUB(CURDATE(), INTERVAL 3 MONTH)), 38.80, 'Pro Plan 4 tazas/día', 'Displasia estable', 'Continuar con condroprotectores', NOW(), NOW()),
(9, 4, DATE_SUB(CURDATE(), INTERVAL 2 MONTH), YEAR(DATE_SUB(CURDATE(), INTERVAL 2 MONTH)), MONTH(DATE_SUB(CURDATE(), INTERVAL 2 MONTH)), 38.50, 'Pro Plan 4 tazas/día', 'Displasia estable', 'Continuar con condroprotectores', NOW(), NOW()),
(9, 4, DATE_SUB(CURDATE(), INTERVAL 1 MONTH), YEAR(DATE_SUB(CURDATE(), INTERVAL 1 MONTH)), MONTH(DATE_SUB(CURDATE(), INTERVAL 1 MONTH)), 38.20, 'Pro Plan 3.5 tazas/día', 'Bajó 300g', 'Mantener tratamiento + ejercicios suaves', NOW(), NOW()),
(9, 4, CURDATE(), YEAR(CURDATE()), MONTH(CURDATE()), 38.00, 'Pro Plan 3.5 tazas/día', 'Peso controlado', 'Continuar igual. Próxima radiografía en 3 meses.', NOW(), NOW()),

-- Zeus (mascota 11) - Husky
(11, 5, DATE_SUB(CURDATE(), INTERVAL 2 MONTH), YEAR(DATE_SUB(CURDATE(), INTERVAL 2 MONTH)), MONTH(DATE_SUB(CURDATE(), INTERVAL 2 MONTH)), 25.50, 'Purina Pro Plan 2.5 tazas/día', 'Peso normal', 'Mantener rutina', NOW(), NOW()),
(11, 5, DATE_SUB(CURDATE(), INTERVAL 1 MONTH), YEAR(DATE_SUB(CURDATE(), INTERVAL 1 MONTH)), MONTH(DATE_SUB(CURDATE(), INTERVAL 1 MONTH)), 25.20, 'Purina Pro Plan 2.5 tazas/día', 'Estable', 'Reforzar cercado del hogar', NOW(), NOW()),
(11, 5, CURDATE(), YEAR(CURDATE()), MONTH(CURDATE()), 25.00, 'Purina Pro Plan 2.5 tazas/día', 'Peso estable', 'Mantener manejo actual', NOW(), NOW()),

-- Princesa (mascota 15) - Cocker Spaniel
(15, 4, DATE_SUB(CURDATE(), INTERVAL 1 MONTH), YEAR(DATE_SUB(CURDATE(), INTERVAL 1 MONTH)), MONTH(DATE_SUB(CURDATE(), INTERVAL 1 MONTH)), 15.50, 'Royal Canin Cocker 2 tazas/día', 'Oídos limpios. Sin signos de infección.', 'Continuar limpieza ótica semanal', NOW(), NOW()),
(15, 4, CURDATE(), YEAR(CURDATE()), MONTH(CURDATE()), 15.00, 'Royal Canin Cocker 2 tazas/día', 'Peso ideal. Oídos sanos.', 'Mantener cuidados. Control en 6 meses.', NOW(), NOW()),

-- Duke (mascota 19) - Dóberman
(19, 6, CURDATE(), YEAR(CURDATE()), MONTH(CURDATE()), 35.00, 'Royal Canin Doberman 3 tazas/día', 'Cardíaco estable. Peso óptimo.', 'Continuar suplemento de taurina. ECG anual.', NOW(), NOW());

-- ============================================================
-- 16. ACTUALIZAR CITAS CONFIRMADAS
-- ============================================================
UPDATE citas SET fecha_confirmacion = NOW(), confirmada_por = 'Sistema' WHERE estado = 'CONFIRMADA';

-- ============================================================
-- FIN DEL SCRIPT
-- ============================================================
