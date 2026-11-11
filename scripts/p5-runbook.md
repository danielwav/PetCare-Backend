# P5: Manual Monthly Billing

Scope: explicit PostgreSQL migration and disposable local regression only. Nothing here connects to a deployment, changes credentials/environment files, deploys Java, runs Maven, or edits the main README. SQL regression is not proof of application authorization or production readiness.

## Release Gates

1. Complete P4 and review the actual application schema, owners, PostgreSQL version, backup/restore procedure and change approval. Stop all application writers, registration, jobs and old replicas. Keep traffic closed through migration, privilege verification and application acceptance. No startup migration or Hibernate `update`: require effective `JPA_DDL_AUTO=validate`, production security configuration and disabled demo seeding as described in P4.
2. Use only an operator-approved connection/service and credential broker. Verify destination independently. Never put credentials in URLs, argv, scripts, tickets or repository files; do not print environment variables or enable SQL/body/token tracing. These scripts neither discover nor establish production connections. Commands below are instructions for an authorized operator, not evidence they were executed.
3. Inventory every existing `CONSULTORIO`/`PRO` clinic before first deployment. P5 does **not** infer subscription dates or payments from `created_at`, trial clocks, plans, or old `PLAN_CHANGE` audits. An old paid plan is not proof of payment or coverage. Review actual external evidence/references and approve an activation **from now** through the transactional application workflow. Never fabricate historical confirmations, references, coverage or monthly audit rows.
4. By default, any paid clinic whose four new fields are all null makes the entire migration fail and roll back. This protects an existing paid deployment from an accidental read-only rollout. If approved legacy clinics must temporarily remain unverified, record that explicit rollout decision and use `p5_allow_unverified_paid_read_only=true`. They retain the same plan/state and all four nulls; the application must expose `PAYMENT_PENDING` and enforce read-only access until a real activation. The flag is not payment verification, a backfill, or a permanent entitlement bypass. We have not inspected or assumed any production counts.
5. SQL-only runs do not activate those clinics. For a legacy-paid deployment, use the approved read-only cutover flag before recording reviewed activations, or rehearse a separately reviewed, atomic coverage cutover before opening traffic. Do not bypass the default refusal by temporarily changing plans or inventing dates. On a rerun, already-verified paid clinics with valid groups/FKs need no approval flag. Any remaining unverified paid clinic still causes default refusal.
6. Use separate DDL ownership and a least-privilege PostgreSQL runtime role. The runtime must not be an owner, superuser, role/database creator, replication/bypass-RLS user, member of an elevated/owner role, schema creator, or able to disable triggers/change `session_replication_role`. `ROLE_SAAS_OPERATOR` is an application role, not this DB role. Existing Render grants on existing public tables do not imply access to the new table/sequence. Review inherited/PUBLIC/default privileges as well as direct ACLs. Required explicit grants are below.

## Inspect and Migrate

Use a previously approved connection, not an unreviewed default `psql` destination. Replace `public` consistently with the reviewed schema; `p5_schema` is required. No `--single-transaction`, since the migration owns its transaction.

```sh
psql -X --no-password -v ON_ERROR_STOP=1 -c '\conninfo'
psql -X --no-password -v ON_ERROR_STOP=1 -v p5_schema=public <<'SQL'
SET search_path = :"p5_schema", pg_catalog;
SELECT current_database(), current_user, current_schema(), version();
SELECT plan, estado, count(*) FROM clinicas GROUP BY plan, estado ORDER BY plan, estado;
SELECT id, plan, estado FROM clinicas WHERE plan IN ('CONSULTORIO','PRO') ORDER BY id;
SELECT count(*) AS audit_rows FROM saas_clinic_audit;
SELECT attname, format_type(atttypid,atttypmod), attnotnull, attidentity
FROM pg_attribute WHERE attrelid='saas_clinic_audit'::regclass AND attnum>0 AND NOT attisdropped;
SELECT conname, convalidated, pg_get_constraintdef(oid) FROM pg_constraint
WHERE conrelid IN ('clinicas'::regclass,'saas_clinic_audit'::regclass) ORDER BY conrelid,conname;
SQL

psql -X --no-password -v ON_ERROR_STOP=1 -v p5_schema=public \
  -f scripts/p5-monthly-migration.sql

# ONLY after documented approval of the unverified-paid read-only cutover:
psql -X --no-password -v ON_ERROR_STOP=1 -v p5_schema=public \
  -v p5_allow_unverified_paid_read_only=true -f scripts/p5-monthly-migration.sql
```

The final two commands are alternatives, not a routine fallback sequence. Stop on any unexpected error. Only literal `true`/`false` are accepted for the approval parameter. Missing/nonexistent/system schemas fail closed. Locks are `ACCESS EXCLUSIVE`, with a 10-second lock timeout and 10-minute statement timeout. Failure rolls back DDL and data; identity sequences may have gaps. Reruns validate existing mappings/checks/keys rather than trusting `IF NOT EXISTS`. They do not alter existing rows, plans, enum definitions, trial clocks or audit history. A disabled P4 append-only trigger or a new audit action constraint requires explicit review, not silent repair.

## Exact SQL Contract

`clinicas` gains nullable, no-default `subscription_started_at TIMESTAMPTZ(6)`, `subscription_ends_at TIMESTAMPTZ(6)`, `subscription_anchor_day INTEGER`, `last_payment_id BIGINT`. Precision 6 matches Hibernate's `Instant` mapping. `p5_clinic_subscription` requires either all four null, or all four non-null with finite ordered dates, anchor 1..31, positive last ID and a paid plan. Null groups are valid for trial/free/approved legacy paid clinics. Composite `p5_clinic_last_payment_fk` links `(last_payment_id,id,plan)` to the payment's `(id,clinic_id,plan)` with `ON DELETE RESTRICT`. The clinic's start is overall continuous coverage start, not necessarily the last payment's period start.

`monthly_payments` columns (only `previous_payment_id` is nullable):

| Column | SQL Type | Contract |
| --- | --- | --- |
| `id` | `BIGINT GENERATED BY DEFAULT AS IDENTITY` | Primary key |
| `clinic_id` | `BIGINT` | Scalar history; **no clinic FK** |
| `plan` | `VARCHAR(20)` | `CONSULTORIO` or `PRO` |
| `operation` | `VARCHAR(20)` | `ACTIVATE` or `RENEW` |
| `payment_reference` | `VARCHAR(120)` | Global unique normalized reference |
| `amount` | `NUMERIC(10,2)` | Exactly `59.00` or `129.00`, respectively |
| `currency` | `VARCHAR(3)` | `PEN` |
| `billing_cycle` | `VARCHAR(10)` | `MONTHLY` |
| `period_started_at`, `period_ends_at`, `confirmed_at` | `TIMESTAMPTZ(6)` | Finite instants |
| `anchor_day` | `INTEGER` | 1..31, original Lima day preserved on early renewal |
| `actor_id` | `BIGINT` | Scalar history; no actor FK |
| `actor_email` | `VARCHAR(120)` | Actor snapshot |
| `reason` | `VARCHAR(500)` | 5..500 characters, no leading/trailing whitespace |
| `previous_payment_id` | `BIGINT NULL` | Composite same-clinic predecessor FK |
| `audit_id` | `BIGINT` | Unique scalar link/FK to an existing audit ID |

Keys are `uk_monthly_payment_reference` (exact name required for collision handling), `uk_monthly_payment_audit`, `uk_monthly_payment_id_clinic` and `uk_monthly_payment_id_clinic_plan`. Predecessor FK `p5_payment_previous_fk` uses `(previous_payment_id,clinic_id)` to `(id,clinic_id)`; `p5_payment_audit_fk` uses `audit_id` to `saas_clinic_audit(id)`. Both use `ON DELETE RESTRICT`. `idx_monthly_payment_clinic_confirmed` indexes `(clinic_id,confirmed_at,id)`. History survives clinic/actor deletion. Audit rows remain protected by P4. The incoming audit FK adds only PostgreSQL internal RI triggers; existing audit columns, checks, indexes and user trigger remain unchanged.

The reference must already be trimmed, uppercased and whitespace-collapsed by the server using a locale-independent rule. SQL accepts only ASCII letters/digits and `. _ / -`, separated by single ASCII spaces, total length 5..120; it rejects rather than silently normalizing input. The SQL amount check operates **after** PostgreSQL numeric scale coercion: the application must reject non-exact/over-precision submitted amounts before persistence, not rely on `NUMERIC(10,2)` to detect rounding.

The period check computes the exact next **calendar month in `America/Lima`**, maintaining local time and clamping the preserved anchor to that month's final day. It also checks the start is on that month's clamped anchor day. January 31 -> February 28 -> March 31 (February 29 in leap years), not a fixed 30 days. UTC dates can differ from Lima dates; `2027-05-01T02:00Z` is April 30 locally. All new Java date fields must be `Instant`, not `LocalDateTime`; existing P4 `created_at TIMESTAMP(6) WITHOUT TIME ZONE` remains its original UTC convention.

The statement-level `p5_monthly_payment_append_only` trigger rejects UPDATE/DELETE/TRUNCATE, including zero-row mutations, and is `ENABLE ALWAYS`, including replica mode. `@Immutable` alone is not protection. PostgreSQL owners/superusers can still remove/disable enforcement; restrict administration and retain external backups/audit controls. A standalone TRUNCATE can also fail earlier because of incoming FKs; do not use CASCADE to circumvent that protection.

**Java coordination:** `MonthlyPayment.operation` has length 20. Its reference/audit unique names, history index, numeric precision, precision-6 Instant fields and clinic column names match SQL. The migration can augment only a catalog-verified, empty Hibernate bootstrap table with the expected primary key, two named uniques, history index and exact four-value plan-enum check. It retains that redundant enum check and adds the stricter P5 keys/checks; populated or partially migrated bootstrap histories fail for review. Canonical P5 reruns still reject missing or weakened checks. SQL's additional composite uniques/FKs/checks are intentional, even if not annotated.

## Required Runtime Grants

The migration deliberately changes no deployment-specific role grants. After it succeeds, a DBA must choose/review the real runtime role, revoke dangerous effective privileges (including inherited and PUBLIC grants), and grant schema access, table SELECT/INSERT, and identity-sequence USAGE **before** enabling the application. Do not grant UPDATE/DELETE/TRUNCATE, table ownership, sequence UPDATE, schema CREATE, or broad DDL. Existing clinic/audit privileges must remain least-privilege; review audit SELECT/INSERT and its sequence USAGE too, without changing P4's table shape.

Run the following as the reviewed DDL/grant owner on the already verified connection. The prompts supply identifiers, not secrets. This block intentionally fails if the selected role still inherits mutation/DDL privileges. Resolve those grants explicitly with the DBA, never grant membership in an owner role just to fix access.

```sql
\set ON_ERROR_STOP on
\prompt 'Reviewed application schema: ' p5_schema
\prompt 'Reviewed runtime PostgreSQL role (not application ROLE_SAAS_OPERATOR): ' p5_runtime_role
BEGIN;
SET LOCAL search_path = :"p5_schema", pg_catalog;
SELECT set_config('p5.grant_schema', :'p5_schema', true),
       set_config('p5.runtime_role', :'p5_runtime_role', true);
DO $$
DECLARE runtime text := current_setting('p5.runtime_role'); r record; seq text;
BEGIN
    IF current_schema() IS DISTINCT FROM current_setting('p5.grant_schema')
       OR current_schema() IN ('pg_catalog','information_schema')
       OR current_schema() LIKE 'pg\_%' ESCAPE '\' THEN
        RAISE EXCEPTION 'Select a reviewed application schema';
    END IF;
    SELECT * INTO STRICT r FROM pg_roles WHERE rolname=runtime;
    IF r.rolsuper OR r.rolcreatedb OR r.rolcreaterole OR r.rolreplication OR r.rolbypassrls
       OR EXISTS (SELECT 1 FROM pg_roles elevated WHERE elevated.oid<>r.oid
           AND pg_has_role(r.oid,elevated.oid,'MEMBER'))
       OR EXISTS (SELECT 1 FROM pg_class WHERE relowner=r.oid)
       OR EXISTS (SELECT 1 FROM pg_proc WHERE proowner=r.oid)
       OR EXISTS (SELECT 1 FROM pg_namespace WHERE nspowner=r.oid)
       OR EXISTS (SELECT 1 FROM pg_database WHERE datdba=r.oid)
       OR has_database_privilege(runtime,current_database(),'CREATE')
       OR EXISTS (SELECT 1 FROM pg_namespace WHERE has_schema_privilege(runtime,oid,'CREATE'))
       OR has_parameter_privilege(runtime,'session_replication_role','SET') THEN
        RAISE EXCEPTION 'Runtime has ownership, membership or elevated privileges; review explicitly';
    END IF;
    seq := pg_get_serial_sequence(format('%I.monthly_payments',current_schema()),'id');
    IF seq IS NULL THEN RAISE EXCEPTION 'Missing payment identity sequence'; END IF;
    EXECUTE format('REVOKE ALL ON TABLE monthly_payments FROM PUBLIC');
    EXECUTE format('REVOKE ALL ON TABLE monthly_payments FROM %I',runtime);
    EXECUTE format('REVOKE ALL ON SEQUENCE %s FROM PUBLIC',seq);
    EXECUTE format('REVOKE ALL ON SEQUENCE %s FROM %I',seq,runtime);
    EXECUTE format('GRANT USAGE ON SCHEMA %I TO %I',current_schema(),runtime);
    EXECUTE format('GRANT SELECT, INSERT ON TABLE monthly_payments TO %I',runtime);
    EXECUTE format('GRANT USAGE ON SEQUENCE %s TO %I',seq,runtime);
    IF has_table_privilege(runtime,'monthly_payments','UPDATE')
       OR has_table_privilege(runtime,'monthly_payments','DELETE')
       OR has_table_privilege(runtime,'monthly_payments','TRUNCATE')
       OR has_table_privilege(runtime,'monthly_payments','TRIGGER')
       OR has_table_privilege(runtime,'monthly_payments','REFERENCES')
       OR has_any_column_privilege(runtime,'monthly_payments','UPDATE')
       OR has_any_column_privilege(runtime,'monthly_payments','REFERENCES')
       OR has_sequence_privilege(runtime,seq,'UPDATE') THEN
        RAISE EXCEPTION 'Dangerous effective grants remain';
    END IF;
END $$;
COMMIT;
```

This conservative block rejects all memberships, even harmless ones, to require review instead of guessing Render role design. Role attributes/memberships and object ownership/default privileges can change later; treat them as recurring deployment checks. On PostgreSQL versions without `has_parameter_privilege`, use a DBA-reviewed equivalent/version gate, not an unchecked removal. PostgreSQL 18.4 was used for local regression. The column-level checks also fail if independently granted UPDATE/REFERENCES survive the table-level revokes; revoke those explicitly after review. Review runtime-accessible SECURITY DEFINER functions/procedures and schema/database ownership so they cannot indirectly mutate history or disable controls. The block does not certify arbitrary pre-existing DB security.

## Application Invariants

These are release acceptance requirements, not claims that the SQL regression has tested Java/HTTP:

- Lock the clinic and atomically insert the new P4 audit row, immutable payment, and clinic update. Audit `action` remains generic `VARCHAR(30)`, values `MONTHLY_ACTIVATED`/`MONTHLY_RENEWED`; never add an audit action enum/check or reshape P4. SQL ensures the audit ID exists/is unique; the server must ensure matching clinic, actor, plan, reason, timestamp and action. Audit and payment must commit or roll back together.
- Capture the displayed `lastPaymentId` in the client's `expectedPaymentId` before confirmation. Under the clinic lock, compare it to the current ID (null for first activation); stale new commands fail without extending coverage. Normalize reference before lookup/insert. A matching replay returns the original result without another payment/audit/extension; different payload/clinic/operation under the same global reference conflicts. Handle a race by the exact `uk_monthly_payment_reference`, not a generic success for any integrity failure. Rehearse ordering of idempotency lookup and optimistic-version checks in the final service.
- Activation of an unverified clinic starts at server `now` with the Lima day as new anchor. Early renewal extends from the existing exclusive end, keeps the anchor and original `subscription_started_at`; it does not reset coverage from confirmation time. At/after expiry, renewal restarts from server `now` with a new anchor and coverage start. An active paid subscription cannot change plan before the current end. Expiry is end-exclusive (`now >= end` means read-only); stale/missing/invalid payment metadata fails closed.
- A payment does not reactivate administrative `SUSPENDIDA`. Keep trial timestamps/old audits unchanged, and enforce payment-pending/expired/suspended access on the server, not only UI. No clinical access is granted to SaaS operators.
- These are manual records of confirmed payments only, not invoices, payment processing, automatic charges, refunds, proration, IGV calculation, or a statement that prices include/exclude tax. Final tax policy is required **before issuing invoices**, but does not block manually recording the fixed plan amounts.

Use a coherent UTC JVM/JDBC timestamp configuration for the verified runtime;
business-month boundaries and UI display still use America/Lima explicitly.
The real-browser rehearsal verified UTC JVM and UTC JDBC handling. Do not mix a
Lima-default JVM with an independently forced UTC JDBC conversion for the legacy
timezone-free audit columns: that configuration shifted payment/audit snapshots
in a diagnostic run. Review existing clinical date expectations before changing
an established deployment timezone. No legacy timestamp conversion is part of P5.

## Integrated Validation

The final isolated build passed 321 backend tests and the 70% service-line-coverage
gate (approximately 86.6%). A separate real PostgreSQL run passed 187 regression
cases, including 71 monthly-billing cases with Open-in-View disabled. All six SQL
regression groups passed, including exact fresh Hibernate bootstrap augmentation,
populated reruns, legacy refusal and data/permission preservation.

The frontend passed 83 Node tests and `build:render`. A real-browser run against
the migrated, restricted-runtime PostgreSQL application passed 145 assertions:
activation, early/expired renewal, server prices, exact references/versions,
committed-response-loss retry without duplicate coverage, independent suspension,
read-only expiry/pending states, histories, authorization and 320/390/1440 layouts.
The run used real HTTP responses, not response mocks. Separate expired/pending
test fixtures were local SQL setup, not real financial confirmations. All scratch
services and clusters were stopped and removed; production was not contacted.

Private evidence on this workstation is under
`/var/folders/sx/_6ljw37n2_j9hk2f_vtqplj40000gn/T/opencode/p5-full-e2e.WyqZqmDr/`
(`REPORT-OSIV-FALSE.md`) and `p5-osiv-pg.QN65epq1/`. These temporary artifacts are
not permanent CI storage. The build used the unchanged Maven dependencies,
compiler and coverage settings with output relocated to a private scratch POM
to avoid duplicated class artifacts in the synced workspace. No test or class
was excluded to pass coverage. Re-run verification on the final release source.

## Verification and Rollback

With writers still stopped, compare baseline clinic/trial/role/audit data and counts; inspect `\d+` and `\dp` for the reviewed schema's `clinicas`, `monthly_payments` and `saas_clinic_audit`. Check every FK/check is validated and both append-only triggers have `tgenabled='A'`. Inventory remaining null paid groups, expected read-only behavior, owners, direct/column/inherited ACLs and the actual identity sequence. Test grants using the real runtime login only in the approved staging environment; do not test mutations against production history. Rehearse concurrent same-reference and stale-version commands, lost-response retries, January/leap/Lima boundaries, exact expiry, expired restart, active-plan-change rejection and suspended clinic payments in the final integrated application.

Rollback: keep traffic closed/read-only and preserve the new columns, keys, immutable payment/audit history. Prefer a reviewed forward fix. Do not drop history or constraints, fabricate dates, narrow existing enums, reenable writes with an old binary that ignores expiry, or assume destructive schema rollback is safe. Follow the approved restore procedure only if necessary and account for any confirmations accepted after the backup.

## Local Regression

Prerequisites: `python3`, `psql`, `initdb`, `pg_ctl`, `createdb` on PATH. No Maven or Python packages. Run from the repository root:

```sh
command -v python3 psql initdb pg_ctl createdb
psql --version
ls -ld /var/folders/sx/_6ljw37n2_j9hk2f_vtqplj40000gn/T/opencode
PYTHONDONTWRITEBYTECODE=1 python3 scripts/p5-test.py \
  --scratch-parent /var/folders/sx/_6ljw37n2_j9hk2f_vtqplj40000gn/T/opencode
```

On another workstation, supply an existing approved scratch parent. The runner verifies it, creates its own `0700` temporary cluster, disables TCP, uses private Unix sockets and trust auth only within that private disposable directory, and ignores inherited PostgreSQL connection/service/options/password settings. No service, passfile, application configuration or environment files are read. It starts/stops only its own cluster and removes only its own scratch directory, including on ordinary assertion failure. Never point it at an application cluster; it accepts no external DB connection argument.

Coverage: atomic default legacy refusal and explicit approval; missing/invalid schema/approval and quoted-schema isolation; migration twice with and without legacy approval, including a verified paid clinic; original plans/trial clocks/roles/audit rows and P4 user-visible shape unchanged; prices/currency/cycle/reference grammar and global replay uniqueness; unique audit link; exact calendar periods including leap year and Lima-vs-UTC day; cross-clinic predecessor/last-ID and cross-plan last-ID denial; partial/invalid clinic groups; append-only UPDATE/DELETE/TRUNCATE under origin/replica, including zero-row mutations; private non-owner runtime SELECT/INSERT/sequence usage and mutation/DDL/replication/unrelated-table denial; drifted columns/defaults/checks/indexes/RLS rejected, including a weakened same-named period check and an audit action check; history retained after clinic deletion and populated reruns. Application-level idempotency, authorization and concurrent transactions remain integration release gates.
