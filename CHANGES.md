# Changelog

Tracks additions and fixes made during development and QA. Updated on every push.

---

## [develop] — 2026-08-21 (push 3)

### Fixed

- **Availability overlap error now stays on the page** (`scheduling/web/AvailabilityPageServlet.java`, `scheduling/availability.jsp`)
  - Overlap conflict sent receptionist to a dead-end generic error page with no context
  - Error is now shown inline on the availability page, with existing published slots visible below so receptionist can see what's already there and act on it

---

## [develop] — 2026-08-21 (push 2)

### Added

- **Treatment catalogue management** — closes issue #22
  - New admin page at `/admin/treatments` — list, add, edit price/description, activate/deactivate
  - Deactivated treatments are hidden from patient booking immediately, history preserved
  - `TreatmentAdminService` (`modular/src/main/java/.../reporting/service/TreatmentAdminService.java`)
  - `TreatmentAdminServlet` (`modular/src/main/java/.../reporting/web/TreatmentAdminServlet.java`)
  - `treatments.jsp` (`modular/src/main/webapp/WEB-INF/jsp/reporting/treatments.jsp`)
  - New `MANAGE_TREATMENTS` action added to `Action.java` and `AdminPolicy`
  - **Treatments** nav link added to admin navigation
  - Servlet registered in `web.xml` at `/admin/treatments`

---

## [develop] — 2026-08-21

### Fixed

- **Booking confirmation step** (`modular/src/main/webapp/WEB-INF/jsp/appointments/book.jsp`)
  - Time slot buttons were individual submit buttons — clicking a time booked immediately with no confirmation, causing accidental bookings.
  - Changed time slots to radio buttons (select a time) with a single **"Confirm booking"** submit button.
  - CSS already had `.slot input` / `.slot label` styles for this pattern — no CSS changes needed.

### Added

- **Treatment cost on dentist's schedule** (`modular/src/main/webapp/WEB-INF/jsp/appointments/dentist-schedule.jsp`)
  - Dentist schedule showed treatment name only — doctor had no visibility of what the patient is being charged.
  - Treatment name now displays with its base cost: `Composite filling — Rs 4,500.00`.
  - Changes: `AppointmentService.DentistDay` record extended with `treatmentCost`; `forDentistWithWarnings` populates it via `ReferenceService`.

---

## How to update this file

After every push, add a new entry at the top under the branch name and date:

```
## [branch-name] — YYYY-MM-DD

### Fixed
- description (file path)

### Added
- description (file path)

### Changed
- description (file path)
```
