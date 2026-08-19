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
| **FR-REC-01** | `/login/reception` must present email and password only. No registration link — staff accounts are created by an administrator (ASM-05) | Specified |
| **FR-REC-02** | An account whose role is not `RECEPTIONIST` must be rejected, with the same message as a wrong password (FR-AUTH-03) | Specified |
| **FR-REC-03** | The email field must take focus on load, so signing in is two fields and Enter | Specified |

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
| **FR-REC-24** | A patient's details must be correctable — a mistyped phone number is the commonest error at a busy desk | Specified |
| **FR-REC-25** | Registering a walk-in whose contact number already exists must warn rather than silently create a duplicate | Specified |

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
| **FR-REC-42** | The end time must be after the start time, and the span must divide into whole slots | Partial |
| **FR-REC-43** | Publishing a session overlapping one already published for that dentist must be rejected | Specified |
| **FR-REC-44** | The published session must record which receptionist published it | Built |

### 4.5 Billing

| ID | Requirement | Status |
|---|---|---|
| **FR-REC-50** | An appointment must be retrievable for billing by its number alone | Built |
| **FR-REC-51** | The bill must itemise consultation fee, treatment cost, service charge, any discount, tax and total | Built |
| **FR-REC-52** | The bill must be printable on one page without navigation chrome | Partial |
| **FR-REC-53** | Issuing a bill must record which receptionist issued it | Built |
| **FR-REC-54** | Whether that receptionist earns a share of the service charge is a policy setting, `clinic.revenue.receptionist-service-share`, and defaults to **none** — the service charge is the clinic's | Built |
| **FR-REC-54** | An appointment already billed must show its existing bill for reprinting, not offer to bill again | Built |
| **FR-REC-55** | A bill must only be issuable once the dentist has marked the appointment complete | Partial |
| **FR-REC-56** | A discount must be enterable and must be rejected if it exceeds the sum of fee and treatment cost | Specified |

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

## 9. Out of scope for this role

Reading any review or rating aggregate (**FR-RVW-10**), reading any complaint (**FR-CMP-09**), reading a patient's medical notes, recording or reading a diagnosis, marking a treatment complete, viewing clinic revenue or
any report, creating or unlocking accounts, changing treatment prices or a dentist's
consultation fee, and deleting a patient record.
