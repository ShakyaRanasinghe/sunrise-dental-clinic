# UI Prototype

Nineteen static screens showing what the modular implementation's interface looks like,
grouped by whose dashboard each one is.

**Open `index.html` in a browser.** No server, no build step, no dependencies.

```bash
xdg-open modular/docs/prototype/index.html      # Linux
```

---

## Grouped by whose dashboard it is

```
prototype/
├── index.html          contact sheet, grouped by role
├── build.py            the generator
├── css/app.css
├── access/     6       sign-in — shared by all four roles
├── patient/    4       Nimal Perera · role PATIENT
├── reception/  4       Kumari Silva · role RECEPTIONIST
├── dentist/    1       Dr. Ranil Silva · role DENTIST
├── admin/      3       Anoma Fernando · role ADMIN
└── shared/     1       reachable without signing in
```

**The prototype groups by role; the JSP views group by feature module.** That is deliberate,
not an inconsistency. This tree answers *"whose screen is this?"* — the question a reviewer
asks. The JSP tree answers *"which module owns it?"* — the question a maintainer asks. Every
page's black bar carries both, so `patient/profile.html` shows
`GET /patient/profile → patients/profile.jsp`, and the bar's left tag names the role
(`PATIENT SCREEN`, `PUBLIC`).

---

## What is, and is not, in here

**It is** the real stylesheet with realistic content, so a screen here is what a JSP will
render.

**It is not** wired up. Forms do not submit, links between screens work but nothing is saved,
and the data is the seeded demo data — Nimal Perera, Dr. Ranil Silva, Rs 5,200.

---

## Screens

| Group | Screen | Route | Becomes |
|---|---|---|---|
| access | `portal-chooser.html` | `GET /login` | `access/portal-chooser.jsp` |
| access | `login-patient.html` | `GET /login/patient` | `access/login-patient.jsp` |
| access | `login-reception.html` | `GET /login/reception` | `access/login-reception.jsp` |
| access | `login-dentist.html` | `GET /login/dentist` | `access/login-dentist.jsp` |
| access | `login-admin.html` | `GET /login/admin` | `access/login-admin.jsp` |
| access | `register.html` | `GET /register` | `access/register.jsp` |
| patient | `home.html` | `GET /patient/home` | `appointments/patient-home.jsp` |
| patient | `book.html` | `GET /patient/book` | `appointments/book.jsp` |
| patient | `profile.html` | `GET /patient/profile` | `patients/profile.jsp` |
| patient | `complaints.html` | `GET /patient/complaints` | `complaints/patient-complaints.jsp` |
| reception | `day.html` | `GET /reception/home` | `appointments/reception-day.jsp` |
| reception | `patients.html` | `GET /reception/patients` | `patients/records.jsp` |
| reception | `availability.html` | `GET /reception/availability` | `scheduling/availability.jsp` |
| reception | `billing.html` | `GET /reception/billing` | `billing/billing.jsp` |
| dentist | `schedule.html` | `GET /dentist/schedule` | `appointments/dentist-schedule.jsp` |
| admin | `reports.html` | `GET /admin/reports` | `reporting/reports.jsp` |
| admin | `accounts.html` | `GET /admin/accounts` | `reporting/accounts.jsp` |
| admin | `complaints.html` | `GET /admin/complaints` | `complaints/admin-complaints.jsp` |
| shared | `help.html` | `GET /help` | `shared/help.jsp` |

## Why a generator

`build.py` emits every page from shared pieces, because those pieces are the JSP fragments:

| In `build.py` | Becomes |
|---|---|
| `header()` | `shared/header.jspf` — brand, role navigation, signed-in user |
| `FOOTER` | `shared/footer.jspf` |
| `login_form()` | `access/login-form.jspf` — included by all four portals |
| `portal_page()` | the four portal views, differing only in label, note and extras |
| `NAV` | what `RolePolicy.navigation()` will return |

So the prototype has the same composition the application will have. Translating it is
mechanical rather than a redesign, and the four login pages stay identical by construction
instead of by discipline.

```bash
python3 build.py        # regenerate after editing
```

`css/app.css` is `layered/src/main/webapp/css/app.css` verbatim, plus two appended blocks: the
components the four-portal design needs (portal cards, portal label, diagnosis block, the
prototype's black bar), and the ones the notes and complaints screens need (note items, category
chips, complaint-state pills). The dividing comments mark where the copy ends.

---

## Design decisions the screens make

**The portal chooser lists three doors, not four.** Patient, reception and dentist.
`/login/admin` exists and works but is not linked, because no member of the public has business
there — **FR-ADM-03**.

**The four portals are the same page with one label changed.** Compare
`access/login-patient.html` with `access/login-reception.html`: the form, the fields and the
error position are identical; the patient page adds a "create an account" link and the others do
not. That is `allowsSelfRegistration()` returning `true` for one subclass — the inheritance in
[`../class-diagram.md`](../class-diagram.md) §3, made visible.

**Nothing medical is asked at sign-up.** `access/register.html` says so on the form: allergies
and medications are added later from `patient/profile.html` (**FR-NOTE-02**).

**The dentist is warned before opening a record.** `dentist/schedule.html` has a Notes column —
*2 critical* against one patient, *None declared* against another — and the opened record leads
with a red banner and the two notes above the clinical fields (**FR-NOTE-08**, **FR-DEN-42**).
"None declared" is written out rather than left blank, because silence must not read as safety
(**FR-DEN-43**).

**The two confidentiality rules are visible in opposite directions.** `dentist/schedule.html`
shows medical notes and says the dentist cannot change them; `reception/day.html` states that
neither diagnoses nor medical notes are shown to reception; `admin/complaints.html` states that
the dentist named never sees a complaint and that every read is audited.

**Create and manage are separated on the accounts screen.** `admin/accounts.html` leads with a
notice: create covers staff only, unlock and deactivate cover everyone including patients. Role
has no *Patient* option in the create panel.

**Reception's screens are dense; the patient's are not.** The day view puts six columns and four
summary tiles on one screen because a receptionist works at speed all day. The patient's booking
flow is three numbered cards with one decision each. Same system, different audiences
(**§2.3** of the SRS).

**Every report states the decision it supports.** Under each figure on `admin/reports.html`:
*"Supports: fee negotiation"*, *"Supports: rostering and retention"*. That is **FR-ADM-16**, and
it is what makes a report decision support rather than a data dump.

**Times already booked are absent, not greyed out.** On `patient/book.html` only open slots
appear (**FR-AVL-03**). On `reception/availability.html` the published grid *does* show booked
times ticked, because reception needs to see the whole day.

---

## Screens the current implementation does not have

Six of these are specified in the SRS and not built in `layered/`:

| Screen | Requirement | Why it matters |
|---|---|---|
| `access/portal-chooser.html` + the four portals | FR-AUTH-01…03 | Today there is one `/login` for everyone |
| `patient/profile.html` | FR-NOTE-01…06 | No screen collects a patient's allergies or medications |
| `patient/complaints.html` | FR-CMP-01…06 | No way for a patient to raise a concern |
| `admin/complaints.html` | FR-ADM-50…58 | No way to review or resolve one |
| `admin/accounts.html` | FR-ADM-20…24 | No screen creates a staff account. Every receptionist, dentist and admin exists only because `demo-data.sql` inserted them |
| `receipt` print layout | FR-REC-52 | The bill currently prints with the navigation chrome |

---

## Known rough edges

Honest list, so a reader does not mistake them for decisions:

- No mobile layout was drawn. The stylesheet is fluid and the screens hold up when narrowed,
  but no breakpoint was designed for a phone
- Error and validation states are shown on one screen only. Every field needs its rejected
  state drawn before the JSPs are written (**FR-UI-03**)
- Nothing shows a loading or empty-search state
- `not-found` and `error` pages are not prototyped
