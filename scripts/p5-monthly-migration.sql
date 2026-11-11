-- Explicit DDL only. Stop writers; read p5-runbook.md before execution.
\set ON_ERROR_STOP on
\if :{?p5_schema}
\else
    DO $$ BEGIN RAISE EXCEPTION 'P5: required psql variable p5_schema is missing'; END $$;
\endif
\if :{?p5_allow_unverified_paid_read_only}
\else
    \set p5_allow_unverified_paid_read_only false
\endif
BEGIN;
SET LOCAL search_path = :"p5_schema", pg_catalog;
SET LOCAL lock_timeout = '10s';
SET LOCAL statement_timeout = '10min';
SET LOCAL TIME ZONE 'UTC';
SELECT set_config('p5.schema', :'p5_schema', true),
       set_config('p5.allow_unverified', :'p5_allow_unverified_paid_read_only', true);
DO $$ BEGIN
    IF current_schema() IS DISTINCT FROM current_setting('p5.schema')
       OR current_schema() IN ('pg_catalog', 'information_schema')
       OR current_schema() LIKE 'pg\_%' ESCAPE '\' THEN
        RAISE EXCEPTION 'P5: select an existing, reviewed application schema';
    END IF;
    IF current_setting('p5.allow_unverified') NOT IN ('true', 'false') THEN
        RAISE EXCEPTION 'P5: approval must be exactly true or false';
    END IF;
    IF to_regclass('clinicas') IS NULL OR to_regclass('saas_clinic_audit') IS NULL THEN
        RAISE EXCEPTION 'P5: complete P4 in the selected schema first';
    END IF;
    IF EXISTS (SELECT 1 FROM pg_class WHERE oid IN
        (to_regclass('clinicas'), to_regclass('saas_clinic_audit'), to_regclass('monthly_payments'))
        AND (relnamespace <> quote_ident(current_schema())::regnamespace
             OR relkind <> 'r' OR relispartition)) THEN
        RAISE EXCEPTION 'P5: unexpected table namespace or relation kind';
    END IF;
END $$;
LOCK TABLE clinicas, saas_clinic_audit IN ACCESS EXCLUSIVE MODE;

-- Existing P4 rows/columns/checks/user triggers are unchanged. The incoming audit FK
-- adds PostgreSQL's own internal referential-integrity triggers, not audit columns.
DO $$ BEGIN
    IF EXISTS (
        WITH expected(name, type) AS (VALUES
            ('id', 'bigint'), ('clinic_id', 'bigint'), ('actor_id', 'bigint'),
            ('actor_email', 'character varying(120)'), ('action', 'character varying(30)'),
            ('previous_plan', 'character varying(20)'), ('new_plan', 'character varying(20)'),
            ('previous_estado', 'character varying(20)'), ('new_estado', 'character varying(20)'),
            ('reason', 'character varying(500)'), ('created_at', 'timestamp(6) without time zone')
        ), actual AS (
            SELECT attname::text name, format_type(atttypid, atttypmod) type,
                   attnotnull, attidentity, attgenerated FROM pg_attribute
            WHERE attrelid = 'saas_clinic_audit'::regclass AND attnum > 0 AND NOT attisdropped
        ) SELECT 1 FROM expected e FULL JOIN actual a USING (name)
        WHERE e.name IS NULL OR a.name IS NULL OR e.type <> a.type OR NOT a.attnotnull
           OR a.attgenerated <> '' OR (a.name = 'id' AND a.attidentity NOT IN ('a', 'd'))
           OR (a.name <> 'id' AND a.attidentity <> '')
    ) OR EXISTS (SELECT 1 FROM pg_constraint
                 WHERE conrelid = 'saas_clinic_audit'::regclass AND contype = 'f')
      OR NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'saas_clinic_audit'::regclass
          AND contype = 'p' AND pg_get_constraintdef(oid) = 'PRIMARY KEY (id)' AND NOT condeferrable)
      OR EXISTS (SELECT 1 FROM pg_constraint c JOIN pg_attribute a ON a.attrelid = c.conrelid
          AND a.attname = 'action' AND a.attnum = ANY(c.conkey)
          WHERE c.conrelid = 'saas_clinic_audit'::regclass AND c.contype = 'c')
      OR NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgrelid = 'saas_clinic_audit'::regclass
          AND tgname = 'p4_saas_audit_append_only' AND tgenabled = 'A' AND NOT tgisinternal)
      OR NOT EXISTS (SELECT 1 FROM pg_attribute WHERE attrelid = 'clinicas'::regclass
          AND attname = 'id' AND atttypid = 'bigint'::regtype AND attnotnull)
      OR NOT EXISTS (SELECT 1 FROM pg_attribute WHERE attrelid = 'clinicas'::regclass
          AND attname = 'plan' AND format_type(atttypid, atttypmod) = 'character varying(20)'
          AND attnotnull) THEN
        RAISE EXCEPTION 'P5: unexpected P4 audit or clinic mapping; review explicitly';
    END IF;
END $$;

ALTER TABLE clinicas ADD COLUMN IF NOT EXISTS subscription_started_at timestamptz(6);
ALTER TABLE clinicas ADD COLUMN IF NOT EXISTS subscription_ends_at timestamptz(6);
ALTER TABLE clinicas ADD COLUMN IF NOT EXISTS subscription_anchor_day integer;
ALTER TABLE clinicas ADD COLUMN IF NOT EXISTS last_payment_id bigint;
DO $$ BEGIN
    IF EXISTS (
        SELECT 1 FROM (VALUES ('subscription_started_at', 'timestamp(6) with time zone'),
            ('subscription_ends_at', 'timestamp(6) with time zone'),
            ('subscription_anchor_day', 'integer'), ('last_payment_id', 'bigint')) e(name, type)
        LEFT JOIN pg_attribute a ON a.attrelid = 'clinicas'::regclass AND a.attname = e.name
        WHERE a.attnum IS NULL OR a.attisdropped OR format_type(a.atttypid, a.atttypmod) <> e.type
           OR a.attnotnull OR a.atthasdef OR a.attidentity <> '' OR a.attgenerated <> ''
    ) THEN RAISE EXCEPTION 'P5: unexpected subscription column mapping'; END IF;
    IF current_setting('p5.allow_unverified') <> 'true' AND EXISTS (
        SELECT 1 FROM clinicas WHERE plan IN ('CONSULTORIO', 'PRO')
          AND subscription_started_at IS NULL AND subscription_ends_at IS NULL
          AND subscription_anchor_day IS NULL AND last_payment_id IS NULL
    ) THEN
        RAISE EXCEPTION 'P5: unverified paid clinics; inventory and approve read-only explicitly';
    END IF;
END $$;

-- One definition builds both the real table and a temporary catalog reference.
-- Compare parsed checks, not fragile text supplied by an existing same-named constraint.
-- Keep expressions stable under pg_get_constraintdef() reparse for ORM augmentation.
DO $migration$
DECLARE definition text := $shape$(
    id bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    clinic_id bigint NOT NULL,
    plan varchar(20) NOT NULL,
    operation varchar(20) NOT NULL,
    payment_reference varchar(120) NOT NULL,
    amount numeric(10,2) NOT NULL,
    currency varchar(3) NOT NULL,
    billing_cycle varchar(10) NOT NULL,
    period_started_at timestamptz(6) NOT NULL,
    period_ends_at timestamptz(6) NOT NULL,
    confirmed_at timestamptz(6) NOT NULL,
    anchor_day integer NOT NULL,
    actor_id bigint NOT NULL,
    actor_email varchar(120) NOT NULL,
    reason varchar(500) NOT NULL,
    previous_payment_id bigint,
    audit_id bigint NOT NULL,
    CONSTRAINT uk_monthly_payment_reference UNIQUE (payment_reference),
    CONSTRAINT uk_monthly_payment_id_clinic UNIQUE (id, clinic_id),
    CONSTRAINT uk_monthly_payment_id_clinic_plan UNIQUE (id, clinic_id, plan),
    CONSTRAINT uk_monthly_payment_audit UNIQUE (audit_id),
    CONSTRAINT p5_payment_plan_price CHECK
        ((plan = 'CONSULTORIO' AND amount = 59.00) OR (plan = 'PRO' AND amount = 129.00)),
    CONSTRAINT p5_payment_operation CHECK
        (operation::text = ANY (ARRAY['ACTIVATE'::text, 'RENEW'::text])),
    CONSTRAINT p5_payment_reference CHECK
        (length(payment_reference) >= 5 AND length(payment_reference) <= 120
         AND payment_reference COLLATE "C" ~ '^[A-Z0-9._/-]+( [A-Z0-9._/-]+)*$'),
    CONSTRAINT p5_payment_currency CHECK (currency = 'PEN'),
    CONSTRAINT p5_payment_cycle CHECK (billing_cycle = 'MONTHLY'),
    CONSTRAINT p5_payment_reason CHECK
        (reason !~ '^[[:space:]]|[[:space:]]$' AND length(reason) BETWEEN 5 AND 500),
    CONSTRAINT p5_payment_anchor CHECK (anchor_day BETWEEN 1 AND 31),
    CONSTRAINT p5_payment_finite CHECK
        (isfinite(period_started_at) AND isfinite(period_ends_at) AND isfinite(confirmed_at)),
    CONSTRAINT p5_payment_period CHECK
        (period_started_at < period_ends_at
         AND extract(day FROM period_started_at AT TIME ZONE 'America/Lima') = least(anchor_day,
             extract(day FROM date_trunc('month', period_started_at AT TIME ZONE 'America/Lima')
                 + interval '1 month - 1 day')::integer)
         AND period_ends_at = (
             date_trunc('month', period_started_at AT TIME ZONE 'America/Lima') + interval '1 month'
             + (least(anchor_day, extract(day FROM
                 date_trunc('month', period_started_at AT TIME ZONE 'America/Lima')
                 + interval '2 months - 1 day')::integer) - 1) * interval '1 day'
             + (period_started_at AT TIME ZONE 'America/Lima'
                 - date_trunc('day', period_started_at AT TIME ZONE 'America/Lima'))
         ) AT TIME ZONE 'America/Lima'),
    CONSTRAINT p5_payment_previous CHECK
        (previous_payment_id IS NULL OR (previous_payment_id > 0 AND previous_payment_id <> id))
)$shape$;
BEGIN
    EXECUTE 'CREATE TEMP TABLE p5_expected_monthly ' || definition || ' ON COMMIT DROP';
    IF to_regclass('monthly_payments') IS NULL THEN
        EXECUTE 'CREATE TABLE monthly_payments ' || definition;
    END IF;
END $migration$;
LOCK TABLE monthly_payments IN ACCESS EXCLUSIVE MODE;

DO $$ BEGIN
    IF (SELECT relkind <> 'r' OR relispartition OR relrowsecurity OR relforcerowsecurity
        FROM pg_class WHERE oid = 'monthly_payments'::regclass)
       OR EXISTS (SELECT 1 FROM pg_inherits WHERE inhrelid = 'monthly_payments'::regclass
                  OR inhparent = 'monthly_payments'::regclass)
       OR EXISTS (
        WITH expected AS (
            SELECT attname, format_type(atttypid, atttypmod) type, attnotnull, attidentity, attgenerated
            FROM pg_attribute WHERE attrelid = 'pg_temp.p5_expected_monthly'::regclass
              AND attnum > 0 AND NOT attisdropped
        ), actual AS (
            SELECT attname, format_type(atttypid, atttypmod) type, attnotnull, attidentity, attgenerated
            FROM pg_attribute WHERE attrelid = 'monthly_payments'::regclass
              AND attnum > 0 AND NOT attisdropped
        ) SELECT 1 FROM expected FULL JOIN actual USING (attname)
          WHERE expected IS DISTINCT FROM actual
    ) OR EXISTS (
        SELECT 1 FROM pg_attribute WHERE attrelid = 'monthly_payments'::regclass
          AND atthasdef AND attidentity = '' AND NOT attisdropped
    ) OR NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conrelid = 'monthly_payments'::regclass
          AND contype = 'p' AND pg_get_constraintdef(oid) = 'PRIMARY KEY (id)'
          AND convalidated AND NOT condeferrable
    ) OR EXISTS (
        SELECT 1 FROM pg_index WHERE indrelid = 'monthly_payments'::regclass
          AND (NOT indisvalid OR NOT indisready)
    ) THEN
        RAISE EXCEPTION 'P5: unexpected monthly_payments columns, constraints or indexes';
    END IF;
END $$;

CREATE TEMP TABLE p5_recognized_plan_checks (conname name PRIMARY KEY) ON COMMIT DROP;
DO $$
DECLARE
    plan_column smallint;
    c record;
    expression text;
BEGIN
    SELECT attnum INTO STRICT plan_column FROM pg_attribute
    WHERE attrelid = 'monthly_payments'::regclass AND attname = 'plan' AND NOT attisdropped;
    FOR c IN SELECT conname, conkey, conbin FROM pg_constraint
        WHERE conrelid = 'monthly_payments'::regclass AND contype = 'c'
          AND convalidated AND NOT connoinherit AND conkey = ARRAY[plan_column]
          AND conname NOT IN (SELECT conname FROM pg_constraint
                             WHERE conrelid = 'pg_temp.p5_expected_monthly'::regclass)
    LOOP
        expression := pg_get_expr(c.conbin, 'monthly_payments'::regclass);
        IF regexp_replace(expression, '::(character varying|text)(\[\])?|[[:space:]()]', '', 'g')
               ~ '^plan=ANYARRAY\[''[A-Z_]+''(,''[A-Z_]+'')*\]$'
           AND ARRAY(SELECT m[1] FROM regexp_matches(expression, '''([A-Z_]+)''', 'g') m
                     ORDER BY m[1]) = ARRAY['CONSULTORIO', 'FREE', 'PRO', 'TRIAL'] THEN
            INSERT INTO pg_temp.p5_recognized_plan_checks VALUES (c.conname);
        END IF;
    END LOOP;

    -- Only the exact empty ORM bootstrap is augmented, never a partial P5 table.
    IF (SELECT count(*) FROM pg_temp.p5_recognized_plan_checks) = 1
       AND (SELECT count(*) FROM pg_constraint WHERE conrelid = 'monthly_payments'::regclass
            AND contype NOT IN ('n', 'p')) = 3
       AND (SELECT count(*) FROM pg_constraint a JOIN pg_constraint e
                ON a.conname = e.conname AND pg_get_constraintdef(a.oid) = pg_get_constraintdef(e.oid)
            WHERE a.conrelid = 'monthly_payments'::regclass
              AND e.conrelid = 'pg_temp.p5_expected_monthly'::regclass
              AND a.contype = 'u' AND a.convalidated AND NOT a.condeferrable
              AND a.conname IN ('uk_monthly_payment_reference', 'uk_monthly_payment_audit')) = 2
       AND (SELECT count(*) FROM pg_index WHERE indrelid = 'monthly_payments'::regclass) = 4
       AND EXISTS (SELECT 1 FROM pg_index WHERE indrelid = 'monthly_payments'::regclass
                   AND indexrelid = to_regclass('idx_monthly_payment_clinic_confirmed'))
       AND NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgrelid = 'monthly_payments'::regclass
                       AND NOT tgisinternal) THEN
        IF EXISTS (SELECT 1 FROM monthly_payments) THEN
            RAISE EXCEPTION 'P5: populated ORM-bootstrap history requires explicit review';
        END IF;
        FOR c IN SELECT conname, pg_get_constraintdef(oid) definition FROM pg_constraint
            WHERE conrelid = 'pg_temp.p5_expected_monthly'::regclass AND contype IN ('c', 'u')
              AND conname NOT IN ('uk_monthly_payment_reference', 'uk_monthly_payment_audit')
        LOOP
            EXECUTE format('ALTER TABLE monthly_payments ADD CONSTRAINT %I %s', c.conname, c.definition);
        END LOOP;
    END IF;
END $$;

DO $$ BEGIN
    IF EXISTS (
        WITH expected AS (
            SELECT conname, contype, pg_get_constraintdef(oid) def, convalidated, condeferrable
            FROM pg_constraint WHERE conrelid = 'pg_temp.p5_expected_monthly'::regclass
              AND contype NOT IN ('n', 'p')
        ), actual AS (
            SELECT conname, contype, pg_get_constraintdef(oid) def, convalidated, condeferrable
            FROM pg_constraint c WHERE conrelid = 'monthly_payments'::regclass
              AND contype NOT IN ('n', 'p', 'f')
              AND NOT EXISTS (SELECT 1 FROM pg_temp.p5_recognized_plan_checks r WHERE r.conname = c.conname)
        ) SELECT 1 FROM expected FULL JOIN actual USING (conname)
          WHERE expected IS DISTINCT FROM actual
    ) THEN RAISE EXCEPTION 'P5: unexpected monthly_payments constraints; review explicitly'; END IF;
END $$;

DO $migration$
DECLARE
    definition text := $shape$CHECK (
        (subscription_started_at IS NULL AND subscription_ends_at IS NULL
         AND subscription_anchor_day IS NULL AND last_payment_id IS NULL)
        OR (subscription_started_at IS NOT NULL AND subscription_ends_at IS NOT NULL
            AND subscription_anchor_day IS NOT NULL AND last_payment_id IS NOT NULL
            AND isfinite(subscription_started_at) AND isfinite(subscription_ends_at)
            AND subscription_started_at < subscription_ends_at
            AND subscription_anchor_day BETWEEN 1 AND 31 AND last_payment_id > 0
            AND plan IN ('CONSULTORIO', 'PRO'))
    )$shape$;
BEGIN
    CREATE TEMP TABLE p5_expected_clinic (
        subscription_started_at timestamptz(6), subscription_ends_at timestamptz(6),
        subscription_anchor_day integer, last_payment_id bigint, plan varchar(20)
    ) ON COMMIT DROP;
    EXECUTE 'ALTER TABLE pg_temp.p5_expected_clinic ADD CONSTRAINT p5_clinic_subscription ' || definition;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'clinicas'::regclass
                   AND conname = 'p5_clinic_subscription') THEN
        EXECUTE 'ALTER TABLE clinicas ADD CONSTRAINT p5_clinic_subscription ' || definition;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint a JOIN pg_constraint e
          ON e.conrelid = 'pg_temp.p5_expected_clinic'::regclass AND e.contype = 'c'
        WHERE a.conrelid = 'clinicas'::regclass AND a.conname = 'p5_clinic_subscription'
          AND a.contype = 'c' AND a.convalidated AND NOT a.connoinherit
          AND pg_get_constraintdef(a.oid) = pg_get_constraintdef(e.oid)
    ) THEN RAISE EXCEPTION 'P5: unexpected clinic subscription check'; END IF;
END $migration$;

-- Last-payment FK is deliberately added only after the history table exists.
DO $$
DECLARE spec record;
BEGIN
    FOR spec IN SELECT * FROM (VALUES
        ('monthly_payments', 'p5_payment_previous_fk',
         'FOREIGN KEY (previous_payment_id, clinic_id) REFERENCES monthly_payments(id, clinic_id) ON DELETE RESTRICT'),
        ('monthly_payments', 'p5_payment_audit_fk',
         'FOREIGN KEY (audit_id) REFERENCES saas_clinic_audit(id) ON DELETE RESTRICT'),
        ('clinicas', 'p5_clinic_last_payment_fk',
         'FOREIGN KEY (last_payment_id, id, plan) REFERENCES monthly_payments(id, clinic_id, plan) ON DELETE RESTRICT')
    ) AS fks(table_name, constraint_name, definition) LOOP
        IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = to_regclass(spec.table_name)
                       AND conname = spec.constraint_name) THEN
            EXECUTE format('ALTER TABLE %I ADD CONSTRAINT %I %s',
                           spec.table_name, spec.constraint_name, spec.definition);
        END IF;
        IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = to_regclass(spec.table_name)
            AND conname = spec.constraint_name AND contype = 'f' AND convalidated
            AND NOT condeferrable AND pg_get_constraintdef(oid) = spec.definition) THEN
            RAISE EXCEPTION 'P5: unexpected foreign key %', spec.constraint_name;
        END IF;
    END LOOP;
    IF (SELECT count(*) FROM pg_constraint WHERE conrelid = 'monthly_payments'::regclass
        AND contype = 'f') <> 2 THEN
        RAISE EXCEPTION 'P5: unexpected history foreign keys (no clinic/actor FK allowed)';
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_monthly_payment_clinic_confirmed
    ON monthly_payments (clinic_id, confirmed_at, id);
DO $$ BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_index i JOIN pg_class c ON c.oid = i.indexrelid
        WHERE c.relname = 'idx_monthly_payment_clinic_confirmed'
          AND i.indrelid = 'monthly_payments'::regclass
          AND i.indisvalid AND i.indisready AND NOT i.indisunique
          AND i.indnkeyatts = 3 AND i.indnatts = 3 AND i.indexprs IS NULL AND i.indpred IS NULL
          AND i.indkey::text = (SELECT string_agg(attnum::text, ' ' ORDER BY ord)
              FROM unnest(ARRAY['clinic_id', 'confirmed_at', 'id']) WITH ORDINALITY AS x(name, ord)
              JOIN pg_attribute a ON a.attrelid = 'monthly_payments'::regclass AND a.attname = x.name)
    ) THEN RAISE EXCEPTION 'P5: unexpected history index definition'; END IF;
END $$;

CREATE OR REPLACE FUNCTION p5_reject_payment_mutation() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION USING ERRCODE = '55000', MESSAGE = 'P5: monthly_payments is append-only';
END $$;
DROP TRIGGER IF EXISTS p5_monthly_payment_append_only ON monthly_payments;
CREATE TRIGGER p5_monthly_payment_append_only
    BEFORE UPDATE OR DELETE OR TRUNCATE ON monthly_payments
    FOR EACH STATEMENT EXECUTE FUNCTION p5_reject_payment_mutation();
ALTER TABLE monthly_payments ENABLE ALWAYS TRIGGER p5_monthly_payment_append_only;
COMMIT;
