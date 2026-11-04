-- Explicit, separate provisioning. Supply psql variables securely; see p4-migration.md.
-- Never pass a plaintext password. Do not use psql -a/-e or shell tracing.
\set ECHO none
\set ECHO_HIDDEN off
\set VERBOSITY terse
\set ON_ERROR_STOP on
\if :{?p4_schema}
\else
    DO $$ BEGIN RAISE EXCEPTION 'P4: required psql variable p4_schema is missing'; END $$;
\endif
\if :{?operator_email}
\else
    DO $$ BEGIN RAISE EXCEPTION 'P4: required psql variable operator_email is missing'; END $$;
\endif
\if :{?nombres}
\else
    DO $$ BEGIN RAISE EXCEPTION 'P4: required psql variable nombres is missing'; END $$;
\endif
\if :{?apellidos}
\else
    DO $$ BEGIN RAISE EXCEPTION 'P4: required psql variable apellidos is missing'; END $$;
\endif
\if :{?password_hash}
\else
    DO $$ BEGIN RAISE EXCEPTION 'P4: required psql variable password_hash is missing'; END $$;
\endif

BEGIN;
SET LOCAL search_path = :"p4_schema", pg_catalog;
DO $$ BEGIN
    IF current_schema() IN ('pg_catalog', 'information_schema')
       OR current_schema() LIKE 'pg\_%' ESCAPE '\' THEN
        RAISE EXCEPTION 'P4: select an existing, reviewed application schema';
    END IF;
END $$;
SET LOCAL lock_timeout = '10s';
SET LOCAL statement_timeout = '1min';
SET LOCAL TIME ZONE 'UTC';
LOCK TABLE roles IN SHARE MODE;
-- Prevent concurrent case-variant registration and role-assignment races.
LOCK TABLE usuarios, usuarios_roles IN SHARE ROW EXCLUSIVE MODE;
CREATE TEMP TABLE p4_operator_input (
    email text, nombres text, apellidos text, password_hash text
) ON COMMIT DROP;
INSERT INTO p4_operator_input VALUES (:'operator_email', :'nombres', :'apellidos', :'password_hash');
\unset password_hash

DO $$
DECLARE
    input record;
    operator_role_id bigint;
    operator_id bigint;
BEGIN
    SELECT lower(btrim(email)) AS email, btrim(nombres) AS nombres,
           btrim(apellidos) AS apellidos, password_hash INTO STRICT input FROM p4_operator_input;
    IF input.email IS NULL OR length(input.email) > 120
       OR input.email ~ '[[:cntrl:]]'
       OR input.email !~ '^[^[:space:]@]+@[^[:space:]@]+\.[^[:space:]@]+$' THEN
        RAISE EXCEPTION 'P4: operator_email must be a nonblank email of at most 120 characters';
    END IF;
    IF input.nombres IS NULL OR input.apellidos IS NULL
       OR input.nombres = '' OR input.apellidos = ''
       OR input.nombres ~ '[[:cntrl:]]' OR input.apellidos ~ '[[:cntrl:]]'
       OR length(input.nombres || ' ' || input.apellidos) > 120 THEN
        RAISE EXCEPTION 'P4: nombres/apellidos must be nonblank, without controls; combined full_name at most 120 characters';
    END IF;
    IF input.password_hash IS NULL
       OR input.password_hash !~ '^\$2[aby]\$1[0-6]\$[./A-Za-z0-9]{53}$' THEN
        RAISE EXCEPTION 'P4: password_hash must be an out-of-band 60-character bcrypt hash (cost 10-16), not plaintext';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'roles'::regclass
        AND conname = 'p4_roles_name_check' AND contype = 'c' AND convalidated)
       OR to_regclass('saas_clinic_audit') IS NULL THEN
        RAISE EXCEPTION 'P4: run and verify p4-saas-migration.sql before provisioning';
    END IF;
    IF EXISTS (SELECT 1 FROM usuarios WHERE lower(btrim(email)) = input.email) THEN
        RAISE EXCEPTION 'P4: email already exists; refusing promotion or modification of any existing account';
    END IF;
    -- Never create or reactivate a role here; migration and authorization are separate.
    IF (SELECT count(*) FROM roles WHERE name = 'ROLE_SAAS_OPERATOR' AND active) <> 1 THEN
        RAISE EXCEPTION 'P4: exactly one active ROLE_SAAS_OPERATOR must already exist';
    END IF;
    SELECT id INTO STRICT operator_role_id FROM roles WHERE name = 'ROLE_SAAS_OPERATOR' AND active;
    INSERT INTO usuarios (full_name, email, password, active, force_password_change,
                          activation_token, token_expiry, created_at, clinica_id, telefono)
    VALUES (input.nombres || ' ' || input.apellidos, input.email, input.password_hash,
            true, true, NULL, NULL, timezone('UTC', current_timestamp), NULL, NULL)
    RETURNING id INTO operator_id;
    INSERT INTO usuarios_roles (usuario_id, rol_id) VALUES (operator_id, operator_role_id);
    IF (SELECT count(*) FROM usuarios_roles WHERE usuario_id = operator_id) <> 1
       OR NOT EXISTS (SELECT 1 FROM usuarios WHERE id = operator_id AND active
                      AND force_password_change AND clinica_id IS NULL) THEN
        RAISE EXCEPTION 'P4: operator isolation postcondition failed';
    END IF;
END $$;
COMMIT;
\echo 'P4: NEW active, clinic-less, sole-role operator created; password change required'
