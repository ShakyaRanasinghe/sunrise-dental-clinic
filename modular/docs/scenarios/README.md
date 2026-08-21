# Real-World User Journey Scenarios

This directory documents how each role experiences the Sunrise Dental Clinic system in
production — not as a list of features, but as the actual sequence of events a real person
goes through from first contact to task complete.

Each scenario is written from the perspective of the person living it. Where the system
has a gap — something the real world requires that is not yet built — it is called out
explicitly so the gap is visible in context, not buried in a specification table.

---

## Scenarios by role

| File | Role | Scenario |
|---|---|---|
| [patient-online-journey.md](patient-online-journey.md) | Patient | Tech-aware patient who discovers the clinic online, registers, books and attends |
| patient-walkin-journey.md | Patient | Patient who walks directly into the physical clinic — *(coming next)* |
| reception-journey.md | Receptionist | Front-desk day: walk-ins, availability, billing — *(coming next)* |
| dentist-journey.md | Dentist | Clinical day: schedule, treating, recording — *(coming next)* |
| admin-journey.md | Administrator | Management view: reports, accounts, complaints — *(coming next)* |

---

## How to read these

Each scenario follows a single person through their complete journey in plain language.
System behaviour is described as the person experiences it, not as the code implements it.

Gaps are marked **[GAP]** inline — these are places where the real-world journey requires
something the system does not yet provide.
