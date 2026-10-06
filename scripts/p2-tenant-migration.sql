-- Explicit PostgreSQL migration. Run with psql -X -v ON_ERROR_STOP=1.
-- Set PGOPTIONS to the reviewed schema's search_path; see p2-migration.md.
\set ON_ERROR_STOP on
BEGIN;
SET LOCAL lock_timeout = '10s';
SET LOCAL statement_timeout = '10min';

-- Stop application writers first. Locks also protect catalog checks and remapping.
LOCK TABLE clinicas, usuarios, duenios, servicios, mascotas, citas,
    veterinarios, asistentes, vacunas, vacunas_mascota IN ACCESS EXCLUSIVE MODE;

ALTER TABLE vacunas ADD COLUMN IF NOT EXISTS clinica_id bigint;
DO $$
DECLARE
    demo_id bigint;
BEGIN
    SELECT id INTO demo_id FROM clinicas WHERE slug = 'demo';
    -- Retain the old single-demo backfill, never guess ownership in multi-clinic data.
    IF EXISTS (SELECT 1 FROM duenios WHERE clinica_id IS NULL)
       OR EXISTS (SELECT 1 FROM servicios WHERE clinica_id IS NULL)
       OR EXISTS (SELECT 1 FROM mascotas WHERE clinica_id IS NULL)
       OR EXISTS (SELECT 1 FROM citas WHERE clinica_id IS NULL)
       OR EXISTS (SELECT 1 FROM veterinarios WHERE clinica_id IS NULL)
       OR EXISTS (SELECT 1 FROM asistentes WHERE clinica_id IS NULL) THEN
        IF demo_id IS NULL OR (SELECT count(*) FROM clinicas) <> 1 THEN
            RAISE EXCEPTION 'Ambiguous null tenant ownership: resolve owners/services/pets/appointments/staff explicitly before P2';
        END IF;
        UPDATE duenios SET clinica_id = demo_id WHERE clinica_id IS NULL;
        UPDATE servicios SET clinica_id = demo_id WHERE clinica_id IS NULL;
        UPDATE mascotas SET clinica_id = demo_id WHERE clinica_id IS NULL;
        UPDATE citas SET clinica_id = demo_id WHERE clinica_id IS NULL;
        UPDATE veterinarios SET clinica_id = demo_id WHERE clinica_id IS NULL;
        UPDATE asistentes SET clinica_id = demo_id WHERE clinica_id IS NULL;
    END IF;
    IF EXISTS (SELECT 1 FROM vacunas WHERE clinica_id IS NULL) AND demo_id IS NULL THEN
        RAISE EXCEPTION 'Legacy vaccine catalog requires an existing, reviewed clinic with slug demo';
    END IF;
    IF EXISTS (
        SELECT 1 FROM mascotas m JOIN duenios d ON d.id = m.duenio_id
        WHERE m.clinica_id IS DISTINCT FROM d.clinica_id
    ) THEN
        RAISE EXCEPTION 'Pet/owner tenant mismatch: review historical ownership before P2';
    END IF;
    IF EXISTS (
        SELECT 1 FROM vacunas_mascota vm
        LEFT JOIN mascotas m ON m.id = vm.mascota_id
        LEFT JOIN clinicas c ON c.id = m.clinica_id
        WHERE c.id IS NULL
    ) THEN
        RAISE EXCEPTION 'Vaccine application has no unambiguous pet clinic';
    END IF;
END $$;

-- Drop only catalog-proven, single-column global unique constraints and plain
-- unique indexes. Never use guessed Hibernate names or CASCADE.
DO $$
DECLARE
    target record;
    obj record;
    table_oid oid;
    column_num smallint;
BEGIN
    FOR target IN SELECT * FROM (VALUES
        ('servicios', 'nombre'), ('duenios', 'email'),
        ('duenios', 'numero_documento'), ('vacunas', 'nombre')
    ) AS targets(table_name, column_name) LOOP
        table_oid := to_regclass(target.table_name);
        SELECT attnum INTO STRICT column_num FROM pg_attribute
        WHERE attrelid = table_oid AND attname = target.column_name AND NOT attisdropped;
        FOR obj IN SELECT conname FROM pg_constraint
            WHERE conrelid = table_oid AND contype = 'u'
              AND conkey = ARRAY[column_num] LOOP
            EXECUTE format('ALTER TABLE %s DROP CONSTRAINT %I', table_oid::regclass, obj.conname);
        END LOOP;
        FOR obj IN SELECT i.indexrelid FROM pg_index i
            WHERE i.indrelid = table_oid AND i.indisunique AND NOT i.indisprimary
              AND i.indnkeyatts = 1 AND i.indkey[0] = column_num
              AND i.indexprs IS NULL AND i.indpred IS NULL
              AND NOT EXISTS (SELECT 1 FROM pg_constraint c WHERE c.conindid = i.indexrelid) LOOP
            EXECUTE format('DROP INDEX %s', obj.indexrelid::regclass);
        END LOOP;
    END LOOP;
END $$;

-- Snapshot only legacy catalog IDs and the clinics where they were actually used.
CREATE TEMP TABLE p2_legacy_vaccines ON COMMIT DROP AS
SELECT id FROM vacunas WHERE clinica_id IS NULL;
CREATE TEMP TABLE p2_vaccine_map ON COMMIT DROP AS
SELECT DISTINCT v.id AS original_id, m.clinica_id, NULL::bigint AS replacement_id
FROM p2_legacy_vaccines v
JOIN vacunas_mascota vm ON vm.vacuna_id = v.id
JOIN mascotas m ON m.id = vm.mascota_id
JOIN clinicas c ON c.id = m.clinica_id
WHERE c.slug <> 'demo';

DO $$
DECLARE
    mapping record;
    replacement bigint;
BEGIN
    -- An existing same-name catalog is not proof of identical historical meaning.
    IF EXISTS (
        SELECT 1 FROM p2_vaccine_map map
        JOIN vacunas original ON original.id = map.original_id
        JOIN vacunas existing ON existing.clinica_id = map.clinica_id
            AND upper(existing.nombre) = upper(original.nombre)
    ) THEN
        RAISE EXCEPTION 'Legacy vaccine clone conflicts with existing tenant catalog; review without merging history';
    END IF;
    UPDATE vacunas SET clinica_id = (SELECT id FROM clinicas WHERE slug = 'demo')
    WHERE id IN (SELECT id FROM p2_legacy_vaccines);
    FOR mapping IN SELECT * FROM p2_vaccine_map LOOP
        INSERT INTO vacunas (clinica_id, nombre, descripcion, intervalo_proxima_dosis_dias,
                             active, created_at, updated_at)
        SELECT mapping.clinica_id, nombre, descripcion, intervalo_proxima_dosis_dias,
               active, created_at, updated_at
        FROM vacunas WHERE id = mapping.original_id
        RETURNING id INTO replacement;
        UPDATE p2_vaccine_map SET replacement_id = replacement
        WHERE original_id = mapping.original_id AND clinica_id = mapping.clinica_id;
    END LOOP;
END $$;

-- Preserve application IDs, dates, lots, notes, veterinarian/cita links and timestamps.
UPDATE vacunas_mascota vm SET vacuna_id = map.replacement_id
FROM p2_vaccine_map map, mascotas m
WHERE vm.vacuna_id = map.original_id AND vm.mascota_id = m.id
  AND m.clinica_id = map.clinica_id;

DO $$
DECLARE
    target record;
    table_oid oid;
    expected smallint[];
    existing record;
BEGIN
    FOR target IN SELECT * FROM (VALUES
        ('servicios', 'nombre', 'uk_servicios_clinica_nombre'),
        ('duenios', 'email', 'uk_duenios_clinica_email'),
        ('duenios', 'numero_documento', 'uk_duenios_clinica_documento'),
        ('vacunas', 'nombre', 'uk_vacunas_clinica_nombre')
    ) AS targets(table_name, column_name, constraint_name) LOOP
        table_oid := to_regclass(target.table_name);
        SELECT ARRAY[
            (SELECT attnum FROM pg_attribute WHERE attrelid = table_oid AND attname = 'clinica_id'),
            (SELECT attnum FROM pg_attribute WHERE attrelid = table_oid AND attname = target.column_name)
        ]::smallint[] INTO expected;
        SELECT contype, conkey, convalidated INTO existing FROM pg_constraint
        WHERE conrelid = table_oid AND conname = target.constraint_name;
        IF FOUND THEN
            IF existing.contype <> 'u' OR existing.conkey <> expected OR NOT existing.convalidated THEN
                RAISE EXCEPTION 'Unexpected existing constraint %', target.constraint_name;
            END IF;
        ELSE
            EXECUTE format('ALTER TABLE %s ADD CONSTRAINT %I UNIQUE (clinica_id, %I)',
                table_oid::regclass, target.constraint_name, target.column_name);
        END IF;
    END LOOP;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
        WHERE conrelid = 'vacunas'::regclass AND contype = 'f'
          AND conkey = ARRAY[(SELECT attnum FROM pg_attribute
              WHERE attrelid = 'vacunas'::regclass AND attname = 'clinica_id')]::smallint[]
          AND confrelid = 'clinicas'::regclass
          AND confkey = ARRAY[(SELECT attnum FROM pg_attribute
              WHERE attrelid = 'clinicas'::regclass AND attname = 'id')]::smallint[]
          AND convalidated) THEN
        ALTER TABLE vacunas ADD CONSTRAINT fk_p2_vacunas_clinica
            FOREIGN KEY (clinica_id) REFERENCES clinicas(id);
    END IF;
END $$;

-- Spring Data IgnoreCase uses upper(). Email/document validation is exact-case.
CREATE UNIQUE INDEX IF NOT EXISTS ux_p2_servicios_clinica_nombre_ci
    ON servicios (clinica_id, upper(nombre));
CREATE UNIQUE INDEX IF NOT EXISTS ux_p2_vacunas_clinica_nombre_ci
    ON vacunas (clinica_id, upper(nombre));

DO $$
DECLARE
    target record;
BEGIN
    -- IF NOT EXISTS must not silently accept an unrelated or invalid same-name index.
    FOR target IN SELECT * FROM (VALUES
        ('servicios', 'ux_p2_servicios_clinica_nombre_ci'),
        ('vacunas', 'ux_p2_vacunas_clinica_nombre_ci')
    ) AS targets(table_name, index_name) LOOP
        IF NOT EXISTS (
            SELECT 1 FROM pg_index i
            WHERE i.indexrelid = to_regclass(target.index_name)
              AND i.indrelid = to_regclass(target.table_name)
              AND i.indisunique AND i.indisvalid AND i.indisready
              AND i.indnkeyatts = 2 AND i.indkey[1] = 0 AND i.indpred IS NULL
              AND i.indkey[0] = (SELECT attnum FROM pg_attribute
                  WHERE attrelid = i.indrelid AND attname = 'clinica_id')
              AND regexp_replace(pg_get_expr(i.indexprs, i.indrelid), '::text|[() ]', '', 'g') = 'uppernombre'
        ) THEN
            RAISE EXCEPTION 'Unexpected case-insensitive index %', target.index_name;
        END IF;
    END LOOP;
    IF EXISTS (
        SELECT 1 FROM vacunas_mascota vm
        JOIN mascotas m ON m.id = vm.mascota_id
        JOIN vacunas v ON v.id = vm.vacuna_id
        WHERE v.clinica_id IS DISTINCT FROM m.clinica_id
    ) THEN
        RAISE EXCEPTION 'Vaccine application/catalog tenant mismatch remains; review existing historical ownership';
    END IF;
END $$;

COMMIT;
