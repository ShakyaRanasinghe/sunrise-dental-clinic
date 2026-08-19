# Application Structure

Structure for the **modular** implementation. Deliberately plain: core Java, servlets, JDBC,
MySQL. Nothing else.

Aligns with [`class-diagram.md`](class-diagram.md), [`use-case-diagram.md`](use-case-diagram.md),
[`sequence-diagrams.md`](sequence-diagrams.md), [`srs/srs.md`](srs/srs.md) and
[`er-diagram.md`](er-diagram.md).

---

## 1. No frameworks

| Layer | What we use | What we are **not** using |
|---|---|---|
| Web | Jakarta Servlet 6.0 + JSP + JSTL | Spring, Spring MVC, Jakarta EE runtime, JAX-RS |
| Persistence | Plain JDBC, `PreparedStatement` | Hibernate, JPA, MyBatis, jOOQ |
| Views | Server-rendered JSP | React, Vue, Thymeleaf, any template engine |
| Wiring | `AppContext`, written by hand | Spring DI, CDI, Guice |
| JSON | `Json`, written by hand | Jackson, Gson |
| Security | `HttpSession` + PBKDF2, written by hand | Spring Security, Shiro, any JWT library |
| Cloud | none | Firebase, Firestore |

The complete dependency list — **five entries**, three of them supplied by the container or the
test runner:

```xml
jakarta.servlet-api          provided by Tomcat
jakarta.servlet.jsp-api      provided by Tomcat
jakarta.servlet.jsp.jstl     tag library, not a framework
mysql-connector-j            the JDBC driver
junit-jupiter                test scope only
```

Maven and JUnit are a build tool and a test library, not application frameworks. Everything the
application does at runtime is written in this project.

---

## 2. Authentication: session only

**`HttpSession` from the Servlet API. No tokens, no JWT, no extra tables.**

I proposed a JWT layer earlier and I was overcomplicating it. For this application a session is
both simpler and better:

| | Session | JWT |
|---|---|---|
| Sent by the browser on a plain link or form | automatically | only if JavaScript attaches it — and there is no client JavaScript |
| Sign-out takes effect | immediately | not until the token expires, unless you add a revocation table |
| Locking an account cuts it off | immediately | same problem |
| Extra code | none — it is in the Servlet API | signer, token service, two tables |

JWT is the right answer when a third-party client calls your API without a cookie jar. Nothing
does that here, so it would be machinery serving a hypothetical. If that changes, it is one
filter and one endpoint added later — the design below does not have to move.

What the session gives us, all already required by the SRS:

| Property | Requirement |
|---|---|
| PBKDF2 hash, 120,000 iterations, per-user salt | NFR-SEC-01 |
| Constant-time comparison | NFR-SEC-02 |
| Lock after 5 failed attempts | NFR-SEC-03 |
| `HttpOnly` cookie, `SameSite=Strict` | NFR-SEC-04 |
| 30-minute idle timeout | NFR-SEC-05 |
| CSRF token per form, held in the session | NFR-SEC-07 |

`AuthenticationFilter` maps to `/*`, reads the session, and puts a `ClinicPrincipal` on the
request. One class, and every servlet downstream reads the principal without knowing how it got
there.

---

## 3. Directory structure

```
modular/
├── pom.xml
├── docs/                          srs/ · api/ · class-diagram/ · er-diagram · architecture
└── src/
    ├── main/
    │   ├── java/com/sunrise/clinic/
    │   │
    │   │   ├── platform/                     shared machinery, no business meaning
    │   │   │   ├── config/    AppConfig
    │   │   │   ├── db/        Database · PooledConnection
    │   │   │   ├── data/    ★ Repository<T,ID> · JdbcDao<T> · TransactionRunner
    │   │   │   ├── di/        AppContext · ClinicServletContext
    │   │   │   ├── error/     exceptions · ErrorResponse
    │   │   │   ├── json/      Json
    │   │   │   ├── web/     ★ BaseServlet · PageServlet · HelpServlet
    │   │   │   └── audit/   ★ AuditEvent · AuditRepository · AuditDao · AuditObserver
    │   │   │
    │   │   ├── access/                       login, roles, accounts
    │   │   ├── patients/                     the patient register
    │   │   ├── scheduling/                   dentists · sessions · slots · treatments
    │   │   ├── appointments/                 booking · completion · dashboards
    │   │   ├── billing/                      bills · pricing · revenue split
    │   │   ├── notifications/                email · SMS
    │   │   ├── reporting/                    income · earnings · footfall
    │   │   └── feedback/                     complaints · dentist reviews
    │   │
    │   │        each of the eight feature modules holds the same four:
    │   │            web/       servlets
    │   │            service/   business rules
    │   │            data/      repository interface + JDBC and in-memory implementations
    │   │            domain/    entities, enums, response records
    │   │
    │   ├── resources/
    │   │   ├── clinic.properties      every key overridable by env var
    │   │   ├── schema.sql             14 tables, keys, constraints
    │   │   ├── procedures.sql         stored procedures, functions, triggers
    │   │   └── demo-data.sql          four seeded roles
    │   │
    │   └── webapp/
    │       ├── css/app.css
    │       └── WEB-INF/
    │           ├── web.xml            every servlet, filter and listener
    │           └── jsp/               grouped by feature, not by role
    │               ├── shared/        header.jspf · footer.jspf · error.jsp
    │               │                  not-found.jsp · help.jsp
    │               ├── access/        login-form.jspf          ← the one shared form
    │               │                  login-patient.jsp · login-reception.jsp
    │               │                  login-dentist.jsp · login-admin.jsp
    │               │                  portal-chooser.jsp · register.jsp
    │               ├── patients/      records.jsp
    │               ├── scheduling/    availability.jsp
    │               ├── appointments/  book.jsp · patient-home.jsp
    │               │                  reception-day.jsp · dentist-schedule.jsp
    │               ├── billing/       billing.jsp · receipt.jsp
    │               ├── reporting/     reports.jsp · accounts.jsp
    │               └── feedback/      patient-complaints.jsp · admin-complaints.jsp
    │
    └── test/java/com/sunrise/clinic/   one package per module
```

★ = three `platform/` directories that need creating; everything else already exists.

Class-by-class contents are in [`class-diagram.md`](class-diagram.md) rather than repeated here.

### The one rule the layout enforces

A `web/` class never imports a `data/` class. Servlets talk to services; only services talk to
repositories. That is **NFR-MNT-02**, and it is checkable by a grep instead of a code review:

```bash
grep -rl 'import com.sunrise.clinic.[a-z]*.data' --include='*.java' \
  modular/src/main/java/com/sunrise/clinic/*/web/     # must return nothing
```

Worth having, because in `layered/` 12 of 20 servlets currently break it.

---

## 4. MySQL

The database is a separate process reached by URL. Nothing is stored in the WAR, nothing on
disk.

| | Local | Deployed |
|---|---|---|
| Host | Docker `sunrise-mysql`, port 3308 | Managed MySQL 8 |
| Database | `sunrise_dental` | `sunrise_dental` |
| Credentials | `DB_URL` · `DB_USER` · `DB_PASSWORD` | same three names, different values |
| Pool | 8 connections, hand-written | sized per environment |

Every key in `clinic.properties` is overridable by an environment variable named after it —
`db.url` by `DB_URL`. One WAR ships to every environment with no rebuild and no credential in
version control.

### Three SQL files

```bash
mysql -u root -p < schema.sql        # 14 tables, keys, constraints
mysql -u root -p sunrise_dental < procedures.sql
mysql -u root -p sunrise_dental < demo-data.sql
```

| File | Contents |
|---|---|
| `schema.sql` | The 14 tables in [`er-diagram.md`](er-diagram.md), plus the **7 missing foreign keys** on staff references |
| `procedures.sql` | `sp_register_appointment` · `fn_calculate_bill` · `trg_prevent_double_booking` · `trg_audit_appointment` · `trg_check_receptionist_role` |
| `demo-data.sql` | One account per role, two dentists, six treatments, three patients |

`procedures.sql` is separate because it is what the assessment's "advanced database features"
criterion asks for, and because it is the file most likely to change while the tables stay put.

### Data discipline

| Rule | Enforced by |
|---|---|
| Every statement parameterised — no concatenated SQL | `JdbcDao` exposes only prepared-statement helpers |
| Connections always returned, including on failure | try-with-resources over `PooledConnection` |
| Booking is one transaction | `TransactionRunner.inTransaction(…)` wraps the slot lock, the insert and the counter |
| No double booking | `SELECT … FOR UPDATE` on the slot row + `UNIQUE (slot.appointment_no)` |

---

## 5. One request, end to end

`GET /reception/patients`

```
Browser  ─ session cookie ─▶  AuthenticationFilter        resolves the principal
                             → PatientRecordsServlet      web/
                             → PatientService             service/
                             → PatientRepository          data/  (interface)
                             → PatientDao                 data/  (JDBC, pooled)
                             → MySQL
                             ← forward to /WEB-INF/jsp/patients/records.jsp
Browser  ◀───── HTML ──────
```

Views live under `WEB-INF`, so no page is reachable by URL — every screen must come through a
servlet that has already loaded its data. Nothing can render half-populated.

---

## 6. What this buys a maintainer

| Question | Answer the layout gives |
|---|---|
| Where do I fix a billing bug? | `billing/` — screens, rules, SQL and types together |
| Where is authentication? | `access/` — nothing else touches credentials |
| Can a servlet reach the database? | No, and one grep proves it |
| How do I add a role? | One `RolePolicy` subclass, one login servlet subclass, one JSP |
| How do I test without a database? | Wire the `InMemory*` repositories — every module has them |
| What differs between environments? | Environment variables. The WAR is identical |

---

## 7. Why this architecture

Four properties were the goal. Each one is a consequence of a specific structural decision, not of
a framework.

### The shape: a grid, three tiers by eight modules

```
                access  patients  scheduling  appointments  billing  notif.  reporting  feedback
  web/            ·        ·          ·            ·           ·        ·        ·         ·     <- Presentation
  service/        ·        ·          ·            ·           ·        ·        ·         ·     <- Business
  data/           ·        ·          ·            ·           ·        ·        ·         ·     <- Data
  domain/         ·        ·          ·            ·           ·        ·        ·         ·
```

**Every class has exactly one cell.** Two coordinates locate it: which capability, which tier. The
tiers run horizontally and are the three-tier architecture; the modules run vertically and are what
makes a feature readable in one place. Neither is a substitute for the other, which is why the
design has both rather than choosing.

The three tiers have one rule each, and they are absolute:

| Tier | Knows | Never touches |
|---|---|---|
| **Presentation** — `web/` | HTTP, sessions, HTML, JSON | SQL, business rules |
| **Business** — `service/`, `domain/` | The clinic's rules | HTTP, `HttpServletRequest`, SQL |
| **Data** — `data/` | SQL, JDBC, row mapping | HTTP, business rules |

### Clear to read

Asking *"where is a bill's total calculated?"* takes no map: it is business logic about billing, so
`billing/service/`. The answer is `StandardBillingStrategy`.

Compare `layered/`, where the same question means opening `web/BillingPageServlet`,
`service/BillingService`, `pattern/billing/StandardBillingStrategy`, `dao/BillDao`,
`repository/BillRepository` and `domain/Bill` — six packages for one feature, and you must know
the whole system before you can find one part of it.

The concrete target: **a competent Java developer with no knowledge of this project should trace
one request end to end in under ten minutes, with nothing but an editor.** No annotations to
decode, no configuration to find, no reflection to reason about. `AppContext` is 250 lines of
constructor calls you can read top to bottom.

### Clear to identify bugs

Each tier fails in its own way, so a symptom names a tier before you open anything:

| Symptom | Tier | Real example from this project |
|---|---|---|
| Wrong page, wrong status, wrong markup, missing stylesheet | Presentation | `HomeServlet` mapped to `/` shadowed Tomcat's default servlet, so every stylesheet request answered a redirect |
| Wrong number, wrong decision, wrong permission | Business | Nothing checks that the dentist completing an appointment is the one treating it |
| Wrong, missing or duplicated row | Data | `BillDao` upserts, so billing twice returns an id that was never persisted |

Three defects, three tiers, each findable without reading the other two. That is the property, and
it is worth more than any amount of layering vocabulary.

**The bisect tool is free.** Every repository interface has an in-memory implementation. Swap the
data tier for it and re-run: if the bug survives, it is business logic; if it vanishes, it is SQL
or mapping. That is why 48 tests run with no database — the same seam serves testing and
diagnosis.

**One grep enforces the boundary**, so the property does not decay:

```bash
grep -rl 'import com.sunrise.clinic.[a-z]*.data' --include='*.java' \
  modular/src/main/java/com/sunrise/clinic/*/web/     # must return nothing
```

### Clear to plug in

There are exactly **five** extension points, all plain Java interfaces, none needing a framework:

| To add | Implement | Register in | Touches nothing else |
|---|---|---|---|
| A storage backend, or a test double | `Repository<T, ID>` | `AppContext` | ✔ |
| A pricing or revenue rule | `BillingStrategy`, `RevenueSplitStrategy` | `AppContext` | ✔ |
| A notification channel — WhatsApp, push | `NotificationChannel` | `NotificationChannelFactory` | ✔ |
| A reaction to a booking — analytics, audit | `AppointmentObserver` | `AppointmentEventPublisher` | ✔ |
| A user role — a fifth portal | `RolePolicy` + `AbstractLoginServlet` | `web.xml` | ✔ |

**A whole module is the sixth plug point.** Adding one means a new directory, its four
sub-packages, and one accessor on `AppContext`. Because the module dependency graph is acyclic,
nothing existing changes — which is the test of whether a boundary is real.

### Clear to scale

Two different questions, worth separating because they have different answers:

**Scaling the code.** Add a module. Eight exist; a ninth costs one directory and one accessor. The
DAG guarantees the blast radius is zero.

**Scaling the load.** The WAR holds almost no state:

| State | Where it lives | Consequence |
|---|---|---|
| Business data | MySQL, a separate process | Add Tomcats freely |
| Connections | 8 per instance, one env var | Tune per environment without a rebuild |
| Sessions | in Tomcat's memory | **Needs sticky sessions to run more than one instance** |
| Lock-out counters | `LoginAttemptService`, in memory | **Does not survive restart, and does not work across instances** |

The first two scale horizontally today. The last two are the honest blockers, and the second is a
defect rather than a limit: `user_account` already has `failed_attempts` and `locked` columns that
nothing writes, so the lock state that NFR-SEC-03 depends on is cleared by any restart. Fixing it
is persisting what the schema already models.

### Why it is not more complicated

What was considered and deliberately refused, with what each would have bought and cost:

| Refused | Would have given | Costs more than it gives because |
|---|---|---|
| DI container | Less wiring code | `AppContext` is 250 readable lines. A container replaces them with annotations and reflection you cannot step through |
| ORM | No hand-written SQL | The SQL is the interesting part. You can paste a DAO's query into a client and run it |
| JWT | A stateless API | Nothing calls the API without a cookie jar. It would need a revocation table to honour sign-out — a session with extra steps |
| Microservices | Independent deployment | One clinic, one database, one deployable. Module boundaries give the same isolation with none of the network |
| CQRS, event sourcing | An audit trail, read scaling | `audit_event` is one table. The read load is one practice |
| A cache layer | Faster reads | No measurement says reads are slow |
| Hexagonal everywhere | Testability throughout | Applied at the data tier only, where it pays for itself. Ports around HTTP would add indirection for no test we want to write |

The pattern is the same each time: **the boundary was added where a test or a change already
wanted it, and nowhere else.** Ports exist at the data tier because tests need to run without
MySQL. Strategies exist for pricing because pricing changes. There is no port around HTTP, because
nothing has ever needed to call the business tier from anything but a servlet.

### The honest costs

Stating them, because an architecture section that lists only benefits is advocacy:

- **`AppContext` is a single long file** every new module edits. Explicit wiring means one place to
  change and a merge conflict when two people add a module the same week.
- **Hand-written JSON has already bitten.** `Json.write` handles records and falls through to
  `toString()` for anything else, which is why five endpoints return `"Dentist{id=d-silva}"`. A
  library would not have had that hole.
- **No ORM means N+1 queries are the developer's problem.** Nothing warns you.
- **Cross-module reads need their own model.** Reports span bills and appointments, so `reporting`
  gets a read-only repository of its own rather than reaching into two other modules' data tiers.
- **32 packages for ~125 classes is more directories than a small application strictly needs.**
  The bet is that the project grows and the navigation pays back. If it stays this size, a flatter
  tree would have been enough.
