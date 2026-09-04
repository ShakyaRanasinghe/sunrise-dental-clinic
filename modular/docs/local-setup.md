# Running the application locally

Everything needed to get Sunrise Dental Clinic running on a development machine, and how to
check each piece independently when it does not.

> `layered/` is not part of this. It stays in the repository as the earlier arrangement the
> restructure copied from and as the git history, but it is not run, maintained or deployed.
> Every command below is about `modular/`.

---

## The short version

```bash
git clone <repo> && cd sunrise-dental-clinic
./scripts/dev-up.sh
```

Then open **<http://localhost:8080>** and sign in. That is the whole thing: the script starts
MySQL, loads the schema, procedures and demo data, builds the WAR and deploys it to Tomcat.

It is **idempotent** — re-run it after any change and it rebuilds and redeploys. It recreates
the application container and reloads the database every time, and leaves the MySQL container
alone.

```bash
./scripts/smoke.sh          # 53 end-to-end checks against what you just started
```

---

## 1. Prerequisites

| Need | Check with | Expected |
|---|---|---|
| JDK 17 or later | `java -version` | 17+. A 21 JDK is fine — the pom targets 17 bytecode |
| Maven 3.8+ | `mvn -v` | 3.8.7 confirmed working |
| Docker | `docker --version` | Provides MySQL and Tomcat, so neither is installed on the host |
| `curl`, `unzip` | — | Used by `dev-up.sh` |

**Nothing else.** No Tomcat install, no MySQL install, no Node, no global npm packages, no
cloud account.

---

## 2. Signing in

Every account uses the password **`Password123`**.

| Portal | Address | Sign in with |
|---|---|---|
| Patient | `/login/patient` | email `nimal@example.lk` |
| Reception | `/login/reception` | username `reception` |
| Dentist | `/login/dentist` | username `silva` |
| Dentist (second) | `/login/dentist` | username `jayasuriya` |
| Administrator | `/login/admin` | username `admin` |

A patient starts at the public home page and signs in via `/login/patient`. Staff go
straight to their issued portal address: `/login/reception`, `/login/dentist` or
(administrator, deliberately unadvertised) `/login/admin`.

Three things worth knowing before you start clicking:

- **Five wrong passwords lock an account**, and it does not unlock itself. Either re-run
  `dev-up.sh`, use the administrator's Accounts screen, or clear it directly:
  ```bash
  docker exec -i sunrise-mysql mysql -uroot -pclinic sunrise_dental \
    -e "UPDATE user_account SET failed_attempts = 0, locked = 0;"
  ```
- **The right password on the wrong portal** fails with the same message as a wrong password.
  That is deliberate, so the administrator's portal cannot be used to discover who is staff.
- **Every role sees only its own pages.** A patient opening `/admin/reports` gets a 403, not a
  redirect — the address exists and is not theirs.

---

## 3. What `dev-up.sh` sets up

| Component | Where | Detail |
|---|---|---|
| MySQL 8.0 | container `sunrise-mysql` | host port **3308** → container 3306 |
| Database | `sunrise_dental` | dropped and reloaded on every run |
| Tomcat 10.1 + JDK 17 | container `sunrise-tomcat` | host port **8080**, WAR mounted as `ROOT.war` |
| Network | `sunrise-net` | so Tomcat reaches MySQL by container name |

### Why port 3308 and not 3306

The host already runs its own `mysqld` on 3306, and it authenticates by unix socket
(`ERROR 1698: Access denied for user 'root'@'localhost'`), so it cannot be used without
`sudo`. 3308 avoids the collision entirely.

**Do not "fix" this back to 3306.** The script carries the same warning as a comment, because
it looks like an oddity until you hit the collision.

### Why the seed load forces utf8mb4

`dev-up.sh` loads SQL with `--default-character-set=utf8mb4`, and that is not optional. The
MySQL client defaults to **latin1**, so the file's UTF-8 bytes are declared latin1 and
converted again — storing a double-encoded em dash where an em dash belonged. It round-trips
cleanly back through the same client, so the corruption is invisible until the application
reads a note and renders `Penicillin â€” rash`.

### The credentials

Development only, and deliberately in the open:

| | |
|---|---|
| MySQL | `root` / `clinic` |
| Application accounts | the five above |
| Password for all of them | `Password123` |

Real deployments override `DB_URL`, `DB_USER` and `DB_PASSWORD` by environment variable, so no
credential that matters is in version control.

---

## 4. Configuration

`modular/src/main/resources/clinic.properties` holds the defaults. **Every key is overridable
by an environment variable named after it** — upper-cased, dots to underscores:

| Key | Environment variable | Local value |
|---|---|---|
| `db.url` | `DB_URL` | `jdbc:mysql://sunrise-mysql:3306/sunrise_dental?…` |
| `db.user` | `DB_USER` | `root` |
| `db.password` | `DB_PASSWORD` | `clinic` |
| `db.pool.size` | `DB_POOL_SIZE` | 8 |
| `clinic.timezone` | `CLINIC_TIMEZONE` | `Asia/Colombo` |
| `clinic.billing.service-charge` | `CLINIC_BILLING_SERVICE_CHARGE` | 200 |
| `clinic.revenue.dentist-treatment-share` | `CLINIC_REVENUE_DENTIST_TREATMENT_SHARE` | 0.60 |
| `clinic.revenue.receptionist-service-share` | `CLINIC_REVENUE_RECEPTIONIST_SERVICE_SHARE` | 0 |

`dev-up.sh` passes the first three to the container, which is why the committed file can keep
the conventional `localhost:3306` default without breaking anything.

**`clinic.timezone` matters more than it looks.** The container runs UTC and the clinic is at
+05:30, so for five and a half hours every night UTC is still on yesterday — and a bill would be
counted under the previous day's takings while the appointment book said otherwise. Startup logs
the clinic zone beside the JVM's so a mismatch announces itself:

```
INFO ... clinic_timezone clinic=Asia/Colombo jvm=Etc/UTC
     (dates are computed in the clinic's zone, not the JVM's)
```

**Trying a different revenue policy?** No rebuild needed — the WAR reads it at start-up:

```bash
docker rm -f sunrise-tomcat
docker run -d --name sunrise-tomcat --network sunrise-net -p 8080:8080 \
  -e DB_URL="jdbc:mysql://sunrise-mysql:3306/sunrise_dental?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" \
  -e DB_USER=root -e DB_PASSWORD=clinic \
  -e CLINIC_REVENUE_RECEPTIONIST_SERVICE_SHARE=0.25 \
  -v "$PWD/modular/target/clinic.war:/usr/local/tomcat/webapps/ROOT.war:ro" \
  tomcat:10.1-jdk17-temurin
```

**Pointing at a hosted database** — Aiven, RDS, a MySQL elsewhere? Three environment variables
and a URL; nothing is rebuilt. See
[`deploy/4-external-database.md`](deploy/4-external-database.md).

**Running from an IDE instead of the container?** Point at the published port:

```bash
export DB_URL='jdbc:mysql://localhost:3308/sunrise_dental?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
export DB_USER=root
export DB_PASSWORD=clinic
```

---

## 5. Checking each layer independently

Run these when something is wrong. Each has a stated expected answer, so a wrong one tells you
which layer to look at.

```bash
# 1. containers up
docker ps --format '{{.Names}}\t{{.Status}}' | grep sunrise
#    -> sunrise-mysql   Up …
#    -> sunrise-tomcat  Up …

# 2. the schema is loaded and complete
docker exec -i sunrise-mysql mysql -uroot -pclinic -N -e "
  SELECT CONCAT(
    (SELECT COUNT(*) FROM information_schema.tables            WHERE table_schema='sunrise_dental'), ' tables, ',
    (SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema='sunrise_dental' AND constraint_type='FOREIGN KEY'), ' fks, ',
    (SELECT COUNT(*) FROM information_schema.routines          WHERE routine_schema='sunrise_dental'), ' routines, ',
    (SELECT COUNT(*) FROM information_schema.triggers          WHERE trigger_schema='sunrise_dental'), ' triggers');"
#    -> 14 tables, 24 fks, 3 routines, 5 triggers

# 3. the stored function works
docker exec -i sunrise-mysql mysql -uroot -pclinic sunrise_dental -N \
  -e "SELECT fn_calculate_bill('t-scaling', 1500.00, 200.00);"
#    -> 5200.00

# 4. the build
mvn -q -f modular/pom.xml -DskipTests clean package && echo built
#    -> built

# 5. the unit tests
mvn -f modular/pom.xml test
#    -> 266 tests, all green, and no database needed

# 6. the tier boundary — both checks, because an import grep alone finds 2 of 12
grep -rl  'import com.sunrise.clinic.[a-z]*\.data' --include='*.java' \
  modular/src/main/java/com/sunrise/clinic/*/web/
grep -rnE 'app\(\)\.[a-z]+(Repository|Dao)\(\)'    --include='*.java' \
  modular/src/main/java/com/sunrise/clinic/*/web/
#    -> both silent

# 7. the whole thing, over HTTP
./scripts/smoke.sh
#    -> 53 passed, 0 failed
```

---

## 6. Useful commands

```bash
./scripts/dev-up.sh                       # rebuild and redeploy
./scripts/smoke.sh                        # 53 end-to-end checks
docker logs -f sunrise-tomcat             # application log
docker restart sunrise-tomcat             # restart the app, keep the database
docker rm -f sunrise-tomcat               # stop the app, keep the database
docker rm -f sunrise-mysql                # stop the database too; next run recreates it

# a SQL prompt — note the charset, for the same reason the loader forces it
docker exec -it sunrise-mysql mysql --default-character-set=utf8mb4 -uroot -pclinic sunrise_dental

# reload only the database, without rebuilding
for f in schema procedures demo-data; do
  docker exec -i sunrise-mysql mysql --default-character-set=utf8mb4 -uroot -pclinic \
    < "modular/src/main/resources/$f.sql"
done
```

### Regenerating the diagrams

PlantUML is not committed — the jar is large and `UML/plantuml.jar` is in `.gitignore`. Fetch it
once:

```bash
curl -sL -o UML/plantuml.jar \
  https://github.com/plantuml/plantuml/releases/download/v1.2024.7/plantuml-1.2024.7.jar

java -jar UML/plantuml.jar -tpng modular/docs/use-case/*.puml
java -jar UML/plantuml.jar -tpng modular/docs/sequence/*.puml
```

No Graphviz needed: every source declares `!pragma layout smetana`, PlantUML's own layout
engine. The ER and class diagrams are Mermaid and render on GitHub with no toolchain at all.

---

## 7. When it does not start

Work down this list. Each row is a failure that actually happened during development.

| Symptom | Cause | What to do |
|---|---|---|
| `/help` never answers, and `docker logs` shows nothing | The container is not running | `docker ps -a --filter name=sunrise-tomcat`, then read the whole log unfiltered |
| Every request answers **500** | A JSP will not compile. The log is silent because Jasper reports it in the response | `curl -s localhost:8080/help` and read the body — the message names the file and line |
| A page answers **404** that should exist | A view name that resolves to no file. A missing view is a silent 404 from the container, not an exception | `mvn -f modular/pom.xml test` — `PageServletViewsTest` names it |
| Sign-in redirects back to the form with no error | The form is posting to the wrong path | Check the `action` attribute in the served HTML |
| Non-ASCII renders as `â€”` | Seed data loaded with the latin1 default | Reload with `--default-character-set=utf8mb4` |
| A report shows yesterday's date, or "patients seen 0" beside real bills | `clinic.timezone` is wrong or unset | Check the `clinic_timezone` line in the startup log |
| Host `mysqld` occupies 3306 and rejects `root` | Socket authentication | Use 3308, as the script does |
| First run is slow | Pulls `mysql:8.0` and `tomcat:10.1-jdk17-temurin` | One-off, a few hundred MB |

---

## 8. What you can do once it is up

Every page resolves. Thirteen role screens across four roles:

| Role | Screens |
|---|---|
| **Patient** | `/patient/home` · `/patient/book` · `/patient/profile` · `/patient/complaints` · `/patient/receipt` |
| **Reception** | `/reception/home` · `/reception/patients` · `/reception/availability` · `/reception/billing` · `/reception/receipt` |
| **Dentist** | `/dentist/schedule` · `/dentist/appointment` |
| **Administrator** | `/admin/reports` · `/admin/accounts` · `/admin/audit` · `/admin/complaints` |

For a guided walk through the whole clinic — publish availability, book, treat, bill, print —
see **[`testing/scenarios.md`](testing/scenarios.md)**. Those are written to be followed by hand,
in order, and each one states what you should see.
