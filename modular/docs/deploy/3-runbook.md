# Runbook

Deploying, rolling back, and what to check afterwards. Written to be followed by somebody who
did not write the application.

## Build the artefact

```bash
cd modular && mvn clean package
```

Produces `modular/target/clinic.war`. The build **runs the tests**, so a failing test is a
failing build — there is no `-DskipTests` in any pipeline.

CI does this on every pull request and attaches the WAR to a tagged release, so the artefact a
clinic receives is the one CI built rather than one built on somebody's laptop.

## Deploy

### First time

```bash
# 1. The schema. Once, by a person — the application never issues DDL.
#    utf8mb4 is not optional; see 2-environments.md.
mysql --default-character-set=utf8mb4 -u root -p < schema.sql
mysql --default-character-set=utf8mb4 -u root -p < procedures.sql
#    Do NOT load demo-data.sql into a real clinic — it creates accounts with a
#    published password.

# 2. The application user, with rights on one schema and nothing else
mysql -u root -p -e "
  CREATE USER 'clinic_app'@'%' IDENTIFIED BY '<a real password>';
  GRANT SELECT, INSERT, UPDATE, DELETE ON sunrise_dental.* TO 'clinic_app'@'%';
  GRANT EXECUTE ON sunrise_dental.* TO 'clinic_app'@'%';"

# 3. The WAR
export DB_URL='jdbc:mysql://<host>:3306/sunrise_dental?useSSL=true&serverTimezone=UTC'
export DB_USER=clinic_app
export DB_PASSWORD='<a real password>'
export CLINIC_TIMEZONE=Asia/Colombo
cp clinic.war "$CATALINA_HOME/webapps/ROOT.war"
"$CATALINA_HOME/bin/startup.sh"

# 4. The first administrator. There is no sign-up for staff, so the first account
#    is created here and every other account is created through its Accounts screen.
```

### Subsequent releases

```bash
"$CATALINA_HOME/bin/shutdown.sh"
cp "$CATALINA_HOME/webapps/ROOT.war" "/var/backups/clinic-$(date +%F-%H%M).war"   # the rollback
rm -rf "$CATALINA_HOME/webapps/ROOT" "$CATALINA_HOME/webapps/ROOT.war"
cp clinic.war "$CATALINA_HOME/webapps/ROOT.war"
"$CATALINA_HOME/bin/startup.sh"
```

Deleting the exploded `ROOT/` directory matters. Tomcat caches compiled JSPs there, and a stale
one will be served in preference to the new WAR's — which during development made a fixed page
look unfixed and cost an hour before recreating the container proved the fix.

**Everyone is signed out by a restart.** Sessions live in Tomcat's memory, so deploy outside
clinic hours or accept that whoever is mid-booking starts again.

## Roll back

```bash
"$CATALINA_HOME/bin/shutdown.sh"
rm -rf "$CATALINA_HOME/webapps/ROOT" "$CATALINA_HOME/webapps/ROOT.war"
cp /var/backups/clinic-<the good one>.war "$CATALINA_HOME/webapps/ROOT.war"
"$CATALINA_HOME/bin/startup.sh"
```

**The WAR rolls back cleanly. A schema change does not.** Nothing in the application issues DDL,
so a release that needed a schema change needs its own reverse script written and tested *before*
that release ships. A release with no schema change — most of them — is a file swap.

## Check it afterwards

In this order. Each step tells you which layer is wrong.

```bash
# 1. Did it start at all?
curl -sf -o /dev/null -w '%{http_code}\n' http://localhost:8080/help     # 200

# 2. Did it reach the database, and in the right timezone?
grep -E 'database_pool_ready|clinic_timezone' "$CATALINA_HOME/logs/catalina.out"

# 3. Does a page render, styled?
curl -s http://localhost:8080/login/patient | head -1                      # <!doctype html>
curl -sf -o /dev/null -w '%{http_code}\n' http://localhost:8080/css/app.css   # 200

# 4. Can somebody sign in?
curl -s -o /dev/null -w '%{redirect_url}\n' \
  -d 'email=<an admin>&password=<their password>' http://localhost:8080/login/admin
#    -> .../admin/reports
```

On a staging instance loaded with demo data, `./scripts/smoke.sh` does all of this and 49 more
checks. **Do not run it against a clinic's database** — it books appointments, issues bills and
raises a complaint.

## When something is wrong

| Symptom | Look at | Usual cause |
|---|---|---|
| Nothing answers | `catalina.out` | The pool could not reach MySQL. The URL, the credential, or the firewall |
| Everything answers 500 | the response body, not the log | A JSP compile error. Jasper reports it in the response and the log stays quiet |
| A page 404s that should exist | `PageServletViewsTest` | A view name that resolves to no file. A missing view is a silent 404 from the container, not an exception |
| Non-ASCII shows as `â€”` | the load command | SQL loaded with the latin1 client default |
| Reports show the wrong day | the `clinic_timezone` log line | `CLINIC_TIMEZONE` unset, so dates are the server's |
| An appointment cannot be completed | `catalina.out` for a trigger message | A statement a trigger refused. The message names the rule |
| Somebody is locked out | the administrator's Accounts screen | Five failed attempts. It does not expire — that is deliberate |

## Backups

The database is the only thing with state. The WAR is rebuildable from a tag and Tomcat holds
nothing but sessions.

```bash
mysqldump --default-character-set=utf8mb4 --single-transaction \
  --routines --triggers -u root -p sunrise_dental > "clinic-$(date +%F).sql"
```

`--routines --triggers` is not optional: three of the business rules live in the routines and
five in the triggers, and a dump without them restores a database that accepts a double booking.
`--single-transaction` takes it without locking the practice out mid-morning.

**Test a restore before you need one.** A dump nobody has restored is a hypothesis.
