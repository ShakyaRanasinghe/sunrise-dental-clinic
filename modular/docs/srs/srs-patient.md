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
| **FR-PAT-01** | `/login/patient` must present email and password fields, a link to create an account, and a link to help | Built |
| **FR-PAT-02** | An account whose role is not `PATIENT` must be rejected here, with the same message as a wrong password (see FR-AUTH-03) | Built |
| **FR-PAT-03** | Self-registration must be reachable from this portal and must create an account with role `PATIENT` and no other. The role must never be taken from the request — a submitted `role` field must be ignored, not honoured | Built — the role is hardcoded, which is the correct behaviour here |
| **FR-PAT-04** | Registration must create both the `user_account` row and the `patient` profile row in one transaction. Neither may exist without the other | Built — and it was **not**, until registration was tested by hand during the demo. `RegisterServlet` created the account alone, so an account that got through signed in to a profile that did not exist. `SelfRegistrationService` now writes both inside one transaction, and `SelfRegistrationServiceTest` covers it |
| **FR-PAT-05** | Registration must reject an email already registered, saying so plainly rather than failing on a database constraint | Built |
| **FR-PAT-06** | Where a walk-in record already exists with the same email or contact number, registration should link the new account to it rather than creating a duplicate patient | **Specified** — registration does not look for a matching walk-in record |
| **FR-PAT-07** | Registration must be the **only** public write endpoint in the system. Every other unauthenticated request is read-only or rejected | Built |

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
| **FR-PAT-17** | On initial load the booking page must show an availability overview: each active dentist with the dates they have open slots in the next 14 days (from today), grouped by dentist then date, with open-slot counts. Clicking a date pre-selects that dentist and date in the booking form | **Built** |
| **FR-PAT-11** | Slots already booked must not be offered, not merely marked as unavailable | Built |
| **FR-PAT-12** | The patient must choose a treatment type from the clinic's catalogue, so the eventual bill is calculable | Built |
| **FR-PAT-13** | On confirmation the appointment number must be shown immediately, since it is what identifies the visit at the front desk | Built |
| **FR-PAT-14** | If the chosen slot was taken between the page loading and submission, the booking must be rejected with a clear message and the refreshed slot list | Built |
| **FR-PAT-15** | A patient must not be able to book on behalf of anyone else. The patient identity comes from the session, never from the form | Built |
| **FR-PAT-16** | Booking must trigger a confirmation email to the patient's recorded address | **Specified** — the event is published; nothing sends it. See FR-NOT-01 |

### 4.2 Medical notes

Collected **after** sign-in, from the patient's own profile — never during registration
(**FR-NOTE-02**). Sign-up asks for a name, a contact number, an email and a password, and
nothing clinical.

| ID | Requirement | Status |
|---|---|---|
| **FR-PAT-40** | The profile screen must let the patient add a medical note about themselves — an allergy, a medication they take, a condition | Built |
| **FR-PAT-41** | Each note must be given a category: allergy, medication, condition or other | Built |
| **FR-PAT-42** | The patient must be able to mark a note **critical**, meaning a dentist must see it before treating them | Built |
| **FR-PAT-43** | The patient must be able to edit and delete their own notes | Built |
| **FR-PAT-44** | The screen must say plainly who can see these notes — the patient and the dentist treating them, and nobody else | Built |
| **FR-PAT-45** | The screen must explain what the notes are for, in a sentence, since a patient volunteering medical information deserves to know why | Built |
| **FR-PAT-46** | A patient must never see, or be able to reach, another patient's notes | Built |
| **FR-PAT-47** | Notes must survive between appointments. Declaring an allergy once must be enough | Built |

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
| **FR-PAT-50** | The patient must be able to raise a complaint about a dentist who has treated them, choosing the dentist from their own appointment history rather than a list of everyone | Built |
| **FR-PAT-51** | The patient must choose a category — conduct, clinical concern, waiting time, billing, other — and write an account of what happened | Built |
| **FR-PAT-52** | The patient should be able to attach the specific appointment it concerns, picked from their own history | Built |
| **FR-PAT-53** | The form must state who reads it: the clinic administrator, and **not** the dentist named (**FR-CMP-12**) | Built |
| **FR-PAT-54** | The patient must be able to see every complaint they have raised and its current state | Built |
| **FR-PAT-55** | A submitted complaint must not be editable or deletable by the patient (**FR-CMP-06**) | Built |
| **FR-PAT-56** | Raising a complaint must not change anything about booking. No warning, no flag, no altered availability (**FR-CMP-10**) | Built |
| **FR-PAT-57** | On submission the patient must be told what happens next and roughly when to expect a response | Built |

**What the patient is told on the form** (FR-PAT-53), because the wording is the requirement:

> *This goes to the clinic administrator. The dentist you name will not see it, and it will not
> affect your appointments or your care. Tell us what happened, with dates if you remember them.*

### 4.5 Rating a visit

Ordinary feedback, offered after the visit has happened. Distinct from raising a concern
(§4.4) — a rating is routine, a concern is an exception.

| ID | Requirement | Status |
|---|---|---|
| **FR-PAT-60** | Once an appointment is `COMPLETED` or `BILLED`, the patient must be offered a rating of 1 to 5 for the dentist who treated them | Built |
| **FR-PAT-61** | A comment must be optional. A star on its own must be a complete submission | Built |
| **FR-PAT-62** | Rating must be skippable and must never block viewing, booking or anything else | Built |
| **FR-PAT-63** | A patient must be able to change their rating for up to 30 days after the visit, and see what they left | Built |
| **FR-PAT-64** | One rating per appointment. A second visit to the same dentist is a second rating | Built |
| **FR-PAT-65** | The patient must be told that the dentist sees only an average, never the individual rating or comment | **Specified** — true of the system, but the screen does not say so |
| **FR-PAT-66** | Cancelled and future appointments must not offer a rating | Built |
| **FR-PAT-67** | In this release the patient must not be shown any dentist's rating when choosing one (**FR-RVW-11**) | **Fixed** — ratings now shown on the public landing page when 5+ reviews exist |
| **FR-PAT-70** | The patient dashboard must show star rating controls next to each `COMPLETED` or `BILLED` appointment. Existing ratings must be displayed as filled stars and be editable within the 30-day window | **Built** |

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
| **FR-PAT-32** | A completed or billed appointment must not be cancellable | Built |
| **FR-PAT-33** | Cancellation must be confirmed before it takes effect | **Specified** — see FR-UI-04. Cancelling takes effect on one click |

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
| Contact number | Optional; if given, digits with `+` `(` `)` `-` and spaces allowed, **no letters**, must start with `0` (or `+94` international), exactly 10 digits | "The contact number can contain digits with + ( ) and - only. No letters." |
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
| Account locked | "This account is locked. Please contact the clinic to have it unlocked." — no unlock path for a patient; an administrator must clear it |
| Session expired mid-booking | Redirect to the public home page `/` (the patient portal is entered from there, unlike the staff portals); nothing is booked |
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

## 10. Identified gaps and enhancements

Gaps found during QA on the `develop` branch. Each entry states the problem observed,
the fix applied and the current status.

| Ref | Gap observed | Fix applied | Status |
|---|---|---|---|
| **GAP-PAT-01** | Booking page: clicking a time slot booked immediately with no confirmation step — accidental bookings were frequent | Changed time slots from submit buttons to radio buttons; added a single **Confirm booking** button below (`book.jsp`) | **Fixed** |
| **GAP-PAT-02** | Dentist dropdown showed name, specialisation and consultation fee crammed into one option label — truncated and unreadable | Dropdown now shows name only; specialisation and fee shown in a details table below the dropdown after selecting (`book.jsp`) | **Fixed** |
| **GAP-PAT-03** | No cost explanation before confirming — a new patient had no way to know what consultation fee or service charge meant, or what their total would be | Added cost breakdown table showing consultation fee (with explanation), service charge (with explanation), treatment cost pointer, and estimated total formula with an "estimate only" note (`book.jsp`) | **Fixed** — awaiting verification |
| **GAP-PAT-04** | FR-PAT-33: cancellation takes effect on one click with no confirmation. Specified but not built | Not yet addressed | **Open** |
| **GAP-PAT-05** | FR-PAT-65: patient is not told that their dentist sees only an aggregate rating, never their individual comment | Not yet addressed | **Open** |
| **GAP-PAT-06** | FR-PAT-06: walk-in registration does not look for an existing patient with the same email or contact number before creating a new row | Not yet addressed | **Open** |
| **GAP-PAT-07** | Booking page required dentist + date selection before showing any availability. Patients who wanted to browse ("when is Dr. Silva available?") had to guess dates one by one with no feedback. No overview of which dentists have open slots, or on which dates | Added availability overview section: shows each active dentist with their open-slot dates for the next 14 days above the booking form. Clicking a date pre-fills the dentist and date in the form. Uses existing `SlotService.openSlotsBetween()` | **Fixed** |
| **GAP-PAT-08** | Patient dashboard (`/patient/home`) showed appointments but had no way to rate a dentist. The rating mechanism existed in the backend (`ReviewService.rate()`) but was only accessible via the API endpoint `POST /api/reviews`. Patients had no visible UI to leave or edit star ratings | Added inline star rating controls on the dashboard for COMPLETED/BILLED appointments. Existing ratings shown as filled stars. Editable within the 30-day window. Uses existing `ReviewService.rate()` and `ReviewService.own()` | **Fixed** |
| **GAP-PAT-09** | Public landing page (`/`) listed dentists by name and specialisation but showed no ratings. `FR-RVW-11` deferred rating display, but the data has been accumulating since day one. Patients choosing a dentist had no quality signal | Added aggregate star ratings (mean + review count) to each dentist card on the landing page. Shown only when 5+ reviews exist (`FR-RVW-12`). Uses existing `ReviewRepository.summaryFor()` | **Fixed** |
| **GAP-PAT-10** | Registration accepted anything in the contact number — a patient who typed letters ("o77 123 4567") had them stored verbatim and then could never be matched to the same person's digits-only record at the desk | `SelfRegistrationService` now validates the contact number server-side: digits with `+` `(` `)` `-` and spaces only, no letters, at least 7 digits (`register.jsp` carries a matching `pattern` for immediate feedback; `SelfRegistrationServiceTest` covers it) | **Fixed** |
| **GAP-PAT-16** | The contact-number rule from GAP-PAT-10 was still weaker than it should be: it accepted any 7+ digits without checking the form of a Sri Lankan number, so "12345", "0712345" and "7112345678" all passed as long as they had no letters. The mistake shows up later as a record nobody can match to the same person's correct number at the desk | The rule is now shared and stricter through `PhoneNumbers` (used by self-registration, the walk-in register, the profile edit and the clinic/phone screens alike): no letters, must start with `0` (or `+94` international), exactly 10 digits. `SelfRegistrationService` and `PatientService` both delegate to it; `register.jsp` pattern and titles updated to match | **Fixed** |
| **GAP-PAT-17** | The public landing page carried the clinic's contact details in a body section ("Visit us") at the bottom of the page — while the navbar duplicated the short-cut with a **Contact** link (`/#contact`) and no page anywhere showed which release was running. The contact block only existed on `/`; a signed-in user never saw it, and there was no release number to quote when reporting a bug | Contact details moved out of the page body into a **site-wide footer**: the same footer renders on every page (`footer.jspf`) and draws the clinic name, address, phone and email from the `clinic_setting` values the admin edits, alongside the release version (`ReleaseInfo`). The footer attributes are decorated per-request in `AuthenticationFilter` — the one pass every rendered page makes — so there is no per-screen wiring to forget (FR-ADM-72 still holds: values are re-read per request). The now-redundant **Contact** navbar link and the "Visit us" body card were removed | **Fixed** |
| **GAP-PAT-18** | The "Raise a concern" form rendered **one full form per appointment** as a flat list below the notice. A patient treated by several different dentists (or several visits with one) had every form — with its own visit, category and textarea — stacked unchecked down the page, so reaching the form for an earlier visit meant scrolling past all of them (FR-PAT-50 was met but the presentation did not scale) | The visits are now **grouped by dentist** (`PatientComplaintsServlet.groupByDentist`) and shown as a compact list of rows — one per doctor the patient has actually seen, with their visit count. Each is a `<details>/<summary>` (no script, per the stack's no-JS rule): the concern form for that dentist opens only when the patient opens that row. Where a dentist was seen more than once, the form offers a **Which visit** dropdown (defaulting to the most recent); a single visit is carried as a hidden field. The form still names only dentists from the patient's own history (FR-PAT-50). `PatientComplaintsGroupingTest` covers the grouping | **Fixed** |
| **GAP-PAT-19** | Branding and the booking availability lookup did not share a single accent: the "Sunrise" logo wordmark and the "Availability this fortnight" chips rendered in the clinic's orange-red accent, which reads as an error/alert colour rather than identity | Both now use a **blue** accent. The logo wordmark is `var(--blue)` and the fortnight availability overview (dentist name rule, specialisation chip, date chips and their hover/interaction states, slot-count pill) uses the matching blue values (`--blue`, `--blue-dark`, `--blue-light`) in `app.css` | **Fixed** |
| **GAP-PAT-11** | The create-account form gave no way to see what was being typed: both password fields were `type=password` with no show/hide control, so on a phone a patient had no way to check a passphrase before submitting — the most error-prone field was the one they could not read back | Added an **eye icon inside each password field**, right-aligned (`register.jsp`, `app.css`): an open-eye swaps to a slashed-eye as the field flips between `password` and `text`. It only changes the input type and the icon, never the value, and a browser without JavaScript keeps the fields as `password` — the secure default. State exposed through `aria-pressed` | **Fixed** |
| **GAP-PAT-12** | Leave a field blank on the create-account form and no field-level validation appeared — only the email field showed a reason, because it was the only one with an HTML5 constraint. A missing name (or a short password) submitted to the server, which answered with the generic error page, and the person typed everything again | The four required fields now all carry browser-side checks like the email field — `required` on name, email, password and confirmation, `minlength="8"` on the password, and the one rule HTML5 cannot express (password vs confirmation) is reported on the confirm field via `setCustomValidity` before any submit. The server remains the final gate, and when something still fails there the servlet **re-renders the form** with the reason shown above the fields and every typed value preserved (`RegisterServlet`, same re-render pattern as the login portals) instead of dumping to the error page. `RegisterPageTest` asserts the whole contract | **Fixed** |
| **GAP-PAT-13** | A patient choosing a dentist — on the public "Our dentists" section or in booking — sees a name, a specialisation and a rating, never a face. There is no photo on any dentist card, and no way for a dentist to provide one, so a patient cannot recognise the person who will treat them | **Specified — not yet built.** Plan: each "Our dentists" card gains a circular profile photo on its right (`border-radius: 50%` with `object-fit: cover`). The dentist's uploaded photo is served by a `/api/dentists/{id}/photo` endpoint; when no photo is set the same slot shows a generated default avatar (initials in a circle). Upload lives on a new `/dentist/profile` page (see GAP-DEN-07). Storage rationale in that entry, since the two are one change | **Open** |
| **GAP-PAT-14** | The sign-in page that every visitor landed on from the public header (`/login`) listed **all** staff portals as visible cards — Reception, Dentist and (by design) a patient card. A member of the public could read the URLs `/login/reception` and `/login/dentist` straight off the homepage flow, even though those doors exist for staff only and no patient has business there. Seeing that a staff surface exists (and where) is a confidentiality leak, not just noise | First fixed by listing **only the patient portal** on the chooser while staff doors stayed address-only. The chooser has since been **removed entirely** — it carried a single door, so a page between the public home and the patient portal bought nothing. The public site now links `/login/patient` directly; staff still reach their own door by the URL issued to them (`/login/reception`, `/login/dentist`, and the unadvertised `/login/admin`, `FR-ADM-03`) | **Fixed** |
| **GAP-PAT-15** | Booking page (`/patient/book`): once a dentist and date are chosen, the **doctor's fee details card** (specialisation, consultation fee, service charge, estimated total) sat *above* the actual reason the patient is there — the **available appointment times**. A patient booking a slot had to scroll past the price table to reach the times; the page's phasing read "details first, then decision" when the decision is the whole point. The availability that matters (which times are open) was buried below the fee breakdown, and the fees were repeated on every reload without being the thing the patient was deciding on | The booking page was re-ordered: **available times come first** (the `1. Treatment and time` card with the open slots is now the top card, so a patient sees at once, for their chosen dentist and date, exactly which times are still bookable), and the **doctor fee details were lifted out of the page flow entirely** into a sub-window — clicking the dentist's name beside the listed times opens a dialog showing specialisation, consultation and service charges and the estimated-total formula. The picker card (dentist + date form) sits below the times as `2. Dentist and date`, so the sequence reads "see your options, pick one" instead of "read a price list first" | **Fixed** — awaiting verification |
| **GAP-PAT-20** | Self-registration (`/register`) treats the contact number as optional: the form accepts a new account with no phone at all, so a patient who registers without one is later impossible to match to the same person's record at the reception desk, and the clinic has no reliable way to reach them | The contact number is now **mandatory**. Form-side, `access/register.jsp` marks the field `required` (with the shared Sri Lankan `pattern`); server-side, `SelfRegistrationService.register` refuses a blank value ("Your contact number is required.") before creating the record (`RegisterServlet` re-renders with the reason). Address and date of birth stay optional. Tests updated: `SelfRegistrationServiceTest` asserts the number is required and carried on every registration | **Fixed** |
| **GAP-PAT-21** | Cancelling an appointment takes effect immediately on one click, so a single mistaken click drops a confirmed slot with no chance to reconsider (FR-PAT-33). The related GAP-PAT-04 is already tracked open for the same confirmation gap | A cancel no longer commits on one click. On the patient dashboard the **Cancel** button is now a `<details>/<summary>` that opens an explicit **"Yes, cancel it"** submit (no JavaScript, per the stack rule) so a stray click cannot drop a confirmed slot — the same two-step guard was applied to the reception day view for consistency. The confirm form still posts the same cancel path, so no servlet change. `patient-home.jsp`, `reception-day.jsp`, `app.css` | **Fixed** |
| **GAP-FTB-06** | The booking screen chose a treatment from a bare dropdown, so a patient whose need matched no listed procedure had no honest way to book — and reception's walk-in booking sent the same. The visit a patient actually wants was neither capturable nor recorded | The dropdown became a **single-treatment radio checklist** (name, description, price per choice) with an **"Other (describe…)"** option whose free text is stored as the booking reason on the appointment. The server requires a treatment or a reason, and the reason round-trips on the appointment record. **Awaiting live deployment.** `appointment.patient_reason`, `AppointmentService.book(…, patientReason)`, `BookAppointmentServlet`, `book.jsp`, `app.css` | **Fixed — awaiting verification** |

---

## 9. Out of scope for this role

Creating an account for anyone else or in any other role, rescheduling in place (cancel and
rebook instead), choosing a specific room or chair, seeing any dentist's full schedule, seeing
clinic revenue, paying online, and viewing another family member's records.
