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

## The reminder sweep

Reminders are the one thing in this application caused by time passing rather than by somebody
doing something, so they are run from outside it. **If nobody sets this up, no reminders go
out** — and nothing will complain, which is why it is here rather than in a footnote.

```bash
# Generate a token, once per deployment
openssl rand -hex 32
```

Set it as `CLINIC_REMINDERS_TOKEN` where Tomcat's other environment variables live, then:

```bash
# Every morning at 08:00, remind everybody due tomorrow
0 8 * * * curl -fsS -X POST -H "X-Clinic-Token: $CLINIC_REMINDERS_TOKEN" \
            http://localhost:8080/api/reminders/run >> /var/log/clinic-reminders.log 2>&1
```

`-f` matters: without it `curl` exits 0 on a 403 and cron reports success while nothing is
being sent.

**Check it before trusting it.** `GET` says which day it would sweep without sending anything:

```bash
curl -sS -H "X-Clinic-Token: $CLINIC_REMINDERS_TOKEN" http://localhost:8080/api/reminders/run
#  -> {"remindingAbout":"2026-08-23","note":"POST to this address to run the sweep."}
```

An administrator can also run it from a signed-in session, which is how to try it while
watching. **With no token configured that is the only way it can be run** — so a clinic that has
not set one up cannot have its patients swept by a stranger.

Running it twice is safe: the second run skips what the first already attempted, and reports how
many it skipped. A reminder that failed is **not** retried — the row saying so is the record to
act on, and retrying daily against a broken gateway would achieve nothing but noise.

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
curl -s http://localhost:8080/login | head -1                            # <!doctype html>
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
