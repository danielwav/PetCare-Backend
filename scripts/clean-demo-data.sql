-- ============================================
-- PETCARE - Clean demo data from Render DB
-- Keeps only: roles, admin user, servicios, vacunas
-- Run via Render SQL Console or psql
-- ============================================

DELETE FROM controles_mensuales_mascota;
DELETE FROM vacunas_mascota;
DELETE FROM inasistencias;
DELETE FROM atenciones_clinicas;
DELETE FROM detalles_costo_cita;
DELETE FROM horarios_veterinarios;
DELETE FROM citas;
DELETE FROM mascotas;
DELETE FROM asistentes;
DELETE FROM duenios;
DELETE FROM veterinarios;

-- Delete all non-admin users
DELETE FROM usuarios_roles WHERE usuario_id IN (
  SELECT id FROM usuarios WHERE email != 'admin@petcare.com'
);
DELETE FROM usuarios WHERE email != 'admin@petcare.com';

SELECT 'Cleaned' AS result, COUNT(*) AS remaining_roles FROM roles
UNION ALL SELECT 'Cleaned', COUNT(*) FROM usuarios
UNION ALL SELECT 'Cleaned', COUNT(*) FROM servicios
UNION ALL SELECT 'Cleaned', COUNT(*) FROM vacunas;
