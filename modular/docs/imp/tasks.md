# Implementation Tasks

Temporary working checklist. Status board in [`README.md`](README.md); the durable plan is
[`../migration-plan.md`](../migration-plan.md).

Tick a box when it is **committed**, not when it compiles.

---

## Step 0 — Decide what ships ← **start here**

Blocks every step below. Two of these tasks are worth doing whatever the answer is.

- [ ] **Get the deadline** from the Moodle submission point. Write it here: `____________`
- [ ] Choose the branch from [`README.md`](README.md) and write the choice here: `____________`
- [ ] **Open a PR from `develop` to `main`.** `main` is 6 commits behind and still contains Spring
      Boot, React and Firebase. Whatever ships goes through `main`, so this is due regardless
- [ ] **Fix `Json.write` in `layered/`** — add a branch for non-record objects, or map the five
      endpoints to response records. This is the one defect worth breaking the freeze for: without
      it `GET /api/dentists` answers `["Dentist{id=d-silva}"]` and the Task B web-service tier does
      not work. ~30 lines. `layered/src/main/java/com/sunrise/clinic/json/Json.java`
- [ ] Re-run `layered/` and confirm `/api/dentists` and `/api/treatments` return usable JSON
- [ ] Decide whether the four login portals are built in `layered/` too, or only in `modular/`

**Gate:** the deadline is written down and the choice is recorded. Nothing below starts until then.

---

## Step 1 — `platform` · ~24 classes · no business logic

Safest first move. Proves the new layout and `pom.xml` before anything valuable depends on them.

- [ ] `platform/config` ← `AppConfig`
- [ ] `platform/db` ← `Database`, `PooledConnection`
- [ ] `platform/json` ← `Json` — **and fix the non-record branch here** (the defect from step 0)
- [ ] `platform/error` ← `DataAccessException`, `ResourceNotFoundException`,
      `SlotUnavailableException`, `ErrorResponse`
- [ ] `platform/data` ← `Repository<T,ID>`, `InMemoryRepository`, `TransactionRunner`, `JdbcDao`,
      `JdbcTransactionRunner`, `SerialTransactionRunner`
- [ ] `platform/web` ← `BaseServlet`, `PageServlet`, `HelpServlet`
- [ ] `platform/audit` ← `AuditEvent`, `AuditRepository`, `AuditDao`,
      `InMemoryAuditRepository`, `AuditObserver`
- [ ] `platform/di` ← `AppContext`, `ClinicServletContext` — will not compile fully until step 8;
      stub the accessors it cannot satisfy yet and keep a `TODO` list in the class
- [ ] Move `JsonTest` to `test/…/platform/`
- [ ] Write `HmacSignerTest`? **No** — there is no JWT. Skip
- [ ] Delete the `.gitkeep` in every directory that now holds a class
- [ ] Gate 1 + 2 pass
- [ ] `refactor(platform): move shared machinery to modular`

**Watch for:** `AppContext` is the one class that cannot be finished in this step. Accept a
partially-wired context and revisit it at the end of every later step.

---

## Step 2 — `access` · ~20 classes · the four portals land here

Everything else needs a principal.

### Move
- [ ] `access/domain` ← `UserAccount`, `Role`, `ClinicPrincipal`
- [ ] `access/service` ← `AuthService`, `PasswordHasher`, `LoginAttemptService`, `AccessControl`
- [ ] `access/data` ← `UserRepository`, `UserDao`, `InMemoryUserRepository`
- [ ] `access/web` ← `AuthenticationFilter`, `HomeServlet`, `LogoutServlet`, `RegisterServlet`,
      `AuthApiServlet`
- [ ] Move `AuthServiceTest`, `PasswordHasherTest`, `LoginAttemptServiceTest`

### New
- [ ] `access/domain/Action` — the enum of things a role may do
- [ ] `access/domain/RolePolicy` abstract + `PatientPolicy`, `ReceptionPolicy`, `DentistPolicy`,
      `AdminPolicy` — FR-OOP-04
- [ ] Replace the 28 `Role.X` checks with `AccessControl.require(user, Action)` — grep
      `Role\.\(PATIENT\|RECEPTIONIST\|DENTIST\|ADMIN\)` to find them all
- [ ] `access/web/AbstractLoginServlet` — the whole authentication sequence, once
- [ ] `PatientLoginServlet`, `ReceptionLoginServlet`, `DentistLoginServlet`, `AdminLoginServlet` —
      4–6 lines each
- [ ] `access/web/PortalChooserServlet`
- [ ] `access/service/UserAccountFactory` — FR-ADM-20…22
- [ ] `access/web/AccountApiServlet` — `POST /api/accounts`
- [ ] Views: `access/login-form.jspf` + four portal JSPs + `portal-chooser.jsp`, from
      [`../prototype/access/`](../prototype/access/)
- [ ] `RolePolicyTest`, `AbstractLoginServletTest`

### Fix on the way
- [ ] Persist lock-out state — `user_account.failed_attempts` and `locked` exist and nothing
      writes them, so any restart clears every lock
- [ ] `GET /api/auth/lock-status` requires `ADMIN`

- [ ] Gate 1 + 2 + 3 pass
- [ ] `refactor(access): move identity and add the four role portals`

---

## Step 3 — `patients` · ~8 classes + notes

Small and self-contained.

- [ ] `patients/domain` ← `Patient`; **new** `PatientResponse` — closes part of the `toString()` defect
- [ ] `patients/data` ← `PatientRepository`, `PatientDao`, `InMemoryPatientRepository`
- [ ] `patients/web` ← `PatientRecordsServlet`, `PatientApiServlet`
- [ ] **New** `patients/service/PatientService` — the two servlets currently call the repository
      directly
- [ ] **New** `PatientNote`, `NoteCategory`, `PatientNoteResponse`
- [ ] **New** `PatientNoteRepository`, `PatientNoteDao`, in-memory
- [ ] **New** `PatientProfileServlet` + `patients/profile.jsp`, from
      [`../prototype/patient/profile.html`](../prototype/patient/profile.html)
- [ ] Note endpoints on `PatientApiServlet` — `/api/patients/{id}/notes`, all four methods
- [ ] `PatientServiceTest`, `PatientNoteServiceTest`

### Fix on the way
- [ ] `POST /api/patients` requires `RECEPTIONIST` or `ADMIN`
- [ ] It must **not** inherit the caller's uid as `user_uid` — that is what gave one patient two
      profile rows

- [ ] Gate 1 + 2 + 3 pass
- [ ] `refactor(patients): move register and add medical notes`

---

## Step 4 — `scheduling` · ~20 classes · biggest data layer

Nothing depends on it yet, so mistakes are cheap.

- [ ] `scheduling/domain` ← `Dentist`, `DentistSession`, `Slot`, `SlotStatus`, `Treatment`,
      `SlotResponse`
- [ ] **New** `DentistResponse`, `TreatmentResponse` — **finishes the `toString()` defect**
- [ ] `scheduling/service` ← `SlotService`; **new** `ReferenceService`
- [ ] `scheduling/data` ← 4 repositories × (interface + Dao + in-memory) = 12 classes
- [ ] `scheduling/web` ← `AvailabilityPageServlet`, `AvailabilityApiServlet`, `ReferenceApiServlet`
- [ ] Views: `scheduling/availability.jsp`
- [ ] `SlotServiceTest`

### Fix on the way
- [ ] Unknown `dentistId` on `POST /api/sessions` → `404`, not `500`
- [ ] Reject a session overlapping one already published for that dentist
- [ ] Reject a past `date`
- [ ] Warn when the span does not divide evenly into slots

- [ ] Gate 1 + 2 + 3 pass
- [ ] `refactor(scheduling): move dentists, sessions, slots and treatments`

---

## Step 5 — `appointments` · ~17 classes · **slow down here**

The heart of the system. The `SELECT … FOR UPDATE` guard and its concurrency test move together.

- [ ] `appointments/domain` ← `Appointment`, `AppointmentStatus`, `AppointmentResponse`,
      `AppointmentDetailResponse`
- [ ] Add `patientNotes` and `hasCriticalNotes` to `AppointmentDetailResponse` — FR-NOTE-07/08
- [ ] `appointments/service` ← `AppointmentService`, `AppointmentNumberGenerator`,
      `AppointmentEvent`, `AppointmentEventPublisher`, `AppointmentObserver`, `ClinicAccess`
- [ ] `ClinicAccess.canViewPatientNotes(...)` — one gate for both clinical fields
- [ ] `appointments/data` ← `AppointmentRepository`, `AppointmentDao`, in-memory,
      `CounterRepository`
- [ ] `appointments/web` ← all five servlets. **Four of them currently reach a repository** — route
      every one through `AppointmentService`
- [ ] Move behaviour onto the entities: `Appointment.canTransitionTo()`, `complete()`, `cancel()`;
      `Slot.isOpen()`, `bookFor()`, `release()`
- [ ] Views: `appointments/book.jsp`, `patient-home.jsp`, `reception-day.jsp`,
      `dentist-schedule.jsp` — the dentist one needs the critical-notes banner
- [ ] Move `AppointmentServiceTest`, **`BookingConcurrencyTest`**, `AppointmentNumberGeneratorTest`

### Fix on the way
- [ ] Implement `GET /api/appointments` — it does not exist, so the API cannot answer "what is
      booked?"
- [ ] `complete` must verify the appointment belongs to the calling dentist, not just the role
- [ ] Enforce the status machine centrally — cancelling a `BILLED` appointment is currently possible

- [ ] Gate 1 + 2 + 3 pass, and `BookingConcurrencyTest` specifically
- [ ] `refactor(appointments): move booking and the role dashboards`

---

## Step 6 — `billing` · ~13 classes

- [ ] `billing/domain` ← `Bill`, `BillBreakdown`, `RevenueSplit`, `BillResponse`
- [ ] `billing/service` ← `BillingService`, `BillingStrategy`, `StandardBillingStrategy`,
      `RevenueSplitStrategy`, `DefaultRevenueSplitStrategy`
- [ ] `billing/data` ← `BillRepository`, `BillDao`, in-memory
- [ ] `billing/web` ← `BillingPageServlet`; **new** `ReceiptServlet` + a print layout — FR-REC-52
- [ ] Views: `billing/billing.jsp`, `billing/receipt.jsp`
- [ ] Move `BillingServiceTest`, `StandardBillingStrategyTest`, `DefaultRevenueSplitStrategyTest`

### Fix on the way
- [ ] **Check for an existing bill before building a new one.** Today the second call answers `201`
      with an id that was never persisted, because `BillDao` upserts
- [ ] Stop `BillDao` masking a duplicate — a second insert should conflict, not update
- [ ] `GET /api/appointments/{no}/bill` needs an ownership check
- [ ] Refuse to bill an appointment that is not `COMPLETED` — the trigger already does; the service
      should say so in plain language first
- [ ] Add a test that billing twice fails

- [ ] Gate 1 + 2 + 3 pass
- [ ] `refactor(billing): move bills, pricing and the revenue split`

---

## Step 7 — `notifications` · ~12 classes

Natural moment to make delivery real.

- [ ] `notifications/domain` ← `Notification`, `NotificationStatus`, `ChannelType`,
      `DispatchResult`
- [ ] `notifications/service` ← `NotificationObserver`, `NotificationChannelFactory`,
      `NotificationChannel`, `EmailChannel`, `SmsChannel`
- [ ] `notifications/data` ← `NotificationRepository`, `NotificationDao`, in-memory
- [ ] **Make `EmailChannel` actually send** over SMTP — FR-NOT-01 is currently *recorded, not sent*
- [ ] **Make `SmsChannel` actually send** over an HTTP gateway — FR-NOT-02
- [ ] Config keys for SMTP host and SMS gateway in `clinic.properties`, overridable by env var
- [ ] Confirm a delivery failure never fails the booking — FR-NOT-04
- [ ] Move `NotificationChannelFactoryTest`; add a channel test with a stubbed transport

- [ ] Gate 1 + 2 + 3 pass
- [ ] `feat(notifications): send confirmation email and reminder SMS`

---

## Step 8 — `reporting` and `feedback` · ~14 classes

Last, because they read from billing and appointments.

### reporting
- [ ] `reporting/service` ← `ReportService`
- [ ] **New** `reporting/data/ReportRepository`, `JdbcReportDao`, in-memory — closes the one
      service that imports a concrete `BillDao`
- [ ] `reporting/web` ← `AdminReportsServlet`; **new** `CsvExportServlet`, `AccountsServlet`,
      `AuditTrailServlet`
- [ ] Views: `reporting/reports.jsp`, `accounts.jsp`, `audit.jsp`
- [ ] Add the "Supports:" line under every report — FR-ADM-16
- [ ] `ReportServiceTest`

### feedback
- [ ] **New** `Complaint`, `ComplaintCategory`, `ComplaintStatus`, `ComplaintResponse`
- [ ] **New** `DentistReview`, `RatingSummary`, `ReviewResponse`
- [ ] **New** `ComplaintService`, `ReviewService`
- [ ] **New** `ComplaintRepository`, `ReviewRepository` + Dao and in-memory
- [ ] **New** `PatientComplaintsServlet`, `AdminComplaintsServlet`, `ComplaintApiServlet`,
      `ReviewApiServlet`
- [ ] Views: `feedback/patient-complaints.jsp`, `admin-complaints.jsp`
- [ ] Confirm the dentist can reach **no** complaint route — FR-CMP-08
- [ ] Confirm a dentist receives `RatingSummary` and never a `ReviewResponse` — NFR-SEC-13
- [ ] `ComplaintServiceTest`, `ReviewServiceTest` including the 30-day window and the five-review floor

- [ ] **Finish `AppContext`** — every accessor now has a real implementation
- [ ] Delete every remaining `.gitkeep`
- [ ] Gate 1 + 2 + 3 pass
- [ ] `refactor(reporting): move reports` and `feat(feedback): complaints and reviews`

---

## Step 9 — Verify and report

- [ ] `mvn -f modular/pom.xml clean package` produces a WAR
- [ ] Load `schema.sql`, `procedures.sql`, `demo-data.sql` into a fresh database
- [ ] Deploy `modular/` to Tomcat 10.1 and sign in as each of the four roles
- [ ] **Gate 4** — deploy both WARs side by side, `curl` the same routes, `diff` the HTML. A clean
      diff is the proof that structure changed and behaviour did not
- [ ] Write the diff script into `modular/docs/` — it is a regression suite for a project whose
      tests are all unit-level
- [ ] Update every `Specified` status in the SRS that is now `Built`
- [ ] Update `layered/` references in `README.md` and `CONTRIBUTING.md` to describe both folders
- [ ] Rewrite the deploy workflows — they still run `npm ci` in the deleted `frontend/` and push to
      Firebase Hosting
- [ ] Tag a release
- [ ] PR `develop` → `main`
- [ ] **Delete `modular/docs/imp/`** in the final commit

---

## Running count

| Step | Classes | Of which new | Fixes closed |
|---|---|---|---|
| 0 | — | — | 1 |
| 1 platform | 24 | 0 | 1 |
| 2 access | 20 + 11 new | 11 | 2 |
| 3 patients | 8 + 7 new | 7 | 2 |
| 4 scheduling | 20 + 2 new | 2 | 4 |
| 5 appointments | 17 | 0 | 3 |
| 6 billing | 13 | 0 | 4 |
| 7 notifications | 12 | 0 | 2 |
| 8 reporting + feedback | 5 + 18 new | 18 | 1 |
| **total** | **~157** | **~38** | **20** |

The class count grows from 125 to about 157 — the new work is the four portals, the role policies,
the account factory, medical notes, complaints and reviews. Twenty documented defects close along
the way, which is the argument for fixing them during the move rather than after it.
