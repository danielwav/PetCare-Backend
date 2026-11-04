# P4: SaaS Operator Migration and Operations

These are explicit operator-run PostgreSQL scripts, not startup migrations. Local SQL regression is not proof of production readiness or API authorization. Record each deployment and its verification separately.

## Mandatory Preflight

1. Complete P2/P3 first. Obtain change approval, a verified backup/restore plan, and baseline counts/exports of `roles`, `usuarios`, `usuarios_roles`, `clinicas` and any existing audit rows. Stop every application writer, background worker and registration endpoint for migration/provisioning. Do not enable demo seeding.
2. Use a reviewed PostgreSQL service in a protected `~/.pg_service.conf`, with an explicit host, database, user and schema. For remote connections use `sslmode=verify-full` and the correct CA, not an unverified production host. Use an approved credential broker or a `0600` passfile outside the repository; never credentials in connection URLs, shell arguments, examples, git, or `PGPASSWORD`.
3. Separate the DDL owner, out-of-band account provisioner and least-privilege application DB login. `ROLE_SAAS_OPERATOR` is an **application role row**, not a PostgreSQL `CREATE ROLE`, DB login, owner or superuser. The runtime login must not own the audit table/function/schema or have DDL, trigger-disabling, replication, or superuser privileges. Grant only required runtime privileges, including audit `SELECT`/`INSERT` and identity-sequence `USAGE`; revoke audit `UPDATE`, `DELETE`, `TRUNCATE` from it and inherited/public grants. Review ownership and default privileges, not only direct grants. The scripts do not guess deployment-specific DB login names or change their grants.
4. **JWT is a release gate.** `application.properties` contains a public development `JWT_SECRET` fallback. The `prod` profile overrides it with `${JWT_SECRET}` without a fallback, so missing/empty keys prevent startup. Before enabling P4, inject a unique cryptographically random Base64 JWT key from the secret manager into every replica, validate it, and confirm the effective Spring configuration has no higher-priority override. Explicitly supplying the public development key is still unsafe; configuration cannot prove key entropy. Do not rely on its length or successful startup. A deployment without the verified key is blocked.
5. Require `SPRING_PROFILES_ACTIVE=prod`, `JPA_DDL_AUTO=validate`, and `APP_SEED_DATA_ENABLED=false` in the effective runtime configuration. The default is Hibernate `update`; it is not an explicit migration or a safe substitute. Do not start the application before the reviewed migration.
6. Review secret handling **before provisioning**: no `set -x`, `psql -a/-e/-b/-L`, terminal recording, SQL tracing, shell/history capture, or HTTP request/body/token logging. The hash is sent in an SQL `INSERT`, so DB/proxy/pgaudit/observability statement logging can capture it even when psql does not echo it. Have the DBA approve a restricted provisioning session with secret-bearing statements/errors excluded or reliably redacted. Review `log_statement`, `log_min_error_statement`, duration/sample logging, parameter logging, pgaudit and proxy settings; do not disable organization-wide auditing casually. If safe handling cannot be guaranteed, **stop**. Treat hashes as sensitive too.

Run this JWT gate inside the same protected deployment environment that will start the JVM. It prints no key. Prefer 64 random bytes, generated/stored by the secret manager; do not paste the key into a command line. Secret rotation invalidates existing access and refresh tokens across all replicas and must be coordinated.

```sh
python3 - <<'PY'
import base64
import os
from pathlib import Path
import re
import sys

try:
    secret = os.environ.get('JWT_SECRET', '')
    decoded = base64.b64decode(secret, validate=True)
    source = Path('src/main/resources/application.properties').read_text()
    fallback = re.search(r'security\.jwt\.secret=\$\{JWT_SECRET:([^}]+)\}', source).group(1)
    if len(decoded) < 64 or decoded == base64.b64decode(fallback):
        raise ValueError()
except Exception:
    sys.exit('BLOCKED: inject a unique Base64 JWT_SECRET of at least 64 random bytes; no dev fallback')
print('JWT environment gate passed; verify effective Spring overrides before release')
PY
```

Do not print the environment, enable an actuator environment dump, or include secret material in deployment tickets. This gate checks presence, encoding, length and the known fallback, not entropy or effective property precedence. Keep it a required successful deployment step, not an optional checklist.

## Inspect and Migrate

From the repository root, use a DBA-reviewed service name instead of `petcare-reviewed-ddl`. The application schema below is `public`; replace it consistently if needed.

```sh
export PGSERVICE=petcare-reviewed-ddl
psql -X --no-password -v ON_ERROR_STOP=1 -c '\conninfo'
psql -X --no-password -v ON_ERROR_STOP=1 <<'SQL'
SET search_path = public, pg_catalog;
SELECT current_database(), current_user, current_schema(), version();
SELECT id, name, active FROM roles ORDER BY id;
SELECT conname, convalidated, pg_get_constraintdef(oid)
FROM pg_constraint WHERE conrelid = 'roles'::regclass ORDER BY conname;
SELECT count(*) AS users, count(*) FILTER (WHERE active) AS active_users FROM usuarios;
SELECT count(*) AS assignments FROM usuarios_roles;
SELECT count(*) AS clinics FROM clinicas;
SELECT to_regclass('saas_clinic_audit');
SQL

psql -X --no-password -v ON_ERROR_STOP=1 -v p4_schema=public \
  -f scripts/p4-saas-migration.sql
```

Do not use `--single-transaction`: the scripts manage their own transaction boundaries. Both require `p4_schema`; an absent/system schema or absent input fails with a nonzero exit. A failed transaction rolls back DDL and row changes; PostgreSQL sequence values can still have gaps. Stop and review errors, never work around them by broadly dropping constraints.

The migration locks `roles` and the audit table, with a 10-second lock timeout and 10-minute statement timeout. It replaces **only validated, catalog-proven, single-column `roles.name` enum-list checks** containing exactly the four existing roles, optionally the fifth operator role. Constraint names and enum ordering are not assumed. Custom name, unknown enum, multi-column and unvalidated checks are preserved. If there is no recognized check, a name collision, or a preserved check blocks the new role, the transaction fails for explicit schema review.

It inserts `ROLE_SAAS_OPERATOR` only if absent. It never changes or reactivates an existing role, creates a user, assigns a role, promotes an account, changes a clinic/plan/trial clock, deletes data or grants API access. Rerunning preserves existing rows and audit history.

`saas_clinic_audit` matches the current `SaasClinicAudit` entity: identity `bigint` key; scalar clinic/actor IDs; actor email; action; old/new plan and state; reason; UTC `timestamp(6) without time zone`. There are deliberately **no foreign keys** to mutable users/clinics, so history survives account/clinic removal. It validates an existing table's columns, nullability, identity/primary key, absence of FKs, enum checks and the `(clinic_id, created_at, id)` index instead of silently trusting `IF NOT EXISTS`. Unexpected existing mappings require review.

The statement-level `ENABLE ALWAYS` trigger rejects **UPDATE, DELETE and TRUNCATE**, including no-op mutations. `INSERT` remains allowed. Hibernate `@Immutable` alone is not enforcement. A DB owner/superuser can bypass/drop a trigger or table; the trigger is not tamper-proof against administrators. Enforce runtime ownership separation, external backups and restricted administration. The migration does not implement retention, redact existing history or auto-delete it.

Verify with the DDL service, compare baseline counts/data, then separately review runtime grants:

```sh
psql -X --no-password -v ON_ERROR_STOP=1 <<'SQL'
SET search_path = public, pg_catalog;
SELECT id, name, active FROM roles ORDER BY id;
SELECT conname, convalidated, pg_get_constraintdef(oid)
FROM pg_constraint WHERE conrelid IN ('roles'::regclass, 'saas_clinic_audit'::regclass)
ORDER BY conrelid, conname;
SELECT tgname, tgenabled, pg_get_triggerdef(oid)
FROM pg_trigger WHERE tgrelid = 'saas_clinic_audit'::regclass AND NOT tgisinternal;
SELECT indexdef FROM pg_indexes WHERE schemaname = current_schema()
AND tablename = 'saas_clinic_audit';
SELECT count(*) AS users FROM usuarios;
SELECT count(*) AS assignments FROM usuarios_roles;
SELECT count(*) AS clinics FROM clinicas;
SELECT count(*) AS audit_rows FROM saas_clinic_audit;
\dp saas_clinic_audit
SQL
```

## New Account Only

Provisioning is a **separate, approved, out-of-band operation** after migration, not registration, a seed, an API promotion, or startup behavior. Choose a brand-new individually owned email and unique temporary password; there are no default credentials. Never reuse or promote an existing active, inactive, clinic, demo or admin account. An existing case/whitespace-normalized email causes refusal without modifying it. A duplicate invocation is deliberately rejected, not idempotently treated as success.

The helper uses hidden `getpass` input, bcrypt cost 12 with a fresh salt, and a private stdin pipe to `psql -X --no-password`. It never puts the password/hash in argv, environment, files or normal output. Python process memory still contains credentials; run on a trusted workstation without debuggers, core dumps or session capture. Use a unique temporary password of at least 16 characters and at most 72 UTF-8 bytes; bcrypt truncation is not allowed. The SQL entry point accepts only a 60-character bcrypt hash at cost 10-16 and rejects plaintext/weak or excessive-cost inputs. Syntactic hash validation cannot prove password strength or uniqueness.

Prepare an isolated tool environment outside the repository. Check the parent directory with `ls` before creating it. The temp parent below is already available in this workspace; select an approved private tools location on another workstation.

```sh
ls /var/folders/sx/_6ljw37n2_j9hk2f_vtqplj40000gn/T/opencode
P4_TOOLS=$(mktemp -d /var/folders/sx/_6ljw37n2_j9hk2f_vtqplj40000gn/T/opencode/p4-tools.XXXXXX)
python3 -m venv "$P4_TOOLS/venv"
"$P4_TOOLS/venv/bin/python" -m pip install 'bcrypt==5.0.0'
export PGSERVICE=petcare-reviewed-provisioner
"$P4_TOOLS/venv/bin/python" scripts/p4-provision-operator.py
```

Review the helper's actual destination output and requested schema before confirming `CREATE`. Do not pass passwords/hashes with `psql -v`, command substitutions, `openssl passwd`, or inline shell assignments. OpenSSL's usual `passwd` modes are not a Spring-compatible BCrypt provisioning workflow. Advanced tooling may feed the required psql variables `p4_schema`, `operator_email`, `nombres`, `apellidos`, `password_hash` through a private pipe, using psql quoting and the same logging controls; the helper is the supported interactive path.

The SQL locks roles against changes and serializes user/assignment writes. It requires the migration and exactly one **already active** operator role. It never creates/reactivates a missing/disabled role. It creates exactly one new account with `active=true`, `force_password_change=true`, `clinica_id=NULL`, null activation token/expiry/phone and **only** `ROLE_SAAS_OPERATOR`. Failure rolls back the new account and assignment. It never reveals the generated account hash.

Record the approved new account ID and verify those postconditions without selecting the password. Deliver the temporary password through the approved separate encrypted channel, never the migration ticket. First login must complete `POST /api/auth/change-password`; SaaS access is denied until the flag clears. Use HTTPS and a secure API client, not token/password literals in `curl` argv. Re-authenticate and verify role isolation after the change.

## API Separation

The operator can use only the required auth routes (`GET /api/auth/me`; `POST /api/auth/login`, `/refresh`, `/change-password`, `/logout`) and `/api/saas/**`. Route allowance is not a promise that every allowed route has an implemented handler. Current SaaS endpoints are:

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/api/saas/clinicas` | Paged clinic metadata search |
| PATCH | `/api/saas/clinicas/{id}/plan` | Explicit plan change with reason |
| PATCH | `/api/saas/clinicas/{id}/estado` | Explicit state change with reason |
| GET | `/api/saas/clinicas/{id}/auditoria` | Paged audit history |

No clinical/tenant API access, patient/owner/staff access, self-service registration, role assignment or privilege promotion is conferred. `ROLE_ADMIN` and all other clinic roles must be denied SaaS access. Mixed-role or clinic-linked operator accounts are invalid and must fail closed, not receive combined privileges. Do not attach a clinic or extra roles to make a failed operator login work. Plan/state writes and audit insertion are transactional; verify audit before/after values, actor ID/email, reason and timestamp in staging.

## Disable and Revoke

There is no operator management/promotion API. To disable one operator, stop/restrict access as needed and use the protected out-of-band service. In an interactive `psql -X --no-password -v ON_ERROR_STOP=1` session, supply the **reviewed numeric account ID**, inspect the locked account, and confirm exactly one update. The guard below refuses an ordinary clinic/mixed-role account:

```sql
\prompt 'Reviewed operator account ID: ' operator_id
BEGIN;
SET LOCAL search_path = public, pg_catalog;
SET LOCAL lock_timeout = '10s';
LOCK TABLE roles IN SHARE MODE;
LOCK TABLE usuarios, usuarios_roles IN SHARE ROW EXCLUSIVE MODE;
SELECT u.id, u.email, u.active, u.force_password_change, u.clinica_id, r.name
FROM usuarios u JOIN usuarios_roles ur ON ur.usuario_id = u.id
JOIN roles r ON r.id = ur.rol_id WHERE u.id = :'operator_id'::bigint FOR UPDATE OF u;
UPDATE usuarios u SET active = false, force_password_change = true,
    activation_token = NULL, token_expiry = NULL
WHERE u.id = :'operator_id'::bigint AND u.clinica_id IS NULL
AND (SELECT count(*) FROM usuarios_roles WHERE usuario_id = u.id) = 1
AND EXISTS (SELECT 1 FROM usuarios_roles ur JOIN roles r ON r.id = ur.rol_id
            WHERE ur.usuario_id = u.id AND r.name = 'ROLE_SAAS_OPERATOR')
RETURNING u.id, u.email, u.active, u.force_password_change;
-- Inspect the returned row, then issue COMMIT separately; otherwise ROLLBACK.
```

Keep the application role assignment on the disabled account for investigation; setting `active=false` is the revocation. To permanently remove the grant, after disabling and explicit review, delete only that account's `usuarios_roles` row for `ROLE_SAAS_OPERATOR` in a protected transaction. Never convert it into a clinic/admin account or delete its audit history. Re-enablement is a new approval/credential-reset workflow, not rerunning the provisioner.

For emergency **all-operator** shutdown, an approved DBA can set `roles.active=false` only for `ROLE_SAAS_OPERATOR`. Verify one row changed; existing users and audit stay intact. Migration reruns do not reactivate it and provisioning refuses it. This does not disable clinic roles or replace an individual account investigation.

JWT authentication and refresh resolve the current account/roles from the DB, so disabled accounts/roles are denied on subsequent requests, including old access/refresh tokens. Already-running requests may finish. Logout/password change are not a cryptographic revocation of already issued stateless JWTs. Do not re-enable an account/role while compromised tokens could still be valid; rotate/reset credentials and coordinate signing-key rotation or wait out access **and refresh** lifetimes under the incident procedure. Verify old-token denial and refresh denial in staging; never paste tokens into tickets/logs.

## Local Regression

Installed tools were verified locally: PostgreSQL/psql **18.4**, Java **21** and Python **3.14**. No existing local application database or production endpoint is used. The following creates a private `0700` scratch cluster, with Unix sockets only and **no TCP listener**. Trust authentication is appropriate only inside this disposable private directory, not for a shared/production server. Verify the parent and tools first, and choose a free port.

```sh
command -v psql postgres initdb pg_ctl pg_isready createdb dropdb python3
psql --version
postgres --version
ls /var/folders/sx/_6ljw37n2_j9hk2f_vtqplj40000gn/T/opencode
P4_SCRATCH=$(mktemp -d /var/folders/sx/_6ljw37n2_j9hk2f_vtqplj40000gn/T/opencode/p4-pg.XXXXXX)
initdb -D "$P4_SCRATCH/data" -U p4_scratch --auth=trust --no-locale --encoding=UTF8
pg_ctl -D "$P4_SCRATCH/data" -l "$P4_SCRATCH/server.log" \
  -o "-h '' -k $P4_SCRATCH -p 55484" -w start
export PGHOST="$P4_SCRATCH" PGPORT=55484 PGUSER=p4_scratch PGDATABASE=p4_scratch_regression
unset PGSERVICE PGHOSTADDR PGOPTIONS PGPASSWORD
createdb --no-password "$PGDATABASE"
PYTHONDONTWRITEBYTECODE=1 python3 scripts/p4-test.py
dropdb --no-password "$PGDATABASE"
pg_ctl -D "$P4_SCRATCH/data" -m fast -w stop
```

The driver requires an explicit loopback/Unix socket host and `p4_scratch_*` database, ignores inherited PG service/host-address/options/password settings, refuses an existing `p4_fixture`, and removes only the fixture it creates. `p4-test.sql` can also be invoked directly with `psql -X --no-password -v ON_ERROR_STOP=1 -f scripts/p4-test.sql` on that same scratch connection, but leaves its fixture for inspection and does not run the Python driver's negative cases. Never run either test on production. If a SQL-only run fails, inspect/drop only your own scratch database, never a pre-existing schema.

Coverage includes migration twice; existing users, roles (including inactive ones), assignments, clinic data and audit history unchanged; known enum checks replaced and custom/multi-column/unvalidated/unknown checks preserved; unknown role and audit enum rejection; audit UPDATE/DELETE/TRUNCATE rejection; isolated new-account provisioning; normalized duplicate existing/new/inactive-email rejection; missing/invalid inputs; BCrypt cost bounds; disabled-role refusal; atomic migration failure on unknown blocking checks, missing recognized enum, table/index drift; and psql quoting of names/email containing apostrophes/backslashes. Test hashes are synthetic scratch-only data, not default login credentials. Python `bcrypt` is needed only by the interactive provisioner, not the regression driver.

Before release, also run the P4 application authorization/service tests against the final integrated application, then stage the complete login, forced-password-change, role-isolation, audit and revocation flows. These scripts do not deploy/restart the service, alter the existing application implementation, or certify production security. Rollback means keep traffic closed, disable operator access, retain the widened role enum/audit history, and follow the approved restore/forward-fix procedure; do not narrow the enum or drop populated audit tables to run an older binary.
