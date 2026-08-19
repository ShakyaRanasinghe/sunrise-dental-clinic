# Migration Plan

How the working code in `layered/` becomes the structure in `modular/`.

Eight steps, one commit each, `layered/` left standing as the reference to diff against. This plan
previously existed only outside the repository, which meant the sequencing for every step below was
lost if the conversation that produced it was.

| | |
|---|---|
| Source | [`../../layered/`](../../layered/) — 125 classes, 15 views, 48 passing tests |
| Target | `modular/` — scaffolding complete, no code yet |
| Order | Dependency order, so every step compiles |
| Estimate | 7–9 hours of focused work |

---

## 1. Two rules the plan is built on

**Move, don't rewrite.** Every line of working logic is keepable; what is wrong is where it lives.
Rewriting throws away 48 passing tests, a working double-booking guard and four functioning role
screens in exchange for structure a `git mv` also delivers.

**Modules are features, not roles.** Booking one appointment involves a patient booking it, a
receptionist booking on their behalf, a dentist seeing it on their schedule and an administrator
reporting on it. Split by role and that logic is duplicated four times or hoisted into a shared
module every role depends on — which is the layered arrangement again, plus four thin shells. The
proof: the three role dashboards all read appointments, so all three land in one module.

---

## 2. Order of work

The order is not arbitrary. **Each module depends only on modules already migrated**, so the build
compiles and the tests run green at every step. Migrate out of order and the tree is broken until
the last file lands.

| # | Module | Depends on | Classes | Why here |
|---|---|---|---|---|
| 1 | `platform` | nothing | ~24 | Safest first move. Proves the new `pom.xml` and layout work before anything valuable depends on them |
| 2 | `access` | 1 | ~20 | Everything else needs a principal. The role work lands here |
| 3 | `patients` | 1, 2 | ~8 | Small and self-contained. Extract the missing `PatientService` |
| 4 | `scheduling` | 1, 2 | ~20 | The biggest data layer. Nothing depends on it yet, so mistakes are cheap |
| 5 | `appointments` | 1–4 | ~17 | The heart of the system. Slow down here |
| 6 | `billing` | 1, 2, 5 | ~13 | Strategy classes rejoin the feature they price |
| 7 | `notifications` | 1, 5 | ~12 | Natural moment to make email and SMS actually send |
| 8 | `reporting` + `feedback` | 1, 2, 5, 6 | ~14 | Last, because they read from billing and appointments |

---

## 3. Where every class goes

Legend: **MOVE** package declaration and imports only · **SPLIT** one class becomes several ·
**FIX** a defect closes on the way · **NEW** does not exist yet.

### Step 1 — `platform`

| Destination | Classes | From | |
|---|---|---|---|
| `platform/config` | `AppConfig` | `config/` | MOVE |
| `platform/db` | `Database` · `PooledConnection` | `db/` | MOVE |
| `platform/json` | `Json` | `json/` | **FIX** — add a non-record branch; see §6 |
| `platform/di` | `AppContext` · `ClinicServletContext` | `app/` | FIX |
| `platform/error` | `DataAccessException` · `ResourceNotFoundException` · `SlotUnavailableException` · `ErrorResponse` | `dao/` `exception/` `dto/` | MOVE |
| `platform/data` | `Repository<T,ID>` · `InMemoryRepository` · `TransactionRunner` · `JdbcDao` · `JdbcTransactionRunner` · `SerialTransactionRunner` | `repository/` `dao/` | MOVE |
| `platform/web` | `BaseServlet` · `PageServlet` · `HelpServlet` | `web/` | MOVE |
| `platform/audit` | `AuditEvent` · `AuditRepository` · `AuditDao` · `InMemoryAuditRepository` · `AuditObserver` | `domain/` `repository/` `dao/` `service/` | MOVE |

### Step 2 — `access`

| Destination | Classes | From | |
|---|---|---|---|
| `access/domain` | `UserAccount` · `Role` · `ClinicPrincipal` | `domain/` `security/` | MOVE |
| `access/domain` | `RolePolicy` + `PatientPolicy` `ReceptionPolicy` `DentistPolicy` `AdminPolicy` · `Action` | — | **NEW** — FR-OOP-04 |
| `access/service` | `AuthService` · `PasswordHasher` · `LoginAttemptService` · `AccessControl` | `security/` | FIX |
| `access/service` | `UserAccountFactory` | — | **NEW** — FR-ADM-20…22 |
| `access/data` | `UserRepository` · `UserDao` · `InMemoryUserRepository` | `repository/` `dao/` | MOVE |
| `access/web` | `AuthenticationFilter` · `HomeServlet` · `LogoutServlet` · `RegisterServlet` · `AuthApiServlet` | `security/` `web/` | MOVE |
| `access/web` | `AbstractLoginServlet` + four portals · `PortalChooserServlet` · `AccountApiServlet` | — | **NEW** — FR-AUTH-01…03 |

### Step 3 — `patients`

| Destination | Classes | From | |
|---|---|---|---|
| `patients/domain` | `Patient` | `domain/` | MOVE |
| `patients/domain` | `PatientResponse` · `PatientNote` · `NoteCategory` · `PatientNoteResponse` | — | **NEW** |
| `patients/service` | `PatientService` · `PatientNoteService` | — | **NEW** |
| `patients/data` | `PatientRepository` · `PatientDao` · `InMemoryPatientRepository` | `repository/` `dao/` | MOVE |
| `patients/data` | `PatientNoteRepository` · `PatientNoteDao` · in-memory | — | **NEW** |
| `patients/web` | `PatientRecordsServlet` · `PatientApiServlet` | `web/` | **FIX** — route through the service |
| `patients/web` | `PatientProfileServlet` | — | **NEW** |

### Step 4 — `scheduling`

| Destination | Classes | From | |
|---|---|---|---|
| `scheduling/domain` | `Dentist` · `DentistSession` · `Slot` · `SlotStatus` · `Treatment` · `SlotResponse` | `domain/` `dto/` | MOVE |
| `scheduling/domain` | `DentistResponse` · `TreatmentResponse` | — | **NEW** — closes the `toString()` defect |
| `scheduling/service` | `SlotService` | `service/` | MOVE |
| `scheduling/service` | `ReferenceService` | — | **NEW** |
| `scheduling/data` | `Dentist·Session·Slot·Treatment` repositories with their Dao and in-memory pairs (12) | `repository/` `dao/` | MOVE |
| `scheduling/web` | `AvailabilityPageServlet` · `AvailabilityApiServlet` · `ReferenceApiServlet` | `web/` | FIX |

### Step 5 — `appointments`

| Destination | Classes | From | |
|---|---|---|---|
| `appointments/domain` | `Appointment` · `AppointmentStatus` · `AppointmentResponse` · `AppointmentDetailResponse` | `domain/` `dto/` | MOVE |
| `appointments/service` | `AppointmentService` · `AppointmentNumberGenerator` · `AppointmentEvent` · `AppointmentEventPublisher` · `AppointmentObserver` · `ClinicAccess` | `service/` `pattern/` `security/` | FIX |
| `appointments/data` | `AppointmentRepository` · `AppointmentDao` · in-memory · `CounterRepository` | `repository/` `dao/` | MOVE |
| `appointments/web` | `BookAppointmentServlet` · `PatientHomeServlet` · `ReceptionDayServlet` · `DentistScheduleServlet` · `AppointmentApiServlet` | `web/` | **FIX** — five servlets, four of them currently reach a repository |

### Step 6 — `billing`

| Destination | Classes | From | |
|---|---|---|---|
| `billing/domain` | `Bill` · `BillBreakdown` · `RevenueSplit` · `BillResponse` | `domain/` `pattern/billing/` `dto/` | MOVE |
| `billing/service` | `BillingService` · `BillingStrategy` · `StandardBillingStrategy` · `RevenueSplitStrategy` · `DefaultRevenueSplitStrategy` | `service/` `pattern/billing/` | **FIX** — add the duplicate-bill check |
| `billing/data` | `BillRepository` · `BillDao` · in-memory | `repository/` `dao/` | **FIX** — stop the upsert masking a duplicate |
| `billing/web` | `BillingPageServlet` · `ReceiptServlet` | `web/` | FIX |

### Step 7 — `notifications`

| Destination | Classes | From | |
|---|---|---|---|
| `notifications/domain` | `Notification` · `NotificationStatus` · `ChannelType` · `DispatchResult` | `domain/` `pattern/factory/` | MOVE |
| `notifications/service` | `NotificationObserver` · `NotificationChannelFactory` · `NotificationChannel` · `EmailChannel` · `SmsChannel` | `service/` `pattern/factory/` | **FIX** — make them actually send |
| `notifications/data` | `NotificationRepository` · `NotificationDao` · in-memory | `repository/` `dao/` | MOVE |

### Step 8 — `reporting` and `feedback`

| Destination | Classes | From | |
|---|---|---|---|
| `reporting/service` | `ReportService` | `service/` | **FIX** — the one service reaching a concrete `BillDao` |
| `reporting/data` | `ReportRepository` · `JdbcReportDao` · in-memory | — | **NEW** |
| `reporting/web` | `AdminReportsServlet` · `CsvExportServlet` · `AccountsServlet` · `AuditTrailServlet` | `web/` | FIX + NEW |
| `feedback/domain` | `Complaint` · `ComplaintCategory` · `ComplaintStatus` · `DentistReview` · `RatingSummary` | — | **NEW** |
| `feedback/service` | `ComplaintService` · `ReviewService` | — | **NEW** |
| `feedback/data` | `ComplaintRepository` · `ReviewRepository` + Dao and in-memory | — | **NEW** |
| `feedback/web` | `PatientComplaintsServlet` · `AdminComplaintsServlet` · `ComplaintApiServlet` · `ReviewApiServlet` | — | **NEW** |

### Dissolved

`dto/`, `mapper/` and `pattern/` disappear. `ClinicMapper`'s four methods **SPLIT** three ways:
`toSlotResponse` → `scheduling`, `toAppointmentResponse` and `toAppointmentDetail` →
`appointments`, `toBillResponse` → `billing`.

---

## 4. The views move too

Fifteen JSP files sit in folders named after roles. Regrouping them by feature means the servlet and
the page it renders live under the same name — and it means changing the `render("…")` string in each
servlet as you go. Targets are the View column of [`servlets.md`](servlets.md) §3.

| Now | Becomes |
|---|---|
| `login.jsp` · `register.jsp` | `access/` — plus `login-form.jspf` and four portal views, **new** |
| `reception/patients.jsp` | `patients/records.jsp` |
| `reception/availability.jsp` | `scheduling/availability.jsp` |
| `patient/book.jsp` · `patient/home.jsp` · `reception/home.jsp` · `dentist/schedule.jsp` | `appointments/` |
| `reception/billing.jsp` | `billing/billing.jsp` — plus `receipt.jsp`, **new** |
| `admin/reports.jsp` | `reporting/reports.jsp` — plus `accounts.jsp`, `audit.jsp`, **new** |
| `layout/*.jspf` · `error.jsp` · `not-found.jsp` · `help.jsp` | `shared/` |
| — | `feedback/patient-complaints.jsp` · `admin-complaints.jsp`, **new** |

The [prototype](prototype/) is the source for every new view: the HTML is written and uses the real
stylesheet, so translating a screen is adding JSP tags to markup that already renders.

---

## 5. Verifying each step

Four gates, in order of cost. The safety net is what makes this a refactor rather than a gamble.

| # | Gate | Command | Why it works |
|---|---|---|---|
| 1 | It compiles | `mvn -f modular/pom.xml compile` | Because the order follows the dependency graph, this should never fail for a missing class. If it does, the module boundary is wrong and that is worth knowing at once |
| 2 | Its tests pass | `mvn -f modular/pom.xml test` | Move each module's tests with it. The in-memory repositories mean no database, so the gate stays fast. All 48 green before the commit |
| 3 | The boundary holds | the two greps in [`architecture/3-growth.md`](architecture/3-growth.md) §2 | An import grep alone finds 2 of `layered/`'s 12 violations; both checks are needed |
| 4 | Behaviour is unchanged | deploy both WARs, `curl` the same routes, `diff` | After step 8. A clean diff is proof the refactor changed structure and not behaviour — and it is exactly the evidence Task C's "carry out relevant tests" wants |

Gate 4 is worth writing as a script. It turns "I think it still works" into a repeatable check, and
it gives a project whose tests are all unit-level a genuine regression suite.

---

## 6. Defects to fix on the way, not after

Ten known defects, each documented elsewhere and each cheapest to close while its class is already
open. Fixing them in `layered/` as well would mean doing the work twice.

| Step | Defect | Where documented |
|---|---|---|
| 1 | `Json.write` falls through to `toString()` for non-records — the cause of five broken endpoints | [`api/README.md`](api/README.md) §1 |
| 2 | Lock-out state is held in memory, so any restart clears it, though `user_account` has the columns | [`architecture/3-growth.md`](architecture/3-growth.md) §4 |
| 2 | `GET /api/auth/lock-status` has no role check — any caller can probe any address | [`api/auth.md`](api/auth.md) |
| 3 | `POST /api/patients` has no role check, and inherits the caller's uid as `user_uid` | [`api/patients.md`](api/patients.md) |
| 4 | An unknown `dentistId` on `POST /api/sessions` produces a `500`, not a `404` | [`api/availability.md`](api/availability.md) |
| 4 | Overlapping sessions are accepted, and silently reparent shared slots | [`api/availability.md`](api/availability.md) |
| 4 | Past-dated availability is accepted | [`api/availability.md`](api/availability.md) |
| 5 | `GET /api/appointments` does not exist — no way to list | [`api/README.md`](api/README.md) §2 |
| 5 | `complete` checks the role but not that the appointment is the caller's | [`api/appointments.md`](api/appointments.md) |
| 6 | Billing twice answers `201` with an id that was never persisted | [`api/appointments.md`](api/appointments.md) |
| 6 | `GET /api/appointments/{no}/bill` has no ownership check | [`api/README.md`](api/README.md) §6 |
| 8 | `ReportService` imports concrete `BillDao` — the one service past its port | [`class-diagram.md`](class-diagram.md) |

Several are now enforced by the database as well: `procedures.sql` refuses a bill on an incomplete
appointment, a review on an appointment that has not happened, and a bill credited to a
non-receptionist. Those triggers hold whatever the application forgets.

---

## 7. Rules while both folders exist

**Freeze `layered/`.** No new features, no refactors. It is the reference implementation and the
fallback. The only edits it accepts are fixes for defects that would block submission.

**One commit per module,** named for it — `refactor(billing): move to feature module`. Eight
readable commits across several days is better Task D evidence than one enormous rename, and
rollback becomes dropping a commit rather than unpicking a mess.

**Decide what ships before starting.** If the migration is unfinished at the deadline, `layered/` is
what gets submitted and the report must describe *it* — not the structure you were partway to.
Writing the report against a half-migrated tree is the one way this work costs marks instead of
earning them.

---

## 8. Honest cost

**7 to 9 hours** of focused work. An earlier estimate of 3–4 was for a pure package move; this plan
folds in the real fixes — the role policy hierarchy, the account factory, the missing services, the
report repository, splitting the mapper, regrouping the views, the ten defects — and those roughly
double it.

The mechanical moves are fast and low-risk. Two parts deserve the slow lane:

- **Rewiring `AppContext`**, which currently exposes 22 accessors across 244 lines
- **The 32 fully-qualified servlet class names in `web.xml`** — already written, so this is a
  matching exercise rather than an authoring one, but a typo produces a runtime failure no compiler
  will catch

---

## 9. What is already done

Scaffolding, so step 1 can start on code rather than on setup.

| Artefact | State |
|---|---|
| `modular/pom.xml` | Written — `clinic-modular`, same five dependencies |
| Module directories | 9 modules × 4 sub-packages, plus `platform`'s 8 | 
| Test packages | One per module |
| `schema.sql` | 14 tables, 24 foreign keys, 1 check constraint. **Verified — loads into MySQL 8** |
| `procedures.sql` | 1 procedure, 2 functions, 5 triggers. **Verified — all nine behaviours exercised against a live database** |
| `demo-data.sql` | Five accounts, three patients, three medical notes, two complaints |
| `clinic.properties` | Copied; every key overridable by environment variable |
| `web.xml` | 32 servlets, 35 mappings, 1 filter. **Verified — parses, no duplicate patterns, every route matches [`servlets.md`](servlets.md) §6** |
| Views | Prototype exists for all 19 screens, using the real stylesheet |

What step 1 starts with, therefore, is moving 24 classes into `platform/` — not deciding anything.

A tickable checklist for all of it, including the step-0 decision that gates everything, is in
[`imp/tasks.md`](imp/tasks.md). That directory is a temporary working board and is deleted when the
implementation is finished.
