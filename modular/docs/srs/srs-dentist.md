# SRS — Dentist (Doctor)

Role-specific requirements for the **dentist** portal of the Sunrise Dental Clinic system.
Common requirements, constraints and definitions are in [`srs.md`](srs.md); this document
does not repeat them.

| | |
|---|---|
| Role constant | `Role.DENTIST` |
| Sign-in portal | `GET`/`POST` `/login/dentist` |
| Home screen | `/dentist/schedule` |
| Profile table | `dentist` |
| Requirement prefix | `FR-DEN-` |

> **Naming.** The clinic and the assessment brief say "dentist"; "doctor" is used
> interchangeably in conversation. The system uses `dentist` throughout — table, class and
> route — and this document follows the code so there is one vocabulary, not two.

---

## 1. Actor profile

A practising dentist, signing in several times a day in the minutes between patients, often
on a tablet beside the chair. Sessions are short and purposeful: see who is next, record
what was done, move on.

This is the **only role permitted to read or write a diagnosis**, and the only role whose
data is scoped to itself rather than to the whole clinic. A dentist sees their own schedule
and no one else's.

A `dentist` row may exist before any account does — the register lists practitioners for
booking purposes, and a login is issued separately (`dentist.user_uid` is nullable). A
dentist without an account is bookable but cannot sign in.

---

## 2. Goals

1. See today's list, in order, without hunting for it
2. Open one appointment and see the patient's relevant history
3. Record the diagnosis and mark the treatment complete
4. Know what is coming later in the week

---

## 3. Sign-in portal

| ID | Requirement | Status |
|---|---|---|
| **FR-DEN-01** | `/login/dentist` must present email and password only. No registration link — a dentist's account is issued by an administrator (ASM-05) | Built |
| **FR-DEN-02** | An account whose role is not `DENTIST` must be rejected, with the same message as a wrong password (FR-AUTH-03) | Built |
| **FR-DEN-03** | On success the dentist must land directly on today's schedule, not on an intermediate dashboard | Built |
| **FR-DEN-04** | Sign-in must resolve the `dentist` profile row from the account. An account with role `DENTIST` and no matching profile must fail with a clear message rather than an empty schedule | Built |

---

## 4. Screens and operations

| Screen | Route | Purpose |
|---|---|---|
| Sign in | `/login/dentist` | Authenticate |
| My schedule | `/dentist/schedule` | Own appointments for a chosen day |
| My availability | `/dentist/availability` | Published time windows reception created for the dentist, with open/booked slot counts |
| Appointment detail | `/dentist/schedule` (expanded) | Record diagnosis, mark complete |
| Help | `/help` | Step-by-step guidance |

### 4.1 Schedule

| ID | Requirement | Status |
|---|---|---|
| **FR-DEN-10** | The schedule must list the signed-in dentist's appointments for a selected date in time order | Built |
| **FR-DEN-11** | The date must default to today | Built |
| **FR-DEN-12** | Each entry must show time, appointment number, patient name, treatment type and status | Built |
| **FR-DEN-13** | The schedule must show only the signed-in dentist's own appointments. The dentist identity must come from the session, never from a request parameter | Built |
| **FR-DEN-14** | A day with nothing booked must say so plainly | Built |
| **FR-DEN-15** | The dentist should be able to see the week ahead, not only one day at a time | **Built** — the schedule opens on a week-ahead section (today + six days, grouped per day, critical-notes flags carried over); the single-day picker remains below for any date beyond it |

### 4.2 What the patient has declared

The dentist is the only role that reads a patient's medical notes, and reads them only for
patients on their own schedule.

| ID | Requirement | Status |
|---|---|---|
| **FR-DEN-40** | Opening an appointment must show that patient's medical notes beside it, without a separate search | Built |
| **FR-DEN-41** | Notes must be grouped or labelled by category — allergy, medication, condition, other — so the list is scannable between patients | Built |
| **FR-DEN-42** | Where any note is marked critical, the **schedule** must indicate it before the appointment is opened. A dentist must not have to open a record to discover there is a warning in it | Built |
| **FR-DEN-43** | A patient who has declared nothing must produce an explicit "nothing declared" line, never blank space. Silence must not be readable as safety | Built |
| **FR-DEN-44** | The dentist must be able to read notes only for patients on their own schedule, not for the register at large (**FR-NOTE-09**) | Built |
| **FR-DEN-45** | The dentist must **not** be able to add, edit or delete a patient's notes. Clinical findings go in `diagnosis`; the notes belong to the patient | Built |
| **FR-DEN-46** | The note's last-updated date must be shown, since a five-year-old declaration carries different weight from last week's | **Specified** — the note carries `updatedAt` and the dentist's screen does not show it |

**Why the dentist cannot edit them.** Two records, two owners, and keeping them separate is what
makes each trustworthy. `patient_note` is what the patient says about themselves;
`appointment.diagnosis` is what the dentist found. If a dentist could edit the patient's
declaration there would be no way to tell which is which afterwards — and the patient would lose
the ability to correct their own record.

### 4.3 Your rating

| ID | Requirement | Status |
|---|---|---|
| **FR-DEN-60** | The dentist must be able to see their own mean rating and the number of reviews behind it | **Partial** — the aggregate is served by `GET /api/reviews`; the dentist has no screen showing it |
| **FR-DEN-61** | The dentist must **not** see any individual rating, any comment, or who left it (**FR-RVW-08**, **NFR-SEC-13**) | Built |
| **FR-DEN-62** | No aggregate may be shown until at least five reviews exist, so one poor visit cannot define a dentist (**FR-RVW-12**) | Built |
| **FR-DEN-63** | The dentist must not see another dentist's rating | Built |

**Why only the aggregate.** A mean rating is a performance signal a professional should have. An
individual comment, in a clinic seeing a handful of patients a day, identifies its author by date
alone — and a patient who thinks their dentist will read their comment before the next
appointment will not write an honest one. The dentist gets the number that is useful; the clinic
manager gets the words.

This is the third distinct visibility rule in the system, and the three do not follow a rank
order:

| Data | Dentist | Reception | Admin |
|---|---|---|---|
| Patient's medical notes | reads | ✘ | ✘ |
| Complaint naming them | ✘ | ✘ | reads |
| Their own reviews | **aggregate only** | ✘ | reads in full |

### 4.4 Your availability

| ID | Requirement | Status |
|---|---|---|
| **FR-DEN-18** | The dentist must be able to see the time windows that reception has published for them, with the number of open and booked slots in each, so they know what patients can book without asking reception | Built |

### 4.5 Diagnosis

| ID | Requirement | Status |
|---|---|---|
| **FR-DEN-20** | The dentist must be able to record a free-text diagnosis against their own appointment | Built |
| **FR-DEN-21** | A recorded diagnosis must be visible to the treating dentist and the patient, and to no one else — not reception, not the administrator | Built |
| **FR-DEN-22** | The dentist must not be able to read or write a diagnosis on another dentist's appointment | Built |
| **FR-DEN-23** | An amended diagnosis must be recorded in the audit trail, since a clinical note that changes silently is worse than one that never changed | **Not applicable** — a diagnosis cannot be amended. `AppointmentStatus` refuses COMPLETED → COMPLETED, so there is no amendment to audit |
| **FR-DEN-24** | The diagnosis field must accept at least 4,000 characters | Built — stored as `TEXT` |

### 4.6 Completing a treatment

| ID | Requirement | Status |
|---|---|---|
| **FR-DEN-30** | The dentist must be able to mark their own appointment `COMPLETED` | Built |
| **FR-DEN-31** | Only a `CONFIRMED` appointment may be completed. A cancelled one must not be | Built |
| **FR-DEN-32** | Completion must be what makes an appointment billable by reception (FR-REC-55) | Built |
| **FR-DEN-33** | Completion must record actor, role and time in the audit trail | Built |
| **FR-DEN-34** | Completion should be possible without a diagnosis, since some visits produce none — but the dentist must be warned before proceeding | **Specified** — the form requires a diagnosis. A "should", and arguably wrong to relax: an appointment marked treated with nothing recorded is what the no-show derivation reads as an absence |
| **FR-DEN-35** | Only an appointment whose date has arrived (today or earlier) may be completed. A future appointment must refuse completion whatever its status, since no treatment has happened and billing must not run ahead of the visit | **Built** — `AppointmentService.complete` refuses when `appointment.date` is after today; the schedule no longer offers "Record and complete" for a future appointment (GAP-DEN-06) |

---

## 5. Data access

| Table | Read | Write |
|---|---|---|
| `user_account` | own row | own row only |
| `dentist` | own row; names of others where a screen needs them | — |
| `appointment` | **own appointments only**, `diagnosis` included | `diagnosis`, `status` on own appointments |
| `patient` | name and contact of patients on own appointments | — |
| `patient_note` | **notes for patients on own appointments only** | — (read-only, FR-DEN-45) |
| `complaint` | **never** — not the content, not its existence, not a count | never |
| `dentist_review` | **own aggregate only** — mean and count, no rows, no comments, no authors | never |
| `treatment` | all | — |
| `slot` | own slots | — |
| `dentist_session` | own sessions | — (reception publishes them, ASM-06) |
| `bill` | — | — |
| `audit_event` | — | written on every change |

**Scoping rule.** Every read and write must be filtered by the dentist id resolved from the
session principal. This is the strictest scoping in the system: reception sees the whole
clinic minus one field, whereas a dentist sees one dentist's worth of everything.

**A complaint about this dentist is invisible to them.** Where a patient raises a concern naming
this dentist, nothing on any screen or endpoint reveals it — not the complaint, not a count, not a
changed patient list (**FR-CMP-08**). A complaint the subject can read is a complaint most
patients will not file, so the restriction is what makes the mechanism work rather than a
courtesy. It is the mirror image of the notes rule: the dentist reads what the administrator
cannot, and the administrator reads what the dentist cannot.

**The confidentiality boundary runs both ways.** The dentist is the only role that can read
`diagnosis`, and also the only role barred from `bill` — a practitioner has no operational
need for the revenue split that credits them, and separating the two limits what a single
compromised account exposes.

---

## 6. Validation

| Field | Rule | Message on failure |
|---|---|---|
| Date | Required, valid date | "Choose a date." |
| Diagnosis | Optional, at most 4,000 characters | "The note is too long — please shorten it." |
| Appointment | Must exist and belong to the signed-in dentist | "That appointment is not on your schedule." |
| Appointment date | Completion refused for a future date (FR-DEN-35) | "…is scheduled for <date>. Treatment can only be recorded on the day of the appointment." |
| Status change | Target must be reachable from the current status | "This appointment has already been completed." |

---

## 7. Web service endpoints

| Method | Endpoint | Notes |
|---|---|---|
| `POST` | `/api/auth/login`, `/api/auth/logout` | |
| `GET` | `/api/auth/me` | |
| `GET` | `/api/appointments` | Scoped to the caller's own dentist id |
| `GET` | `/api/appointments/{no}` | Own appointment only; diagnosis **and patient notes** included |
| `GET` | `/api/patients/{id}/notes` | Only for a patient on the caller's own schedule |
| `POST` | `/api/appointments/{no}/complete` | Records diagnosis and marks complete |
| `GET` | `/api/availability?dentistId&date` | Own availability |
| `GET` | `/api/treatments` | |
| `GET` | `/api/ratings/me` | Own mean and count only |

Notably absent: `/api/patients` — a dentist reaches patient details through their own
appointments, never by searching the register. The same rule governs notes: the endpoint takes a
patient id, but the service rejects any patient the caller is not treating.

---

## 8. Error handling

| Situation | Behaviour |
|---|---|
| Another dentist's appointment number requested | Not found — never "exists but forbidden", which would confirm it exists |
| Notes requested for a patient not on this dentist's schedule | Not found, for the same reason |
| Patient has declared no notes | Explicit "nothing declared" line, never an empty panel (**FR-DEN-43**) |
| Account has role `DENTIST` but no `dentist` profile row | Clear message naming the configuration fault, not an empty schedule |
| Appointment already completed | Refused, stating the current status |
| Appointment scheduled for a future date | Refused — treatment can only be recorded on the day of the appointment (**FR-DEN-35**) |
| Diagnosis submitted after the session expired | Redirect to `/login/dentist`; the note is **not** saved, which FR-DEN-24-length notes make costly — a draft-preserving redirect is worth specifying |
| Database unavailable | Plain apology page; no stack trace, no SQL text |

---

## 10. Identified gaps and enhancements

Gaps found during QA on the `develop` branch.

| Ref | Gap observed | Fix applied | Status |
|---|---|---|---|
| **GAP-DEN-01** | Dentist schedule showed treatment name only — the dentist had no visibility of what the patient was being charged for their treatment, which is relevant for explaining the visit to the patient | Treatment name now shown alongside its base cost on the schedule (`dentist-schedule.jsp`, `AppointmentService.DentistDay`) | **Fixed** |
| **GAP-DEN-02** | FR-DEN-15: dentist can only view one day at a time — no week-ahead view | Week-ahead section on the schedule: today + 6 days grouped by date, each appointment with time, patient, treatment, status and a critical-notes flag (`AppointmentService.forDentistWeekWithWarnings`, `findByDentistIdAndDateBetween`) | **Fixed** |
| **GAP-DEN-03** | FR-DEN-46: note's last-updated date not shown on the appointment detail screen | Not yet addressed | **Open** |
| **GAP-DEN-04** | FR-DEN-60: dentist has no screen showing their own mean rating and review count — the API endpoint exists but is not surfaced | Not yet addressed | **Open** |
| **GAP-DEN-05** | FR-DEN-18: reception publishes availability for a dentist but the dentist has no screen showing what was published — they can only see already-booked appointments, not the open windows patients can still book | Availability screen at `/dentist/availability`: published sessions filtered to future dates, grouped by date, each with open/booked slot counts (`SlotService.publishedFor`, `SlotService.allSlots`) | **Fixed** |
| **GAP-DEN-06** | FR-DEN-35: patients may book future dates, but nothing stopped a dentist completing such an appointment early. A completed-but-unborn appointment became billable before the visit — a diagnosis written for a consultation that never happened, and a day's billing screen that could show a charge before the patient had sat down | `AppointmentService.complete` refuses when `appointment.date` is after today; the schedule shows "Scheduled for <date>…" instead of offering "Record and complete" for a future appointment (`dentist-schedule.jsp`, rule covered by `futureAppointmentCannotBeCompleted` test) | **Fixed** |
| **GAP-DEN-07** | A dentist has no profile at all. The role's only screens are Schedule, Availability and Help — there is no `/dentist/profile`, so a dentist cannot set a photo (nor manage how their name and specialisation appear). Paired with GAP-PAT-13: the "Our dentists" cards have nothing to show because nothing can be uploaded | **Specified — not yet built.** Plan: a `/dentist/profile` page (noted in `DentistPolicy.ownNavigation()`) with a multipart photo upload on `@MultipartConfig`. The bytes are validated and re-encoded server-side — content type must be `image/jpeg|png|webp`, size capped, and the image centre-cropped to a fixed square (256×256) with `javax.imageio` before storing — then saved with `avatar MEDIUMBLOB` + `avatar_type` columns on the `dentist` table (added to `schema.sql` and an idempotent `ALTER` in the container seeder for existing databases). Storage is in MySQL, not on disk: Render's free filesystem is ephemeral and would lose photos on every redeploy. The same byte row feeds GAP-PAT-13's `/api/dentists/{id}/photo` endpoint | **Open** |
| **GAP-DEN-08** | On opening My schedule the dentist first saw "The week ahead" — a forward-looking overview of upcoming days — and only below it the day's patients that still need treating with the diagnosis / "Record and complete" form. The doctor's first job is to treat the next patient due now, so the overview was leading the screen with exactly what is not actionable yet | Reordered `dentist-schedule.jsp`: "Today's patients" (the day's CONFIRMED appointments with the diagnosis form) now sits at the top, finished ones are folded behind the "Previous on &lt;date&gt;" expander, and "The week ahead" moved below both. Ordering flows from the servlet's `pending`/`done` split and FR-DEN-15's week grouping | **Fixed** |
| **GAP-DEN-09** | A dentist records a treatment note / comment on the diagnosis form while completing an appointment, but that comment is never shown to the patient — the patient dashboard's **finished appointments** list shows the visit but not the dentist's note, so the written outcome the doctor intends the patient to see is invisible to them | The patient dashboard now shows the dentist's comment. `AppointmentService.forSelfDetail` returns the patient's appointments as `AppointmentDetailResponse` (which carries `diagnosis`), and `PatientHomeServlet` drives the dashboard from it; `patient-home.jsp` renders a **"Dentist's comment"** column, populated on COMPLETED/BILLED visits. Access is safe by construction: `ClinicAccess.canViewClinical` gates the `diagnosis` field and admissions for patients (`srs-patient.md`, FR-PAT / FR-NOTE-11), so only the signed-in patient sees their own note. `forSelf`/`AppointmentResponse` (reception/admin day views) are unchanged. `AppointmentService`, `PatientHomeServlet`, `patient-home.jsp` | **Fixed** |
| **GAP-DEN-10** | The dentist dashboard carries a **Home** tab that does no more than the **Sunrise** logo already does — both return the dentist to the same dashboard landing. It is one redundant doorway the doctor has to dodge past | **Fixed** — the dentist dashboard no longer offers a **Home** tab. `DentistPolicy.navigation()` now returns only the dentist's own tabs (Schedule, Availability), matching the reception cleanup GAP-REC-07. The Sunrise logo remains the single landing control (and points to the same dashboard). `DentistPolicy` | **Fixed** |
| **GAP-DEN-11** | The re-ordered schedule (GAP-DEN-08) still reads badly when today has no bookings: the "Today's patients" empty-state says **"Nothing booked with you on &lt;date&gt;"** where the date shown is a generic/future date rather than plainly "no patients booked today", which reads as an error. And the "The week ahead" heading underneath adds no information the day views above it do not already convey | **Fixed** — the "Today's patients" empty-state no longer echoes the date as if it were an error: when today is the selected day it now says plainly **"No patients booked with you today."**, and when a specific other day is picked it names that day ("No patients booked with you on \&lt;date&gt;."). The **"The week ahead"** block is **retained**, not removed: it is not redundant — it lists the next seven days' bookings (FR-DEN-15) across multiple days, which the single-day "Today's patients" view cannot convey; the label accurately describes that multi-day content (which now sits below today per GAP-DEN-08). `dentist-schedule.jsp` | **Fixed** |
| **GAP-DEN-12** | "Today's patients" renders every patient as a full details card — heading, details table and record-and-complete form each — so a full day is a long scroll of open forms | **Fixed** — each pending patient is one expandable card (`<details class="card pending-list">`, first open): the summary shows time, patient, status and the critical-notes flag, and the details table plus record form render only inside the opened card. Styled like the done list, no script (`dentist-schedule.jsp`, `app.css`) | **Fixed** |
| **GAP-DEN-14** | Only the patient has a **"My details"** profile section; a dentist has nowhere to see or edit their own profile (name, specialisation, phone) as the public cards show it | **Fixed** — "My details" in the account menu (`/dentist/profile`): read-only table with an Edit control for name/specialisation/phone (phone validated), consultation fee shown disabled as administrator-owned, saves confirm in a sub-window (`DentistProfileServlet`, `ReferenceService.updateOwnDetails`, `scheduling/dentist-profile.jsp`, `header.jspf`, `web.xml`) | **Fixed** |
| **GAP-DEN-13** | Completing an **"Other (describe…)"** visit records the diagnosis but **no price**: with no catalog treatment the visit can never be billed | **Fixed** — `appointment.custom_price` holds the dentist-entered price, required and positive exactly when treatment-less (`Appointment.complete(diagnosis, customPrice)`, `AppointmentService`, both servlets, schedule price field, API `customPrice`) | **Fixed** |
| **GAP-FTB-04** | A dentist has no way to publish a **public phone number**; the clinic's front desk has no per-dentist line to hand a patient who wants to reach their own doctor directly. (Distinct from GAP-DEN-07's profile photo work.) | A dentist sets their number on the availability dashboard; it is stored on the `dentist` table and rendered on the public "Our dentists" cards (`access/home.jsp`). `Dentist.phone`, `DentistResponse.phone`, `ReferenceService.updateOwnPhone`, `DentistAvailabilityServlet` (phone POST). Verified live: silva set `+94 77 123 4567` on his availability screen; the public dentist card rendered it. `schema.sql`, `Dentist.java`, `DentistResponse.java`, `DentistDao.java`, `ReferenceService.java`, `DentistAvailabilityServlet.java`, `dentist-availability.jsp`, `home.jsp`, `app.css` | **Fixed** |
| **GAP-FTB-07** | Every dentist is offered the whole treatment catalogue to book, regardless of whether they perform it — an orthodontist could be booked for a routine scaling they do not do. There is no way for a dentist to say which procedures they actually offer | A dentist toggles their offered treatments on the availability dashboard; the booking screen then offers a patient only the treatments that dentist performs. `dentist_treatment` junction + `DentistTreatmentDao`/in-memory, `ReferenceService.treatmentsFor` / `setTreatmentOffered` / `dentistTreatmentToggles`, `DentistAvailabilityServlet` (toggle POST), booking treatment list filtered by selected dentist. Verified live: toggling `t-whitening` off for silva removed it from his booking options (toggled back on afterwards). `schema.sql`, `demo-data.sql`, `DentistTreatmentRepository(+Dao,+InMemory)`, `ReferenceService.java`, `DentistAvailabilityServlet.java`, `dentist-availability.jsp`, `BookAppointmentServlet.java` | **Fixed** |
| **GAP-FTB-12** | There is a single shared help page (`/help`) written for the public; a dentist uses the system differently (schedule, publishing availability, toggling offered treatments, recording diagnoses) and has no help page for that scenario, rooted in the project's SRS/docs | **Fixed** — `shared/help-dentist.jsp` covers schedule, availability publishing, the public phone number, offered treatments and recording treatment; served at `/help/dentist`, linked from the dentist sign-in screen and nav Help entry. (Parent gap GAP-FTB-12 in `gap-checklist.md`) | **Fixed** |
| **GAP-FTB-14** | Profile/settings pages render every editable field as an in-page form at once; the user wants a **read-only view by default** with an **Edit** control revealing only the changeable fields, and every successful update in the system (a dentist updating their phone, toggling treatments, publishing availability) to confirm with a **sub-window ("successfully updated")** rather than only a top-of-page notice | **Fixed** — the phone card on the availability dashboard is now read-only with an Edit disclosure and saving confirms in the shared `.sub-window.open` success window; treatment toggles (`?toggled=1`) and recorded treatments confirm in it too (`dentist-availability.jsp`, `dentist-schedule.jsp`, `app.css`). (Parent gap GAP-FTB-14 in `gap-checklist.md`) | **Fixed** |

---

## 9. Out of scope for this role

Reading any individual review, comment or reviewer identity (**FR-DEN-61**), seeing another
dentist's rating, seeing any complaint, including one that names them (**FR-CMP-08**), adding, editing or deleting
a patient's medical notes (the patient owns those), publishing or
altering own availability (reception does this, ASM-06), booking or cancelling
appointments, issuing or viewing bills, seeing any revenue or earnings figure, searching the
patient register, viewing another dentist's schedule, and any account management.
