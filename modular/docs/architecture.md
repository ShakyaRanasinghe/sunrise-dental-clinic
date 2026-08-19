# Application Structure

Structure for the **modular** implementation. Deliberately plain: core Java, servlets, JDBC,
MySQL. Nothing else.

Aligns with [`class-diagram.md`](class-diagram.md), [`srs/srs.md`](srs/srs.md) and
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
