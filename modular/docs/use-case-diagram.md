# Use Case Design

Use case diagrams for the **modular** implementation of the Sunrise Dental Clinic system.

Derived from [`srs/srs.md`](srs/srs.md) and the four role documents, and consistent with
[`class-diagram.md`](class-diagram.md) and [`er-diagram.md`](er-diagram.md).

| | |
|---|---|
| Notation | UML 2.5 use case diagrams |
| Source | PlantUML, in [`use-case/`](use-case/) — text, so a change is a reviewable diff |
| Rendering | `java -jar plantuml.jar -tpng use-case/*.puml` |
| Actors | 4 primary, 2 secondary |
| Use cases | 70 distinct, 87 declarations across 9 diagrams — 12 shared behaviours are drawn in more than one frame |
| Relationships | 25 `<<include>>`, 12 `<<extend>>`, 4 actor generalisations |

---

## 1. The diagrams

| # | Diagram | Covers |
|---|---|---|
| 0 | [`0-system-context.puml`](use-case/0-system-context.puml) | Every actor, the four roles generalised, and the top-level use cases |
| 1a | [`1a-patient-access.puml`](use-case/1a-patient-access.puml) | Registration, sign-in, lock-out |
| 1b | [`1b-patient-booking.puml`](use-case/1b-patient-booking.puml) | Availability, booking, cancelling, bills |
| 1c | [`1c-patient-feedback.puml`](use-case/1c-patient-feedback.puml) | Medical notes, concerns, ratings |
| 2a | [`2a-reception-register.puml`](use-case/2a-reception-register.puml) | The register and publishing availability |
| 2b | [`2b-reception-booking.puml`](use-case/2b-reception-booking.puml) | Booking on behalf, and billing |
| 3 | [`3-dentist.puml`](use-case/3-dentist.puml) | Schedule, medical notes, treatment |
| 4a | [`4a-admin-manage.puml`](use-case/4a-admin-manage.puml) | Reports, accounts and pricing |
| 4b | [`4b-admin-governance.puml`](use-case/4b-admin-governance.puml) | Concerns and reviews |

**Why nine diagrams and not one.** A single frame holding every use case was drawn first and was
unreadable — association lines crossed the system boundary and one use case was clipped by the
render bounds. Splitting by actor, then by capability where an actor has many, keeps every frame
between six and fifteen use cases. Diagram 0 exists to carry what splitting loses: the whole actor
set in one place, and the fact that all four roles share sign-in, sign-out and help.

**The cost of splitting: twelve use cases are drawn more than once.** *Sign In* and *Authenticate
Credentials* appear in each role's access diagram; *Reserve Slot*, *Validate Booking* and *Send
Confirmation* appear in both booking diagrams, because booking is a shared behaviour with two
initiators. They are the same use case, not copies — the identifier is the same in each file — but
a reader counting ellipses will reach 87 where the system has 70. Recorded because a silent
discrepancy between a diagram and a count is exactly what makes a reader distrust both.

---

## 2. Actors

| Actor | Kind | Description |
|---|---|---|
| **Clinic User** | Primary, abstract | Generalisation of the four roles. Exists only so the three universal use cases are drawn once |
| **Patient** | Primary | A member of the public. The only role that creates its own account |
| **Receptionist** | Primary | Front desk. Highest interaction volume of any actor |
| **Dentist** | Primary | The treating clinician. The only actor who reads clinical data |
| **Administrator** | Primary | Clinic management. Reads money and governance, never clinical records |
| **SMTP Server** | Secondary, `<<system>>` | Receives appointment confirmations |
| **SMS Gateway** | Secondary, `<<system>>` | Receives appointment reminders |

**Actor generalisation.** `Patient`, `Receptionist`, `Dentist` and `Administrator` each
specialise `Clinic User`, which owns *Sign In*, *Sign Out* and *View Help*. Without the
generalisation those three use cases need twelve association lines instead of three, and a reader
cannot tell at a glance that they are the same behaviour rather than four similar ones.

**The two secondary actors have arrows pointing away from the system**, because the system
initiates contact with them. A secondary actor drawn with the arrow reversed is the commonest
error in use case diagrams, and it inverts the meaning: it would say the mail server asks the
clinic to send an email.

**Deliberately not actors:** the browser (a channel, not a participant), MySQL (part of the
system, not outside it), and "the clinic" (an organisation, not a role that operates the system).

---

## 3. `<<include>>` and `<<extend>>`

The rubric asks for these to be used *accurately*, so the rule and every application of it are
stated rather than left to inspection.

### The rule

| | `<<include>>` | `<<extend>>` |
|---|---|---|
| Meaning | The base use case **always** performs the included behaviour | The extending use case **conditionally** adds to the base |
| Arrow direction | base ──▷ included | extension ──▷ base |
| Remove the target and | the base is broken | the base still works |
| Reads as | "Booking *always* reserves a slot" | "A discount *may* be applied to a bill" |

Both are dashed dependencies with the stereotype on the line. The direction differs, and getting
it backwards is the error markers look for: `<<include>>` points at the *sub*-behaviour,
`<<extend>>` points at the *base*.

### Every `<<include>>`, and why

| Base | Includes | Why it is always, not sometimes |
|---|---|---|
| Register Account | Validate Registration | An account is never created without validation |
| Sign In | Authenticate Credentials | Signing in *is* authenticating |
| Book Appointment | Validate Booking | Every booking is validated before anything is written |
| Book Appointment | Reserve Slot | The row lock is the booking. Remove it and double-booking returns |
| Book Appointment | Send Confirmation | Always attempted (FR-NOT-01). Delivery may fail; the attempt does not |
| Cancel Appointment | Release Slot | A cancellation that leaves the slot taken is not a cancellation |
| Register Walk-In | Validate Patient Details | Same reasoning as registration |
| Register Walk-In | Warn On Duplicate Contact Number | The check runs every time (FR-REC-25); only the warning is conditional |
| Publish Availability | Generate Slots | Publishing that produces no bookable slots achieves nothing |
| Publish Availability | Reject Overlapping Session | Validation, so it runs on every publish |
| Issue Bill | Calculate Total | A bill without a total is not a bill |
| Issue Bill | Divide Revenue | The three-way split is written on every bill (FR-BIL-03) |
| Open Appointment | Read Patient Medical Notes | Notes are shown with the appointment, not fetched on request (FR-NOTE-07) |
| Complete Treatment | Write Audit Event | Every mutation is audited (FR-AUD-01) |
| Review Concern | Read Complaint | Reviewing is reading |
| Review Concern | Write Audit Event | Reads of a complaint are audited too (FR-CMP-11) |
| Resolve Concern | Record Resolution | Closing requires a written resolution (FR-ADM-53) |
| View Clinic Reports | Aggregate Bills | Every report is an aggregate over bills |
| Create Staff Account | Validate Account Details | Same reasoning as registration |

### Every `<<extend>>`, and why

| Extension | Extends | The condition that makes it optional |
|---|---|---|
| Lock Account After Five Failures | Sign In | Only on the fifth consecutive failure (NFR-SEC-03) |
| Send Reminder | Book Appointment | Time-triggered before the appointment, not at booking (FR-NOT-02) |
| Mark Note Critical | Declare Medical Notes | The patient may or may not flag a note (FR-NOTE-04) |
| Attach Appointment | Raise Concern | A concern may name a visit, or the care generally (FR-CMP-03) |
| Add Comment | Rate Visit | A star alone is a complete submission (FR-RVW-02) |
| Apply Discount | Issue Bill | Most bills carry no discount (FR-REC-56) |
| Print Receipt | Issue Bill | A bill can be issued and printed later, or not at all |
| Warn Of Critical Medical Notes | View My Schedule | Only where a patient on that day has flagged something |
| Record Diagnosis | Complete Treatment | Some visits produce no note, and completion must not be blocked (FR-DEN-34) |
| Export CSV | View Clinic Reports | Reading a report does not require exporting it |
| Create Dentist Profile | Create Staff Account | Only when the role chosen is `DENTIST` (FR-ADM-21) |
| Dismiss Concern | Review Concern | One of two outcomes, and the less common one |

### Two that were nearly wrong

Worth recording, because both are the mistake the rubric is testing for.

**Send Confirmation** was first drawn as `Send Confirmation ..> Book Appointment : <<extend>>`,
copying the earlier diagram in `UML/`. That is wrong: a confirmation is sent on **every**
booking, so it is behaviour the base always performs — `<<include>>`, pointing the other way.
*Send Reminder* is the genuine extension, because it fires on a timer rather than as part of
booking.

**Record Diagnosis** looks like an `<<include>>` of *Complete Treatment* and is not. FR-DEN-34
requires completion to be possible with no diagnosis, so the behaviour is conditional. Drawing it
as an include would specify a system that refuses to close an appointment where the dentist found
nothing to write.

---

## 4. Use cases traced to the brief

The brief names six functions. All six appear, and the extensions are marked as such.

| Brief requirement | Use case | Diagram |
|---|---|---|
| 1. User authentication (login) | Sign In, Authenticate Credentials, Lock Account After Five Failures | 1a |
| 2. Register new appointment | Book Appointment, Book On Behalf Of Patient, Register Walk-In | 1b, 2a, 2b |
| 3. Display appointment details | View My Appointments, View Day Schedule, Open Appointment | 1b, 2b, 3 |
| 4. Calculate and print bill | Issue Bill, Calculate Total, Print Receipt | 2b |
| 5. Help section | View Help | 0 |
| 6. Exit system | Sign Out | 0 |

Beyond the brief, as it permits: publishing availability, decision-support reports, account
management, medical notes, complaints, and reviews.

---

## 5. Assumptions

The marking criteria ask for diagrams "supported by relevant assumptions". These are the ones the
diagrams encode; where an assumption changed, the diagram would change.

| # | Assumption | What it changes if false |
|---|---|---|
| 1 | A person holds one role at a time | Actor generalisation would need multiple inheritance, and *Sign In* could not resolve one home screen |
| 2 | Only patients self-register | *Register Account* would be an administrator use case with a role parameter, not a patient one |
| 3 | Reception publishes availability, dentists do not | *Publish Availability* would move to the Dentist actor (ASM-06) |
| 4 | A walk-in can be booked without an account | *Register Walk-In* must be separate from *Register Account*, which it is |
| 5 | Notification delivery is best-effort | *Send Confirmation* would become a blocking step and booking could fail on a mail outage |
| 6 | The patient authors medical notes; the dentist authors the diagnosis | *Read Patient Medical Notes* is the dentist's only relationship to them — no create, edit or delete |
| 7 | A complaint is never visible to the dentist named | There is no Dentist association to any complaint use case, and its absence is the requirement |
| 8 | A dentist sees an aggregate rating, never a review | *View My Rating* and *Read Dentist Reviews* are separate use cases with different actors |

---

## 6. Evaluation

### What the design gets right

**Boundaries follow purpose rather than rank.** Three confidentiality rules point in three
directions, and no actor is simply "more privileged": the dentist reads medical notes the
administrator cannot; the administrator reads complaints the dentist cannot; the dentist sees a
rating aggregate while the administrator reads the reviews behind it. In the diagrams this is
visible as *absent* associations — there is no line from Dentist to any complaint use case, and
that absence is FR-CMP-08 rather than an oversight.

**The include/extend split matches the requirement statuses.** Every `<<include>>` corresponds to
behaviour a requirement calls mandatory; every `<<extend>>` to one a requirement calls optional or
conditional. That is checkable: §3's tables cite the requirement for each.

**Actor generalisation removes nine association lines** and, more usefully, states something
true — that sign-in is one behaviour with four entry points, which is exactly what
`AbstractLoginServlet` implements in the class design.

### What is weaker

**Diagram 0 duplicates the detail diagrams.** *Manage Appointments* in the context diagram is
*Book Appointment*, *Cancel Appointment* and *View My Appointments* in 1b. Two levels of
abstraction mean two places to update, and they can drift. The alternative — one diagram — was
tried and was unreadable, so this is a trade rather than a fix.

**"Manage X" use cases are not really use cases.** *Manage Patient Register* names a screen, not a
goal a person has. They appear only in the context diagram, where the purpose is orientation, and
the detail diagrams use goal-shaped names throughout. A stricter reading would drop them.

**Nine of the 45 use cases correspond to nothing built yet.** *Declare Medical Notes*, *Raise
Concern*, *Rate Visit*, *Create Staff Account* and the rest are `Specified` in the SRS. The
diagram describes the target, not the current system, and a reader who assumes otherwise would be
misled — hence this paragraph.

**No error or alternate flows are shown.** *Reserve Slot* failing under a concurrent booking is
the most important behaviour in the system, and a use case diagram cannot express it. That belongs
in a sequence diagram, which is the gap named in §7.

### Validity of the result

Four checks were applied rather than assumed, by script over the PlantUML sources:

1. **Every use case is either associated with an actor or is the target of an `<<include>>` or
   `<<extend>>`.** An untouched use case means a missing actor, or a use case nobody wants.
2. **Every actor has at least one association.** No decorative actors.
3. **Every use case maps to a requirement id**, and every functional requirement group maps to at
   least one use case. Where a requirement had no use case, one was missing; where a use case had
   no requirement, it was invented and was removed.
4. **Every `<<include>>`/`<<extend>>` was checked against the requirement's own wording** — is the
   behaviour described as always, or as conditional? §3 cites the requirement per row so the check
   is repeatable rather than a claim.

Check 3 removed two use cases from an earlier draft — *Search Appointments* and *View
Notifications* — neither of which any requirement asks for.

Check 1 surfaced one legitimate exception. **`Notify Patient` in the context diagram has no
primary actor at all**, only associations to the two `<<system>>` actors. That is correct rather
than a defect: nobody asks the system to notify a patient, it happens as a consequence of booking.
It is the one use case in the set that is system-initiated, and a diagram where every use case had
a human initiator would have hidden that.

---

## 7. How the three diagram sets support each other

The marking criteria ask for "critical analysis from different perspectives, covering how use
case, class and sequence diagrams support the design". Each answers a different question about the
same system, and they are consistent by construction:

| Diagram | Answers | Example |
|---|---|---|
| **Use case** | *Who* wants *what* | Patient wants *Book Appointment* |
| **Class** | *What* the system is made of | `AppointmentService.book(...)`, `Slot.bookFor(...)` |
| **ER** | *Where* the result is kept | `appointment` row, `slot.status` moved to `BOOKED` |
| **Sequence** | *In what order*, and what happens when it fails | the `SELECT … FOR UPDATE` before the insert |

Traced through one use case:

| Layer | *Book Appointment* becomes |
|---|---|
| Use case (1b) | Patient → Book Appointment, including Validate Booking, Reserve Slot, Send Confirmation |
| Class (§4) | `BookAppointmentServlet` → `AppointmentService.book()` → `AppointmentRepository`, publishing an `AppointmentEvent` a `NotificationObserver` consumes |
| ER (§2) | insert `appointment`, update `slot.status`, increment `appointment_counter`, insert `notification` |
| Requirement | FR-APT-01…05, FR-PAT-10…16 |

The three `<<include>>` relationships are the same three things the service does in one
transaction, and the same three tables the ER design writes. That correspondence is the
consistency check worth stating: an `<<include>>` with no corresponding method call, or a method
call with no `<<include>>`, means one of the two diagrams is wrong.

**The gap.** There are no sequence diagrams for `modular/` yet. The brief asks for approximately
three, and the natural three are *Sign In*, *Book Appointment* and *Issue Bill* — the same three
the class design already shows the collaborators for. Until they exist, the "in what order" row
above is unanswered, and the concurrency guard that makes FR-APT-04 true is asserted in prose
rather than drawn.

---

## 8. Regenerating

PlantUML source, so a change is a text diff. Rendering needs the jar; no Graphviz, because
`!pragma layout smetana` uses PlantUML's own layout engine.

```bash
cd modular/docs/use-case
java -jar plantuml.jar -tpng *.puml      # PNGs land beside the sources
```

Each file opens with an invisible `title` that reserves a top margin — without it, an actor placed
at the upper edge is clipped by the render bounds, which is what happened on the first attempt.
