-- Explicit additive migration. Back up first, stop writers, and select the reviewed schema.
\set ON_ERROR_STOP on
BEGIN;
SET LOCAL search_path = public, pg_catalog;
SET LOCAL lock_timeout = '10s';
SET LOCAL statement_timeout = '1min';
LOCK TABLE asistentes IN ACCESS EXCLUSIVE MODE;
DO $$
DECLARE
    existing_length integer;
BEGIN
    SELECT character_maximum_length INTO STRICT existing_length
    FROM information_schema.columns
    WHERE table_schema = current_schema() AND table_name = 'asistentes' AND column_name = 'telefono'
      AND data_type = 'character varying';
    IF existing_length = 9 THEN
        ALTER TABLE asistentes ALTER COLUMN telefono TYPE varchar(20);
    ELSIF existing_length IS DISTINCT FROM 20 THEN
        RAISE EXCEPTION 'Unexpected asistentes.telefono length: %; review before migration', existing_length;
    END IF;
END $$;
COMMIT;
