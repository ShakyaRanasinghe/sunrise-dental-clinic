# SRS — Administrator

Role-specific requirements for the **administrator** portal of the Sunrise Dental Clinic
system. Common requirements, constraints and definitions are in [`srs.md`](srs.md); this
document does not repeat them.

| | |
|---|---|
| Role constant | `Role.ADMIN` |
| Sign-in portal | `GET`/`POST` `/login/admin` |
| Home screen | `/admin/reports` |
| Profile table | none — identity is the `user_account` row |
| Requirement prefix | `FR-ADM-` |

---

## 1. Actor profile

The clinic owner or manager. Signs in weekly or monthly rather than daily, to answer
questions about money, workload and staffing, and occasionally to fix an account someone has
locked themselves out of.

This role is **read-heavy and write-light**: it reads across the whole clinic but changes
almost nothing operational. It is the only role that can see revenue, and the only one that
can create or unlock an account.

Like the receptionist, an administrator has **no profile table** — the role carries no
attributes beyond name, email and role.

**The most privileged account in the system, and deliberately not omnipotent.** An
administrator cannot read a patient's diagnosis. Clinical confidentiality is a stronger
constraint than administrative seniority, and the system enforces that rather than trusting
it.

---

## 2. Goals

1. Know what the clinic earned over a period, and where the money went
2. Know which dentists and receptionists are generating it
3. Know how busy the clinic actually was
4. Get the figures out into a spreadsheet
5. Create a staff account, and unlock one that has locked

---

## 3. Sign-in portal

| ID | Requirement | Status |
|---|---|---|
| **FR-ADM-01** | `/login/admin` must present username and password only (GAP-ADM-12; email still works). No registration link, and no route to create an administrator from outside | Built |
| **FR-ADM-02** | An account whose role is not `ADMIN` must be rejected, with the same message as a wrong password (FR-AUTH-03) | Built |
| **FR-ADM-03** | This portal must not be advertised to the public — there is no listing on any public page (the shared "all doors" chooser it was kept off was removed), and it remains reachable only by direct URL | Built — the staff-only chooser at `/staff` (GAP-FTB-10) lists the admin door alongside reception and dentist but is itself unadvertised, reachable only by direct URL, so it does not surface this portal on any public page |
| **FR-ADM-04** | A failed sign-in on this portal must be written to the audit trail, whether or not the email exists | **Partial** — a failed administrator sign-in is written to the application log, not to the audit trail. The hook is there and writes the wrong sink |

---

## 4. Screens and operations

| Screen | Route | Purpose |
|---|---|---|
| Sign in | `/login/admin` | Authenticate |
| Clinic reports | `/admin/reports` | Income, revenue split, earnings, footfall |
| Staff accounts | `/admin/staff` | Create an account; unlock, deactivate | 
| Clinic identity | `/admin/clinic` | Edit clinic name, phone, email, address |
| Complaints | `/admin/complaints` | Read, review and resolve patient concerns |
| Audit trail | `/admin/audit` | Who changed what, and when |
| Help | `/help` | Step-by-step guidance |

### 4.1 Reports

| ID | Requirement | Status |
|---|---|---|
| **FR-ADM-10** | Reports must cover a date range the administrator chooses, defaulting to a sensible recent period | Built |
| **FR-ADM-11** | Gross takings, bills issued, patients seen and registered patients must be shown as headline figures | Built |
| **FR-ADM-12** | The three-way revenue split — dentist, clinic, reception — must be shown for the period | Built |
| **FR-ADM-13** | Earnings must be broken down per dentist and per receptionist | Built |
| **FR-ADM-14** | Daily takings must be shown across the period, so a trend is visible rather than only a total | Built |
| **FR-ADM-15** | Footfall — appointments attended — must be shown across the period | Built |
| **FR-ADM-16** | Every report must state the decision it supports, so a figure is actionable rather than merely present | Built |
| **FR-ADM-17** | A period with no activity must say so, not render an empty chart | Built |
| **FR-ADM-18** | Reports must be exportable as CSV for use in a spreadsheet | Built |
| **FR-ADM-19** | Reports should include a no-show rate, since it is what justifies a reminder policy | Built |

**Decisions each report supports** (FR-ADM-16):

| Report | Question it answers | Decision |
| Rating per dentist | How do patients rate each dentist? | Training, mentoring, whether earnings and satisfaction are diverging |
|---|---|---|
| Gross takings, daily takings | What is coming in, and is it steady? | Cash-flow planning |
| Revenue split | How much does the clinic retain? | Fee and share negotiation |
| Earnings per dentist | Who generates what? | Rostering, retention |
| Earnings per receptionist | Who handles the desk load? | Shift allocation |
| Footfall | How busy was the clinic? | Staffing levels, opening hours |
| No-show rate (FR-ADM-19) | How much capacity is wasted? | Whether reminders are worth their cost |

### 4.2 Staff accounts

| ID | Requirement | Status |
|---|---|---|
| **FR-ADM-20** | The administrator must be able to create an account for a **receptionist, dentist or administrator**. These three roles cannot self-register; only patients can (**ASM-05**, **FR-PAT-03**) | Built |
| **FR-ADM-21** | Creating a `DENTIST` account must create or link the matching `dentist` profile row in the same transaction, including specialization and consultation fee | Built |
| **FR-ADM-22** | Account creation must go through a single factory, so every role is created one way and a new role means one new case rather than a new screen | Built |
| **FR-ADM-23** | The administrator must be able to unlock **any** account locked by failed sign-in attempts, of any role, **including a patient's**, resetting the counter | Built — endpoint exists, no screen |
| **FR-ADM-24** | The administrator must be able to deactivate **any** account of any role, including a patient's, without deleting it, since audit records reference it | Built |
| **FR-ADM-24a** | Deactivating a patient account must not delete or hide the `patient` record. The person stays bookable by reception over the counter; only their portal access ends | Specified |
| **FR-ADM-25** | The administrator must not be able to read or set any password. A new account is issued a one-time credential the holder must change | Built |
| **FR-ADM-26** | An administrator must not be able to deactivate or lock their own account, which would leave the clinic with no administrator | Built |
| **FR-ADM-27** | Every account change must be written to the audit trail with actor, target and time | Built |

**Create and manage have different scopes, deliberately.** The two are easy to conflate and this
screen must not:

| Action | Which roles | Why |
|---|---|---|
| **Create** an account | receptionist, dentist, administrator **only** | Patients self-register (**FR-PAT-03**), so an administrator never needs to create one |
| **Unlock** an account | **any** role, patients included | A patient who fails five sign-ins has no other way back in — see [`srs-patient.md`](srs-patient.md) §8 |
| **Deactivate** an account | **any** role, patients included | A patient may ask for their portal access to end; a departing member of staff must lose theirs |

So the screen is **Accounts**, not *Staff accounts*: it manages every account in the system and
creates only three of the four kinds. Naming it after staff would misdescribe most of what it
does — worth stating because the prototype's file is still called `admin-staff.html`.

**Why this matters beyond the rubric.** Today no screen creates a staff account:
`RegisterServlet` hardcodes `Role.PATIENT`, so every receptionist, dentist and administrator
in the system exists only because the seed script inserted them. A clinic could not add a
receptionist without a developer. FR-ADM-20 to FR-ADM-22 close that gap.

### 4.3 Complaints

The administrator is the **only** role that reads these. The dentist named never does
(**FR-CMP-08**), and neither does reception (**FR-CMP-09**).

| ID | Requirement | Status |
|---|---|---|
| **FR-ADM-50** | The administrator must be able to list every complaint, filtered by state, dentist or date | Built |
| **FR-ADM-51** | Complaints awaiting review must be distinguishable at a glance from those already closed | Built |
| **FR-ADM-52** | The administrator must be able to move a complaint to `UNDER_REVIEW`, then to `RESOLVED` or `DISMISSED` | Built |
| **FR-ADM-53** | Closing a complaint must require a written resolution. A concern closed with no explanation is indistinguishable from one ignored | Built |
| **FR-ADM-54** | The administrator must be able to see the appointment a complaint concerns, and the dentist named | Built |
| **FR-ADM-55** | The administrator must **not** be able to edit or delete the patient's account of what happened. Only the state and the resolution are theirs to write | Built |
| **FR-ADM-56** | Every read of a complaint must be audited, naming the administrator who read it (**FR-CMP-11**) | Built |
| **FR-ADM-57** | Complaint volume per dentist should be visible on the reports screen as a count only, never as content, since a rising count is a management signal | **Partial** — `ComplaintService.countByDentist` exists and the reports screen does not show it |
| **FR-ADM-59** | The administrator must be able to read individual reviews with their comments, for every dentist (**FR-RVW-09**) | Built |
| **FR-ADM-60** | The reports screen must show mean rating and review count per dentist alongside earnings, since a dentist earning well on falling ratings is exactly the signal a manager needs | **Partial** — `ReviewService.summaryFor` exists and the reports screen does not show it |
| **FR-ADM-61** | The administrator must not be able to edit or delete a review. Curating the feedback would make the average meaningless | Built |
| **FR-ADM-58** | A complaint must not expose the patient's medical notes or any diagnosis, even where the complaint concerns clinical care. The administrator reads the complaint, not the record it refers to | Built |

**FR-ADM-58 is the awkward one, and it is deliberate.** A clinical-concern complaint may be
impossible to judge without the clinical record — and the administrator still cannot see it. The
resolution is procedural rather than technical: a clinical complaint is referred to a dentist
other than the one named, who can read the record in their own right. Recording that limitation
is more honest than quietly widening the administrator's access.

### 4.4 Audit trail

| ID | Requirement | Status |
|---|---|---|
| **FR-ADM-30** | The administrator must be able to read the audit trail, filtered by actor, target or date range | Built |
| **FR-ADM-31** | Audit records must not be editable or deletable through any screen or endpoint | Built |
| **FR-ADM-32** | The trail must answer "who changed this appointment, and when" for any appointment number | Built |
| **FR-ADM-33** | The trail must never expose a diagnosis, even where the audited action changed one. It records that a change happened, not its content | Built |

### 4.5 Clinic identity

| ID | Requirement | Status |
|---|---|---|
| **FR-ADM-70** | The administrator must be able to edit the clinic name, phone number, email and address — values that appear on the landing page, help page, receipts and appointment slips | Built — `/admin/clinic`, `ClinicIdentityService`, `clinic_setting` table |
| **FR-ADM-71** | All four fields must be required; an empty value must be rejected with a clear message | Built |
| **FR-ADM-72** | Changes must take effect immediately — no cache, no restart. A patient who refreshes the landing page after the admin saves must see the new details | Built — values read from the database on every request, no caching layer |

### 4.6 Reference data

| ID | Requirement | Status |
|---|---|---|
| **FR-ADM-40** | The administrator should be able to add a treatment and set its cost | **Specified** — no treatment catalogue screen. A "should" |
| **FR-ADM-41** | The administrator should be able to change a dentist's consultation fee | **Specified** — a dentist's fee is set when the account is created and not editable afterwards |
| **FR-ADM-42** | A treatment must be deactivatable rather than deletable, since past appointments reference it | **Specified** — the `active` column exists and no screen sets it |
| **FR-ADM-43** | Changing a price must not alter any bill already issued. A bill records the amounts charged at the time | Built — amounts are copied onto the bill, not referenced |

---

## 5. Data access

| Table | Read | Write |
|---|---|---|
| `user_account` | **all rows**, never `password_hash` | create, unlock, deactivate |
| `patient` | all rows, aggregate and detail | — |
| `dentist` | all rows | `consultation_fee`, `active` (FR-ADM-41) |
| `appointment` | all rows **except `diagnosis`** | — |
| `patient_note` | **never** | never |
| `bill` | **all rows** — the only role that may | — |
| `slot`, `dentist_session` | all | — |
| `treatment` | all | insert, deactivate (FR-ADM-40) |
| `complaint` | **all rows** — the only role that may read | `status`, `resolution`, `reviewed_by_uid` only. Never the patient's account of events (FR-ADM-55) |
| `dentist_review` | **all rows**, comments and authors included | never (FR-ADM-61) |
| `audit_event` | **all rows** — the only role that may read | never writes directly; written on its behalf |
| `notification` | all — to see whether a reminder was delivered | — |

**Three hard exclusions, all deliberate:**

- **`appointment.diagnosis`** is unreadable by this role. Seniority does not override
  clinical confidentiality (NFR-SEC-06).
- **`user_account.password_hash`** is never served to any screen or endpoint, so an
  administrator cannot exfiltrate a hash for offline attack (FR-ADM-25).
- **`patient_note`** is unreadable by this role (**FR-NOTE-11**), even when reviewing a clinical complaint (**FR-ADM-58**). The most privileged account in
  the system cannot see that a patient declared a penicillin allergy. Administrative seniority
  is not clinical need, and an audit of an incident is answered from `audit_event`, which records
  that a note existed and was read without reproducing its content.

---

## 6. Validation

| Field | Rule | Message on failure |
|---|---|---|
| From date | Required, valid, not after the To date | "The start date must come before the end date." |
| To date | Required, valid, not in the future | "The end date cannot be in the future." |
| Date range | At most 366 days, to keep a report responsive | "Choose a range of a year or less." |
| Staff email | Required, valid, not already registered | "That email is already registered." |
| Staff name | Required, 2–255 characters | "Enter the person's full name." |
| Role | Required, one of the four | "Choose a role." |
| Specialization | Required when the role is `DENTIST` | "Enter the dentist's specialization." |
| Consultation fee | Required when the role is `DENTIST`, greater than zero | "Enter a fee greater than zero." |
| Treatment cost | Required, not negative | "Enter a cost of zero or more." |
| Complaint resolution | Required when closing, 10–2000 characters | "Say how this was resolved." |

---

## 7. Web service endpoints

| Method | Endpoint | Notes |
|---|---|---|
| `POST` | `/api/auth/login`, `/api/auth/logout` | |
| `GET` | `/api/auth/me` | |
| `POST` | `/api/accounts` | Staff account with an explicit role. Separate from `/api/auth/register`, which is the patient path and hardcodes `PATIENT` |
| `POST` | `/api/auth/unlock` | Clear a lock-out |
| `GET` | `/api/auth/lock-status` | Failed attempts and lock state |
| `GET` | `/api/appointments` | All, diagnosis excluded |
| `GET` | `/api/complaints` | All complaints |
| `PATCH` | `/api/complaints/{id}` | State and resolution only |
| `GET` | `/api/patients?q=` | |
| `GET` | `/api/dentists`, `/api/treatments` | |

Reports are served as pages and CSV rather than as JSON; FR-WS-04 covers documenting a JSON
reporting endpoint if one is added.

---

## 8. Error handling

| Situation | Behaviour |
|---|---|
| Date range inverted | Rejected before any query runs, naming which date to change |
| Range with no data | "No bills issued in this period." — not an empty table |
| Creating an account with an existing email | Rejected plainly, no database constraint error surfaced |
| Creating a `DENTIST` without specialization or fee | Rejected, naming the missing field |
| Unlocking an account that is not locked | Reported as already unlocked, treated as success |
| Attempting to deactivate own account | Refused, explaining that a clinic must retain an administrator |
| CSV export of a very large range | Streamed rather than buffered, so a long period does not exhaust memory |
| Database unavailable | Plain apology page; no stack trace, no SQL text |

---

## 10. Identified gaps and enhancements

Gaps found during QA on the `develop` branch.

| Ref | Gap observed | Fix applied | Status |
|---|---|---|---|
| **GAP-ADM-01** | FR-ADM-40 / FR-ADM-42: no UI to manage the treatment catalogue — prices, names and active status were hardcoded in `demo-data.sql` and only changeable via direct DB access | Built `/admin/treatments` — list all treatments, add new, edit name/price/description inline, deactivate/reactivate. `TreatmentAdminService`, `TreatmentAdminServlet`, `treatments.jsp`. New `MANAGE_TREATMENTS` action added. **FR-ADM-40 and FR-ADM-42 are now Built** | **Fixed** |
| **GAP-ADM-02** | FR-ADM-41: dentist consultation fee is set at account creation and cannot be changed afterwards — no edit path exists | Not yet addressed | **Open** |
| **GAP-ADM-03** | FR-ADM-04: failed administrator sign-in is written to the application log, not to the audit trail | Not yet addressed | **Open** |
| **GAP-ADM-04** | FR-ADM-57: complaint volume per dentist exists in `ComplaintService.countByDentist` but is not shown on the reports screen | Not yet addressed | **Open** |
| **GAP-ADM-05** | FR-ADM-60: mean rating and review count per dentist exists in `ReviewService.summaryFor` but is not shown on the reports screen | Not yet addressed | **Open** |
| **GAP-ADM-06** | Clinic identity — name, phone, email, address — is stored only in `clinic.properties` with deploy-time defaults; no admin UI or database table exists to change them at runtime. An admin must edit the file and redeploy, or set environment variables and restart the container | `clinic_setting` table, `ClinicIdentityService`, `ClinicIdentityServlet` at `/admin/clinic`. Admin edits all four fields; reception edits phone only (`ReceptionPhoneServlet`, `MANAGE_PHONE`). Consumers (`HomeServlet`, `HelpServlet`, `ReceiptServlet`, `AppointmentSlipServlet`) now read from the database via `app().clinicIdentity()` | **Fixed** |
| **GAP-ADM-07** | The clinic phone number (admin's clinic settings and reception's phone screen) accepted anything non-empty — letters, a stray digit count, a number not starting with the local trunk `0` — and stored it verbatim, so the number printed on receipts, slips, the help page and the landing page could be unbookable | `ClinicIdentityService.update` now runs `clinic.phone` through the shared `PhoneNumbers` rule (no letters, start with `0` or `+94`, exactly 10 digits) before saving; `phone.jsp` and `clinic-identity.jsp` carry the matching browser-side `pattern` | **Fixed** |
| **GAP-ADM-08** | The administrator's navbar still carried a **Home** tab pointing at `/admin/reports`, duplicating the Sunrise logo, which lands on the same screen (the redundant Home was already removed for patient and reception, GAP-REC-07) | `AdminPolicy.navigation()` now returns only the admin's own tabs — Reports, Accounts, Treatments, Clinic, Complaints, Audit — matching the patient/reception pattern (`AdminPolicy.java`) | **Fixed** |
| **GAP-ADM-09** | Every complaint on `admin/complaints` was rendered fully expanded — its details table and review/resolve form — in one long list, so a queue of more than a handful became unscannable and the forms tempted an admin into acting with the whole page open | The queue is now split in two: **"To act on"** (open — Received / Being looked at) first as compact `<details>` cards showing patient, named dentist and status at a glance, with the review and resolve controls revealed only when the card is clicked open (an in-progress one starts expanded); then a single **"Reviewed"** `<details>` accordion folding away resolved/closed complaints with their resolutions. Purely CSS `<details>/<summary>`, no script (`AdminComplaintsServlet` exposes `open`/`closed` lists; `admin-complaints.jsp` + `complaint-card` styles) | **Fixed** |
| **GAP-ADM-10** | The revenue dials live in `clinic.properties`/env and need a rebuild + restart to change: the administrator cannot configure the dentist's treatment-share percentage (or the service charge) from the dashboard, and Reports still shows a "Reception handling" metric for a commission that is always zero | **Fixed** — **Pricing** tab (`/admin/pricing`, nav) edits share % + charge with validation into `clinic_setting`; strategy and charge read per bill (no restart); reception stat/table/CSV removed; issued bills keep their split (`PricingServlet`, `ClinicIdentityService`, `AppContext` supplier wiring, `reports.jsp`, `ReportService`) | **Fixed** |
| **GAP-ADM-12** | Staff sign in with email addresses | **Fixed** — staff portals ask for a username (email fallback); `UserAccountFactory` validates and enforces unique usernames at creation; Accounts form field, handover card and table column; seeds + backfills done; FR-ADM-01 updated | **Fixed** |
| **GAP-ADM-11** | Staff accounts and new dentists carry database UUIDs (or name slugs): the people screens show no quotable identifier | **Fixed** — `user_account.account_no` minted per role in the factory; dentist ids minted `YYMMDDDENNNNN`; `AccountRow.accountNumber()` listed leftmost; existing rows backfilled | **Fixed** |
| **GAP-FTB-12** | There is a single shared help page (`/help`) written for the public; the administrator uses the system differently (reports, accounts, treatments, clinic settings, complaints, audit) and has no help page for that scenario, rooted in the project's SRS/docs | **Fixed** — `shared/help-admin.jsp` covers reports, accounts, treatments, clinic identity, complaints and audit; served at `/help/admin`, linked from the admin sign-in screen and nav Help entry; the shared `/help` stays for the public. (Parent gap GAP-FTB-12 in `gap-checklist.md`) | **Fixed** |
| **GAP-FTB-14** | Profile/settings pages render every editable field as an in-page form at once; the user wants a **read-only view by default** with an **Edit** control revealing only the changeable fields, and every successful update in the system (admin editing clinic identity, treatments, accounts, resolving complaints) to confirm with a **sub-window ("successfully updated")** rather than only a top-of-page notice | **Fixed** — clinic identity, the treatment catalogue (`?saved=1`), resolved concerns and issued accounts all confirm in the shared `.sub-window.open` success window (`clinic-identity.jsp`, `treatments.jsp`, `admin-complaints.jsp`, `accounts.jsp`; `app.css`). (Parent gap GAP-FTB-14 in `gap-checklist.md`) | **Fixed** |
| **GAP-PAT-29** | The booking route lets a patient inspect a treatment's full details only if the patient role surfaces them; the administrator authors each treatment's description (`/admin/treatments`) | **Fixed** on the patient side (GAP-PAT-29 in `srs-patient.md`): the description authored here now surfaces behind each treatment's info mark. No admin change was required | **Fixed** |
| **GAP-PAT-32** | Patients can also raise general concerns naming no dentist; these reach the administrator anonymously | **Fixed** — open and reviewed cards label them "Anonymous · general concern"; `describe()` null-guards both names; named concerns unchanged (`admin-complaints.jsp`, `ComplaintService`) | **Fixed** |

---

## 9. Out of scope for this role

Reading any patient's medical notes, reading any patient's diagnosis, reading any password hash, booking or cancelling
appointments, issuing bills, publishing availability, editing a bill already issued, and
deleting any patient, appointment, bill or audit record.
