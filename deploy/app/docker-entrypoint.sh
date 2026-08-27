#!/bin/sh
# Sunrise Dental Clinic - container entrypoint.
#
# Seeds the database that DB_URL points to (schema + stored procedures +
# demo data) on first boot, then hands over to Tomcat. Seeding is
# idempotent: tables are CREATE IF NOT EXISTS, procedures are dropped and
# re-created, and demo rows are INSERT ... ON DUPLICATE KEY UPDATE (the
# remaining inserts use --force so a duplicate simply skips). This replaces
# the manual "load schema.sql, procedures.sql, demo-data.sql" steps that a
# hosted Aiven database cannot run for itself.
#
# The SQL files use `USE sunrise_dental;` because that is the name used in
# the local development database. A hosted database may have any name (the
# app uses whatever DB_URL says), so those CREATE DATABASE / USE lines are
# stripped before loading and the client is pointed at the database in
# DB_URL instead. That keeps "the database the app connects to" and "the
# database that is seeded" the same object, whatever it is called.
set -e

# Default to Tomcat when the container starts without a command.
if [ $# -eq 0 ]; then
    set -- catalina.sh run
fi

if [ "${DB_SEED:-true}" != "true" ]; then
    echo "db_seed disabled (DB_SEED=$DB_SEED); skipping seed"
    exec "$@"
fi

URL="${DB_URL:-}"
if [ -z "$URL" ]; then
    echo "db_seed skipped: DB_URL not set"
    exec "$@"
fi

# Parse  jdbc:mysql://HOST:PORT/DBNAME?params
BASE=$(printf '%s' "$URL" | sed 's|^jdbc:mysql://||')
HOST=$(printf '%s' "$BASE" | cut -d: -f1)
REST=$(printf '%s' "$BASE" | cut -d: -f2)
PORT=$(printf '%s' "$REST" | cut -d/ -f1)
DBNAME=$(printf '%s' "$REST" | cut -d/ -f2 | cut -d? -f1)

if [ -z "$HOST" ] || [ -z "$PORT" ] || [ -z "$DBNAME" ]; then
    echo "db_seed skipped: could not parse DB_URL=$URL"
    exec "$@"
fi

seed() {
    echo "db_seed loading $1 into $DBNAME"
    # Drop the CREATE DATABASE / USE lines so everything lands in $DBNAME.
    sed -e '/^CREATE DATABASE/,/;/d' -e '/^USE [[:space:]]*[a-zA-Z0-9_]*/d' "$1" \
        | MYSQL_PWD="$DB_PASSWORD" mysql --ssl-mode=REQUIRED \
            --default-character-set=utf8mb4 --force \
            -h "$HOST" -P "$PORT" -u "$DB_USER" --database="$DBNAME"
}

seed /opt/app-db/schema.sql
seed /opt/app-db/procedures.sql
seed /opt/app-db/demo-data.sql

echo "db_seed done"
exec "$@"