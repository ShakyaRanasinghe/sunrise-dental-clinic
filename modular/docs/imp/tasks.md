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

**On screen after this step:** `/help` renders, and nothing else. That is the point — it proves the
layout, the `pom.xml`, JSP compilation and the JSTL fallback locale before anything valuable depends
on them.

**Watch for:** `AppContext` is the one class that cannot be finished in this step. Accept a
partially-wired context and revisit it at the end of every later step.

---

## Step 2 — `access` + stub landings · ~20 classes · **first deploy**

Everything else needs a principal. This step also adds four stub landing pages, so that the moment
sign-in works it goes somewhere rather than to a 404 — see [`README.md`](README.md) for why that
matters.

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

### Stub landings — four small JSPs so sign-in has somewhere to go
- [ ] `shared/stub-home.jspf` — the real header and navigation from
      [`../prototype/`](../prototype/), plus "Signed in as … — this screen arrives in step N"
- [ ] `appointments/patient-home.jsp`, `reception-day.jsp`, `dentist-schedule.jsp` — stubs including
      only the stub fragment. Step 4 fills them in
- [ ] `reporting/reports.jsp` — stub. Step 7 fills it in
- [ ] Four throwaway servlets to render them, or one `StubHomeServlet` mapped four times and deleted
      at step 4. **Note in the class comment that it is temporary**

### Fix on the way
- [ ] Persist lock-out state — `user_account.failed_attempts` and `locked` exist and nothing
      writes them, so any restart clears every lock
- [ ] `GET /api/auth/lock-status` requires `ADMIN`

- [ ] Gate 1 + 2 + 3 pass
- [ ] **Deploy it.** `mvn package`, drop the WAR on Tomcat 10.1, load `schema.sql` +
      `procedures.sql` + `demo-data.sql`
- [ ] Sign in through all four portals with the seeded accounts and land on a page
- [ ] Confirm a wrong-portal sign-in gives the same message as a wrong password — FR-AUTH-03
- [ ] Confirm five failures lock the account, and an administrator can unlock it
- [ ] `refactor(access): move identity and add the four role portals`

**On screen after this step — the first deployable increment.** The portal chooser, four distinct
sign-in pages, role routing to four landing pages, session timeout, sign-out, lock-out. That
demonstrates the brief's requirement 1 (*user authentication*) and requirement 6 (*exit system*) on
its own.

---

## Step 3 — `patients` + `scheduling` · ~28 classes · two commits

Merged, because neither owns a role landing page: as separate steps they were two stretches with no
visible change. They are independent of each other, so commit them separately inside one step.

Medical notes are **deferred to step 8** — they need nothing from here and the register is the part
`appointments` depends on.

### 3a — `patients`

- [ ] `patients/domain` ← `Patient`; **new** `PatientResponse` — closes part of the `toString()` defect
- [ ] `patients/data` ← `PatientRepository`, `PatientDao`, `InMemoryPatientRepository`
- [ ] `patients/web` ← `PatientRecordsServlet`, `PatientApiServlet`
- [ ] **New** `patients/service/PatientService` — the two servlets currently call the repository
      directly
- [ ] `PatientServiceTest`

### Fix on the way
- [ ] `POST /api/patients` requires `RECEPTIONIST` or `ADMIN`
- [ ] It must **not** inherit the caller's uid as `user_uid` — that is what gave one patient two
      profile rows

- [ ] `refactor(patients): move the register and extract PatientService`

### 3b — `scheduling` · biggest data layer

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

**On screen after this step:** the patient register with search and walk-in registration, and
publish-availability. `GET /api/dentists` and `/api/treatments` return real JSON for the first time,
which is the `toString()` defect closed and visible.

---

## Step 4 — `appointments` · ~17 classes · **the demo moment, and slow down here**

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

- [ ] **Delete the stub landing servlet and the stub fragment** from step 2
- [ ] Gate 1 + 2 + 3 pass, and `BookingConcurrencyTest` specifically
- [ ] **Deploy and walk the whole journey**: reception publishes availability, a patient books,
      reception sees it on the day view, the dentist sees it on their schedule
- [ ] `refactor(appointments): move booking and the role dashboards`

**On screen after this step — the application starts looking like the prototype.** Three real
dashboards on real data, and booking working end to end including the concurrency guard. This
demonstrates the brief's requirements 2 (*register new appointment*) and 3 (*display appointment
details*), so with step 2 that is three of the six. **A defensible stopping point.**

---

## Step 5 — `billing` · ~13 classes

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

**On screen after this step — all six of the brief's functions work.** Authentication, register an
appointment, display it by number, calculate and print the bill, help, exit. **The strongest
stopping point if time runs short**: what remains after this is beyond the brief.

---

## Step 6 — `notifications` · ~12 classes

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

**On screen after this step:** nothing new, but a real email arrives after a booking. Verify with a
local SMTP catcher rather than a real mailbox.

---

## Step 7 — `reporting` · ~9 classes

Reads from billing and appointments, so it comes after both.

- [ ] `reporting/service` ← `ReportService`
- [ ] **New** `reporting/data/ReportRepository`, `JdbcReportDao`, in-memory — closes the one
      service that imports a concrete `BillDao`
- [ ] `reporting/web` ← `AdminReportsServlet`; **new** `CsvExportServlet`, `AccountsServlet`,
      `AuditTrailServlet`
- [ ] Views: `reporting/reports.jsp`, `accounts.jsp`, `audit.jsp`
- [ ] Add the "Supports:" line under every report — FR-ADM-16
- [ ] `ReportServiceTest`
- [ ] Gate 1 + 2 + 3 pass
- [ ] `refactor(reporting): move reports and add accounts and audit screens`

**On screen after this step:** the admin's three screens. Create a staff account, unlock a locked
patient, read the audit trail, export a report as CSV.

---

## Step 8 — `feedback` + medical notes · ~25 new classes · all new work

Nothing to move — every class here is new. This is the feature work the earlier steps' migration
made room for, and it is last because it depends on appointments and billing.

### 8a — medical notes, in `patients`
- [ ] **New** `PatientNote`, `NoteCategory`, `PatientNoteResponse`
- [ ] **New** `PatientNoteRepository`, `PatientNoteDao`, in-memory
- [ ] **New** `patients/service/PatientNoteService`
- [ ] **New** `PatientProfileServlet` + `patients/profile.jsp`, from
      [`../prototype/patient/profile.html`](../prototype/patient/profile.html)
- [ ] Note endpoints on `PatientApiServlet` — `/api/patients/{id}/notes`, all four methods
- [ ] Add `patientNotes` and `hasCriticalNotes` to the dentist's appointment view, and the critical
      banner on the schedule — FR-NOTE-07, FR-NOTE-08, FR-DEN-42
- [ ] Confirm reception and the administrator receive a response object with **no field** for them
- [ ] Confirm a patient with no notes renders "None declared", never a blank — FR-NOTE-12
- [ ] `PatientNoteServiceTest`
- [ ] `feat(patients): medical notes declared by the patient`

### 8b — complaints and reviews, in `feedback`
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

- [ ] **Finish `AppContext`** — every accessor now has a real implementation, no stubs left
- [ ] Delete every remaining `.gitkeep`
- [ ] Gate 1 + 2 + 3 pass
- [ ] `feat(feedback): complaints and dentist reviews`

**On screen after this step:** every one of the 24 pages resolves. The prototype and the running
application match.

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

| Step | Classes | Of which new | Fixes closed | Deployable after |
|---|---|---|---|---|
| 0 | — | — | 1 | `layered/` only |
| 1 platform | 24 | 0 | 1 | help page |
| 2 access + stubs | 20 + 11 new | 11 | 2 | **yes — sign in as four roles** |
| 3 patients + scheduling | 28 + 3 new | 3 | 6 | yes — register, availability, real reference JSON |
| 4 appointments | 17 | 0 | 3 | **yes — three dashboards, booking end to end** |
| 5 billing | 13 | 0 | 4 | **yes — all six brief functions** |
| 6 notifications | 12 | 0 | 2 | yes |
| 7 reporting | 5 + 4 new | 4 | 1 | yes — admin screens |
| 8 feedback + notes | 0 + 25 new | 25 | 0 | yes — all 24 pages |
| 9 verify | — | — | — | — |
| **total** | **~162** | **~44** | **20** | |

The class count grows from 125 to about 162. Roughly a quarter of the work is not migration at all —
it is the four portals, the role policies, the account factory, medical notes, complaints and
reviews. Worth knowing before setting the 7–9 hour estimate against a calendar, because the new
features are the part that can be cut if the deadline demands it, and the migration is the part that
cannot be half-done.

---

## If you only have time for some of it

| Stop after | You can demonstrate | Brief requirements covered |
|---|---|---|
| step 2 | Four portals, role routing, lock-out, sign-out | 1, 6 |
| step 4 | The above, plus three dashboards and booking | 1, 2, 3, 6 |
| **step 5** | **The above, plus billing** | **all six** |
| step 8 | Everything, including the features beyond the brief | all six, plus extensions |

Below step 2, `layered/` is the better submission. Between steps 2 and 4, it is a judgement call.
From step 5 onward, `modular/` is strictly better — same behaviour, better structure, twenty fewer
defects.
