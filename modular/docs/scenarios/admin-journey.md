# Administrator Journey — Clinic Management

**Who this is:** The clinic owner or manager. They do not use the system every day the
way the receptionist does. They sign in weekly, monthly, or when something specific needs
attention — a report to review, a staff account to create, a complaint to resolve, or an
account that has locked itself out. Their relationship with the system is oversight and
governance, not day-to-day operation.

The administrator portal is deliberately not linked from the public login chooser. It is
reached directly at `/login/admin`. This is intentional — no member of the public has
reason to know it exists.

---

## The journey, step by step

### 1. Signing in

The administrator navigates directly to `http://<clinic-address>/login/admin`. They enter
their email and password.

On success they land immediately on the **Reports** screen — their home screen — because
the first question a clinic owner asks when they open the system is almost always
about money: *how are we doing?*

---

### 2. Reading the reports

The reports screen (`/admin/reports`) shows financial and operational data for a date
range the administrator chooses. It defaults to a recent period so the numbers are
immediately useful without having to configure anything.

**What the administrator sees:**

**Headline figures** — the top-line summary for the period:
- **Gross takings** — the total amount all patients paid across all bills issued in
  the period. This is the total money that came into the clinic.
- **Bills issued** — the count of completed visits that were billed
- **Patients seen** — the count of unique patients treated
- **Registered patients** — the total patient count on the register

**Where the money went — the revenue split:**

This section answers *"of the gross takings, where did each rupee go?"*

| Section | What it means |
|---|---|
| **Dentists** | The share paid out to all dentists combined — each dentist earns a percentage of the treatment cost for every appointment they complete (default 60%) |
| **The clinic** | The clinic's own retained earnings — what is left after paying dentists and receptionists. This is the clinic's operating margin. It is the figure to watch when deciding whether to change the service charge or a dentist's revenue share |
| **Receptionists** | The share paid to receptionists as handling commission — zero by default; only non-zero if the clinic pays a commission on the service charge |
| **Total** | Should match the gross takings figure above |

> **To clarify your question:** Yes — **The Clinic** is the clinic's own profit/margin from
> the period. **Gross** (or Total) is the full amount paid by all patients. The difference
> between gross and what the dentists and receptionists earned is what the clinic keeps.

**Per dentist earnings** — a breakdown of how much each individual dentist generated in
the period, with bill count, patient count and total earnings. This answers: *"which
dentist is the busiest, and which brings in the most?"*

**Per receptionist earnings** — similarly, how much service charge each receptionist
handled. Useful for assessing desk workload distribution.

**Daily takings** — a day-by-day breakdown of income across the period, so a trend is
visible rather than just a total. A spike or a dip on a particular day is visible at
a glance.

**Daily footfall** — patients seen per day across the period. Footfall and takings
together answer *"were we busy but earning less, or quiet but billing more per visit?"*

**No-show rate** — appointments that were confirmed but never completed or billed.
A high no-show rate is the signal that a reminder system is worth investing in.

> The administrator can export any report to CSV for use in a spreadsheet
> via the **Export CSV** button.

---

### 3. Managing staff accounts

The administrator goes to **Accounts** (`/admin/accounts`) to manage who can access the
system.

**Creating a new staff account:**

The administrator fills in:
- Full name
- Email address
- Role (Receptionist, Dentist, or Administrator)
- For a dentist: specialisation and consultation fee

On submission the system generates a **one-time password** which is shown **once on
screen immediately**. The administrator must hand this to the new staff member immediately
— in person, by phone or by a secure message. Once the administrator navigates away from
that screen, the password is gone. It is never stored and cannot be looked up.

The new staff member signs in with that one-time password and should change it on first
use.

> **[GAP]** The system does not enforce a password change on first login. The one-time
> password remains valid indefinitely unless the staff member changes it themselves or
> the administrator resets it. A forced password change on first login would close this
> security gap.

**Viewing all accounts:**

Below the creation form, a table lists every account in the system across all roles —
patients, receptionists, dentists and administrators. For each account the table shows:
name, email, role, current state (Active, Inactive, Locked) and the number of failed
login attempts if any.

**Deactivating an account:**

If a staff member leaves the clinic, or a patient account needs to be suspended, the
administrator clicks **Deactivate** on that account's row.

The account is deactivated — not deleted. Every appointment, bill and audit record that
references this account remains intact. The history is preserved.

> **On what a deactivated user sees when they try to sign in:** The current system shows
> the generic message *"Incorrect email or password."* — the same message shown for a
> wrong password. It does **not** say the account has been deactivated or blocked by the
> administrator. The user would not know whether they typed the wrong password or their
> access has been removed.
>
> **[GAP]** A deactivated account should show a distinct message such as *"This account
> has been deactivated. Please contact the clinic."* This would prevent confusion — a
> receptionist whose account is deactivated by mistake would otherwise think they are
> typing the wrong password and waste time at the desk. The current behaviour treats
> deactivated the same as wrong credentials.

**Reactivating an account:**

If the administrator deactivated an account by mistake or a staff member returns, they
click **Reactivate** to restore access.

**Unlocking a locked account:**

An account locks after five consecutive wrong passwords. The locked user cannot sign in
even with the correct password. The administrator clicks **Unlock** on that account's
row. This resets the failed-attempt counter and restores access immediately.

> This applies to any role — a patient who locks themselves out must contact the clinic;
> the administrator is the only one who can clear it.

**Resetting a password:**

If a staff member forgets their password, the administrator clicks **Reset password**.
A new one-time password is generated and shown once on screen. The administrator hands
it to the staff member the same way as the initial password.

---

### 4. Managing the treatment catalogue

The administrator goes to **Treatments** (`/admin/treatments`) to manage what treatments
the clinic offers and what each costs.

**What they see:** A table of all treatments — active and inactive — showing name,
description, current price and status.

**Adding a new treatment:**
The administrator fills in the treatment name, price (Rs) and a short description, then
clicks **Add treatment**. The treatment immediately appears in the patient booking
dropdown.

**Editing an existing treatment:**
Each treatment row has inline fields for name, price and description. The administrator
edits and clicks **Save**. The updated price takes effect immediately for all future
bookings.

> Changing a price does not alter any bill already issued. Bills record the amounts
> at the time of billing — a past bill is never changed by a price update. Only future
> bookings use the new price.

**Deactivating a treatment:**
If the clinic stops offering a treatment, the administrator clicks **Deactivate**. The
treatment disappears from the patient booking dropdown immediately but is kept in the
database because past appointments reference it. It can be reactivated at any time.

> **[GAP]** There is currently no way to edit a dentist's consultation fee after the
> account is created. The fee is set once at account creation and is not editable from
> any screen. If a dentist's fee changes — a negotiated increase, a promotional rate —
> the administrator cannot update it without direct database access.

---

### 5. Reading and resolving complaints

The administrator goes to **Complaints** (`/admin/complaints`) to see concerns raised
by patients about dentists.

**What they see:** Every complaint in the system, with the patient's account of what
happened, which dentist is named, which appointment it concerns, and the current state.

States a complaint moves through:
- **Open** — just submitted by the patient, not yet reviewed
- **Under review** — the administrator has opened it and is looking into it
- **Resolved** — the administrator has written a resolution and closed it
- **Dismissed** — the administrator has reviewed it and found no action warranted,
  with a written reason

The administrator can move complaints through these states and must write a resolution
before closing. A complaint closed with no explanation is indistinguishable from one
that was ignored, which is why a written resolution is required.

> **The dentist named in a complaint never sees it.** A complaint the subject can read is
> one most patients will not file. The restriction is what makes the mechanism work.

> **[GAP]** The reports screen does not show complaint volume per dentist. A rising
> complaint count against a specific dentist is a management signal that belongs on the
> same screen as that dentist's earnings — a dentist generating revenue on declining
> patient satisfaction is exactly what the owner needs to see. The data exists
> (`ComplaintService.countByDentist`) but is not surfaced on the reports screen.

---

### 6. Reading the audit trail

The administrator goes to **Audit** (`/admin/audit`) to see a chronological record of
every significant action taken in the system.

**What the audit trail is for in the real world:**

The audit trail answers *"who did what, and when?"* It exists for three real-world
reasons:

1. **Accountability** — if a booking was cancelled, a bill was issued, or an account
   was changed, there is a permanent record of who did it and when. No action can be
   taken and then denied.

2. **Investigation** — if a patient disputes a bill, or a staff member says they did
   not cancel an appointment, the trail is the authoritative record. It cannot be edited
   or deleted by anyone, including the administrator.

3. **Compliance** — clinics handling patient data are often required to maintain access
   logs. The audit trail provides this for appointment events and account changes.

**What is recorded:**

Every entry in the audit trail records:
- Who did it (`actor` — name and role)
- What they did (`action`)
- What they did it to (`target` — appointment number, account id, etc.)
- When it happened (timestamp)

The actions currently recorded are:

| Action | What triggered it |
|---|---|
| `APPOINTMENT_BOOKED` | Any booking — patient self-booking or reception booking on behalf |
| `APPOINTMENT_CANCELLED` | Any cancellation — patient or reception |
| `APPOINTMENT_COMPLETED` | Dentist marks treatment complete |
| `APPOINTMENT_BILLED` | Reception issues a bill |
| `ACCOUNT_CREATED` | Administrator creates a staff account |
| `DENTIST_PROFILE_CREATED` | Administrator creates a dentist account (profile row created) |
| `ACCOUNT_UNLOCKED` | Administrator unlocks a locked account |
| `ACCOUNT_DEACTIVATED` | Administrator deactivates an account |
| `ACCOUNT_REACTIVATED` | Administrator reactivates an account |
| `PASSWORD_RESET` | Administrator resets a staff member's password |

**What is deliberately not recorded:**

- The content of a diagnosis — the trail records that a treatment was completed, not
  what the dentist found. Clinical notes are confidential even in the audit trail.
- The content of a patient's medical notes — the trail records that notes were read,
  not what they said.
- Password hashes — never logged anywhere.

**The administrator can filter** by who performed the action, by which appointment or
account was affected, and by date range.

> **[GAP]** The audit trail shows raw action codes (`APPOINTMENT_BOOKED`,
> `ACCOUNT_DEACTIVATED`) rather than plain-language descriptions. A clinic manager
> reading the trail must understand what each code means. Plain-language labels such as
> *"Appointment booked by reception for Nimal Perera"* would make the trail readable
> without training.

---

## Gaps summary — administrator path

| # | Gap | Impact | Status |
|---|---|---|---|
| 1 | Deactivated account shows generic "Incorrect email or password" instead of a clear "account deactivated" message | Deactivated staff cannot tell if they are typing the wrong password or have been removed | Open |
| 2 | Dentist consultation fee is not editable after account creation | Fee changes require direct database access | Open |
| 3 | No forced password change on first login | One-time passwords remain valid indefinitely | Open |
| 4 | Complaint volume per dentist not shown on reports screen | Rising complaints against a specific dentist are invisible unless the administrator checks the complaints screen separately | Open |
| 5 | Audit trail shows raw action codes, not plain-language descriptions | Trail requires training to read | Open |
| 6 | Reports screen does not show mean rating per dentist alongside earnings | A dentist earning well on falling ratings is the exact signal the owner needs — it exists in the service but is not surfaced | Open |
