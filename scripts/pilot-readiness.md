# Clinic Pilot Validation

## Status

The corrected application passed a complete isolated browser/API pilot against a
new PostgreSQL database. Validation did not modify production. Follow the release
steps below and record deployment status and production checks separately.

The pilot used the actual Angular build, Spring Boot JAR, JWT authentication and
database responses. Browser requests to the configured remote API were bridged
to a fixed loopback backend before navigation. External network access and mail
delivery were blocked. No production accounts, databases, credentials or backups
were used. Test accounts and infrastructure were disposable.

## Corrections

- Editing an owner without `usuarioId` preserves the existing login association.
- Owner attention/history responses redact internal notes. Staff responses and
  the stored notes are unchanged; the owner's JSON field is null.
- Accounts with a mandatory password change cannot access operational APIs until
  the change completes. Login, refresh, current-user lookup and password change
  remain available. An existing password is verified as supplied, rather than
  incorrectly requiring it to already satisfy the new-password policy.
- Assistant phone storage accepts the same 8-20-character phone format as request
  validation. The explicit migration below widens the legacy nine-character field.
- Availability excludes elapsed and noncancelled booked intervals. Creation and
  rescheduling reject intervals that cross midnight instead of wrapping LocalTime.
- Appointment, attendance, no-show, veterinarian scheduling and pet-cancellation
  writers share the existing clinic lock. Authoritative reloads prevent stale
  state from undoing cancellations, completed attention or schedule changes.
- Frontend appointment/attention failures are visible and release loading states.
  Failed submissions retain values; duplicate submissions are blocked.
- Medication rows are saved as labeled text in `tratamiento`; vital signs are
  saved as labeled text in `observacionesClinicas`. Both composed fields retain
  their existing 1000-character limit. They are not new structured medical tables.
- The admin sidebar exposes the operational modules already authorized by the
  backend. Owner and SaaS-operator access remains separate.
- Staff user editing includes the required phone and preserves unchanged roles.
  Admin/owner profile edits persist through actual authorized API requests.
  Veterinarian/assistant self-profiles are explicitly read-only rather than
  falsely reporting a successful save.

## Verification

Backend full verification:

```sh
./mvnw clean verify
```

Result: 248 tests passed, including the service-coverage verification gate.
The 104 tests in these six suites also passed on a separate PostgreSQL 18.4
scratch cluster rather than H2:

- `AppointmentConcurrencyTest`: 59.
- `ClinicPilotIntegrationTest`: 8.
- `CitaServiceTest`: 16.
- `VeterinarioServiceTest`: 6.
- `DuenioServiceTest`: 10.
- `AtencionClinicaServiceTest`: 5.

Frontend verification, from the frontend repository:

```sh
node --test tests/clinical-pilot.test.cjs tests/saas.test.cjs
npm run build:render
```

Result: 68 tests passed and the production build succeeded. Existing style-budget
warnings in login, registration, public plans and the public directory remain.

The real-browser run passed 17 acceptance groups plus four profile/user-editing
regression groups. Coverage includes fresh clinic registration, staff creation
and password changes, profiles/schedules, owner linking and editing, pets,
services, appointment pricing/confirmation, attention, reloaded history, quotas,
two-clinic isolation, plan changes, suspension/reactivation and immutable audit.
All 16 admin navigation modules were opened and checked; this is not exhaustive
CRUD certification of every module.

To exercise an attention without waiting for tomorrow's booking, the pilot first
verified that future attention was rejected, then changed only that synthetic
appointment's date/start/end in the guarded scratch database. Attention and its
clinical status were subsequently created through the actual UI/API, not SQL.
Only the synthetic operator and extra quota-fixture accounts required out-of-band
setup. No API responses were mocked.

Latest private browser evidence on this workstation:

`/var/folders/sx/_6ljw37n2_j9hk2f_vtqplj40000gn/T/opencode/p4_pilot_y16jb8io/`

It contains `PILOT-RESULT.md`, sanitized JSON reports and desktop/mobile screenshots.
All browser, JVM, HTTP server and PostgreSQL processes were stopped; the scratch
cluster was removed. Temporary evidence is not a permanent CI artifact.

## Deploying The Corrections

1. Review both repository diffs and release commits. Re-run the checks against the
   final release source. Do not stage Angular caches, APKs or temporary evidence.
2. Back up PostgreSQL and verify a restore. Keep demo seeding disabled and stop
   application writers before applying the schema change.
3. On the explicitly reviewed database and `public` schema, execute:

```sh
psql -X --no-password -v ON_ERROR_STOP=1 -f scripts/pilot-readiness-migration.sql
```

Use protected connection-service/passfile configuration, not passwords in command
arguments or committed files. The script is transactional and rerunnable: it only
widens `asistentes.telefono` from varchar(9) to varchar(20), accepts an existing
varchar(20), and refuses an unexpected mapping. The real pilot rehearsed both the
legacy widening and full P2/P3/P4/readiness migrations twice without data loss.

4. Start the backend with `ddl-auto=validate` and seeds disabled, then deploy the
   frontend. Check health, login, forced password change, owner linkage/privacy,
   booking availability and SaaS authorization. Use a staging clinic for writes.
5. Preserve the widened field during a rollback; do not truncate phone values to
   run an older application. Keep data and audit history intact.

## Boundaries Before A Real Customer Pilot

This work did not configure production backup automation, alerting or email
delivery. Verify those operational controls separately, including a restore drill.
The isolated run pinned clinical scheduling to `America/Lima`; scheduling still
uses the JVM default timezone, while trial clocks use UTC. Confirm the deployment
timezone and existing data expectations before scheduling real appointments.

Paid-plan billing, paid subscription expiry, automatic renewal and payment
collection remain unimplemented. No plan or trial dates were changed in production.
