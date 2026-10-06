\set ON_ERROR_STOP on
BEGIN READ ONLY;
SELECT current_database(), current_user, current_schema(), current_setting('search_path');
SELECT id, slug FROM clinicas ORDER BY id;
SELECT 'duenios' AS table_name, count(*) AS total,
       count(*) FILTER (WHERE clinica_id IS NULL) AS null_tenants FROM duenios
UNION ALL SELECT 'servicios', count(*), count(*) FILTER (WHERE clinica_id IS NULL) FROM servicios
UNION ALL SELECT 'mascotas', count(*), count(*) FILTER (WHERE clinica_id IS NULL) FROM mascotas
UNION ALL SELECT 'citas', count(*), count(*) FILTER (WHERE clinica_id IS NULL) FROM citas
UNION ALL SELECT 'veterinarios', count(*), count(*) FILTER (WHERE clinica_id IS NULL) FROM veterinarios
UNION ALL SELECT 'asistentes', count(*), count(*) FILTER (WHERE clinica_id IS NULL) FROM asistentes;
SELECT count(*) AS vaccines,
       count(*) FILTER (WHERE to_jsonb(v)->>'clinica_id' IS NULL) AS null_vaccine_tenants
FROM vacunas v;
SELECT count(*) AS vaccine_applications FROM vacunas_mascota;
SELECT v.id AS legacy_vaccine, m.clinica_id, count(*) AS applications
FROM vacunas v JOIN vacunas_mascota vm ON vm.vacuna_id = v.id
JOIN mascotas m ON m.id = vm.mascota_id
WHERE to_jsonb(v)->>'clinica_id' IS NULL
GROUP BY v.id, m.clinica_id ORDER BY v.id, m.clinica_id;
SELECT 'servicios.nombre' AS key, clinica_id, upper(nombre) AS value, count(*)
FROM servicios GROUP BY clinica_id, upper(nombre) HAVING count(*) > 1
UNION ALL SELECT 'duenios.email', clinica_id, email, count(*)
FROM duenios GROUP BY clinica_id, email HAVING count(*) > 1
UNION ALL SELECT 'duenios.numero_documento', clinica_id, numero_documento, count(*)
FROM duenios GROUP BY clinica_id, numero_documento HAVING count(*) > 1;
SELECT to_jsonb(v)->>'clinica_id' AS clinic, upper(nombre), count(*)
FROM vacunas v GROUP BY to_jsonb(v)->>'clinica_id', upper(nombre) HAVING count(*) > 1;
SELECT conrelid::regclass AS table_name, conname, pg_get_constraintdef(oid)
FROM pg_constraint WHERE conrelid IN ('servicios'::regclass, 'duenios'::regclass, 'vacunas'::regclass)
ORDER BY conrelid::regclass::text, conname;
SELECT indrelid::regclass AS table_name, indexrelid::regclass AS index_name,
       indisunique, indisvalid, pg_get_indexdef(indexrelid)
FROM pg_index WHERE indrelid IN ('servicios'::regclass, 'duenios'::regclass, 'vacunas'::regclass)
ORDER BY indrelid::regclass::text, indexrelid::regclass::text;
COMMIT;
