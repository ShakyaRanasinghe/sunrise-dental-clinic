# SRS — Patient

Role-specific requirements for the **patient** portal of the Sunrise Dental Clinic system.
Common requirements, constraints and definitions are in [`srs.md`](srs.md); this document
does not repeat them.

| | |
|---|---|
| Role constant | `Role.PATIENT` |
| Sign-in portal | `GET`/`POST` `/login/patient` |
| Home screen | `/patient/home` |
| Profile table | `patient` |
| Requirement prefix | `FR-PAT-` |

---

## 1. Actor profile

A member of the public who is a patient of the clinic. Uses the system a few times a year,
from a phone as often as a laptop, with **no training and no expectation of any**.

**This is the only role that creates its own account.** A patient signs up; a dentist,
receptionist or administrator does not, and cannot. Staff accounts are issued by an
administrator (**ASM-05**, **FR-ADM-20**), so `/register` creates an account with role
`PATIENT` and there is no route by which it could produce any other.

A patient may also exist in the system with **no account at all** — a walk-in registered at
the front desk. Such a patient has a `patient` record with a null `user_uid`, is bookable by
reception, and simply cannot sign in until they register.

The design consequence: every patient screen must be usable on first encounter. Where a staff
screen may trade discoverability for speed, a patient screen may not.

---

## 2. Goals

1. Create an account without telephoning the clinic
2. Get an appointment with the right dentist at a convenient time
3. See what is already booked
4. Cancel something no longer needed
5. See what a visit cost

---

## 3. Sign-in portal

The patient portal is the **only** sign-in page carrying a route to account creation. The other
three portals must not offer one, because their roles cannot self-register.

| ID | Requirement | Status |
|---|---|---|
| **FR-PAT-01** | `/login/patient` must present email and password fields, a link to create an account, and a link to help | Specified |
| **FR-PAT-02** | An account whose role is not `PATIENT` must be rejected here, with the same message as a wrong password (see FR-AUTH-03) | Specified |
| **FR-PAT-03** | Self-registration must be reachable from this portal and must create an account with role `PATIENT` and no other. The role must never be taken from the request — a submitted `role` field must be ignored, not honoured | Built — the role is hardcoded, which is the correct behaviour here |
| **FR-PAT-04** | Registration must create both the `user_account` row and the `patient` profile row in one transaction. Neither may exist without the other | Partial — not currently transactional |
| **FR-PAT-05** | Registration must reject an email already registered, saying so plainly rather than failing on a database constraint | Built |
| **FR-PAT-06** | Where a walk-in record already exists with the same email or contact number, registration should link the new account to it rather than creating a duplicate patient | Specified |
| **FR-PAT-07** | Registration must be the **only** public write endpoint in the system. Every other unauthenticated request is read-only or rejected | Specified |

**Why patients self-register and staff do not.** A patient is a member of the public who may
want access at any hour, and making them telephone the clinic for a login would put the front
desk in the way of every online booking — the very cost the system exists to remove. Staff are
different: there are a handful of them, they are employed rather than self-selecting, and
letting anyone create a `RECEPTIONIST` account would be a privilege-escalation hole rather than
a convenience. The asymmetry is deliberate, and `FR-PAT-03` is what keeps it safe — the role on
this path is a constant in the code, never a value from the form.

---

## 4. Screens and operations

| Screen | Route | Purpose |
|---|---|---|
| Sign in | `/login/patient` | Authenticate |
| Create account | `/register` | Self-registration — patients only |
| My profile | `/patient/profile` | Contact details, and medical notes about themselves |
| Raise a concern | `/patient/complaints` | Report a dentist, and follow what happened to it |
| Rate a visit | on `/patient/home` | Leave a dentist a rating out of five after a completed visit |
| My appointments | `/patient/home` | Everything booked, and past visits |
| Book an appointment | `/patient/book` | Choose dentist and date, then a time |
| Help | `/help` | Guidance, no sign-in required |

### 4.1 Booking

| ID | Requirement | Status |
|---|---|---|
| **FR-PAT-10** | The patient must choose a dentist and a date, then be shown only the slots still open for that pairing | Built |
| **FR-PAT-11** | Slots already booked must not be offered, not merely marked as unavailable | Built |
| **FR-PAT-12** | The patient must choose a treatment type from the clinic's catalogue, so the eventual bill is calculable | Built |
| **FR-PAT-13** | On confirmation the appointment number must be shown immediately, since it is what identifies the visit at the front desk | Built |
| **FR-PAT-14** | If the chosen slot was taken between the page loading and submission, the booking must be rejected with a clear message and the refreshed slot list | Built |
| **FR-PAT-15** | A patient must not be able to book on behalf of anyone else. The patient identity comes from the session, never from the form | Built |
| **FR-PAT-16** | Booking must trigger a confirmation email to the patient's recorded address | Partial — recorded, not sent |

### 4.2 Medical notes

Collected **after** sign-in, from the patient's own profile — never during registration
(**FR-NOTE-02**). Sign-up asks for a name, a contact number, an email and a password, and
nothing clinical.

| ID | Requirement | Status |
|---|---|---|
| **FR-PAT-40** | The profile screen must let the patient add a medical note about themselves — an allergy, a medication they take, a condition | Specified |
| **FR-PAT-41** | Each note must be given a category: allergy, medication, condition or other | Specified |
| **FR-PAT-42** | The patient must be able to mark a note **critical**, meaning a dentist must see it before treating them | Specified |
| **FR-PAT-43** | The patient must be able to edit and delete their own notes | Specified |
| **FR-PAT-44** | The screen must say plainly who can see these notes — the patient and the dentist treating them, and nobody else | Specified |
| **FR-PAT-45** | The screen must explain what the notes are for, in a sentence, since a patient volunteering medical information deserves to know why | Specified |
| **FR-PAT-46** | A patient must never see, or be able to reach, another patient's notes | Specified |
| **FR-PAT-47** | Notes must survive between appointments. Declaring an allergy once must be enough | Specified |

**What the patient is told on the screen** (FR-PAT-44, FR-PAT-45), because the wording is part
of the requirement:

> *These notes go to the dentist who treats you, so they know before they start. Reception and
> clinic staff cannot see them. Add anything a dentist should know — medicines you react badly
> to, medicines you take, conditions you have.*

### 4.3 Viewing

| ID | Requirement | Status |
|---|---|---|
| **FR-PAT-20** | The patient must see their own upcoming and past appointments with number, dentist, treatment, date, time and status | Built |
| **FR-PAT-21** | The patient must be able to see the diagnosis recorded for their own completed appointment | Built |
| **FR-PAT-22** | The patient must be able to view and print the bill for their own appointment | Built |
| **FR-PAT-23** | A patient must never see another patient's appointment, diagnosis or bill, whether by screen or by guessing an appointment number | Built |

### 4.4 Raising a concern about a dentist

| ID | Requirement | Status |
|---|---|---|
| **FR-PAT-50** | The patient must be able to raise a complaint about a dentist who has treated them, choosing the dentist from their own appointment history rather than a list of everyone | Specified |
| **FR-PAT-51** | The patient must choose a category — conduct, clinical concern, waiting time, billing, other — and write an account of what happened | Specified |
| **FR-PAT-52** | The patient should be able to attach the specific appointment it concerns, picked from their own history | Specified |
| **FR-PAT-53** | The form must state who reads it: the clinic administrator, and **not** the dentist named (**FR-CMP-12**) | Specified |
| **FR-PAT-54** | The patient must be able to see every complaint they have raised and its current state | Specified |
| **FR-PAT-55** | A submitted complaint must not be editable or deletable by the patient (**FR-CMP-06**) | Specified |
| **FR-PAT-56** | Raising a complaint must not change anything about booking. No warning, no flag, no altered availability (**FR-CMP-10**) | Specified |
| **FR-PAT-57** | On submission the patient must be told what happens next and roughly when to expect a response | Specified |

**What the patient is told on the form** (FR-PAT-53), because the wording is the requirement:

> *This goes to the clinic administrator. The dentist you name will not see it, and it will not
> affect your appointments or your care. Tell us what happened, with dates if you remember them.*

### 4.5 Rating a visit

Ordinary feedback, offered after the visit has happened. Distinct from raising a concern
(§4.4) — a rating is routine, a concern is an exception.

| ID | Requirement | Status |
|---|---|---|
| **FR-PAT-60** | Once an appointment is `COMPLETED` or `BILLED`, the patient must be offered a rating of 1 to 5 for the dentist who treated them | Specified |
| **FR-PAT-61** | A comment must be optional. A star on its own must be a complete submission | Specified |
| **FR-PAT-62** | Rating must be skippable and must never block viewing, booking or anything else | Specified |
| **FR-PAT-63** | A patient must be able to change their rating for up to 30 days after the visit, and see what they left | Specified |
| **FR-PAT-64** | One rating per appointment. A second visit to the same dentist is a second rating | Specified |
| **FR-PAT-65** | The patient must be told that the dentist sees only an average, never the individual rating or comment | Specified |
| **FR-PAT-66** | Cancelled and future appointments must not offer a rating | Specified |
| **FR-PAT-67** | In this release the patient must not be shown any dentist's rating when choosing one (**FR-RVW-11**) | Deferred by decision |

**What the patient is told beside the stars** (FR-PAT-65):

> *Your dentist sees an average across all their patients, never your individual rating or
> comment. The clinic manager can see what you wrote.*

That sentence is doing real work: it is honest about who reads the comment, so a patient can
decide how frank to be, and it explains why the rating is safe to give.

**Rating versus raising a concern.** The screens keep these apart deliberately. A three-star
rating with *"felt rushed"* is feedback; the same experience described as a formal concern gets a
written resolution and an audit trail. Offering only one of the two would either bury real
problems in star averages or make every mild grumble a case file.

### 4.6 Cancelling

| ID | Requirement | Status |
|---|---|---|
| **FR-PAT-30** | The patient must be able to cancel their own appointment while its status is `CONFIRMED` | Built |
| **FR-PAT-31** | Cancelling must return the slot to bookable so another patient can take it | Built |
| **FR-PAT-32** | A completed or billed appointment must not be cancellable | Partial |
| **FR-PAT-33** | Cancellation must be confirmed before it takes effect | Specified |

---

## 5. Data access

| Table | Read | Write |
|---|---|---|
| `user_account` | own row | own row at registration, with role `PATIENT` fixed in code |
| `patient` | own row | own row at registration |
| `patient_note` | **own notes only** | insert, update, delete — own notes only |
| `complaint` | **own complaints only**, including state and resolution | insert only — no edit, no delete (FR-CMP-06) |
| `dentist_review` | **own reviews only** | insert, and update within 30 days (FR-PAT-63) |
| `appointment` | own rows only, `diagnosis` included | insert on booking, status on cancelling |
| `slot` | open slots only | status, indirectly through booking |
| `dentist` | name, specialization, fee | — |
| `treatment` | name, cost | — |
| `bill` | own bills only | — |
| `audit_event` | — | written on the patient's behalf |
| `notification` | — | written on the patient's behalf |

**Scoping rule.** Every read must be filtered by the patient id resolved from the session
principal. The patient id must never be accepted from a request parameter, or a patient could
read another's record by editing a URL.

---

## 6. Validation

| Field | Rule | Message on failure |
|---|---|---|
| Email | Required, valid form, not already registered | "That email is already registered. Sign in instead?" |
| Password | Required, minimum 8 characters | "Use at least 8 characters." |
| Full name | Required, 2–255 characters | "Please enter your full name." |
| Contact number | Required, Sri Lankan mobile or landline | "Enter a number like 0771234567." |
| Date of birth | Optional; if given, in the past | "Date of birth cannot be in the future." |
| Address | Optional, at most 500 characters | "Address is too long." |
| Dentist | Required, must be an active dentist | "Choose a dentist." |
| Date | Required, today or later | "Choose today or a later date." |
| Slot | Required, must still be open | "That time has just been taken. Please choose another." |
| Treatment | Required, must be an active treatment | "Choose a treatment." |
| Note category | Required, one of the four | "Choose what kind of note this is." |
| Note detail | Required, 3–1000 characters | "Say a little more, in up to 1000 characters." |
| Note critical flag | Optional, defaults to false | — |
| Complaint dentist | Required, must be a dentist who has treated this patient | "Choose the dentist this concerns." |
| Complaint category | Required, one of the five | "Choose what this is about." |
| Complaint detail | Required, 20–4000 characters | "Please describe what happened in a little more detail." |
| Complaint appointment | Optional, must be one of the patient's own | "That appointment is not one of yours." |
| Rating | Required, integer 1–5 | "Choose between one and five stars." |
| Rating comment | Optional, at most 1000 characters | "Please keep it under 1000 characters." |
| Rating appointment | Must be the caller's own, and `COMPLETED` or `BILLED` | "You can rate a visit once it has happened." |
| `role` field, if submitted | **Ignored.** Never read from the request on this path | — |

Every rule must be enforced on the server. Browser-side checks are a convenience and may be
bypassed, so they are never the only check.

---

## 7. Web service endpoints

| Method | Endpoint | Notes |
|---|---|---|
| `POST` | `/api/auth/login` | |
| `POST` | `/api/auth/register` | Creates a `PATIENT` account only — the role is not read from the body |
| `POST` | `/api/auth/logout` | |
| `GET` | `/api/auth/me` | |
| `GET` | `/api/appointments` | Scoped to the caller |
| `GET` | `/api/appointments/{no}` | Own appointment only |
| `GET` | `/api/appointments/{no}/bill` | Own bill only |
| `POST` | `/api/appointments` | Book |
| `POST` | `/api/appointments/{no}/cancel` | Own appointment only |
| `GET` | `/api/availability?dentistId&date` | |
| `GET` | `/api/patients/{id}/notes` | Own notes only |
| `POST` | `/api/patients/{id}/notes` | Own notes only |
| `PUT` | `/api/patients/{id}/notes/{noteId}` | Own notes only |
| `DELETE` | `/api/patients/{id}/notes/{noteId}` | Own notes only |
| `GET` | `/api/complaints` | Own complaints only |
| `POST` | `/api/complaints` | Raise one |
| `GET` | `/api/complaints/{id}` | Own complaint only |
| `POST` | `/api/reviews` | Rate the visit — own appointment only |
| `GET` | `/api/reviews` | The rating they left |
| `PUT` | `/api/reviews` | Change it, within 30 days |
| `GET` | `/api/dentists` | |
| `GET` | `/api/treatments` | |

Staff accounts are created through the administrator's own path, `POST /api/accounts`, which
requires `ADMIN` — see [`srs-admin.md`](srs-admin.md) and [`../api/auth.md`](../api/auth.md).

---

## 8. Error handling

| Situation | Behaviour |
|---|---|
| Wrong credentials | Generic failure message; the failed-attempt counter increments |
| Email already registered | "That email is already registered. Sign in instead?" — with a link to the sign-in page |
| Account locked | "This account is temporarily locked. Please try again later." — no unlock path for a patient; an administrator must clear it |
| Session expired mid-booking | Redirect to `/login/patient`; nothing is booked |
| Slot taken concurrently | Booking rejected, refreshed slot list shown, no partial appointment written |
| Another patient's appointment number entered | Not found — never "exists but forbidden", which would confirm it exists |
| Another patient's note id requested | Not found, for the same reason |
| Complaint names a dentist who has never treated them | Rejected: "Choose the dentist this concerns" — the list only offers dentists from their own history |
| Attempt to edit or delete a submitted complaint | Refused, explaining that a raised concern stays on the record |
| Rating an appointment that has not happened or was cancelled | The control is not offered; a direct request is refused as "You can rate a visit once it has happened." |
| Rating the same appointment twice | Treated as changing the existing rating, not as a second one |
| Editing a rating after 30 days | Refused, stating the window has closed |
| A `role` field submitted to `/register` | Silently ignored; the account is created as `PATIENT` |
| Database unavailable | Plain apology page; no stack trace, no SQL text |

---

## 9. Out of scope for this role

Creating an account for anyone else or in any other role, rescheduling in place (cancel and
rebook instead), choosing a specific room or chair, seeing any dentist's full schedule, seeing
clinic revenue, paying online, and viewing another family member's records.
