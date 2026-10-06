-- Explicit migration only. Stop writers and select the reviewed schema first.
\set ON_ERROR_STOP on
BEGIN;
SET LOCAL lock_timeout = '10s';
SET LOCAL statement_timeout = '10min';
SET LOCAL TIME ZONE 'UTC';
LOCK TABLE clinicas IN ACCESS EXCLUSIVE MODE;

ALTER TABLE clinicas ADD COLUMN IF NOT EXISTS trial_started_at timestamp without time zone;
ALTER TABLE clinicas ADD COLUMN IF NOT EXISTS trial_ends_at timestamp without time zone;

-- Recognize Hibernate's enum-list checks by catalog column identity AND shape.
-- Do not drop arbitrary plan checks, multi-column checks, state or slug checks.
DO $$
DECLARE
    plan_column smallint;
    constraint_row record;
    expression text;
BEGIN
    SELECT attnum INTO STRICT plan_column FROM pg_attribute
    WHERE attrelid = 'clinicas'::regclass AND attname = 'plan' AND NOT attisdropped;
    FOR constraint_row IN SELECT conname, conbin FROM pg_constraint
        WHERE conrelid = 'clinicas'::regclass AND contype = 'c'
          AND conkey = ARRAY[plan_column] LOOP
        expression := regexp_replace(pg_get_expr(constraint_row.conbin, 'clinicas'::regclass),
            '::(character varying|text)(\[\])?|[[:space:]()]', '', 'g');
        IF expression ~ '^plan=ANYARRAY\[''(TRIAL|FREE|PRO|CONSULTORIO)''(,''(TRIAL|FREE|PRO|CONSULTORIO)'')*\]$'
           AND position('''TRIAL''' IN expression) > 0
           AND position('''FREE''' IN expression) > 0
           AND position('''PRO''' IN expression) > 0 THEN
            EXECUTE format('ALTER TABLE clinicas DROP CONSTRAINT %I', constraint_row.conname);
        END IF;
    END LOOP;
    ALTER TABLE clinicas ADD CONSTRAINT p3_clinicas_plan_check
        CHECK (plan IN ('TRIAL', 'FREE', 'PRO', 'CONSULTORIO'));
END $$;

-- Only untouched historical TRIAL clocks are backfilled. Partial clocks fail
-- validation rather than guessing or extending an existing trial.
UPDATE clinicas
SET trial_started_at = created_at,
    trial_ends_at = created_at + interval '14 days'
WHERE plan = 'TRIAL' AND trial_started_at IS NULL AND trial_ends_at IS NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
        WHERE conrelid = 'clinicas'::regclass AND conname = 'p3_clinicas_trial_dates_check') THEN
        ALTER TABLE clinicas ADD CONSTRAINT p3_clinicas_trial_dates_check CHECK (
            (trial_started_at IS NULL AND trial_ends_at IS NULL AND plan <> 'TRIAL')
            OR (trial_started_at IS NOT NULL AND trial_ends_at IS NOT NULL
                AND trial_ends_at = trial_started_at + interval '14 days')
        );
    END IF;
END $$;
COMMIT;
