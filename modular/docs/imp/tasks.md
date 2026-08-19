# Implementation Tasks

Temporary working checklist. Status board in [`README.md`](README.md); the durable plan is
[`../migration-plan.md`](../migration-plan.md).

Tick a box when it is **committed**, not when it compiles.

---

## Step 0 — Decide what ships ← **start here**

**Decided: `modular/` is the only code path.** `layered/` stays in the repository as the source the
migration copies from and as the git history, but it is not run, maintained or shipped. Nothing in
these tasks edits it.

That removes the fallback, which makes the stopping points at the end of this file the thing to
watch rather than a contingency.

- [ ] **Get the deadline** from the Moodle submission point. Write it here: `____________`
- [ ] Pick the target stopping point from the table at the end of this file: `____________`
- [ ] **Open a PR from `develop` to `main`.** `main` is 8 commits behind and still contains Spring
      Boot, React and Firebase. Whatever ships goes through `main`, so this is due regardless
- [ ] Run `./scripts/dev-up.sh` and confirm it reports *14 tables, 24 foreign keys, 3 routines,
      5 triggers* — see [`../local-setup.md`](../local-setup.md)

**Gate:** the deadline and the target step are written down, and `dev-up.sh` reports a clean
environment. Nothing below starts until then.

---

## Step 1 — `platform` · ✅ **done** · 18 classes moved

Moved, compiling, 15 tests green. `platform` imports nothing outside `platform`, verified by grep.

- [x] `platform/config` ← `AppConfig`
- [x] `platform/db` ← `Database`, `PooledConnection`
- [x] `platform/json` ← `Json` — **plus the `writeBean` branch**, see below
- [x] `platform/error` ← `DataAccessException`, `ResourceNotFoundException`,
      `SlotUnavailableException`, `ErrorResponse`
- [x] `platform/data` ← `Repository<T,ID>`, `InMemoryRepository`, `TransactionRunner`, `JdbcDao`,
      `JdbcTransactionRunner`, `SerialTransactionRunner`
- [x] `platform/audit` ← `AuditEvent`, `AuditRepository`, `AuditDao`, `InMemoryAuditRepository`
- [x] `JsonTest` → `test/…/platform/`, with local fixtures replacing feature types
- [x] `.gitkeep` removed from every directory that now holds a class
- [x] Gate 1 + 2 pass
- [ ] `refactor(platform): move shared machinery to modular`

### Three things the step boundary got wrong, and what was done

**`BaseServlet`, `PageServlet` and `HelpServlet` could not move.** They import `AppContext`,
`ClinicPrincipal`, `AccessControl` and `AuthenticationFilter` — all `access`. `HelpServlet` looked
independent to an import grep but extends `PageServlet`, so it inherits the coupling. **Deferred to
step 2**, which is where `access` lands.

**`AppContext` and `ClinicServletContext` could not move.** `AppContext` imports `mapper`,
`pattern`, `security` and `service` — that is every module. It arrives in step 2 partially wired and
is finished in step 8.

**`AuditObserver` could not move.** It implements `AppointmentObserver` and reads
`AppointmentEvent`, both `appointments`. **Deferred to step 4.** The other four audit classes moved
cleanly once `AuditEvent` stopped referencing `Role` — see below.

### Two design fixes this step owned

**`AuditEvent.actorRole` is now `String`, not `Role`.** `platform` may depend on no feature module
and `Role` belongs to `access`, so the type had to go. Denormalising is right on its own terms as
well: an audit record is a snapshot of what happened, so it must survive the enum being renamed or a
value retired. The database column stays an `ENUM`; the Java side stores its name. `AuditDao` reads
and writes it as text.

**`Json.write` no longer stringifies unknown objects.** The old fallthrough was
`writeString(String.valueOf(value))`, which is why five endpoints answered `200` with
`["Dentist{id=d-silva}"]`. A `writeBean` branch now reads `getX()` and `isX()` accessors. Proved
against a class shaped exactly like `Dentist`:

```
before:  ["Dentist{id=d-silva}"]
after:   [{"consultationFee":1500,"id":"d-silva","name":"Dr. Ranil Silva",
           "specialization":"General Dentistry","active":true}]
```

Three tests cover it, and they fail against the old behaviour. A `requirePublic` guard was added
too, because reflection cannot read a non-public type and the failure otherwise named a property
rather than the cause.

**Mapping to a response record is still the better habit** — a record states exactly which fields
cross the wire and a bean does not. `writeBean` is the safety net for what was never mapped, not a
licence to stop mapping. Steps 3 and 4 still add `PatientResponse`, `DentistResponse` and
`TreatmentResponse`.

---

## Step 2 — `access` + stub landings · ~20 classes · **first deploy**

Everything else needs a principal. This step also adds four stub landing pages, so that the moment
sign-in works it goes somewhere rather than to a 404 — see [`README.md`](README.md) for why that
matters.

### Inherited from step 1 — could not move without `access`
- [x] `platform/web` ← `BaseServlet`, `PageServlet`, `HelpServlet`
- [x] `platform/di` ← `AppContext`, `ClinicServletContext` — partially wired; stub the accessors
      later modules will supply, and keep the `TODO` list in the class

### Move
- [x] `access/domain` ← `UserAccount`, `Role`, `ClinicPrincipal`
- [x] `access/service` ← `AuthService`, `PasswordHasher`, `LoginAttemptService`, `AccessControl`
- [x] `access/data` ← `UserRepository`, `UserDao`, `InMemoryUserRepository`
- [x] `access/web` ← `AuthenticationFilter`, `HomeServlet`, `LogoutServlet`, `RegisterServlet`,
      `AuthApiServlet`
- [x] Move `AuthServiceTest`, `PasswordHasherTest`, `LoginAttemptServiceTest`

### New
- [x] `access/domain/Action` — the enum of things a role may do
- [x] `access/domain/RolePolicy` abstract + `PatientPolicy`, `ReceptionPolicy`, `DentistPolicy`,
      `AdminPolicy` — FR-OOP-04
- [x] Replace the 28 `Role.X` checks with `AccessControl.require(user, Action)` — grep
      `Role\.\(PATIENT\|RECEPTIONIST\|DENTIST\|ADMIN\)` to find them all
- [x] `access/web/AbstractLoginServlet` — the whole authentication sequence, once
- [x] `PatientLoginServlet`, `ReceptionLoginServlet`, `DentistLoginServlet`, `AdminLoginServlet` —
      4–6 lines each
- [x] `access/web/PortalChooserServlet`
- [x] `access/service/UserAccountFactory` — FR-ADM-20…22
- [x] `access/web/AccountApiServlet` — `POST /api/accounts`
- [x] Views: `access/login-form.jspf` + four portal JSPs + `portal-chooser.jsp`, from
      [`../prototype/access/`](../prototype/access/)
- [x] `RolePolicyTest`, `AbstractLoginServletTest`

### Stub landings — four small JSPs so sign-in has somewhere to go
- [x] `shared/stub-home.jspf` — the real header and navigation from
      [`../prototype/`](../prototype/), plus "Signed in as … — this screen arrives in step N"
- [x] `appointments/patient-home.jsp`, `reception-day.jsp`, `dentist-schedule.jsp` — stubs including
      only the stub fragment. Step 4 fills them in
- [x] `reporting/reports.jsp` — stub. Step 7 fills it in
- [x] Four throwaway servlets to render them, or one `StubHomeServlet` mapped four times and deleted
      at step 4. **Note in the class comment that it is temporary**

### Fix on the way
- [x] Persist lock-out state — `user_account.failed_attempts` and `locked` exist and nothing
      writes them, so any restart clears every lock
- [x] `GET /api/auth/lock-status` requires `ADMIN`

- [x] Gate 1 + 2 + 3 pass
- [x] **Deploy it.** `mvn package`, drop the WAR on Tomcat 10.1, load `schema.sql` +
      `procedures.sql` + `demo-data.sql`
- [x] Sign in through all four portals with the seeded accounts and land on a page
- [x] Confirm a wrong-portal sign-in gives the same message as a wrong password — FR-AUTH-03
- [x] Confirm five failures lock the account, and an administrator can unlock it
- [x] `refactor(access): move identity and add the four role portals`

### Found by running it, and fixed in this step
Four defects that reading the code did not surface. Recorded because each is the kind that would
have reached the report as a working feature.

- [x] **A signed-in patient could open all four role landing pages** — 200, not 403.
      `AuthenticationFilter` authenticated and deliberately left every role check to the servlets,
      and `StubHomeServlet` checked nothing. Fixed by adding a *coarse* prefix check to the filter,
      derived from `RolePolicy.ownedPrefix()` so it is not a second list of URL patterns; the
      fine-grained `AccessControl.require` checks stay in the servlets. `RolePolicyTest` now asserts
      the exclusivity the check depends on, so a fifth role cannot quietly break it
- [x] **No page carried `<!doctype html>`** — every screen rendered in quirks mode, so the
      stylesheet's box model was computed against the wrong rules. Nine directive newlines sat where
      the doctype belonged; `trim-directive-whitespaces` in `web.xml` plus the doctype in
      `header.jspf` fixes it for every view at once
- [x] **The lock-out message said "temporarily locked … try again later"** while the same SRS row
      said an administrator must clear it. The 24-hour expiry was removed earlier and the copy was
      not. Corrected in `AuthService`, `help.jsp`, the prototype, `srs-patient.md` and `api/auth.md`,
      along with the `retryAfterSeconds` field that no longer exists
- [x] **An anonymous API call was refused 403 `forbidden` on `/api/auth/lock-status`** but 401
      `unauthenticated` everywhere else — the same condition reported two ways, because
      `/api/auth/` is in the filter's public list and `AccessControl` raised one exception for both
      "not signed in" and "wrong role". Split into `NotAuthenticatedException`

- [x] **The sign-in form posted to the view's own path**, so signing in from a browser answered 404
      while the identical credentials over `curl` succeeded. The action was
      `${pageContext.request.servletPath}`, and inside a forward that is the JSP's path, not the
      request's. The servlet now supplies `loginPath` from `RolePolicy`. Found only by clicking the
      button — every check up to that point had posted straight to the URL. A test now reads the
      fragment and asserts the action, because nothing without a browser can catch it

Also fixed while getting the first deploy to answer: every view used `<c:set>` before
`header.jspf` declared the `c` prefix, which Jasper refuses outright — the taglib directives moved
into `shared/taglibs.jspf`, included first by every view. Static includes are now absolute
(`/WEB-INF/jsp/…`), because a relative `shared/taglibs.jspf` inside `shared/` resolves to
`shared/shared/`.

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

- [x] `patients/domain` ← `Patient`; **new** `PatientResponse` — closes part of the `toString()` defect
- [x] `patients/data` ← `PatientRepository`, `PatientDao`, `InMemoryPatientRepository`
- [x] `patients/web` ← `PatientRecordsServlet`, `PatientApiServlet`
- [x] **New** `patients/service/PatientService` — the two servlets currently call the repository
      directly
- [x] `PatientServiceTest`

### Fix on the way
- [x] `POST /api/patients` requires `RECEPTIONIST` or `ADMIN`
- [x] It must **not** inherit the caller's uid as `user_uid` — that is what gave one patient two
      profile rows

- [x] `refactor(patients): move the register and extract PatientService`

#### Beyond the list, and why
- [x] **`READ_PATIENT_RECORD`, a new action.** The contract lets a dentist read one patient record
      but not list the register, and `SEARCH_PATIENTS` could not express both. Searching the whole
      register is a front-desk capability; reading the record of the patient in the chair is a
      clinical one, so it is two actions rather than one with an exception
- [x] **`REGISTER_PATIENT` granted to `ADMIN`.** It already held `SEARCH_PATIENTS`, `CANCEL_ANY` and
      `ISSUE_BILL`, and the contract says reception *or* admin — lacking this one was an
      inconsistency, and `PatientServiceTest` caught it
- [x] **The duplicate warning, FR-REC-25.** Cheap here and part of what registering means; a warning
      never a refusal, because a household shares a number
- [x] **Navigation declared by the role.** `RolePolicy.navigation()`, so the header loops instead of
      carrying a chain of role tests — a switch on role written in the least testable file in the
      project, edited every time any module adds a screen
- [ ] **Still outstanding: `PUT /api/patients/{id}` — FR-REC-24.** The register can add and search
      but not correct a mistyped number. Not in this step's list; carried forward

#### Found while running it
- [x] **Jakarta EL 5.0 does not resolve a record's accessor as a property.** `${item.path}` on a
      record raised `PropertyNotFoundException`, because EL looks for `getPath()`. Fixed by calling
      the accessor as a method — `${item.path()}` — which the codebase already does for
      `user.policy()`, rather than adding getter boilerplate to every response record. This will
      apply to every `…Response` record in the steps that follow


### 3b — `scheduling` · biggest data layer

- [x] `scheduling/domain` ← `Dentist`, `DentistSession`, `Slot`, `SlotStatus`, `Treatment`,
      `SlotResponse`
- [x] **New** `DentistResponse`, `TreatmentResponse` — **finishes the `toString()` defect**
- [x] `scheduling/service` ← `SlotService`; **new** `ReferenceService`
- [x] `scheduling/data` ← 4 repositories × (interface + Dao + in-memory) = 12 classes
- [x] `scheduling/web` ← `AvailabilityPageServlet`, `AvailabilityApiServlet`, `ReferenceApiServlet`
- [x] Views: `scheduling/availability.jsp`
- [x] `SlotServiceTest`

### Fix on the way
- [x] Unknown `dentistId` on `POST /api/sessions` → `404`, not `500`
- [x] Reject a session overlapping one already published for that dentist
- [x] Reject a past `date`
- [x] Warn when the span does not divide evenly into slots

- [x] Gate 1 + 2 + 3 pass
- [x] `refactor(scheduling): move dentists, sessions, slots and treatments`

#### Beyond the list, and why
- [x] **Money is `BigDecimal`, not `double`.** Every money field in `layered/` was a `double`, while
      the columns are `DECIMAL(10,2)` and `fn_calculate_bill` computes in DECIMAL. A type that cannot
      represent 0.01 exactly will drift from the total the database computes for the same inputs.
      Changed for `Dentist.consultationFee` and `Treatment.baseCost` because this step defines their
      outward contract; **`Bill`'s fourteen money fields follow in step 5**, and will be written
      against `BigDecimal` from the start
- [x] **A fifth validation gap.** A `slotMinutes` longer than the window published a session and
      generated no slots at all — an availability window with nothing bookable in it. Now a 400
- [x] **`findActive` promoted onto `DentistRepository`.** It existed on `DentistDao` alone, so the
      servlet had to hold the concrete class — the same leak that put `search` on `PatientDao`
- [x] **`SlotResponse` carries the dentist's name.** It carried only `dentistId`, so a screen
      listing a week across every dentist could not say whose slot each row was. Resolved in one
      pass per query rather than one lookup per row
- [x] **`SlotService.allSlots`, distinct from `openSlots`.** A patient browsing should see only what
      they can book; the receptionist publishing needs to see what is already taken, because the
      window they are about to replace may have bookings in it

#### Found by running it
- [x] **"Dr. Dr. Ranil Silva".** The overlap message prepended a title to a name that already
      carried one as stored. Nothing else in the codebase prepends one

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

There is no fallback: `layered/` is not a submission candidate. So the target step chosen in step 0
is a commitment, and **step 5 is the one to aim at** — it is the first point at which all six of the
brief's functions work. Steps 6 to 8 are beyond the brief and can be dropped without losing a
requirement.
