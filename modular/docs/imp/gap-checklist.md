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

- [ ] **GAP-FTB-04** — a dentist can publish their own **phone number** on the public "Our
      dentists" page.
      **Accept:** a dentist sets a number on their dashboard and it appears on the public page.
      **Done (awaiting live deployment):** `dentist.phone` column + `Dentist.phone`/`DentistResponse`
      fields; `ReferenceService.updateOwnPhone`; edit on the dentist availability screen; shown on
      `access/home.jsp` dentist cards.

- [x] **GAP-FTB-05** — remove the misleading **"Only patients create their own account…"** notice
      from the registration page (administrators and staff can too).
      **Done:** the `.notice info` block was removed from `access/register.jsp`.

- [ ] **GAP-FTB-06** — booking offers a **single-treatment radio checklist** and an
      **"Other (describe…)"** free-text reason instead of a bare dropdown.
      **Accept:** a patient picks one listed treatment, or "Other" and states their reason; the
      reason is stored on the appointment.
      **Done (awaiting live deployment):** `appointment.patient_reason` column + threaded field;
      `AppointmentService.book` accepts an optional reason; `BookAppointmentServlet` requires either
      a treatment or a reason; `book.jsp` renders the radios + Other textarea.

- [ ] **GAP-FTB-07** — a dentist **enables/disables which treatments they offer**, and the booking
      screen shows a patient only those.
      **Accept:** toggling a treatment hides it from that dentist's booking options (and from the
      booking form); an "Other" booking always remains possible.
      **Done (awaiting live deployment):** `dentist_treatment` junction table + `DentistTreatmentDao`
      / in-memory repository; `ReferenceService.treatmentsFor` / `setTreatmentOffered` /
      `dentistTreatmentToggles`; toggle list on the dentist availability screen; booking treatment
      list filtered by the selected dentist.

- [x] **GAP-FTB-08** — the patient's "Concerns you have raised" list was too table-heavy.
      **Done:** compact `<details class="concern-item">` rows (summary = category, dentist, status
      pill, date; body = detail + resolution) in `patient-complaints.jsp`.

- [x] **GAP-FTB-09** — navigation / role-tag / avatar colours made the palette feel off-brand.
      **Done:** `.user-menu__avatar`(lg), `.site-nav a` hover + active, and `.role-tag` now use the
      blue accent in `app.css`.

**Note:** GAP-FTB-04/06/07 are coded, compile, and pass 316 tests; they await schema re-seed and
live deployment verification (see "How to close a gap" — the box flips to [x] when deployed + green).

---

## How to close a gap

1. Fix on `dev`; run the module build + tests (and the affected view test) green.
2. Flip the SRS row's status to **Fixed** and update the "Resolution" column to say what was done.
3. Tick the box here.