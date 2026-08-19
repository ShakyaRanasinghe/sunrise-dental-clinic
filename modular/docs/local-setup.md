# Local Setup

Everything needed to run `modular/` on a development machine, and how to check each piece.

One command does all of it:

```bash
./scripts/dev-up.sh
```

Idempotent — re-run it after every change. It starts MySQL if needed, reloads the schema, builds the
WAR and redeploys.

> `layered/` is **not** part of the local setup. It stays in the repository as the source the
> migration copies from and as the git history, but it is not run, maintained or shipped.

---

## 1. Prerequisites

| Need | Verify | Expected |
|---|---|---|
| JDK 17 or later | `java -version` | 17+. A 21 JDK is fine — the pom targets 17 bytecode |
| Maven 3.8+ | `mvn -v` | 3.8.7 confirmed working |
| Docker | `docker --version` | Provides MySQL and Tomcat, so neither is installed on the host |
| `curl`, `unzip` | — | Used by `dev-up.sh` |

Nothing else. No Tomcat install, no MySQL install, no Node, no global npm packages.

---

## 2. What `dev-up.sh` sets up

| Component | Where | Detail |
|---|---|---|
| MySQL 8.0 | container `sunrise-mysql` | host port **3308** → container 3306 |
| Database | `sunrise_dental` | dropped and reloaded on every run |
| Tomcat 10.1 + JDK 17 | container `sunrise-tomcat` | host port **8080**, WAR mounted as `ROOT.war` |
| Network | `sunrise-net` | so Tomcat reaches MySQL by container name |

### Why port 3308 and not 3306

The host already runs its own `mysqld` on 3306, and it authenticates by unix socket
(`ERROR 1698: Access denied for user 'root'@'localhost'`), so it cannot be used without `sudo`.
3308 avoids the collision entirely.

**Do not "fix" this back to 3306.** The script carries the same warning as a comment, because it
looks like an oddity until you hit the collision.

### The credentials

Development only, and deliberately in the open:

| | |
|---|---|
| MySQL | `root` / `clinic` |
| Application accounts | the five in `demo-data.sql` |
| Password for all of them | `Password123` |

Real deployments override `DB_URL`, `DB_USER` and `DB_PASSWORD` by environment variable, so no
credential that matters is in version control.

---

## 3. Configuration

`modular/src/main/resources/clinic.properties` holds the defaults. **Every key is overridable by an
environment variable named after it** — upper-cased, dots to underscores:

| Key | Environment variable | Local value |
|---|---|---|
| `db.url` | `DB_URL` | `jdbc:mysql://sunrise-mysql:3306/sunrise_dental?…` |
| `db.user` | `DB_USER` | `root` |
| `db.password` | `DB_PASSWORD` | `clinic` |
| `db.pool.size` | `DB_POOL_SIZE` | 8 |

`dev-up.sh` passes the first three to the container, which is why the committed file can keep the
conventional `localhost:3306` default without breaking anything.

**Running from an IDE instead of the container?** Point at the published port and set the password:

```bash
export DB_URL='jdbc:mysql://localhost:3308/sunrise_dental?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
export DB_USER=root
export DB_PASSWORD=clinic
```

---

## 4. Checking the environment

Run these to confirm each layer independently. Each has a stated expected answer, so a wrong one
tells you which layer to look at.

```bash
# 1. containers up
docker ps --format '{{.Names}}\t{{.Status}}' | grep sunrise
#    -> sunrise-mysql   Up …
#    -> sunrise-tomcat  Up …          (absent until step 1 compiles classes)

# 2. the schema is the modular one, not a stale copy
docker exec -i sunrise-mysql mysql -uroot -pclinic -N -e "
  SELECT CONCAT(
    (SELECT COUNT(*) FROM information_schema.tables            WHERE table_schema='sunrise_dental'), ' tables, ',
    (SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema='sunrise_dental' AND constraint_type='FOREIGN KEY'), ' fks, ',
    (SELECT COUNT(*) FROM information_schema.routines          WHERE routine_schema='sunrise_dental'), ' routines, ',
    (SELECT COUNT(*) FROM information_schema.triggers          WHERE trigger_schema='sunrise_dental'), ' triggers');"
#    -> 14 tables, 24 fks, 3 routines, 5 triggers

# 3. the stored procedure works
docker exec -i sunrise-mysql mysql -uroot -pclinic sunrise_dental -N \
  -e "SELECT fn_calculate_bill('t-scaling', 1500.00, 200.00);"
#    -> 5200.00

# 4. the build
mvn -q -f modular/pom.xml -DskipTests clean package && echo built
#    -> built

# 5. the tier boundary (both checks — an import grep alone misses chained calls)
grep -rl  'import com.sunrise.clinic.[a-z]*\.data' --include='*.java' \
  modular/src/main/java/com/sunrise/clinic/*/web/
grep -rnE 'app\(\)\.[a-z]+(Repository|Dao)\(\)'    --include='*.java' \
  modular/src/main/java/com/sunrise/clinic/*/web/
#    -> both silent

# 6. the tests
mvn -f modular/pom.xml test
#    -> 64 tests, all green, and no database needed
```

---

## 5. Useful commands

```bash
./scripts/dev-up.sh                       # rebuild and redeploy
docker logs -f sunrise-tomcat             # application log
docker rm -f sunrise-tomcat               # stop the app, keep the database
docker rm -f sunrise-mysql                # stop the database too; next run recreates it

# a SQL prompt
docker exec -it sunrise-mysql mysql -uroot -pclinic sunrise_dental

# reload only the database, without rebuilding
docker exec -i sunrise-mysql mysql -uroot -pclinic < modular/src/main/resources/schema.sql
docker exec -i sunrise-mysql mysql -uroot -pclinic < modular/src/main/resources/procedures.sql
docker exec -i sunrise-mysql mysql -uroot -pclinic < modular/src/main/resources/demo-data.sql
```

### Regenerating the diagrams

PlantUML is not committed — the jar is large and `UML/plantuml.jar` is already in `.gitignore`.
Fetch it once:

```bash
curl -sL -o UML/plantuml.jar \
  https://github.com/plantuml/plantuml/releases/download/v1.2024.7/plantuml-1.2024.7.jar

java -jar UML/plantuml.jar -tpng modular/docs/use-case/*.puml
java -jar UML/plantuml.jar -tpng modular/docs/sequence/*.puml
```

No Graphviz needed: every source declares `!pragma layout smetana`, PlantUML's own layout engine.
Mermaid diagrams — the ER and class diagrams — render on GitHub with no toolchain at all.

---

## 6. Known environment quirks

| Quirk | Why | What to do |
|---|---|---|
| Host `mysqld` occupies 3306 and rejects `root` | Socket authentication | Use 3308, as the script does |
| `/help` is the only route that answers before step 1 | Nothing else is migrated | Expected. `dev-up.sh` says so and exits cleanly |
| An unknown path redirects to `/login` when signed out, but 404s when signed in | The filter treats any unmatched path as protected, so it does not reveal which addresses exist to someone who is not signed in | Intended |
| A role's page answers 403 rather than redirecting to your own home | A redirect would suggest the address was wrong, when it exists and is not yours | Intended |
| Tomcat fails to start with `ClassNotFoundException` | `web.xml` names all 32 servlets, and they arrive module by module | Expected mid-migration. `docker logs sunrise-tomcat` names the missing class |
| First `dev-up.sh` run is slow | Pulls `mysql:8.0` and `tomcat:10.1-jdk17-temurin` | One-off, a few hundred MB |
| `mvn` warns about a missing `maven-war-plugin` webapp directory | `webapp/` had only `WEB-INF` until views arrive | Harmless |

---

## 7. Ready-for-implementation checklist

- [x] JDK 17+, Maven 3.8+, Docker present
- [x] MySQL 8 container on 3308
- [x] `sunrise_dental` loaded with the **modular** schema — 14 tables, 24 foreign keys
- [x] `procedures.sql` loaded — 3 routines, 5 triggers, all exercised
- [x] `demo-data.sql` loaded — 5 accounts, 3 patients, 3 medical notes, 2 complaints
- [x] `modular/pom.xml` packages a WAR
- [x] `web.xml` written and verified against the servlet contract
- [x] `dev-up.sh` reproduces the whole environment from nothing
- [x] `platform` migrated — step 1, 18 classes, 15 tests
- [x] `access` migrated and **deployed** — step 2, four portals, 64 tests total
- [ ] `patients` + `scheduling` — **step 3 of [`imp/tasks.md`](imp/tasks.md)**

The application answers on http://localhost:8080. Sign in at `/login` and pick a portal.
