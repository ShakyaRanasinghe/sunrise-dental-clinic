#!/usr/bin/env bash
#
# Rebuild and redeploy the WAR without touching the database.
#
#   scripts/redeploy.sh          ->  http://localhost:8080
#
# Use this after code changes when you want to keep existing data.
# Use dev-up.sh only when you want a full clean reset.
#
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APP=sunrise-tomcat
MYSQL=sunrise-mysql
NET=sunrise-net
PORT=8080
MYSQL_PW=clinic
DB=sunrise_dental

say() { printf '\n\033[1m==> %s\033[0m\n' "$*"; }

say "building modular"
mvn -q -f "$ROOT/modular/pom.xml" -DskipTests clean package
WAR="$ROOT/modular/target/clinic.war"
CLASSES=$(unzip -l "$WAR" | grep -c '\.class' || true)
printf '    clinic.war  (%s compiled classes)\n' "$CLASSES"

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
    database:  jdbc:mysql://localhost:3308/$DB  (data preserved)

    logs:  docker logs -f $APP
    stop:  docker rm -f $APP
EOF
else
  echo "    /help did not answer." >&2
  echo "      docker logs $APP" >&2
  exit 1
fi
