# P2 Tenant Catalog Migration

Run `p2-tenant-migration.sql` explicitly **before deploying P2 or starting Hibernate schema update**. This is not an automatic startup migration. No production SQL was executed as part of implementation.

For a fresh installation, first bootstrap the entity schema in an isolated database without exposing application traffic, stop the application, then run this migration before opening access. Hibernate alone creates case-sensitive composite constraints, not the PostgreSQL `upper(nombre)` unique indexes. The migration is required for fresh installations too.

## Preparation

1. Back up the database and verify restoration. Rehearse on an isolated restored copy, including a second run to confirm idempotency.
2. Stop all application instances, jobs, and other database writers. Allow a maintenance window: the script takes exclusive table locks and builds indexes transactionally, not concurrently.
3. Confirm the target database and schema. Entity mappings are `clinicas`, `duenios`, `servicios`, `mascotas`, `citas`, `veterinarios`, `asistentes`, `usuarios`, `vacunas`, and `vacunas_mascota`. Camel-case fields use the application's snake-case physical naming strategy. Do not run against a differently mapped schema without reviewing it.
4. Review the preflight below. A legacy null vaccine catalog requires an existing clinic with slug `demo`. If absent, provision/review that clinic explicitly using the application's clinic model before migration; the script does not invent production clinic records.
5. Resolve ambiguous null clinic IDs explicitly using audited ownership evidence. The only automatic owner/service/pet/appointment/staff backfill is the existing legacy rule when the **only** clinic is `demo`. Multi-clinic null ownership aborts the whole migration, even if some associations appear to suggest a clinic. Do not assign those rows wholesale to demo.

## Connection And Run

Use standard PostgreSQL environment variables supplied by your credential manager or shell environment: `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD` (or `PGPASSFILE`), and appropriate `PGSSLMODE`. Never place passwords or connection URLs in repository files or command arguments. Set `PGOPTIONS` to `-c search_path=YOUR_REVIEWED_SCHEMA,pg_catalog` with your actual reviewed schema, not a guessed default.

```sh
psql -X -v ON_ERROR_STOP=1 -f scripts/p2-preflight.sql
psql -X -v ON_ERROR_STOP=1 -f scripts/p2-tenant-migration.sql
psql -X -v ON_ERROR_STOP=1 -f scripts/p2-preflight.sql
```

`p2-preflight.sql` is read-only and can run before `vacunas.clinica_id` exists. Confirm connection identity and search path in its output before running the migration. The migration requires table/constraint/index alteration permissions and insertion into the vaccine identity sequence. It fails on schema differences rather than dropping unrelated objects. Do not bypass failures or use `CASCADE`.

## Behavior

- One transaction includes DDL, backfill, cloning, and history remapping. Any error rolls back table data and schema changes. PostgreSQL sequence values can advance on rollback; gaps are harmless and do not mean records were lost.
- Catalog discovery removes only single-key, global unique constraints/plain nonpartial unique indexes on `servicios.nombre`, `duenios.email`, `duenios.numero_documento`, and `vacunas.nombre`. Composite, primary, partial, expression, and unrelated unique objects are preserved. Dependent foreign keys cause a safe failure, never a cascading drop.
- Creates `uk_servicios_clinica_nombre`, `uk_duenios_clinica_email`, `uk_duenios_clinica_documento`, and `uk_vacunas_clinica_nombre`, plus unique `(clinica_id, upper(nombre))` indexes for service/vaccine IgnoreCase validation. Owner email/document repository validation is exact matching, so no invented case-insensitive email rule is added.
- Adds the nullable vaccine clinic column and a clinic foreign key if needed. Originals with null clinic become demo catalog records, retaining IDs and every catalog field. For each non-demo pet clinic with existing applications of a legacy vaccine, clones that catalog entry including active state and original timestamps, then changes only the corresponding `vacunas_mascota.vacuna_id` references.
- Application row IDs, dates, lots, notes, veterinarian links, appointment links, and timestamps are untouched. No catalog, owner, service, pet, appointment, or historical application is deleted. An existing same-name target vaccine aborts rather than merging possibly distinct meanings. Duplicate tenant keys, including case-only service/vaccine name collisions, abort rather than deleting or renaming records.
- Final checks reject historical vaccine applications whose catalog clinic differs from their pet clinic. Review such records using historical evidence; the script never silently repairs already tenant-owned vaccines.
- Pet/owner clinic mismatches abort migration instead of permitting cross-clinic owner history access. Resolve historical ownership explicitly before retrying.
- Reruns do not clone already migrated vaccines. Existing expected constraints/indexes are checked rather than blindly accepted. Index comparison uses PostgreSQL's rendered `upper(nombre)` expression.
- New clinics intentionally start with empty service/vaccine catalogs. Create their catalogs manually through tenant-scoped application operations. Only historical non-demo usage causes migration clones; no unused demo catalog is copied to other clinics.

## Verification And Recovery

Compare pre/post table counts: all counts except `vacunas` must be unchanged; its increase must equal the number of distinct `(legacy vaccine, non-demo pet clinic)` pairs. Keep a pre-migration history export or snapshot and compare application fields, allowing only the documented vaccine reference changes. Verify both matching-case and case-only service/vaccine duplicates are rejected within one clinic, and the same names/emails/documents work across different clinics. Start P2 only after checks pass; confirm demo seeds are idempotent and do not populate new clinics.

On a migration error, keep P2 stopped, inspect the exception, and resolve ownership/collisions with an audited, reviewed operation before retrying. Do not deploy old globally scoped code after a successful migration. After deployment, rollback requires a coordinated database restore/application rollback, not a destructive reverse script: globally unique keys may no longer be satisfiable without data loss.

## Local Regression

`p2-test.sql` creates a reduced entity-derived fixture in schema `p2_test`. Run it only on a fresh, disposable **local** database named `p2_migration_test`, with connection environment variables pointing to an isolated local PostgreSQL instance:

```sh
createdb p2_migration_test
psql -X -v ON_ERROR_STOP=1 -d p2_migration_test -f scripts/p2-test.sql
```

It refuses other database names and an existing fixture schema. It exercises migration from a catalog without `clinica_id`, reruns, clone counts/fields, application history, intentionally empty clinics, preservation of unrelated uniqueness/indexes, and tenant/case-insensitive uniqueness. This fixture is not a substitute for rehearsal on a restored production schema and data.
