# Gap Checklist — v1.0.1 follow-up

Working checklist for the gaps found after v1.0.1 shipped. One box per gap. Tick a box when
the fix is **committed and green**, not when it compiles. Each entry names its SRS gap ID, the
role file it is documented in, and the acceptance criterion.

Status definitions used in the SRS tables: **Open** · **Fixed** — awaiting verification ·
**Fixed**.

---

## Patient — `srs-patient.md`

- [x] **GAP-PAT-20** — make the contact number **mandatory** on self-registration
      (HTML5 `required` + server-side `PhoneNumbers` gate). *Because:* a patient who registers
      without a number can never be matched to their record at the desk.
      **Accept:** a blank phone is refused with a field reason; no account is created without it.
      **Done:** `access/register.jsp` marks the field `required`; `SelfRegistrationService.register`
      refuses a blank number; `RegisterServlet` re-renders with the reason. Tests green.

- [x] **GAP-PAT-21** — add a **confirmation step** before a cancellation commits (no-script, so a
      styled dialog / yes-no step that needs no JavaScript). Relates to open GAP-PAT-04.
      **Accept:** a single stray click cannot drop a confirmed appointment; cancelling requires an
      explicit second confirmation.
      **Done:** the **Cancel** control is now a `<details>/<summary>` that reveals a **"Yes, cancel
      it"** submit — no script. Applied to the patient dashboard and the reception day view
      (`patient-home.jsp`, `reception-day.jsp`, `app.css`). No servlet change.

- [x] **GAP-PAT-30** — a finished visit's info shows charges but not **what the dentist did**:
      opening a finished appointment (dashboard receipt view, full receipt page) never shows
      the dentist's recorded description.
      **Accept:** a finished visit's info shows the dentist's description to the patient it
      belongs to (never to reception/admin).
      **Done:** `BillResponse` carries `diagnosis`; the dashboard receipt view prints it from
      the gated detail row and `billing/receipt.jsp` prints it only when the viewer is the
      patient (`showClinical`; reception/admin copies stay clinical-free). Verified live.

---

## Reception — `srs-reception.md`

- [x] **GAP-REC-11** — add a **contact-number field** to the walk-in register form, validated
      through the shared `PhoneNumbers` rule, consistent with the mandatory phone (GAP-PAT-20).
      **Accept:** a walk-in patient is stored with their phone and the number is validated on save.
      **Verified — already satisfied, no code change.** Both walk-in forms
      (`patients/register.jsp`, `scheduling/walkin.jsp`) already have a **required** phone input,
      server-side enforced (`requiredField` in `PatientRecordsServlet` and `WalkInServlet`) and
      validated through `PatientService.validPhone` / `PhoneNumbers`. The real gap was the
      self-registration phone (GAP-PAT-20), now fixed.

- [x] **GAP-REC-12** — give every patient a **unique identifier** in the database and surface it as
      the **leftmost column** of the reception Patients table, so same-named patients are
      distinguishable at a glance.
      **Accept:** the Patients table shows the unique ID on the left and two same-named rows can be
      told apart by it.
      **Done:** `PatientResponse.patientNumber()` exposes the unique stored `id`; a **Patient ID**
      cell is now the leftmost column of `patients/register.jsp` and the walk-in search table
      (`scheduling/walkin.jsp`).

- [x] **GAP-REC-14** — reception works off the same unreadable patient ids (GAP-PAT-33).
      **Accept:** the register, walk-in search and booking show the readable patient number
      and the desk can search by it.
      **Done:** `PatientDao` search covers `patient_no` (SQL + in-memory); displays flow
      through `patientNumber()`. Verified live (search by number finds the row).

- [x] **GAP-REC-13** — an **"Other (describe…)"** booking has no catalog treatment, so
      `BillingService` throws "nothing to price" and reception **cannot bill the visit at
      all**, even though the dentist did the work.
      **Accept:** reception can issue the bill for a treatment-less visit, priced at the
      amount the dentist recorded when completing it; the receipt names the patient's
      stated reason as the Treatment line.
      **Done:** billing prices treatment-less visits from `appointment.custom_price`
      (still refuses when no price was recorded); receipt/billing-list/day-view/slip
      fall back to the patient's reason. Verified live (8000 priced, 9700 total).

---

## Dentist — `srs-dentist.md`

- [x] **GAP-DEN-09** — make the dentist's **treatment comment visible on the patient dashboard**
      under the finished appointments.
      **Accept:** a completed visit shows the dentist's recorded note to the patient who was treated.
      **Done:** `AppointmentService.forSelfDetail` serves the patient their appointments as
      `AppointmentDetailResponse` (carrying `diagnosis`); `PatientHomeServlet` drives the dashboard
      from it; `patient-home.jsp` renders a **"Dentist's comment"** column filled on
      COMPLETED/BILLED visits. Gated by `ClinicAccess.canViewClinical` so only the treated patient
      sees it. Reception/admin day views (diagnosis-free `AppointmentResponse`) unchanged.
      Deployed + verified live (billed visits show "Scaling done, no decay").

- [x] **GAP-DEN-10** — remove the redundant **Home** tab from the dentist dashboard (the Sunrise
      logo already returns there).
      **Accept:** the dentist dashboard no longer offers Home; the logo is the sole landing control.
      **Done:** `DentistPolicy.navigation()` now returns only the dentist's own tabs (Schedule,
      Availability), matching the reception cleanup (GAP-REC-07). Deployed + verified live (nav
      shows only Schedule / Availability, no Home).

- [x] **GAP-DEN-11** — fix the **"Today's patients" empty-state** so it plainly says nothing is
      booked today (not "Nothing booked with you on <date>"), and either give **"The week ahead"**
      non-redundant content or remove it.
      **Accept:** an empty day says no patients booked today; the week-ahead block either adds
      information or is gone.
      **Done:** the empty-state now says **"No patients booked with you today."** when today is
      selected (naming the date only when a specific other day is picked). **"The week ahead"** is
      **retained** — it is not redundant: it lists the next seven days' bookings (FR-DEN-15), which
      the single-day "Today's patients" view cannot show. Deployed + verified live (today →
      "No patients booked with you today"; other day → names that date).

- [x] **GAP-DEN-12** — "Today's patients" renders every patient as a full details card
      (heading, details table, record-and-complete form), so a full day is a long scroll of
      open forms.
      **Accept:** each patient is one expandable card showing time, name, status and the
      critical-notes flag; the details table and record form render only inside the opened
      card (first one open).
      **Done:** pending rows are `<details class="card pending-list">` (first `open`),
      styled like the done list (`app.css`); summary carries time, patient, status and
      the flag. Tests green, verified live.

- [x] **GAP-DEN-14** — only the patient has a **"My details"** profile section; a dentist has
      nowhere to see or edit their own profile (name, specialisation, phone) as the public
      cards show it.
      **Accept:** the account menu offers the dentist "My details" (`/dentist/profile`),
      read-only by default with an Edit control for name/specialisation/phone; the
      consultation fee stays view-only (administrator-owned).
      **Done:** `DentistProfileServlet` (`/dentist/profile`, `scheduling/dentist-profile.jsp`)
      with the read-only + `${editing}`-gated form and success sub-window;
      `ReferenceService.updateOwnDetails`/`ownProfile` (fee untouched, phone validated);
      header menu entry; `web.xml` mapping; `servlets.md` 41 servlets / 46 routes.
      347 green, verified live.

- [x] **GAP-DEN-16** — the availability screen leads with "Treatments I offer" while the
      time windows sit below, and the windows table is bare counts — a dentist cannot see
      at a glance what is booked and what remains.
      **Accept:** the time windows come first with the treatments list below; each day
      headlines booked/open totals and an exhausted window reads Full.
      **Done:** sections swapped; day headlined "N booked · M still open"; open cells are
      green pills, exhausted ones read Full (`dentist-availability.jsp`). Tests green,
      verified live.

- [x] **GAP-DEN-15** — a dentist never sees the revenue share the administrator configured:
      the dashboard shows the consultation fee but nothing says what fraction of each
      treatment price comes to them.
      **Accept:** the dentist's details screen states the configured share (e.g. "You
      receive 60% of each treatment price, plus your full consultation fee"), reading
      the live setting.
      **Done:** profile prints the live share (`ReferenceService.dentistSharePercent`,
      rate only per §9). Verified live (60→70→60 with no restart).

- [x] **GAP-DEN-13** — completing an **"Other (describe…)"** visit records the diagnosis but
      **no price**: there is no catalog treatment to price it from, so the visit can never
      be billed.
      **Accept:** completing a treatment-less visit requires the dentist to enter the price
      for that specific work; it is stored on the appointment and used at billing.
      **Done:** `appointment.custom_price` (`schema.sql`, migrated live); `complete()`
      requires a positive price exactly when treatment-less (named visits ignore it);
      schedule form shows the price field only there; API accepts `customPrice`.
      Verified live (refused without, completed with 8000).

---

## Footer / public / dashboard — v1.0.1 follow-up batch (GAP-FTB)

- [x] **GAP-FTB-01** — footer shows the shipped **release tag** instead of the internal Maven
      snapshot version.
      **Accept:** the footer names the released version.
      **Done:** `ReleaseInfo.VERSION` is `"1.0.1"`; its javadoc distinguishes the released tag from
      the internal `3.0.0-SNAPSHOT` Maven coordinate. No test references it.

- [x] **GAP-FTB-02** — a patient can **view their receipts** from the dashboard without re-searching.
      **Accept:** each BILLED visit opens its receipt from the patient dashboard.
      **Done:** `PatientHomeServlet` loads bills for BILLED visits into a `receipts` map; the
      **BILLED** pill is a `:target` sub-window showing the receipt (id, visit, charges) with an
      "Open full receipt" link (`ReceiptServlet`, also mapped `/patient/receipt`).

- [x] **GAP-FTB-03** — booking screen wording "Availability this fortnight" was unclear.
      **Accept:** the overview heading plainly says open days in the next two weeks.
      **Done:** `book.jsp` heading is **"Open days in the next two weeks"** with a clearer subtitle.

- [x] **GAP-FTB-04** — a dentist can publish their own **phone number** on the public "Our
      dentists" page.
      **Accept:** a dentist sets a number on their dashboard and it appears on the public page.
      **Done:** `dentist.phone` column + `Dentist.phone`/`DentistResponse`
      fields; `ReferenceService.updateOwnPhone`; edit on the dentist availability screen; shown on
      `access/home.jsp` dentist cards. Deployed + verified live (silva sets `+94 77 123 4567` on his
      availability screen; the home-page dentist card renders it).

- [x] **GAP-FTB-05** — remove the misleading **"Only patients create their own account…"** notice
      from the registration page (administrators and staff can too).
      **Done:** the `.notice info` block was removed from `access/register.jsp`.

- [x] **GAP-FTB-06** — booking offers a **single-treatment radio checklist** and an
      **"Other (describe…)"** free-text reason instead of a bare dropdown.
      **Accept:** a patient picks one listed treatment, or "Other" and states their reason; the
      reason is stored on the appointment.
      **Done:** `appointment.patient_reason` column + threaded field;
      `AppointmentService.book` accepts an optional reason; `BookAppointmentServlet` requires either
      a treatment or a reason; `book.jsp` renders the radios + Other textarea. Deployed + verified
      live (an "Other" booking stores `patient_reason` with `treatment_id` NULL).

- [x] **GAP-FTB-07** — a dentist **enables/disables which treatments they offer**, and the booking
      screen shows a patient only those.
      **Accept:** toggling a treatment hides it from that dentist's booking options (and from the
      booking form); an "Other" booking always remains possible.
      **Done:** `dentist_treatment` junction table + `DentistTreatmentDao`
      / in-memory repository; `ReferenceService.treatmentsFor` / `setTreatmentOffered` /
      `dentistTreatmentToggles`; toggle list on the dentist availability screen; booking treatment
      list filtered by the selected dentist. Deployed + verified live (toggling `t-whitening` off for
      silva removed it from his booking options; toggled back on afterwards).

- [x] **GAP-FTB-08** — the patient's "Concerns you have raised" list was too table-heavy.
      **Done:** compact `<details class="concern-item">` rows (summary = category, dentist, status
      pill, date; body = detail + resolution) in `patient-complaints.jsp`.

- [x] **GAP-FTB-09** — navigation / role-tag / avatar colours made the palette feel off-brand.
      **Done:** `.user-menu__avatar`(lg), `.site-nav a` hover + active, and `.role-tag` now use the
      blue accent in `app.css`.

- [x] **GAP-FTB-10** — there is no single staff front door: an administrator, receptionist or
      dentist each needs their own portal address, and a new staff member has no one place that
      lists the staff logins.
      **Accept:** `/staff` lists the three staff roles (administrator, reception, dentist), each
      linking to its own sign-in screen; the patient door is not listed. Blue-themed to match the
      current palette.
      **Done:** `StaffPortalServlet` (`/staff`) renders `access/staff-portal.jsp`; blue
      `.portal-grid` / `.staff-card` rules in `app.css`; web.xml mapping. No navigation link points
      at it — staff reach it by its own address. Deployed + verified live.

**Note:** GAP-FTB-04/06/07 are coded, compile, and pass 316 tests; they were verified live on the
deployed instance (schema migrated in place on the running MySQL, then each acceptance step checked
over HTTP) and are now marked Fixed (see "How to close a gap").

---

# v1.0.2 UX polish batch (built, verified live, released as v1.0.2)

A review of the public page, sign-up, profile, booking and help flows turned up a batch of
presentation-grade changes. Each entry states its SRS gap ID, the role file it belongs to and
the acceptance criterion. Every box is ticked: each change is built, committed and verified
live, and each SRS row is marked **Fixed**.

## Patient — `srs-patient.md`

- [x] **GAP-PAT-31** — opening Book with no open slots anywhere renders a bare title:
      the patient overview is empty and the walk-in flow names no doctors.
      **Accept:** both booking entries say plainly that no doctor is available today and
      to check later (patient card; walk-in notice when no active doctors).
      **Done:** `book.jsp` no-doctors card; `walkin.jsp` no-doctors notice. Tests green,
      verified live.

- [x] **GAP-PAT-32** — a concern can only name a dentist the patient has seen: a general
      concern about the clinic (no doctor involved, raisable before any visit) is impossible,
      and every concern carries the patient's identity to the administrator.
      **Accept:** a general-concern form needing no dentist and no history; stored and shown
      to the administrator without the patient's identity (anonymous); named concerns unchanged.
      **Done:** `raiseGeneral` stores NULL patient/dentist (`complaint` columns nullable);
      General card on the page (works with no history); admin queue labels Anonymous +
      general concern; `own()` can never return them. 366 green, verified live.

- [x] **GAP-PAT-33** — patient ids are database UUIDs, unreadable over the phone and
      unquotable at the desk.
      **Accept:** every new patient gets a readable number `YYMMDDPATNNNN` (enrol date +
      role + daily sequence, same family as `APT-…`); the Patients register, walk-in
      search and booking show it and it is searchable; existing rows are backfilled.
      **Done:** `patient.patient_no` + `PersonNumberGenerator` (DB row-lock sequence,
      UUID PKs untouched); minted on self-reg and walk-in; `patientNumber()` falls back
      to the id for unnumbered seeds; register/search show and find it. 375 green,
      verified live.

- [x] **GAP-PAT-22** — remove the **"Book this service &rarr;"** link that appears under each
      service on the public home page.
      **Accept:** a service description is shown with no "Book" call-to-action inviting a
      sign-up from the middle of the page.
      **Done:** the `.service-item__cta` link was removed from `access/home.jsp`; services
      show name + description only. 330 tests green.

- [x] **GAP-PAT-23** — the public **"Our dentists"** cards are `<details>` rows that only show
      their body (fee, phone, rating, register button) when opened; the user wants the whole card
      visible without expanding.
      **Accept:** every dentist card on the public home is fully displayed without a
      `<details>/<summary>` toggle.
      **Done:** dentist cards are plain `<div class="dentist-card">` blocks (`access/home.jsp`);
      the `summary` chevron/expand CSS was replaced with static `.dentist-card__summary` rules
      in `app.css`. 330 tests green.

- [x] **GAP-PAT-24** — the **Help** link is missing from the patient dashboard navigation bar
      (each role's signed-in nav currently omits Help entirely).
      **Accept:** a signed-in patient sees a **Help** entry in the dashboard nav (and, per
      GAP-FTB-12, each role gets its own help page from that link).
      **Done:** `PatientPolicy.ownNavigation()` gains `Help → /help/patient`; the other three
      policies gain their own Help entries too (GAP-FTB-12). 330 tests green.

- [x] **GAP-PAT-25** — the **"Raise a concern"** page opens with a large explanatory block
      (`<h2>What happened</h2>` plus the **"Who reads this: the clinic's administrator…"** notice
      and the       **"Choose the dentist your concern is about. The form opens when you do."** line).
      **Accept:** that intro block is removed so the page gets straight to the dentist list and
      form.
      **Done:** the `<h2>What happened</h2>` + notice + preamble are removed from
      `patient-complaints.jsp`; the card now opens as "Your visits" straight into the
      per-dentist list. The page subtitle still names the administrator as the reader
      (FR-CMP-12). 330 tests green.

- [x] **GAP-PAT-26** — the self-registration card is tall and narrow, forcing scrolling; the user
      wants a **wider card** with **two inputs per line** to shorten the form. Also remove the
      **"Nothing medical is asked here. Allergies and medications are added from your profile once
      you have signed in."** notice and the phone helper text **"So the clinic can reach you about
      an appointment (e.g. 077 123 4567)."**
      **Accept:** the create-account form fits without vertical scrolling, is wider than the
      current `.narrow` card, lays fields two-per-line where possible, and carries neither the
      "Nothing medical…" notice nor the phone helper sentence.
      **Done:** `access/register.jsp` uses `.narrow.register-wide` (720px, `app.css`) with
      name+email, password+confirm and contact+dob in `.form-row` pairs; both sentences are
      deleted; a "Need help signing up?" link points at `/help`. 330 tests green.

- [x] **GAP-PAT-27** — the patient has **no place to record their own diagnosis / dental-history
      details** even though the role is designed to do so; the profile screen has no such field.
      **Accept:** a diagnosis / history details field exists in the patient profile and can be
      edited and saved there.
      **Done:** `patient.diagnosis_details TEXT NULL` (`schema.sql`; live DB migrated in place);
      threaded through `Patient`/`PatientResponse`/`PatientDao`/`PatientService.ProfileUpdate`
      (5th component) and `PatientProfileServlet`; `profile.jsp` shows it read-only and edits it
      via textarea. 330 tests green.

- [x] **GAP-PAT-28** — on the booking page, once a dentist and time are chosen the submit button
      reads **"Confirm booking"** with no reference to who or what; and the page still carries a
      **"2. Dentist and date"** picker card below the times (redundant on a page the patient is
      already on). The user wants the button to read **"Proceed appointment &lt;dentist name&gt;"**,
      the redundant **"2. Dentist and date"** card **removed**, and the **time slots shown before
      the treatment picker** under a heading that names the doctor's available times.
      **Accept:** the submit button names the chosen dentist; the "2. Dentist and date" card is
      gone; the times for the selected dentist are shown before the treatment list.
      **Done:** `book.jsp` renders one card headed "Available times with … on …" (slots first,
      treatment picker below), the submit reads "Proceed appointment with …", the picker card is
      replaced by a "Choose a different dentist or date" link; dentist-details dialog kept.
      330 tests green.

- [x] **GAP-PAT-29** — each treatment type has **no info affordance** to inspect its details,
      even though the administrator authors a description for it.
      **Accept:** each treatment in the booking list has an info icon/link that opens the
      administrator-written description in a sub-window.
      **Done:** each treatment choice carries an `i` mark (`.treatment-info`, `app.css`) opening
      a `:target` sub-window with the full description + price (`book.jsp`). 330 tests green.

## Shared / all roles — `srs-patient.md` (+ role files noted below)

- [x] **GAP-FTB-11** — the footer labels the running build as **"Release v1.0.1"**.
      **Accept:** the footer reads **"Version v1.0.1"** (label changed from "Release" to
      "Version"). Public footer (`footer.jspf`).
      **Done:** `footer.jspf` now renders "Version v…". 330 tests green.

- [x] **GAP-FTB-12** — there is one shared help page (`/help`). Each role should instead have a
      **help page specific to how it uses the system**, referencing the project SRS/docs; each
      role's sign-in page (and the sign-up page) should link to its own help; and the **help link on
      the       `/staff` portal should be removed** since the staff scenarios differ by role.
      **Accept:** patient, reception, dentist and admin each get a role-specific help page rooted
      in the repo's SRS documentation; the patient login and register pages link to the patient
      help; the `/staff` portal no longer offers the shared help link. Affects `srs-patient.md`,
      `srs-reception.md`, `srs-dentist.md`, `srs-admin.md`.
      **Done:** `HelpServlet` dispatches `/help/<role>` to `shared/help-<role>.jsp` (new; each
      grounded in its role SRS); `web.xml` maps `/help/*` (public via the `/help` filter prefix,
      so sign-in screens can link their own help); each policy nav ends in its own Help; staff
      logins link their own help, register links `/help`, `/staff` help link removed;
      reception register/walk-in link `/help/reception`. `servlets.md` mappings 43→44, routes
      44→45. 330 tests green.

- [x] **GAP-FTB-13** — the public help page's contents card is headed **"Jump to a topic"**.
      **Accept:** that label is replaced with something simpler/appropriate for the page.
      **Done:** `shared/help.jsp` (and the four role pages) head the card **"Help topics"**.
      330 tests green.

- [x] **GAP-FTB-14** — profile pages show every editable field as an in-page form when editing;
      the user wants a **read-only view by default** with an **Edit** button that reveals **only the
      fields that role may change**, and every successful update anywhere in the system (profile,
      availability, settings, etc.) should show a **confirmation sub-window ("successfully
      updated")** rather than only a top-of-page notice.
      **Accept:** each role's profile shows read-only values with an Edit control that makes the
      editable subset interactive; after any update the user gets a success sub-window. Affects
      `srs-patient.md`, `srs-reception.md`, `srs-dentist.md`, `srs-admin.md`.
      **Done:** new always-visible `.sub-window.open` style (`app.css`, with a success tick);
      **every** update point confirms in it — patient profile/book/cancel/rate/notes,
      dentist phone + treatment toggles (`?toggled=1`) + recorded treatments, reception
      day-view cancels + walk-in register (`?registered=1`) + published availability,
      admin clinic identity + treatment catalogue (`?saved=1`) + resolved concerns +
      issued accounts. Warnings and errors stay inline. 332 tests green.

---

## Admin — `srs-admin.md`

- [x] **GAP-ADM-14** — staff login pages wear the full public navigation (Services, Dentists,
      Help, Sign in, Register) although staff never use it, and signing out drops every role
      on the public home page instead of the staff doors.
      **Accept:** staff logins and `/staff` show the bare branded bar (theme + role label,
      no public links); signing out lands staff on `/staff` and patients on `/`.
      **Done:** `hidePublicNav` from role (patient keeps bar) + `/staff`; header gates on
      `not` (never `empty` on the Boolean); logout reads the role before clearing.
      383 green, verified live.

- [x] **GAP-ADM-13** — the Accounts table has no role filter, and the appointments tables
      (reception day view, billing day list, admin complaints) sort one fixed way with no
      control.
      **Accept:** Accounts filters by role; the day tables sort by time asc/desc (and by
      dentist where useful); complaints sort newest/oldest; all server-side links, no script.
      **Done:** `roleFilter` on Accounts; `sort` on day (`time_asc/desc`, `dentist`,
      kept across cancel), billing and complaints (`newest/oldest`); header links and
      filter form. 382 green, verified live.

- [x] **GAP-ADM-12** — staff sign in with email addresses; the desk wants username logins.
      Only staff change — patients keep email sign-in.
      **Accept:** reception/dentist/admin portals ask for a username (email still works as
      fallback); the administrator assigns the username when creating the account; existing
      staff rows are backfilled; the Accounts table shows usernames.
      **Done:** `user_account.username` (unique, NULL for patients); `AuthService` looks up
      username-then-email with per-screen messages; portals derive label/field from role
      (subclasses untouched); admin form validates (3–32, unique) and lists it; seeds,
      local + prod backfilled. 381 green, verified live (username, fallback, validation).

- [x] **GAP-ADM-11** — staff accounts and new dentists carry database UUIDs (or name slugs):
      the administrator's people screens show no quotable identifier.
      **Accept:** every new account gets a readable number `YYMMDD + ROLE + NNNN`
      (PAT/REC/DEN/ADM) and every new dentist id follows the same family; the Accounts
      table lists the number leftmost; existing rows are backfilled.
      **Done:** `user_account.account_no` minted in `UserAccountFactory` per role;
      dentist ids minted `YYMMDDDENNNNN`; Accounts lists Account ID leftmost
      (`AccountRow.accountNumber`). 375 green, verified live (create → DEN numbers).

- [x] **GAP-ADM-10** — the revenue dials live in `clinic.properties`/env and need a rebuild +
      restart to change: the administrator cannot configure the dentist's treatment-share
      percentage (or the service charge) from the dashboard, and the Reports screen still
      shows a "Reception handling" metric for a commission that is always zero.
      **Accept:** a **Pricing** tab (`/admin/pricing`) edits the dentist share % and the
      service charge with validation, applying to the next bill without restart; the
      reception metric is gone from Reports (dashboard and CSV); historical bills keep
      the split they were issued with.
      **Done:** `PricingServlet` + `pricing.jsp` + nav (42 servlets/47 routes); keys in
      `clinic_setting` via `ClinicIdentityService` (percent↔fraction, validated);
      strategy + charge read per bill (supplier wiring); reception stat/table/CSV gone.
      Verified live (70% bill split 4650/1550/0, then restored). 355 tests green.

---

## How to close a gap

1. Fix on `dev`; run the module build + tests (and the affected view test) green.
2. Flip the SRS row's status to **Fixed** and update the "Resolution" column to say what was done.
3. Tick the box here.