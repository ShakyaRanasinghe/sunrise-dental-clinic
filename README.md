# Sunrise Dental Clinic — Appointment & Patient Management System

A role-based web application for a private dental clinic. It replaces a paper-and-
spreadsheet process — eliminating double bookings, lost records, long waits and billing
errors — with one shared, reliable source of information.

> **Stack:** Core Java 17 · Jakarta Servlets & JSP · JDBC · MySQL 8 · HTML/CSS
> **Architecture:** 3 tiers × 8 feature modules · **Patterns:** Template Method, Strategy,
> Factory Method, Observer, Repository (DAO), DTO, MVC, Builder, Singleton

**No application framework is used.** Dependency wiring, request routing, transactions,
JSON, password hashing and access control are all implemented in this project rather
than delegated to a framework. See [What we wrote ourselves](#-what-we-wrote-ourselves).

---

## ✨ Features

| Role | Capabilities |
|------|--------------|
| **Patient** | Register · browse availability · **book / cancel appointments** · declare **medical notes** · raise a **complaint** · rate a visit · view receipts |
| **Receptionist** | **Publish dentist availability (slots)** · register walk-ins · search patient records · **generate bills** |
| **Dentist** | View own schedule · read the patient's **declared medical notes** · record **diagnosis (confidential)** · mark treatments completed |
| **Administrator** | **Income & footfall reports** · revenue split · no-show rate · CSV export · create and unlock accounts · review complaints · read the audit trail |

**Highlights**

- 🦷 **Self-service booking** into receptionist-published slots, with a **real double-booking
  guard**: a `SELECT … FOR UPDATE` row lock inside a transaction, plus a `UNIQUE` key on
  `slot.appointment_no` as the database-level backstop.
- 💳 **Billing with a three-way revenue split** (dentist / clinic / receptionist), printable receipt.
- 🔒 **Field-level confidentiality** — a patient's diagnosis is visible only to the treating
  dentist and the patient, never to reception or admin. Enforced by serving a different
  DTO, so the field cannot leak by accident.
- 🔐 **PBKDF2 password hashing** (120,000 iterations, per-user salt, constant-time compare)
  and **account lock-out** after five failed attempts.
- 📊 **Decision-support reports** — income by period, earnings per dentist and per
  receptionist, daily takings and footfall, exportable as CSV.

---

## 🏛️ Architecture

```
┌── Presentation ──────────┐                ┌── Business ────────────┐   ┌── Data ─────────┐
│  JSP + JSTL views        │                │  Servlets              │   │                 │
│  HTML / CSS / vanilla JS │ ─── forward ─▶ │    ↓ Services          │ ▶ │  MySQL 8        │
│  (server-rendered)       │                │    ↓ Repositories      │   │  (plain JDBC)   │
│                          │ ◀── JSON ───── │  + design patterns     │   │                 │
└──────────────────────────┘                └────────────────────────┘   └─────────────────┘
        pages under /WEB-INF/jsp                 com.sunrise.clinic          schema.sql
        (unreachable except via a servlet)
```

Requests pass through `AuthenticationFilter`, which establishes the caller's identity from
the session. Page servlets render JSP; API servlets under `/api/**` return JSON for the
same operations.

### Packages

| Package | Responsibility |
|---------|----------------|
| `app` | `AppContext` — builds the object graph; `ClinicServletContext` — start-up/shutdown |
| `web` | Servlets: page controllers and the JSON API |
| `service` | Business logic: booking, availability, billing, reports |
| `repository` | Persistence interfaces + in-memory implementations (tests) |
| `dao` | JDBC/MySQL implementations of those interfaces |
| `db` | Connection pool and transaction management |
| `domain` | Entities and enumerations |
| `pattern` | Singleton, Strategy (billing), Factory Method (notification channels), Observer |
| `security` | Password hashing, authentication, role checks, lock-out |
| `json` | Hand-written JSON reader/writer |
| `dto` / `mapper` | Response shapes and the mapping to them |

---

## 🔧 What we wrote ourselves

Every capability below would normally come from a framework. The brief does not allow one,
so each is implemented here — which is the substance of the project.

| Normally provided by a framework | Our implementation |
|---|---|
| Dependency injection | `app/AppContext` — explicit constructor wiring, no scanning or reflection |
| Declarative transactions | `db/Database.inTransaction` + `repository/TransactionRunner` |
| ORM / repositories | `dao/*` — hand-written SQL on `PreparedStatement` |
| Connection pooling | `db/Database` + `db/PooledConnection` (JDK dynamic proxy) |
| JSON serialisation | `json/Json` — records serialised via `getRecordComponents()` |
| Request routing | Servlet mappings in `web.xml` + explicit sub-path dispatch |
| Exception → HTTP mapping | `web/BaseServlet.handle`, `web/PageServlet.page` |
| Authentication & sessions | `security/AuthenticationFilter`, `security/AuthService` |
| Password hashing | `security/PasswordHasher` — PBKDF2 via `javax.crypto` |
| Method-level authorization | `security/AccessControl.require(...)` |
| Configuration binding | `config/AppConfig` — properties + environment overrides |
| Boilerplate generation | Hand-written constructors, accessors and builders |

---

## 📁 Two folders, one of which ships

| Folder | What it is |
|---|---|
| **`modular/`** | **The application.** Everything is built, run, tested and deployed here |
| `layered/` | The earlier layered arrangement, kept as the source the restructure copied from and as the git history. **Not run, not maintained, not deployable** |

`modular/` arranges the same three tiers as a grid: eight feature modules —
`platform`, `access`, `patients`, `scheduling`, `appointments`, `billing`, `reporting`,
`feedback` — each holding its own `web/ service/ data/ domain/`. Every class has exactly one
cell, so "where does this go" and "what breaks if I change this" have answers you can read off
the directory tree. The reasoning, and the eight alternatives considered against it, is in
[`modular/docs/architecture/`](modular/docs/architecture/).

CI builds `modular/` only, and enforces the tier boundaries as a build step.

| Documentation | |
|---|---|
| [Run it locally](modular/docs/local-setup.md) | One command, and what to check when it does not work |
| [Manual test scenarios](modular/docs/testing/scenarios.md) | 50 walkthroughs in plain language — the fastest way to see what it does |
| [Deployment](modular/docs/deploy/) | Architecture, environments, runbook |
| [Architecture](modular/docs/architecture/) | Why the code is arranged this way, and the alternatives |
| [Requirements](modular/docs/srs/) | The SRS, with the verified status of every requirement |
| [API](modular/docs/api/) | Every JSON endpoint, with worked examples |

---

## 🚀 Running it

**Prerequisites:** JDK 17, Maven 3.8+, Docker. No local MySQL or Tomcat needed.

```bash
./scripts/dev-up.sh          # database, schema, demo data, build, deploy
./scripts/smoke.sh           # 53 end-to-end checks against the running instance
```

That is the whole thing: <http://localhost:8080>. It is idempotent — re-run it after any
change. See [`modular/docs/local-setup.md`](modular/docs/local-setup.md) for what it does and
why MySQL is published on 3308.

<details>
<summary>Without Docker</summary>

```bash
# 1. Create the schema and load the demo data. --default-character-set=utf8mb4 is
#    not optional: the client defaults to latin1 and will double-encode every
#    non-ASCII character in the seed data.
for f in schema procedures demo-data; do
  mysql --default-character-set=utf8mb4 -u root -p < "modular/src/main/resources/$f.sql"
done

# 2. Point the application at your database (or edit clinic.properties)
export DB_URL="jdbc:mysql://localhost:3306/sunrise_dental?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
export DB_USER=root
export DB_PASSWORD=yourpassword

# 3. Build and deploy
cd modular && mvn package
cp target/clinic.war "$CATALINA_HOME/webapps/ROOT.war"
```
</details>

Any setting in `clinic.properties` can be overridden by an environment variable named
after it — upper-cased, dots to underscores — so the same WAR runs in every environment
without a rebuild.

### Demo accounts

All use the password `Password123`.

| Role | Email |
|------|-------|
| Administrator | `admin@sunrisedental.lk` |
| Receptionist | `reception@sunrisedental.lk` |
| Dentist | `silva@sunrisedental.lk` |
| Patient | `nimal@example.lk` |

---

## ✅ Testing

Two suites, deliberately different in kind.

```bash
cd modular && mvn test      # 266 JUnit 5 tests, no database required
./scripts/smoke.sh          # 53 checks against a running instance
```

**The unit tests** sit below the web tier and run against in-memory repositories, so they are
fast and precise. They cover the booking workflow — including a **concurrency test** that
releases twelve simultaneous bookings at one slot on a latch and asserts exactly one wins —
the appointment and complaint status machines, the revenue split invariant across 49
combinations of its two dials, every confidentiality gate, password hashing and lock-out.

**The smoke test** assembles the whole application and drives it over HTTP as the four roles.
It exists because the unit tests cannot see a servlet mapping, a JSP that will not compile, a
JDBC statement a trigger refuses, a timezone or a charset — and every defect found late in
this project was one of those. It would have caught all of them.

---

## 📝 Changes

Additions and fixes made during development and QA are tracked in [`CHANGES.md`](CHANGES.md).

---

## 📐 Design

UML sources and rendered diagrams are in [`UML/`](UML/): use case diagrams (patient portal
and staff back-office), the domain class diagram, and sequence diagrams for login, booking,
billing and triage.
