# Sequence Diagrams

Three sequence diagrams for the **modular** implementation, covering the use cases the brief and
the marking criteria care most about.

Consistent with [`use-case-diagram.md`](use-case-diagram.md),
[`class-diagram.md`](class-diagram.md) and [`er-diagram.md`](er-diagram.md).

| | |
|---|---|
| Notation | UML 2.5 sequence diagrams |
| Source | PlantUML, in [`sequence/`](sequence/) |
| Rendering | `java -jar plantuml.jar -tpng sequence/*.puml` |

| # | Diagram | Use case | Why this one |
|---|---|---|---|
| 1 | [`1-seq-sign-in.puml`](sequence/1-seq-sign-in.puml) | Sign In | The brief's requirement 1, and the only place the four-portal design is visible in behaviour |
| 2 | [`2-seq-book-appointment.puml`](sequence/2-seq-book-appointment.puml) | Book Appointment | The brief's requirement 2, and the only place the double-booking guard can be shown |
| 3 | [`3-seq-issue-bill.puml`](sequence/3-seq-issue-bill.puml) | Issue Bill | The brief's requirement 4, and where the pricing strategy earns its place |

---

## The tiers are drawn as boxes

Every diagram groups its participants into **Presentation**, **Business** and **Data**. That is
not decoration: it makes the three-tier claim checkable rather than asserted. Read any of the
three diagrams and two rules hold without exception:

- **No arrow crosses from Presentation directly to Data.** A servlet's messages all land on a
  service. That is **NFR-MNT-02** drawn — and the direct contrast with `layered/`, where 12 of 20
  servlets call a repository.
- **No arrow leaves the Business tier for the actor.** Services return values; only the
  presentation tier speaks HTTP.

A tier violation in a future change would be visible as a line crossing a box boundary it should
not, which is easier to spot in review than in code.

---

## 1. Sign In

Shows the portal role check that makes four sign-in pages safe.

The `alt` fragment is the point of the diagram. On the success branch the servlet asks the role's
own `RolePolicy` for its home path and redirects. On the other branch — **correct password, wrong
portal** — it re-renders with the *same* generic message and comparable timing, so a portal cannot
be used to discover which role an email belongs to (**FR-AUTH-03**).

Worth noticing what the servlet does *not* do: it never compares a password, never reads
`user_account`, and never decides where to send anyone. `AuthService` verifies, `PasswordHasher`
compares, `ReceptionPolicy` answers the route. The servlet reads a form and chooses between two
responses.

`LoginAttemptService` is consulted before the lookup, so a locked account is refused without a
database read.

---

## 2. Book Appointment

The most important diagram in the set, because it is the only place the clinic's original problem
is visibly solved.

Everything between `BEGIN` and `COMMIT` is one transaction (**NFR-REL-01**). Inside it:

1. `SELECT … FOR UPDATE` takes a row lock on the slot
2. the appointment number is drawn from `appointment_counter`
3. the appointment row is inserted
4. the slot is marked `BOOKED`

**The lock is what makes FR-APT-04 true.** Without it, two patients reading the same open slot
would both pass an `isOpen()` check and both insert. The `UNIQUE (slot.appointment_no)` key is
drawn as a note because it is the second line of defence, not the first — a constraint violation
is a failed request, whereas the lock makes the second caller wait and then see the truth.

The `else` branch shows what a lost race looks like from outside: `ROLLBACK`, a `409`, and — the
part worth stating — **nothing written**. No appointment, no consumed slot, no counter increment.

After the commit, `AppointmentEventPublisher` notifies observers. The service does not call the
notifier directly, so adding an SMS reminder or an audit entry needs no change here. The cost of
that indirection, stated because the pattern register demands it: this diagram no longer tells you
everything that happens after a booking — you have to know who registered.

---

## 3. Issue Bill

Two nested `alt` fragments, because two conditions gate a bill.

The pricing is delegated twice: `StandardBillingStrategy` computes the breakdown from the
dentist's consultation fee, the treatment's base cost and the clinic's service charge;
`DefaultRevenueSplitStrategy` divides the total three ways. Neither calculation lives in
`BillingService`, which is **FR-BIL-06** — a pricing change is a new strategy class rather than an
edit to the service.

The worked numbers are the seeded ones: 1500 + 3500 + 200 = **5200**, which is what the running
application returns.

**One branch documents a defect rather than a design.** The already-billed path shows what the
system *should* do — `409`, existing receipt shown for reprint. A note records what it does now:
`BillingService` performs no duplicate check, `BillDao` upserts, and the second call answers `201`
with a bill id that was never persisted. FR-BIL-04 is `Partial` in the SRS for exactly this
reason. Drawing the intended behaviour and annotating the actual one is more useful than drawing
either alone.

---

## What these three do not show

Honest limits, since a reader may assume otherwise:

- **No error path for a database outage.** Every diagram assumes MySQL answers. The
  `DataAccessException` → `500` mapping lives in `BaseServlet.handle()` and is the same for all
  three, so drawing it three times would add lines and no information.
- **No CSRF token exchange.** Specified (**NFR-SEC-07**) and not yet designed, so drawing it would
  be invention rather than documentation.
- **Only three of 70 use cases.** The brief asks for approximately three and these are the three
  that carry the most design weight. *Declare Medical Notes*, *Raise Concern* and *Review
  Concern* would each be a short diagram, and none would show anything the class diagram does not
  already state.

---

## Cross-diagram consistency

Each message in these diagrams must correspond to a method in
[`class-diagram.md`](class-diagram.md), and each database statement to a table in
[`er-diagram.md`](er-diagram.md). Traced for *Book Appointment*:

| Sequence message | Class diagram | ER table |
|---|---|---|
| `book(patientId, slotId, treatmentId, actor)` | `AppointmentService.book(...)` | — |
| `lockForUpdate(slotId)` | `SlotRepository` | `slot` |
| `nextFor(date)` | `AppointmentNumberGenerator.nextFor(date)` | `appointment_counter` |
| `save(appointment)` | `AppointmentRepository.save(...)` | `appointment` |
| `bookFor(no)` | `Slot.bookFor(appointmentNo)` | `slot.status`, `slot.appointment_no` |
| `publish(BOOKED, appointment)` | `AppointmentEventPublisher.publish(event)` | — |
| `onAppointmentEvent(event)` | `NotificationObserver` | `notification` |

Every row resolves. A message with no method, or a method invoked in no sequence, means one of the
two diagrams is wrong — which is the check worth running after any design change, and the reason
these three diagrams are worth keeping current rather than drawing once.
