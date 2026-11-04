"""Run p4-test.sql and real-script rejection cases on a LOCAL scratch database."""

import os
from pathlib import Path
import subprocess
import sys

sys.dont_write_bytecode = True


def main():
    host = os.environ.get("PGHOST", "")
    database = os.environ.get("PGDATABASE", "")
    if host not in ("127.0.0.1", "::1") and not host.startswith("/"):
        raise ValueError("Explicit local PGHOST (loopback or Unix socket directory) is required")
    if not database.startswith("p4_scratch_"):
        raise ValueError("Explicit PGDATABASE named p4_scratch_* is required")
    # Ignore service, hostaddr, password and inherited options: scratch connections only.
    env = {key: value for key, value in os.environ.items() if not key.startswith("PG")}
    command = ["psql", "-X", "--no-password", "-v", "ON_ERROR_STOP=1", "-At",
               "-h", host, "-p", os.environ.get("PGPORT", "5432"), "-d", database,
               "-U", os.environ.get("PGUSER", os.environ.get("USER", ""))]
    directory = Path(__file__).resolve().parent

    def run(sql, error=None):
        result = subprocess.run(command + ["-f", "-"], input=sql, text=True,
                                capture_output=True, env=env, cwd=directory)
        if error is None and result.returncode:
            raise AssertionError(result.stdout + result.stderr)
        if error is not None and (result.returncode == 0 or error not in result.stderr):
            raise AssertionError(f"Expected rejection {error!r}:\n{result.stdout}{result.stderr}")
        return result.stdout.strip()

    if run("SELECT count(*) FROM pg_namespace WHERE nspname = 'p4_fixture';") != "0":
        raise ValueError("Pre-existing p4_fixture: refusing to reuse or remove it")
    prefix = "SET search_path = p4_fixture, pg_catalog;\n\\set p4_schema p4_fixture\n"
    migration = prefix + "\\ir p4-saas-migration.sql\n"

    def provision(**changes):
        values = dict(operator_email="rejected@example.invalid", nombres="Test", apellidos="Operator",
                      password_hash="$2b$12$" + "." * 53)
        values.update(changes)
        # Same quoting used by the interactive helper, exercised with quotes/backslashes below.
        import importlib.util
        spec = importlib.util.spec_from_file_location("p4_provision", directory / "p4-provision-operator.py")
        helper = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(helper)
        return prefix + "".join(f"\\set {key} {helper.psql_literal(value)}\n"
                                for key, value in values.items() if value is not None) + \
            "\\ir p4-provision-operator.sql\n"

    def snapshot():
        return run(prefix + """
            SELECT jsonb_build_array(
                (SELECT jsonb_agg(to_jsonb(t) ORDER BY id) FROM roles t),
                (SELECT jsonb_agg(to_jsonb(t) ORDER BY id) FROM usuarios t),
                (SELECT jsonb_agg(to_jsonb(t) ORDER BY usuario_id, rol_id) FROM usuarios_roles t),
                (SELECT jsonb_agg(to_jsonb(t) ORDER BY id) FROM saas_clinic_audit t),
                (SELECT jsonb_agg(pg_get_constraintdef(oid) ORDER BY conname) FROM pg_constraint
                 WHERE conrelid = 'roles'::regclass));
        """)

    try:
        print(run("\\ir p4-test.sql\n"))
        baseline = snapshot()
        cases = [
            ({"operator_email": " NEW.OPERATOR@EXAMPLE.INVALID "}, "email already exists"),
            ({"operator_email": " Existing@Example.Invalid "}, "email already exists"),
            ({"operator_email": "INACTIVE@example.invalid"}, "email already exists"),
            ({"operator_email": "bad email"}, "operator_email must"),
            ({"operator_email": "x" * 110 + "@example.invalid"}, "operator_email must"),
            ({"nombres": " "}, "nombres/apellidos must"),
            ({"apellidos": "x" * 120}, "nombres/apellidos must"),
            ({"password_hash": "not-a-hash"}, "password_hash must"),
            ({"password_hash": "$2b$04$" + "." * 53}, "password_hash must"),
            ({"password_hash": "$2b$31$" + "." * 53}, "password_hash must"),
            ({"password_hash": None}, "required psql variable password_hash"),
            ({"operator_email": None}, "required psql variable operator_email"),
            ({"nombres": None}, "required psql variable nombres"),
            ({"apellidos": None}, "required psql variable apellidos"),
        ]
        for values, error in cases:
            run(provision(**values), error)
            assert snapshot() == baseline, f"Rejected provisioning changed data: {values.keys()}"
        run(prefix + "UPDATE roles SET active = false WHERE name = 'ROLE_SAAS_OPERATOR';")
        disabled = snapshot()
        run(provision(), "exactly one active ROLE_SAAS_OPERATOR")
        assert snapshot() == disabled, "Provisioning reactivated the disabled role"
        run(prefix + "UPDATE roles SET active = true WHERE name = 'ROLE_SAAS_OPERATOR';")

        # Unknown checks that block the new role must survive a failed atomic migration.
        run(prefix + "DELETE FROM usuarios_roles WHERE rol_id IN "
            "(SELECT id FROM roles WHERE name = 'ROLE_SAAS_OPERATOR'); "
            "DELETE FROM roles WHERE name = 'ROLE_SAAS_OPERATOR';")
        run(prefix + "ALTER TABLE roles ADD CONSTRAINT keep_blocker CHECK (name <> 'ROLE_SAAS_OPERATOR') NOT VALID;")
        blocked = snapshot()
        run(migration, 'violates check constraint "keep_blocker"')
        assert snapshot() == blocked, "Failed migration changed data or removed an unknown check"
        run(prefix + "ALTER TABLE roles DROP CONSTRAINT keep_blocker;")
        run(migration)
        run(prefix + "ALTER TABLE roles DROP CONSTRAINT p4_roles_name_check;")
        no_enum = snapshot()
        run(migration, "no recognized roles.name enum check")
        assert snapshot() == no_enum, "Unrecognized enum migration was not atomic"
        run(prefix + "ALTER TABLE roles ADD CONSTRAINT p4_roles_name_check CHECK "
            "(name IN ('ROLE_ADMIN', 'ROLE_VETERINARIO', 'ROLE_ASISTENTE', 'ROLE_DUENIO', 'ROLE_SAAS_OPERATOR'));")
        run(prefix + "ALTER TABLE saas_clinic_audit ADD COLUMN unexpected text;")
        run(migration, "unexpected saas_clinic_audit mapping")
        run(prefix + "ALTER TABLE saas_clinic_audit DROP COLUMN unexpected;")
        run(prefix + "ALTER TABLE saas_clinic_audit DROP CONSTRAINT saas_clinic_audit_new_plan_check;")
        run(migration, "missing or unexpected audit enum check on new_plan")
        run(prefix + "ALTER TABLE saas_clinic_audit ADD CONSTRAINT reordered_plan_check "
            "CHECK (new_plan IN ('CONSULTORIO', 'PRO', 'FREE', 'TRIAL'));")
        run(migration)
        run("\\ir p4-saas-migration.sql\n", "required psql variable p4_schema")
        run("\\set p4_schema p4_does_not_exist\n\\ir p4-saas-migration.sql\n",
            "select an existing, reviewed application schema")
        run(prefix + "DROP INDEX idx_saas_audit_clinic_created; "
            "CREATE INDEX idx_saas_audit_clinic_created ON saas_clinic_audit (actor_id);")
        run(migration, "unexpected idx_saas_audit_clinic_created")

        # Exercise helper quoting with characters meaningful to psql and SQL.
        run(provision(operator_email="o'connor@example.invalid", nombres="D'Angelo", apellidos="Back\\Slash"))
        assert run(prefix + "SELECT full_name FROM usuarios WHERE email = 'o''connor@example.invalid';").endswith(
            "D'Angelo Back\\Slash")
        print(f"P4 PASS: SQL regression, {len(cases)} provisioning rejections, disabled role, "
              "atomic migration failures, helper quoting")
    finally:
        run("DROP SCHEMA IF EXISTS p4_fixture CASCADE;")


if __name__ == "__main__":
    try:
        main()
    except (ValueError, AssertionError, subprocess.CalledProcessError) as error:
        print(f"P4 FAIL: {error}", file=sys.stderr)
        sys.exit(1)
