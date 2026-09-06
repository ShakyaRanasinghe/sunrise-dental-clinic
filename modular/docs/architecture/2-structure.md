# Tiers, Modules and Layout

The three tiers, the eight feature modules, and where every file goes.

The shape in one picture is in [`README.md`](README.md); why this shape rather than another is in
[`1-alternatives.md`](1-alternatives.md).

---

The servlet-by-servlet contract — every mapping, view and role — is
[`../servlets.md`](../servlets.md).

## The three tiers

Each has one rule, and the rules are absolute.

| Tier | Package | Knows about | Never touches |
|---|---|---|---|
| **Presentation** | `web/` | HTTP, sessions, HTML, JSON | SQL, business rules |
| **Business** | `service/`, `domain/` | The clinic's rules | HTTP, `HttpServletRequest`, SQL |
| **Data** | `data/` | SQL, JDBC, row mapping | HTTP, business rules |

The tiers are drawn as boxes in every diagram in [`../sequence-diagrams.md`](../sequence-diagrams.md),
which makes the claim checkable: no arrow crosses from presentation directly to data.

---

## Directory structure

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

Class-by-class contents are in [`class-diagram.md`](../class-diagram.md) rather than repeated here.

### The one rule the layout enforces

A `web/` class never reaches the data tier. Servlets talk to services; only services talk to
repositories. That is **NFR-MNT-02**, and it is checkable by grep rather than by code review — but
it takes **two** checks, not one, and [`3-growth.md`](3-growth.md) explains why an import grep alone
would have reported `layered/` as clean while two thirds of its servlets talked to the database.

Worth having, because in the removed `layered/` arrangement 12 of 20 servlets broke it
(the evidence survives in git history).

---

---

## One request, end to end

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

---

## What this buys a maintainer

| Question | Answer the layout gives |
|---|---|
| Where do I fix a billing bug? | `billing/` — screens, rules, SQL and types together |
| Where is authentication? | `access/` — nothing else touches credentials |
| Can a servlet reach the database? | No, and one grep proves it |
| How do I add a role? | One `RolePolicy` subclass, one login servlet subclass, one JSP |
| How do I test without a database? | Wire the `InMemory*` repositories — every module has them |
| What differs between environments? | Environment variables. The WAR is identical |

---
