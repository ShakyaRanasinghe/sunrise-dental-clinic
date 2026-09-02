# Gap Checklist — v1.0.1 follow-up

Working checklist for the gaps found after v1.0.1 shipped. One box per gap. Tick a box when
the fix is **committed and green**, not when it compiles. Each entry names its SRS gap ID, the
role file it is documented in, and the acceptance criterion.

Status definitions used in the SRS tables: **Open** · **Fixed** — awaiting verification ·
**Fixed**.

---

## Patient — `srs-patient.md`

- [ ] **GAP-PAT-20** — make the contact number **mandatory** on self-registration
      (HTML5 `required` + server-side `PhoneNumbers` gate). *Because:* a patient who registers
      without a number can never be matched to their record at the desk.
      **Accept:** a blank phone is refused with a field reason; no account is created without it.

- [ ] **GAP-PAT-21** — add a **confirmation step** before a cancellation commits (no-script, so a
      styled dialog / yes-no step that needs no JavaScript). Relates to open GAP-PAT-04.
      **Accept:** a single stray click cannot drop a confirmed appointment; cancelling requires an
      explicit second confirmation.

---

## Reception — `srs-reception.md`

- [ ] **GAP-REC-11** — add a **contact-number field** to the walk-in register form, validated
      through the shared `PhoneNumbers` rule, consistent with the mandatory phone (GAP-PAT-20).
      **Accept:** a walk-in patient is stored with their phone and the number is validated on save.

- [ ] **GAP-REC-12** — give every patient a **unique identifier** in the database and surface it as
      the **leftmost column** of the reception Patients table, so same-named patients are
      distinguishable at a glance.
      **Accept:** the Patients table shows the unique ID on the left and two same-named rows can be
      told apart by it.

---

## Dentist — `srs-dentist.md`

- [ ] **GAP-DEN-09** — make the dentist's **treatment comment visible on the patient dashboard**
      under the finished appointments.
      **Accept:** a completed visit shows the dentist's recorded note to the patient who was treated.

- [ ] **GAP-DEN-10** — remove the redundant **Home** tab from the dentist dashboard (the Sunrise
      logo already returns there).
      **Accept:** the dentist dashboard no longer offers Home; the logo is the sole landing control.

- [ ] **GAP-DEN-11** — fix the **"Today's patients" empty-state** so it plainly says nothing is
      booked today (not "Nothing booked with you on <date>"), and either give **"The week ahead"**
      non-redundant content or remove it.
      **Accept:** an empty day says no patients booked today; the week-ahead block either adds
      information or is gone.

---

## How to close a gap

1. Fix on `dev`; run the module build + tests (and the affected view test) green.
2. Flip the SRS row's status to **Fixed** and update the "Resolution" column to say what was done.
3. Tick the box here.