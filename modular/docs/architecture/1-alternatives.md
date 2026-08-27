# Architectures Considered

Nine architectures were weighed for this system. This document states what each is, what it would
have bought, and why it lost — because an architecture chosen without naming the rejected
alternatives is an assumption rather than a decision.

---

## 1. What the choice had to satisfy

The constraints are not preferences. They came from the brief, the lecturer and the clinic, and
they eliminate most of the field before any argument about elegance.

| # | Constraint | Source | Eliminates |
|---|---|---|---|
| **C1** | No application framework | CON-01, lecturer | Anything whose value comes from framework infrastructure |
| **C2** | Web interaction by Jakarta Servlet | CON-02 | Client-side architectures, and any transport but HTTP |
| **C3** | Plain JDBC, no ORM | CON-03 | Repository patterns that assume a mapping layer |
| **C4** | Must **demonstrably** be three-tier | Marking criteria | Structures where the tiers are implicit |
| **C5** | One clinic, one database, one deployable | Scenario | Distributed architectures |
| **C6** | Maintained by a human, not a framework's conventions | Stated requirement | Anything where behaviour is configured rather than written |
| **C7** | ~125 classes, one developer | Reality | Ceremony that does not pay for itself at this size |

**C1 and C6 together are the decisive pair.** Most modern architectural patterns assume a
framework is doing the plumbing. Strip the framework out and the pattern's cost stays while its
benefit disappears — you are left hand-writing what the framework would have provided.

---

## 2. The nine

### a. Package-by-layer monolith — *what `layered/` is*

All servlets in `web/`, all services in `service/`, all DAOs in `dao/`. The conventional Java
layout.

**Would give:** the tiers are unmissable — three packages, three tiers. Familiar to every Java
developer. Trivially satisfies **C4**.

**Why it lost:** it satisfies C4 and fails C6. One feature is scattered across six packages: a
billing change means opening `web/BillingPageServlet`, `service/BillingService`,
`pattern/billing/StandardBillingStrategy`, `dao/BillDao`, `repository/BillRepository` and
`domain/Bill`. You must understand the whole system before you can locate one part of it. And the
evidence is in this repository: **12 of 20 servlets in `layered/` call a repository directly**,
because when the tier boundary is only a package name, nothing makes crossing it feel wrong.

### b. Package-by-feature, three tiers inside each module — **chosen**

Eight feature modules, each holding `web/ service/ data/ domain/`.

**Would give:** one feature in one directory, and the tiers preserved *within* each. Satisfies C4
more strongly than (a), because the separation can be demonstrated eight times over rather than
asserted once globally.

**Why it won:** it is the only option that satisfies all seven constraints. The reasoning is
worked through in §3.

### c. Hexagonal / Ports and Adapters, applied fully

Ports around every boundary — an inbound port for HTTP, an outbound port for persistence, adapters
implementing each. The domain knows nothing of either.

**Would give:** the domain becomes testable and transport-agnostic. Genuinely valuable when a
system has several inbound channels.

**Why it lost:** this system has **one** inbound channel — a servlet — and will not grow another.
A port around HTTP would add an interface, an adapter and a mapping for every operation, in
exchange for a substitutability nothing needs. Against C7, that is ceremony with no payer.

**What was kept from it.** Ports *are* used at the data tier: `Repository<T, ID>` is an outbound
port, `JdbcDao` and the in-memory implementations are its adapters. That boundary pays for itself
immediately — it is why 48 tests run with no database. The rule applied throughout: **a boundary
goes where a test or a change already wants it, and nowhere else.**

### d. Clean Architecture / Onion

Concentric layers with the dependency rule pointing inward; a use-case class per operation,
entities at the centre knowing nothing outward.

**Would give:** a very explicit dependency direction, and a use-case class per behaviour that maps
one-to-one onto the use case diagram.

**Why it lost:** the class count roughly doubles — a `BookAppointment` interactor, a request model,
a response model and a boundary interface, where this design has one service method. For 70 use
cases that is several hundred extra classes, and against C7 the indirection costs more reading than
it saves. Clean Architecture earns its keep when business rules must outlive several UI and
persistence generations; a single clinic's appointment book is not that.

**What was kept from it.** The dependency direction. `domain/` depends on nothing; `service/`
depends on `data/` interfaces, never implementations; the module graph is acyclic. That is the
useful half of the idea without the class explosion.

### e. MVC only — servlet straight to DAO, no service layer

**Would give:** the fewest classes and the shortest call stack. Defensible for genuinely thin CRUD.

**Why it lost:** this system is not thin CRUD. Booking holds a row lock, generates a per-day
sequence, writes two tables and publishes an event, all in one transaction; billing runs two
strategies and writes a three-way split. Business rules with nowhere to live end up in servlets,
which is how you get a rule duplicated between a page and an API endpoint and drifting. It also
fails **C4** outright — two tiers are not three.

### f. Microservices

A deployable per capability, communicating over HTTP or a broker.

**Would give:** independent deployment and scaling per capability.

**Why it lost:** it fails **C5** and collides head-on with **C1**. One clinic, one database, one
WAR — there is nothing to deploy independently. And with no framework, "microservices" means
hand-writing service discovery, inter-service authentication, distributed tracing and a
transaction strategy for a booking that spans two services. The double-booking guard, which is
currently one `SELECT … FOR UPDATE`, would become a distributed lock or a saga.

**What was kept from it.** Module boundaries give the same *isolation* benefit — a bounded blast
radius for a change — with none of the network. That is the modular-monolith bargain: the seams
are real, and if a capability ever genuinely needs its own deployable, a module is the unit you
would extract.

### g. Event-driven, CQRS, event sourcing

Commands and queries on separate models; state derived from an append-only event log.

**Would give:** a complete audit trail for free, and independent read scaling.

**Why it lost:** both benefits are already met more cheaply. The audit trail is one table,
`audit_event`, written by an observer. Read scaling is not a problem a single practice has — the
reports screen aggregates a few thousand rows. Event sourcing would replace a readable `appointment`
table with a projection you cannot query directly in a client, which cuts against C6 hard.

**What was kept from it.** The Observer on booking. `AppointmentEventPublisher` is event-driven in
the small: the booking service publishes and does not know who listens, so notifications and audit
are additive. One pattern, at the one place decoupling pays.

### h. Serverless / functions

Each endpoint a function behind a managed gateway.

**Why it lost:** fails C1, C2 and C5 simultaneously. It also inverts the deployment story — the
brief and Task D want one artefact deployed to a container, not thirty functions and a gateway
configuration. Connection pooling against MySQL from short-lived functions is a known problem this
project has no reason to acquire.

### i. Client–server SPA plus REST API — *what the removed React version was*

A JavaScript application in the browser, calling a JSON API.

**Would give:** a richer interface and a clean API boundary.

**Why it lost:** it was actually built and then removed, because React is an application framework
and **C1** forbids it. Worth recording what the removal cost and what it revealed: the JSON API
survived and is now consumed by nothing, which is precisely why five of its endpoints returned
`toString()` for months without anyone noticing. An architecture where nothing exercises its own
API cannot keep that API honest.

---

## 3. Why the chosen one fits *this* solution

Not "it is the best architecture" — it is the one that satisfies all seven constraints, and the
only one that does.

| | a. by-layer | **b. by-feature** | c. hexagonal | d. clean | e. MVC only | f. micro | g. CQRS |
|---|:--:|:--:|:--:|:--:|:--:|:--:|:--:|
| C1 no framework | ✔ | **✔** | ✔ | ✔ | ✔ | ✘ | ~ |
| C2 servlet transport | ✔ | **✔** | ✔ | ✔ | ✔ | ~ | ✔ |
| C3 plain JDBC | ✔ | **✔** | ✔ | ✔ | ✔ | ✔ | ✘ |
| C4 demonstrably 3-tier | ✔ | **✔✔** | ~ | ~ | ✘ | ~ | ✘ |
| C5 one deployable | ✔ | **✔** | ✔ | ✔ | ✔ | ✘ | ✔ |
| C6 human-maintainable | ✘ | **✔** | ~ | ~ | ✘ | ✘ | ✘ |
| C7 pays at 125 classes | ✔ | **✔** | ~ | ✘ | ✔ | ✘ | ✘ |

Three specific reasons it fits the clinic rather than fitting in general:

**The domain divides cleanly along capability lines.** Appointments, billing, scheduling, the
patient register — these are how the clinic itself describes its work, not how a programmer
describes the code. A module boundary that matches a business boundary stays put; one that matches
a technical layer moves whenever the technology does.

**The confidentiality rules are per-capability, not per-layer.** Medical notes are readable by the
dentist and nobody else; complaints by the administrator and nobody else; reviews by the dentist as
an aggregate only. Three rules pointing in three directions, each belonging to one capability. In a
by-layer design those rules live in a shared `security` package remote from the data they guard; in
this one, each sits in the module that owns the data.

**The rubric wants both, and this gives both.** "Three-tier architecture" is an explicit Excellent
criterion, and so is code a maintainer can follow. Package-by-layer satisfies the first and fails
the second; package-by-feature alone would satisfy the second and blur the first. Tiers *inside*
modules is the arrangement that satisfies both, and it can be demonstrated eight times rather than
claimed once.

---

## 4. What this choice costs

An alternatives document that ends with the chosen option looking costless has not been honest.

| Cost | Why it is accepted |
|---|---|
| 32 packages for ~125 classes | More directories than this size strictly needs. The bet is that the system grows; if it stays this size a flatter tree would have been enough |
| `AppContext` is one long file every module edits | Explicit wiring means one place to look and one place to conflict. A DI container would remove the conflict and the readability together |
| Cross-module reads need their own model | `reporting` spans bills and appointments, so it gets a read-only repository rather than reaching into two modules' data tiers |
| Hand-written JSON and DI have no library's edge cases handled | Already demonstrated: `Json.write` falls through to `toString()` for non-records, which is why five endpoints returned `"Dentist{id=d-silva}"` |
| Two levels of grouping to learn | A newcomer must absorb "modules, and tiers within modules" rather than one axis |

The last row is the one that would change the decision. If the system were half this size, the
second axis would not pay for itself and package-by-layer would be the right answer.
