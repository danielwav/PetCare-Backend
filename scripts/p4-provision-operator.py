"""Interactive, out-of-band NEW account provisioning. Requires Python bcrypt."""

import getpass
import os
from pathlib import Path
import subprocess
import sys
import warnings


def psql_literal(value):
    # psql variable quoting, not SQL interpolation; reject meta-command injection.
    if any(ord(char) < 32 or ord(char) == 127 for char in value):
        raise ValueError("Control characters are not allowed")
    return "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'"


def main():
    if len(sys.argv) != 1 or not sys.stdin.isatty() or not sys.stderr.isatty():
        raise ValueError("Run interactively with no arguments, without tracing or terminal recording")
    if not os.environ.get("PGSERVICE"):
        raise ValueError("Set PGSERVICE to the reviewed connection service first")
    warnings.simplefilter("error", getpass.GetPassWarning)
    try:
        import bcrypt
    except ImportError:
        raise ValueError("Install bcrypt in an isolated virtualenv; see p4-migration.md") from None

    schema = input("Reviewed application schema: ").strip()
    if not schema or schema.startswith("pg_") or schema == "information_schema":
        raise ValueError("An application schema is required")
    # This contains no secrets. Confirm the actual destination before collecting credentials.
    subprocess.run(
        ["psql", "-X", "--no-password", "-v", "ON_ERROR_STOP=1", "-c",
         "SELECT current_database(), current_user, inet_server_addr(), inet_server_port();"],
        check=True,
    )
    if input(f"Create a NEW operator in schema {schema}? Type CREATE: ") != "CREATE":
        raise ValueError("Cancelled")
    email = input("New operator email: ").strip()
    nombres = input("Given names: ").strip()
    apellidos = input("Family names: ").strip()
    password = getpass.getpass("Unique temporary password (16+ characters, hidden): ")
    if len(password) < 16 or len(password.encode("utf-8")) > 72:
        raise ValueError("Use at least 16 characters and at most 72 UTF-8 bytes for bcrypt")
    if password != getpass.getpass("Confirm temporary password (hidden): "):
        raise ValueError("Passwords do not match")
    password_hash = bcrypt.hashpw(password.encode("utf-8"), bcrypt.gensalt(rounds=12)).decode("ascii")
    del password
    values = dict(p4_schema=schema, operator_email=email, nombres=nombres,
                  apellidos=apellidos, password_hash=password_hash)
    script = "\\set ECHO none\n\\set ECHO_HIDDEN off\n"
    script += "".join(f"\\set {key} {psql_literal(value)}\n" for key, value in values.items())
    path = str(Path(__file__).resolve().with_suffix(".sql"))
    script += "\\ir " + psql_literal(path) + "\n"
    # Values travel only over a private pipe, never argv, env, shell history or files.
    subprocess.run(["psql", "-X", "--no-password", "-v", "ON_ERROR_STOP=1", "-f", "-"],
                   input=script, text=True, check=True)
    print("Deliver the temporary password through the approved separate secure channel.")


if __name__ == "__main__":
    try:
        main()
    except (ValueError, subprocess.CalledProcessError, getpass.GetPassWarning) as error:
        print(f"P4: {error}", file=sys.stderr)
        sys.exit(1)
    except (KeyboardInterrupt, EOFError):
        print("P4: cancelled", file=sys.stderr)
        sys.exit(1)
