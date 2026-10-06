-- LOCAL FIXTURE ONLY. Requires an empty scratch database, never production.
-- Run from scripts/: psql -X -v ON_ERROR_STOP=1 ... -f p3-test.sql
\set ON_ERROR_STOP on
BEGIN;
CREATE SCHEMA p3_fixture;
SET search_path = p3_fixture;
CREATE TABLE clinicas (
    id bigint PRIMARY KEY, slug varchar(80) NOT NULL UNIQUE,
    plan varchar(20) NOT NULL, estado varchar(20) NOT NULL,
    created_at timestamp NOT NULL,
    CONSTRAINT unpredictable_hibernate_plan_name CHECK (plan IN ('TRIAL', 'FREE', 'PRO')),
    CONSTRAINT keep_state_check CHECK (estado IN ('ACTIVA', 'INACTIVA')),
    CONSTRAINT keep_slug_check CHECK (length(slug) > 0),
    CONSTRAINT keep_custom_plan_check CHECK (length(plan) > 0),
    CONSTRAINT keep_multicolumn_check CHECK (plan <> 'FREE' OR estado = 'ACTIVA')
);
CREATE TABLE usuarios (id bigint PRIMARY KEY, clinica_id bigint, active boolean);
INSERT INTO clinicas VALUES
    (1, 'demo', 'TRIAL', 'ACTIVA', '2020-01-01 12:34:56'),
    (2, 'free', 'FREE', 'ACTIVA', '2020-01-02'),
    (3, 'pro', 'PRO', 'ACTIVA', '2020-01-03');
INSERT INTO usuarios SELECT n, 1, true FROM generate_series(1, 10) AS n;
COMMIT;

\ir p3-plan-migration.sql
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM clinicas WHERE id = 1
        AND trial_started_at = created_at AND trial_ends_at = created_at + interval '14 days') THEN
        RAISE EXCEPTION 'Historical trial was not backfilled from created_at';
    END IF;
END $$;
UPDATE clinicas SET trial_started_at = '2021-01-01', trial_ends_at = '2021-01-15' WHERE id = 1;
INSERT INTO clinicas VALUES (4, 'consultorio', 'CONSULTORIO', 'ACTIVA', '2020-01-04', NULL, NULL);
\ir p3-plan-migration.sql

DO $$
BEGIN
    IF (SELECT count(*) FROM usuarios WHERE clinica_id = 1 AND active) <> 10 THEN
        RAISE EXCEPTION 'Existing over-limit staff must be retained';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM clinicas WHERE id = 1 AND plan = 'TRIAL'
        AND trial_started_at = timestamp '2021-01-01' AND trial_ends_at = timestamp '2021-01-15') THEN
        RAISE EXCEPTION 'Rerun reset an existing clock';
    END IF;
    IF (SELECT count(*) FROM clinicas WHERE plan IN ('FREE', 'PRO', 'CONSULTORIO')
        AND trial_started_at IS NULL AND trial_ends_at IS NULL) <> 3 THEN
        RAISE EXCEPTION 'Nontrial plans must remain unchanged';
    END IF;
    IF (SELECT count(*) FROM pg_constraint WHERE conrelid = 'clinicas'::regclass
        AND conname IN ('keep_state_check', 'keep_slug_check', 'keep_custom_plan_check', 'keep_multicolumn_check')) <> 4 THEN
        RAISE EXCEPTION 'Unrelated check removed';
    END IF;
    BEGIN
        UPDATE clinicas SET trial_ends_at = trial_started_at + interval '15 days' WHERE id = 1;
        RAISE EXCEPTION 'Invalid trial duration accepted';
    EXCEPTION WHEN check_violation THEN NULL;
    END;
    BEGIN
        UPDATE clinicas SET trial_started_at = NULL, trial_ends_at = NULL WHERE id = 1;
        RAISE EXCEPTION 'Missing trial dates accepted';
    EXCEPTION WHEN check_violation THEN NULL;
    END;
    BEGIN
        UPDATE clinicas SET plan = 'UNKNOWN' WHERE id = 3;
        RAISE EXCEPTION 'Unknown plan accepted';
    EXCEPTION WHEN check_violation THEN NULL;
    END;
END $$;
DROP SCHEMA p3_fixture CASCADE;
