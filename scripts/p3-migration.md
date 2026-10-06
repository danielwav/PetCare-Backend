# P3: Explicit Plan Migration and Demo Seeding

## Deployment Order

1. Complete the reviewed P2 tenant migration first. Back up the database and stop application writers.
2. Review the target schema and existing `clinicas` checks with the preflight queries below. Use an operator-provided connection, not credentials committed to the repository.
3. Run `p3-plan-migration.sql` **before starting the P3 application**. Hibernate schema update is not a substitute: an existing enum check can reject `CONSULTORIO` even after Java enum changes.
4. Set `APP_SEED_DATA_ENABLED=false` in production and start P3. Verify plans, trial dates, retained counts and usage/over-limit displays before reopening traffic.

```sh
PGOPTIONS='-c search_path=public' psql -X -v ON_ERROR_STOP=1 \
  -f scripts/p3-plan-migration.sql
```

Use the reviewed schema instead of `public` if needed. The migration locks only `clinicas`, uses a transaction and times out rather than waiting indefinitely. Stop writers first. Re-running it never changes populated trial clocks, `created_at`, `updated_at`, plans, staff or clinical data. It performs no deletions and gives no demo exemptions.

```sql
SELECT current_database(), current_schema();
SELECT conname, conkey, pg_get_constraintdef(oid)
FROM pg_constraint WHERE conrelid = 'clinicas'::regclass AND contype = 'c';
SELECT id, slug, plan, created_at FROM clinicas ORDER BY id;
```

Only catalog-proven, single-column `plan` checks matching Hibernate's enum-list shape containing `TRIAL`, `FREE`, `PRO` (optionally `CONSULTORIO`) are replaced. State, slug, multi-column and custom plan checks remain intact. If a custom check blocks `CONSULTORIO`, review it explicitly; do not broadly drop checks by guessed name. Unknown existing plans make the migration fail atomically.

Existing `TRIAL` rows with both dates absent receive `trial_started_at = created_at` and `trial_ends_at = created_at + interval '14 days'` once, not deployment time. Historical trials may already be expired. Timestamps use UTC semantics (`timestamp without time zone`, matching nullable Java `LocalDateTime`); confirm legacy `created_at` semantics before migration. Do not reinterpret or restart historical clocks automatically.

The date check requires both dates for TRIAL and exactly 14 days between populated dates. Nontrial plans may have both dates null, or retain the paired historical dates. Partial clocks or inconsistent durations fail validation; resolve those deliberately, never reset them to now. No database trigger or startup task restarts a trial.

After migration:

```sql
SELECT id, slug, plan, trial_started_at, trial_ends_at FROM clinicas ORDER BY id;
SELECT conname, pg_get_constraintdef(oid)
FROM pg_constraint WHERE conrelid = 'clinicas'::regclass AND contype = 'c';
```

## Seed Separation

`DataInitializer` retains `@Profile("!test")` and is conditional on `app.seed-data.enabled=true`, defaulting to disabled. `APP_SEED_DATA_ENABLED` is the environment form resolved by Spring for this condition. Use `APP_SEED_DATA_ENABLED=false` in production; do not enable it during deployment or migration. Independent `RoleInitializer` remains enabled and unaffected.

For a deliberately seeded disposable demo environment only:

```sh
APP_SEED_DATA_ENABLED=true ./mvnw spring-boot:run
```

Explicit seed creation gives a new demo clinic a UTC 14-day TRIAL. Existing demo plans and trial dates are not modified by seeding. The opt-in initializer retains legacy P2 demo seeding behavior, including known demo account updates, so it must not be enabled against production. Direct `createBean(DataInitializer.class)` tests deliberately bypass component-registration conditions and still run the seed.

## Existing Demo Usage

The deployed demo has 10 staff against the TRIAL cap of 5. Retain every account and record; report usage as over-limit rather than deleting, disabling or granting an unlimited/free-demo exception. While its TRIAL remains active, reads and ordinary clinical writes remain allowed, but further staff additions are blocked. Expired trials follow the normal P3 expiry rules. FREE is read-only, not unlimited; existing active PRO remains active. No self-service plan upgrade is authorized by this migration or initializer.

## Local Verification

`p3-test.sql` creates and removes a fixture schema in an **empty scratch database only**. It checks rerun clock preservation, CONSULTORIO acceptance, FREE/PRO preservation, unrelated constraint retention, invalid dates/plans and retention of 10 over-limit staff. It does not test application entitlement enforcement.

```sh
psql -X -v ON_ERROR_STOP=1 -h 127.0.0.1 -p 55473 -d YOUR_SCRATCH_DB \
  -f scripts/p3-test.sql
./mvnw -Dtest=DataInitializerTest test
./mvnw -DskipTests package
```

No production SQL is run automatically. The scripts are operator-invoked only and store no credentials.
