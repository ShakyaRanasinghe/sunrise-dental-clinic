# Sunrise Dental Clinic — Appointment & Patient Management System

A role-based web application for a private dental clinic. It replaces a paper-and-
spreadsheet process — eliminating double bookings, lost records, long waits and billing
errors — with one shared, reliable source of information.

> **Stack:** Core Java 17 · Jakarta Servlets & JSP · JDBC · MySQL 8 · HTML/CSS
> **Architecture:** 3-tier (presentation / business / data) · **Patterns:** Singleton, Strategy,
> Factory Method, Observer, Repository (DAO), DTO, MVC, Builder

**No application framework is used.** Dependency wiring, request routing, transactions,
JSON, password hashing and access control are all implemented in this project rather
than delegated to a framework. See [What we wrote ourselves](#-what-we-wrote-ourselves).

---

## ✨ Features

| Role | Capabilities |
|------|--------------|
| **Patient** | Register · browse dentist availability · **book / cancel appointments** · view receipts |
| **Receptionist** | **Publish dentist availability (slots)** · register walk-ins · search patient records · **generate bills** |
| **Dentist** | View own schedule · record **diagnosis (confidential)** · mark treatments completed |
| **Administrator** | **Income & footfall reports** · three-way revenue split · CSV export · unlock accounts |

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

## 🚀 Running it

**Prerequisites:** JDK 17, Maven 3.9+, MySQL 8, Tomcat 10.1+ (Jakarta EE 10).

```bash
# 1. Create the schema and load the demo data
mysql -u root -p < backend/src/main/resources/schema.sql
mysql -u root -p sunrise_dental < backend/src/main/resources/demo-data.sql

# 2. Point the application at your database (or edit clinic.properties)
export DB_URL="jdbc:mysql://localhost:3306/sunrise_dental?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
export DB_USER=root
export DB_PASSWORD=yourpassword

# 3. Build the WAR
cd backend && mvn package

# 4. Deploy it
cp target/clinic.war "$CATALINA_HOME/webapps/"
```

Then open <http://localhost:8080/clinic/>.

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

```bash
cd backend && mvn test
```

48 JUnit 5 tests covering the booking workflow (including a **concurrency test** that fires
twelve simultaneous bookings at one slot and asserts exactly one succeeds), billing and the
revenue split, the JSON reader/writer, password hashing, and the lock-out state machine.

The tests run against the in-memory repositories, so no database is required.

---

## 📐 Design

UML sources and rendered diagrams are in [`UML/`](UML/): use case diagrams (patient portal
and staff back-office), the domain class diagram, and sequence diagrams for login, booking,
billing and triage.
