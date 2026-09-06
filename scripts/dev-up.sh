#!/usr/bin/env bash
#
# Bring modular/ up locally: database, schema, procedures, demo data, build, deploy.
#
#   scripts/dev-up.sh            ->  http://localhost:8080
#
# Idempotent: safe to re-run after every change. Recreates the app container and
# reloads the database; leaves the MySQL container alone.
#
# Deploys modular/ only — the sole code path since the earlier layered/
# arrangement was removed (its history survives in git).
#
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
NET=sunrise-net
MYSQL=sunrise-mysql
APP=sunrise-tomcat
DB=sunrise_dental
PORT=8080
MYSQL_PW=clinic
RES="$ROOT/modular/src/main/resources"

say() { printf '\n\033[1m==> %s\033[0m\n' "$*"; }

# ---------------------------------------------------------------- network
docker network inspect "$NET" >/dev/null 2>&1 || {
  say "creating network $NET"
  docker network create "$NET" >/dev/null
}

# ---------------------------------------------------------------- database
if ! docker ps --format '{{.Names}}' | grep -qx "$MYSQL"; then
  docker rm -f "$MYSQL" >/dev/null 2>&1 || true
  say "starting MySQL 8 on host port 3308"
  # 3308, not 3306: the host's own mysqld already holds 3306 and authenticates
  # by unix socket, so it is unusable without sudo. Do not "fix" this to 3306.
  docker run -d --name "$MYSQL" --network "$NET" \
    -e MYSQL_ROOT_PASSWORD="$MYSQL_PW" -e MYSQL_ROOT_HOST=% \
    -p 3308:3306 mysql:8.0 >/dev/null
  printf '    waiting for MySQL'
  for _ in $(seq 1 60); do
    docker exec "$MYSQL" mysqladmin ping -uroot -p"$MYSQL_PW" >/dev/null 2>&1 && break
    printf '.'; sleep 2
  done
  echo ' ready'
else
  say "MySQL already running"
fi

say "loading $DB"
docker exec "$MYSQL" mysql -uroot -p"$MYSQL_PW" \
  -e "DROP DATABASE IF EXISTS \`$DB\`;" 2>/dev/null

# --default-character-set=utf8mb4 is not optional. The client defaults to latin1, so
# MySQL was told the file's UTF-8 bytes were latin1 and converted them again - storing
# a double-encoded em dash that read back as "â€”" in the application. Every non-ASCII
# character in the seed data was corrupt, and it round-tripped through the CLI cleanly
# because the same wrong charset undid it on the way out.
load() {
  docker exec -i "$MYSQL" mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_PW" < "$1" 2>&1 \
    | grep -v '^mysql: \[Warning\]' || true
}
load "$RES/schema.sql"
[ -f "$RES/procedures.sql" ] && load "$RES/procedures.sql"
load "$RES/demo-data.sql"

docker exec -i "$MYSQL" mysql -uroot -p"$MYSQL_PW" -N 2>/dev/null <<SQL | sed 's/^/    /'
SELECT CONCAT('tables ', (SELECT COUNT(*) FROM information_schema.tables
                           WHERE table_schema='$DB'),
              ', foreign keys ', (SELECT COUNT(*) FROM information_schema.table_constraints
                           WHERE table_schema='$DB' AND constraint_type='FOREIGN KEY'),
              ', routines ', (SELECT COUNT(*) FROM information_schema.routines
                           WHERE routine_schema='$DB'),
              ', triggers ', (SELECT COUNT(*) FROM information_schema.triggers
                           WHERE trigger_schema='$DB'));
SQL

# ---------------------------------------------------------------- build
say "building modular"
mvn -q -f "$ROOT/modular/pom.xml" -DskipTests clean package
WAR="$ROOT/modular/target/clinic.war"
[ -f "$WAR" ] || { echo "    no WAR at $WAR" >&2; exit 1; }

CLASSES=$(unzip -l "$WAR" | grep -c '\.class' || true)
printf '    clinic.war  (%s compiled classes)\n' "$CLASSES"

if [ "$CLASSES" -eq 0 ]; then
  cat <<EOF

    No compiled classes yet, so there is nothing to deploy. The database is
    ready and the WAR packages, which is everything the environment can verify
    until step 1 of docs/imp/tasks.md moves the first classes across.

    Database: jdbc:mysql://localhost:3308/$DB   (root / $MYSQL_PW)
EOF
  exit 0
fi

# ---------------------------------------------------------------- deploy
say "deploying to http://localhost:$PORT"
docker rm -f "$APP" >/dev/null 2>&1 || true
docker run -d --name "$APP" --network "$NET" -p "$PORT:8080" \
  -e DB_URL="jdbc:mysql://$MYSQL:3306/$DB?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" \
  -e DB_USER=root \
  -e DB_PASSWORD="$MYSQL_PW" \
  -v "$WAR:/usr/local/tomcat/webapps/ROOT.war:ro" \
  tomcat:10.1-jdk17-temurin >/dev/null

printf '    waiting for Tomcat'
for _ in $(seq 1 40); do
  curl -sf -o /dev/null "http://localhost:$PORT/help" && break
  printf '.'; sleep 2
done
echo

if curl -sf -o /dev/null "http://localhost:$PORT/help"; then
  cat <<EOF

    up:        http://localhost:$PORT
    database:  jdbc:mysql://localhost:3308/$DB
    accounts:  admin · reception · silva (usernames; patient: nimal@example.lk)
    password:  Password123   (all of them)

    logs:  docker logs -f $APP
    stop:  docker rm -f $APP
EOF
else
  echo
  # The first version of this message blamed a missing class, which sent the
  # first real failure down the wrong path: the container had started cleanly and
  # every request was answering 500 from a JSP compile error. Point at both.
  echo "    /help did not answer. Two things to check, in this order:" >&2
  echo "      docker logs $APP              — a listener or missing class fails here" >&2
  echo "      curl -s localhost:$PORT/help  — a 500 body carries the JSP or servlet error" >&2
  exit 1
fi
