# SRS — Receptionist

Role-specific requirements for the **reception** portal of the Sunrise Dental Clinic system.
Common requirements, constraints and definitions are in [`srs.md`](srs.md); this document
does not repeat them.

| | |
|---|---|
| Role constant | `Role.RECEPTIONIST` |
| Sign-in portal | `GET`/`POST` `/login/reception` |
| Home screen | `/reception/home` |
| Profile table | none — identity is the `user_account` row |
| Requirement prefix | `FR-REC-` |

---

## 1. Actor profile

Front-desk staff, signed in for the whole working day, handling a queue of people in person
and on the telephone. This role touches the system more than the other three combined.

Its needs are the opposite of the patient's. A receptionist is trained, works at speed, and
is interrupted constantly. Screens must favour keyboard entry, few clicks and dense
information over gentle guidance. A confirmation dialogue that protects a patient from a
mistake merely slows a receptionist down, so confirmations are reserved for genuinely
destructive actions.

**No receptionist profile table exists**, and none is needed: the role carries no attributes
beyond name, email and role, all already on `user_account`. Where a receptionist must be
recorded — who published availability, who issued a bill — the account uid is stored on the
row concerned.

---

## 2. Goals

1. Register a walk-in and book them in the same visit to the desk
2. Find any patient's record fast, from a partial name or a phone number
3. Publish dentists' availability so patients can self-serve
4. Issue and print a bill at the end of a treatment
5. Answer "when am I booked?" for anyone who telephones

---

## 3. Sign-in portal

| ID | Requirement | Status |
|---|---|---|
| **FR-REC-01** | `/login/reception` must present email and password only. No registration link — staff accounts are created by an administrator (ASM-05) | Built |
| **FR-REC-02** | An account whose role is not `RECEPTIONIST` must be rejected, with the same message as a wrong password (FR-AUTH-03) | Built |
| **FR-REC-03** | The email field must take focus on load, so signing in is two fields and Enter | Built |

---

## 4. Screens and operations

| Screen | Route | Purpose |
|---|---|---|
| Sign in | `/login/reception` | Authenticate |
| Today | `/reception/home` | Every appointment for a chosen day |
| Patient records | `/reception/patients` | Search the register; add a walk-in |
| Availability | `/reception/availability` | Publish a dentist's session |
| Billing | `/reception/billing` | Look up an appointment, issue and print its bill |
| Book | `/patient/book` | Book on a patient's behalf |
| Help | `/help` | Step-by-step guidance |

### 4.1 The day view

| ID | Requirement | Status |
|---|---|---|
| **FR-REC-10** | The home screen must list every appointment for a selected date with number, time, patient, dentist, treatment and status | Built |
| **FR-REC-11** | The date must default to today and be changeable in one control | Built |
| **FR-REC-12** | A day with nothing booked must say so, not show an empty table | Built |
| **FR-REC-13** | The day view must not show any appointment's diagnosis (NFR-SEC-06) | Built |

### 4.2 Patient register

| ID | Requirement | Status |
|---|---|---|
| **FR-REC-20** | The register must be searchable by partial name, contact number or email in one field | Built |
| **FR-REC-21** | A walk-in must be registrable with name and contact number alone; address, email and date of birth are optional. This creates a `patient` **record**, not an account — the patient may register for portal access themselves later (**FR-PAT-06**) | Built |
| **FR-REC-22** | The register must show whether each patient holds a portal account, so reception knows whether they can self-serve | Built |
| **FR-REC-23** | Each patient row must offer a direct route to book them | Built |
| **FR-REC-24** | A patient's details must be correctable — a mistyped phone number is the commonest error at a busy desk | **Specified** — no `PUT /api/patients/{id}`. The register can add and search but not correct. Carried forward from step 3 and never picked up |
| **FR-REC-25** | Registering a walk-in whose contact number already exists must warn rather than silently create a duplicate | Built |

### 4.3 Booking on behalf

| ID | Requirement | Status |
|---|---|---|
| **FR-REC-30** | Reception must be able to book any patient into any open slot | Built |
| **FR-REC-31** | Where reception books on behalf, the patient must be named explicitly on the request, not inferred from the session | Built |
| **FR-REC-32** | The appointment must record that reception created it, and which member of staff | Built |
| **FR-REC-33** | The appointment number must be displayed prominently on success, since reception reads it aloud or writes it on a card | Built |
| **FR-REC-34** | Reception must be able to cancel an appointment on a patient's behalf | Built |

### 4.4 Publishing availability

| ID | Requirement | Status |
|---|---|---|
| **FR-REC-40** | Reception must publish availability as a dentist, a date, a start time, an end time and a slot length | Built |
| **FR-REC-41** | Publishing must generate the individual slots automatically; reception must not enter them one at a time | Built |
| **FR-REC-42** | The end time must be after the start time, and the span must divide into whole slots | Built |
| **FR-REC-43** | Publishing a session overlapping one already published for that dentist must be rejected | Built |
| **FR-REC-44** | The published session must record which receptionist published it | Built |

### 4.5 Billing

| ID | Requirement | Status |
|---|---|---|
| **FR-REC-50** | An appointment must be retrievable for billing by its number alone | Built |
| **FR-REC-51** | The bill must itemise consultation fee, treatment cost, service charge, any discount, tax and total | Built |
| **FR-REC-52** | The bill must be printable on one page without navigation chrome | Built |
| **FR-REC-53** | Issuing a bill must record which receptionist issued it | Built |
| **FR-REC-54** | Whether that receptionist earns a share of the service charge is a policy setting, `clinic.revenue.receptionist-service-share`, and defaults to **none** — the service charge is the clinic's | Built |
| **FR-REC-54** | An appointment already billed must show its existing bill for reprinting, not offer to bill again | Built |
| **FR-REC-55** | A bill must only be issuable once the dentist has marked the appointment complete | Built |
| **FR-REC-56** | A discount must be enterable and must be rejected if it exceeds the sum of fee and treatment cost | **Specified** — the discount column exists and every bill records 0.00. `StandardBillingStrategy` sets no discount and no screen offers one |

### 4.6 Clinic phone

| ID | Requirement | Status |
|---|---|---|
| **FR-REC-70** | The receptionist must be able to edit the clinic phone number without involving the administrator, since the reception desk is the first point of contact when a line changes | Built — `/reception/phone`, `ReceptionPhoneServlet`, `ClinicIdentityService.update` with `MANAGE_PHONE` |
| **FR-REC-71** | Only the phone number is editable from this screen; clinic name, email and address require the administrator (`MANAGE_CLINIC_SETTINGS`) | Built — `ClinicIdentityService` checks the key against `RECEPTION_KEYS` |
| **FR-REC-72** | The phone number must be required; an empty value must be rejected | Built |

---

## 5. Data access

| Table | Read | Write |
|---|---|---|
| `user_account` | own row | own row only |
| `patient` | **all rows** | insert walk-in; update details (FR-REC-24) |
| `patient_note` | **never** | never |
| `complaint` | **never** | never |
| `dentist_review` | **never**, not even an aggregate | never |
| `appointment` | all rows **except `diagnosis`** | insert, status, cancel |
| `slot` | all | status, indirectly through booking |
| `dentist_session` | all | insert on publishing |
| `dentist` | all | — |
| `treatment` | all | — |
| `bill` | all | insert on issuing |
| `clinic_setting` | **never** | **never** — clinic phone, like every setting, is edited only by the administrator (`/admin/clinic`, GAP-REC-10) |
| `audit_event` | — | written on every change |
| `notification` | — | written on booking |

**Two hard exclusions.** Neither `appointment.diagnosis` nor any row of `patient_note` may be
served to this role, by screen or by web service. Both are enforced by returning a response
object that has no field for them, so neither can leak through a shared object by oversight.

This is a deliberate trade with a real cost: a receptionist preparing a room cannot see that the
patient has declared a latex allergy. The clinic accepted that in exchange for keeping clinical
data to clinicians; the mitigation is that the treating dentist sees the critical flag before
treatment starts (**FR-NOTE-08**). If the decision is revisited, it changes one method, not the
schema.

---

## 6. Validation

| Field | Rule | Message on failure |
|---|---|---|
| Patient name | Required, 2–255 characters | "Enter the patient's full name." |
| Contact number | Required, valid Sri Lankan number | "Enter a number like 0771234567." |
| Email | Optional; if given, valid and not already used | "That email belongs to another patient." |
| Date of birth | Optional; if given, in the past | "Date of birth cannot be in the future." |
| Search term | Optional; empty lists everyone | — |
| Dentist | Required, must be active | "Choose a dentist." |
| Session date | Required, today or later | "Choose today or a later date." |
| Start / end time | Both required, end after start | "The end time must be after the start time." |
| Slot length | Required, 5–240 minutes, must divide the span | "Choose a length that divides the session evenly." |
| Appointment number | Required, must exist | "No appointment found with that number." |
| Discount | Optional, not negative, not more than fee plus treatment | "A discount cannot exceed the treatment total." |

---

## 7. Web service endpoints

| Method | Endpoint | Notes |
|---|---|---|
| `POST` | `/api/auth/login`, `/api/auth/logout` | |
| `GET` | `/api/auth/me` | |
| `GET` | `/api/patients?q=` | Search the register |
| `POST` | `/api/patients` | Register a walk-in — returns `201` |
| `GET` | `/api/appointments` | All, diagnosis excluded |
| `GET` | `/api/appointments/{no}` | Diagnosis excluded |
| `POST` | `/api/appointments` | Book on behalf; `patientId` required |
| `POST` | `/api/appointments/{no}/cancel` | |
| `POST` | `/api/appointments/{no}/bill` | Issue the bill |
| `GET` | `/api/appointments/{no}/bill` | Fetch for reprint |
| `GET` | `/api/availability?dentistId&date` | |
| `GET` | `/api/availability/week?from&to` | |
| `POST` | `/api/sessions` | Publish availability |
| `GET` | `/api/dentists`, `/api/treatments` | |

---

## 8. Error handling

| Situation | Behaviour |
|---|---|
| Unknown appointment number | "No appointment found with that number." — the search field keeps what was typed |
| Slot taken while the form was open | Booking rejected, slot list refreshed, nothing written |
| Billing an unbilled but incomplete appointment | Refused, stating the dentist must complete it first |
| Billing an already-billed appointment | The existing bill is shown for reprint |
| Session expired mid-entry | Redirect to `/login/reception`; typed data is lost, which FR-REC-24-style drafts do not currently mitigate |
| Overlapping session published | Rejected, naming the session it clashes with |
| Database unavailable | Plain apology page; no stack trace, no SQL text |

---

## 10. Identified gaps and enhancements

Gaps found during QA on the `develop` branch.

| Ref | Gap observed | Fix applied | Status |
|---|---|---|---|
| **GAP-REC-01** | Availability page: publishing a session that overlapped an existing one sent the receptionist to a generic dead-end error page — no context, no way to see what was already published or fix it | Error now caught inline; availability page re-renders with the conflict message and the already-published slots visible below (`AvailabilityPageServlet.java`, `availability.jsp`) | **Fixed** |
| **GAP-REC-02** | Availability page: no overview of what was already published across all dentists — receptionist only discovered existing sessions after attempting to publish and hitting a conflict | Upcoming sessions table (all dentists, from today) now shown at the top of the availability page on every load (`availability.jsp`, `SlotService`, `SessionDao`) | **Fixed** |
| **GAP-REC-03** | FR-REC-23 marked Built incorrectly — patient register had no Book button per row, and `patientId` was never passed through the booking UI. Walk-in bookings were attributed to the receptionist's session, not the patient | Added Book link per patient row in `register.jsp`; `patientId` carried through GET reload and POST hidden field in `book.jsp`; `BookAppointmentServlet` reads and passes `patientId` as request attribute | **Fixed** |
| **GAP-REC-04** | No unified walk-in booking flow — reception must switch between Availability and Patients across 3 screens to check open slots and book a walk-in patient. In real-world use a queue is waiting; the task should be one continuous flow: search/register patient → see today's open slots → book | Built `/reception/walkin` — search or register patient, see all open slots for any date grouped by dentist, one click to book. Added to reception navigation as **Walk-in**. `WalkInServlet`, `walkin.jsp` — issue #4 | **Fixed** |
| **GAP-REC-05** | Day view showed raw enum status labels (`CONFIRMED`, `COMPLETED`, `BILLED`, `CANCELLED`) — not meaningful to front-desk staff. `CONFIRMED` covers both "waiting" and "with dentist" with no distinction. `COMPLETED` (action needed — bill now) looked identical to `CONFIRMED`. No direct Bill link on ready-to-bill rows | Replaced labels with plain language: `Waiting`, `Ready to bill` (orange highlight), `Paid & done`, `Cancelled`. Direct **Bill** link added on `COMPLETED` rows (`reception-day.jsp`) — issue #5 | **Fixed** |
| **GAP-REC-06** | FR-REC-24: patient details are not correctable — a mistyped phone number cannot be fixed through any screen | Not yet addressed | **Open** |
| **GAP-REC-06** | FR-REC-43: overlapping session rejection existed in the service but the error surfaced as a generic page rather than actionable feedback (see GAP-REC-01) | Fixed via GAP-REC-01 | **Fixed** |
| **GAP-REC-07** | Walk-in booking (GAP-REC-04) duplicated what the Patients register already does — the same search/register and booking path was offered twice, in **Walk-in** and **Patients** | Removed the **Walk-in** tab (and the redundant **Home** tab, since the Sunrise logo is the same doorway); booking a walk-in patient now happens through the single **Patients** register as FR-REC-21 and GAP-REC-03 describe (`ReceptionPolicy.java`) | **Fixed** |
| **GAP-REC-08** | The walk-in register accepted any contact number — letters, a wrong digit count, or a number that did not begin with the local trunk `0`. Only self-registration validated the field, and with a weaker rule (7 digits, no start/format check), so a mistyped number was stored verbatim and could never match the same person's digits-only record later | The register now validates through a shared `PhoneNumbers` rule used by every entry point (walk-in register, self-registration, profile edit, admin and reception clinic phone): no letters, must start with `0` (or `+94` international), exactly 10 digits. `PatientService.validPhone`, `PhoneNumbers` | **Fixed** |
| **GAP-REC-09** | The day view still offered **Cancel** on a `COMPLETED` row — a treated visit could be cancelled (and its slot released) before billing, while the record of the treatment stood. FR-PAT-32 forbade it for patients but the state machine let reception do it, and only `BILLED` was refused | The status machine no longer permits `COMPLETED → CANCELLED`: the `Cancel` button disappears from the day view the moment a visit is marked done (`AppointmentStatus.permitted()`, `Appointment.cancel()`), and a direct cancel request on a completed appointment is refused with `409`. Cancel is now offered only while the appointment is still `CONFIRMED` | **Fixed** |
| **GAP-REC-10** | Reception had its own **Phone** screen (`/reception/phone`, "Phone" tab) that edited the clinic phone number, duplicating the administrator's Clinic screen — two doorways to the same setting, where the admin already owns it (GAP-ADM-06) | Removed reception's phone editing entirely: dropped the **Phone** tab and `MANAGE_PHONE` from the reception role, and deleted `ReceptionPhoneServlet` + `reception/phone.jsp` + its `web.xml` mapping. Clinic phone, like every other identity field, is now edited only at `/admin/clinic` (`ReceptionPolicy.java`, `ClinicIdentityService` javadoc) | **Fixed** |
| **GAP-REC-11** | Walk-in registration appeared not to collect a phone, so a patient registered at the desk could be stored without one and never be reachable | **Verified — already satisfied, no change needed.** Both walk-in entry points already collect a **required** contact number and enforce it server-side: the reception register (`patients/register.jsp` → `PatientRecordsServlet`, `requiredField(request, "contactNumber", ...)`) and the walk-in booking form (`scheduling/walkin.jsp` → `WalkInServlet`). Both validate through the shared `PhoneNumbers` rule (`PatientService.validPhone`). What was actually missing was the **self-registration** phone being optional — that is GAP-PAT-20, now fixed (mandatory in `SelfRegistrationService.register` + `required` on `access/register.jsp`) | **Verified — no change needed** |
| **GAP-REC-12** | Two or more patients can carry the same name, and with nothing else unique shown in the **Patients** list reception cannot tell which row is the person in front of them — the same name appears several times with no distinguishing value, so a walk-in or a repeat visitor is easily booked against the wrong record. There is no patient identifier surfaced anywhere in the list | Every patient now shows a **Patient ID** column as the **leftmost** cell of the reception register and the walk-in search results. The value is the patient's unique stored `id` (the primary key) exposed through `PatientResponse.patientNumber()`, so two same-named rows are always distinguishable and can never collide — friendly seeded ids like `p-nimal` read as-is, machine UUIDs are printed verbatim so they stay unique. `patients/register.jsp` and `scheduling/walkin.jsp` | **Fixed** |
| **GAP-REC-13** | An **"Other (describe…)"** booking has no catalog treatment, so billing throws "nothing to price" and reception **cannot bill the visit at all** | **Fixed** — treatment-less visits bill at `appointment.custom_price`; the receipt Treatment line (and billing-list, day-view, slip fallbacks) names the patient's stated reason (`BillingService`, `BillResponse`, `billing.jsp`, `reception-day.jsp`, `slip.jsp`) | **Fixed** |
| **GAP-REC-14** | Reception works off the same unreadable patient ids (GAP-PAT-33) | **Fixed** — search covers `patient_no` (SQL + in-memory); register, walk-in and booking displays flow through `patientNumber()` | **Fixed** |
| **GAP-FTB-12** | There is a single shared help page (`/help`) written for the public; reception uses the system differently and has no help page for its own scenario (searching patients, day view, walk-in register, billing), rooted in the project's SRS/docs | **Fixed** — `shared/help-reception.jsp` covers the day view, register, walk-in booking, availability and billing; served at `/help/reception`, linked from the reception sign-in screen, its nav Help entry and the register/walk-in screens. (Parent gap GAP-FTB-12 in `gap-checklist.md`) | **Fixed** |
| **GAP-FTB-14** | Profile and settings pages render every editable field as an in-page form at once; the user wants a **read-only view by default** with an **Edit** control revealing only the changeable fields, and every successful update in the system (reception edits a walk-in record, updates a patient, etc.) to confirm with a **sub-window ("successfully updated")** rather than only a top-of-page notice | **Fixed** — the shared `.sub-window.open` confirmation pattern (see `srs-patient.md` GAP-FTB-14); reception's own update points confirm in it too: day-view cancels, walk-in register (`?registered=1`) and published availability. (Parent gap GAP-FTB-14 in `gap-checklist.md`) | **Fixed** |

---

## 9. Out of scope for this role

Reading any review or rating aggregate (**FR-RVW-10**), reading any complaint (**FR-CMP-09**), reading a patient's medical notes, recording or reading a diagnosis, marking a treatment complete, viewing clinic revenue or
any report, creating or unlocking accounts, changing treatment prices or a dentist's
consultation fee, and deleting a patient record.
