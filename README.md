# Sunrise Dental Clinic — Appointment & Patient Management System

A distributed, cloud-backed appointment and patient-management system for a private
dental clinic. It replaces a paper-based process — eliminating double bookings, lost
records, long waits, and billing errors — with a role-based web application.

> **Stack:** React (SPA) · Spring Boot (Java 17, REST) · Firebase (Firestore + Auth + Cloud Functions)
> **Architecture:** 3-tier, distributed · **Patterns:** Singleton, Strategy, Factory, Observer, Repository/DAO, DTO, Facade, MVC

---

## ✨ Features

| Role | Capabilities |
|------|--------------|
| **Patient** | Register · view dentist availability (today / this week) · **book / cancel appointments** · view & download bill · AI symptom-triage chat |
| **Receptionist** | **Publish dentist availability (slots)** · register walk-in appointments · search & update · **generate bills** · manage patient records |
| **Dentist** | View own schedule · record **diagnosis (confidential)** · mark treatments completed |
| **Administrator** | Manage staff & roles (RBAC) · manage treatment catalogue & pricing · **income & footfall reports** · **runtime AI-model configuration** · audit log |

**Highlights**

- 🦷 **Self-service booking** into receptionist-published slots — with an **atomic booking
  guard** that makes double-booking impossible.
- 💳 **Billing with a 3-way revenue split** (dentist / clinic / receptionist), printable PDF receipt.
- 🔒 **Field-level confidentiality** — a patient's clinical diagnosis is visible only to the
  treating dentist and the patient, never to admin or reception.
- 🤖 **AI symptom triage** (grounded, always disclaimed) with an **admin-configurable model at runtime**.
- 📊 **Decision-support reports** — daily footfall by dentist/receptionist, income by
  date/range/month, per-doctor and per-receptionist earnings.
- 📧 **Email/SMS confirmations** via a Cloud Function trigger + an Observer pipeline.
- 🌐 **Tri-lingual UI** (English / Sinhala / Tamil).

---

## 🏛️ Architecture

```
┌── Presentation ──────────┐   HTTPS/JSON   ┌── Business ────────────┐   ┌── Data ─────────┐
│  React SPA               │ ─────────────▶ │  Spring Boot REST API  │ ▶ │ Firestore (prod)│
│  role dashboards, charts │ ◀───────────── │  Controller→Service→   │   │ In-memory (test)│
│  booking, AI chat        │                │  Repository, patterns  │   │ Firebase Auth   │
└──────────────────────────┘                └───────────┬────────────┘   └─────────────────┘
        │                                                │ writes
   Firebase Hosting                              Firestore onCreate
   (dev / qa / prod channels)                 ┌──────────▼──────────┐
                                              │ Cloud Functions      │
                                              │ • confirmation (trig)│
                                              │ • reminder (schedule)│
                                              └──────────────────────┘
```

A genuinely **distributed** system: the browser, the REST server, the cloud database, and
serverless functions are separate, independently-deployable tiers.

### Design patterns

| Pattern | Where | Why |
|---------|-------|-----|
| **Singleton** | `AppointmentNumberGenerator` | one atomic source of unique numbers (`APT-YYYYMMDD-####`) |
| **Strategy** | `BillingStrategy`, `RevenueSplitStrategy` | swap pricing / commission policy without code changes |
| **Factory Method** | `NotificationChannelFactory` | create Email/SMS channels; add a channel with zero caller changes |
| **Observer** | `AppointmentEventPublisher` | on booking → send confirmation + write audit, decoupled |
| **Repository / DAO** | `*Repository` (in-memory + Firestore) | swap storage; run & test with no cloud account |
| **DTO** | `AppointmentResponse` / `AppointmentClinicalResponse` | field-level access control by construction |
| **MVC + Dependency Injection** | Spring | layered, testable |

---

## 📁 Project structure

```
sunrise-dental-clinic/
├── backend/            Spring Boot REST API (Java 17, Maven)
│   └── src/main/java/com/sunrise/clinic/
│       ├── domain/         entities + enums
│       ├── pattern/        Singleton, Strategy (billing), Factory (notifications)
│       ├── repository/     Repository interface + in-memory & Firestore adapters
│       ├── service/        business logic (booking, billing, availability) + Observer
│       ├── controller/     REST endpoints
│       └── config/         security, OpenAPI
├── frontend/           React SPA (Vite)
├── functions/          Firebase Cloud Functions (triggers, reminders)
├── UML/                Use case, class & sequence diagrams
└── .github/workflows/  CI/CD (dev → qa → prod)
```

---

## 🚀 Getting started

### Backend

```bash
cd backend
mvn test          # run the test suite (no cloud account needed — uses the in-memory adapter)
mvn spring-boot:run
# API docs at http://localhost:8080/swagger-ui.html
```

The app runs fully **offline** against an in-memory data adapter. To use Firestore, set the
`firestore` Spring profile and provide Firebase credentials (see `docs/`).

### Frontend

```bash
cd frontend
npm install
npm run dev
```

---

## ✅ Testing

Unit + integration tests run against the in-memory repository, so the whole suite is green
with no external services:

```bash
cd backend && mvn test
```

Design patterns, the atomic booking guard (incl. a 500-thread uniqueness test), and the
billing + revenue-split maths are all covered. See `docs/traceability.md` for the
requirement → test mapping.

---

## 🌱 Environments & deployment

Three environments as Firebase Hosting channels on one project, promoted through Git:

```
feature/* → develop (dev) → qa → main (prod)     tags: v0.1.0 … v1.0.0
```

GitHub Actions builds & tests every PR, deploys `develop` to **dev**, runs the full suite on
**qa**, and deploys tagged releases to **prod**. Production deploys require an approval
(GitHub Environments protection rule).

---

## 📄 Licence

Coursework project — for educational assessment.
