# Finding Your Way As It Grows

The four properties the structure was chosen for, and — the part that matters most — what happens
to each as features are added.

An architecture that reads well at 125 classes and badly at 400 has not solved the problem. So each
section below states the property, what delivers it, and **where it breaks down**.

---

## 1. Clear to read

### The property

Two coordinates locate any class: *which capability*, and *which tier*. Asking "where is a bill's
total calculated?" needs no map — it is business logic about billing, so `billing/service/`. The
answer is `StandardBillingStrategy`.

The concrete target: **a competent Java developer with no knowledge of this project should trace one
request end to end in under ten minutes, with nothing but an editor.** No annotations to decode, no
configuration to find, no reflection to reason about. `AppContext` is 250 lines of constructor calls
you can read top to bottom.

### As it grows

The search space stays constant, because the question is answered by two enumerations rather than by
memory:

| Modules | Directories to consider | How you find a class |
|---|---|---|
| 8 | 32 | pick a capability, pick a tier |
| 15 | 60 | pick a capability, pick a tier |
| 30 | 120 | pick a capability, pick a tier |

That is the whole point of the second axis. In a package-by-layer tree, `service/` at 30 features
holds 120 files in one directory and you find things by knowing their names. Here `billing/service/`
holds five, whatever the rest of the system does.

**Naming carries the tier**, so a file name alone tells you where it belongs: `*Servlet` is
presentation, `*Service` and `*Strategy` are business, `*Dao` and `*Repository` are data. A class
whose name does not fit one of those is a signal to look at it twice.

### Where it breaks down

**`web.xml` at around 50 servlets.** Every servlet is declared there — deliberately, so one file
lists every URL the system answers. At 18 that is a feature; at 50 it is a wall. The fix when it
arrives is `@WebServlet` annotations per servlet, which trades the single index for locality. Worth
doing *then*, not now.

**`AppContext` at around 15 modules.** One accessor per service, all in one file. Two developers
adding modules the same week conflict. The fix is splitting it into per-module factory classes that
a thin root composes — still explicit, still no container.

---

## 2. Clear to identify bugs

### The property

Each tier fails in its own way, so a symptom names a tier before you open anything. All three
examples below are real defects found in this project:

| Symptom | Tier | The actual defect |
|---|---|---|
| Wrong page, wrong status, wrong markup, missing stylesheet | **Presentation** | `HomeServlet` mapped to `/` shadowed Tomcat's default servlet, so every stylesheet request answered a redirect to `/login` |
| Wrong number, wrong decision, wrong permission | **Business** | Nothing checks that the dentist completing an appointment is the one treating it |
| Wrong, missing or duplicated row | **Data** | `BillDao` upserts, so billing twice answers `201` with an id that was never persisted |

Three defects, three tiers, each findable without reading the other two.

### The bisect tool is free

Every repository interface has an in-memory implementation. Swap the data tier for it and re-run:

- **bug survives** → business logic
- **bug vanishes** → SQL, mapping, or the schema

That is the same seam that lets 48 tests run with no database. It was built for testing and it
diagnoses for free.

### Two greps keep it honest

The boundary decays silently unless something checks it — `layered/` reached 12 tier violations out
of 20 servlets precisely because nothing was checking.

**An import grep alone is not enough, and it is worth knowing why.** Run against `layered/`, a check
for servlets importing a repository type finds **two** files. The real number of servlets reaching
the data tier is **twelve**. The difference is method chaining: a servlet writes

```java
app().patients().search(term)      // no import of PatientRepository needed
```

so the type never appears in an import statement. A check that only reads imports would have
reported this codebase as clean while two thirds of its presentation tier talked to the database.

Both checks, therefore — cheap enough for CI:

```bash
# 1. No web/ class may name a data/ type
grep -rl 'import com.sunrise.clinic.[a-z]*\.data' --include='*.java' \
  modular/src/main/java/com/sunrise/clinic/*/web/                     # must be empty

# 2. No web/ class may reach a repository through the context, however it is spelled.
#    In the modular design a servlet obtains only its own service from AppContext.
grep -rnE 'app\(\)\.[a-z]+(Repository|Dao)\(\)' --include='*.java' \
  modular/src/main/java/com/sunrise/clinic/*/web/                     # must be empty
```

Check 2 is the one with teeth, and it only works because the naming convention is enforced: an
`AppContext` accessor returning a repository is named for it. Conventions that a grep can read are
worth more than conventions that only a reviewer can.

### Where it breaks down

**A bug that spans tiers still spans tiers.** The double-booking guard is a business rule expressed
as a data-tier lock; if it broke, the symptom would be a duplicated row and the cause a missing
`FOR UPDATE`. Tier isolation narrows the search, it does not eliminate it.

**Observer indirection hides control flow.** After a booking commits, an event is published and
observers run. The sequence diagram no longer tells you everything that happens next — you have to
know who registered. That is the price of the decoupling, and it is why the pattern register records
a cost for every pattern.

---

## 3. Clear to plug into

### Five extension points

All plain Java interfaces. None needs a framework, and none requires editing the code it extends.

| To add | Implement | Register in | Touches nothing else |
|---|---|---|---|
| A storage backend, or a test double | `Repository<T, ID>` | `AppContext` | ✔ |
| A pricing or revenue rule | `BillingStrategy`, `RevenueSplitStrategy` | `AppContext` | ✔ |
| A notification channel — WhatsApp, push | `NotificationChannel` | `NotificationChannelFactory` | ✔ |
| A reaction to a booking — analytics, audit | `AppointmentObserver` | `AppointmentEventPublisher` | ✔ |
| A user role — a fifth portal | `RolePolicy` + `AbstractLoginServlet` | `web.xml` | ✔ |

**A whole module is the sixth.** Because the module graph is acyclic, adding one has a blast radius
of zero on what already exists.

### Two worked examples

**Adding SMS reminders — an existing module.**

| Step | File |
|---|---|
| 1. Write the channel | `notifications/service/SmsChannel.java` |
| 2. Add one case | `notifications/service/NotificationChannelFactory.java` |
| 3. Wire the gateway URL | `clinic.properties` + `platform/config/AppConfig` |
| 4. Test it | `test/…/notifications/SmsChannelTest.java` |

Four files. `AppointmentService` does not change, no servlet changes, no schema change —
`notification.channel` is already an enum with `SMS` in it.

**Adding insurance claims — a new module.**

| Step | What |
|---|---|
| 1. Create `insurance/{web,service,data,domain}` | Four directories |
| 2. Write the four tiers | `Claim`, `ClaimService`, `ClaimRepository` + `ClaimDao`, `ClaimServlet` |
| 3. One accessor | `AppContext.claimService()` |
| 4. Declare the servlet | `web.xml` |
| 5. Extend the schema | one table, one foreign key to `bill` |
| 6. Add the module edge | `insurance ..> billing` in the dependency graph |

Nothing in the other eight modules changes. Step 6 is the one to think about: if the new module
needed an edge *back* from `billing` to `insurance`, the graph would cycle and the boundary would be
wrong — which is the design review the DAG performs for you.

### Where it breaks down

**The five extension points are the ones anticipated.** An unanticipated variation still means
editing a class. Adding a second *currency*, for instance, has no seam — `BillBreakdown` holds
`double` amounts and assumes rupees. The honest position: five seams exist because five kinds of
change were foreseen, not because the design is open to all change.

---

## 4. Clear to scale

Two different questions with different answers.

### Scaling the code

Add a module. Eight exist; a ninth costs one directory tree and one accessor. The acyclic graph
guarantees the blast radius, and the guarantee is checkable rather than hoped for.

### Scaling the load

The WAR holds almost no state:

| State | Where it lives | Consequence |
|---|---|---|
| Business data | MySQL, a separate process | Add Tomcat instances freely |
| Connections | 8 per instance, one env var | Tune per environment with no rebuild |
| Sessions | Tomcat's memory | **Needs sticky sessions to run more than one instance** |
| Lock-out counters | `LoginAttemptService`, in memory | **Does not survive restart, and does not work across instances** |

The first two scale horizontally today. The last two are the honest blockers — and the fourth is a
**defect**, not merely a limit: `user_account` already has `failed_attempts` and `locked` columns
that nothing writes, so the lock state NFR-SEC-03 depends on is cleared by any restart. Fixing it
means persisting what the schema already models.

### Where it breaks down

**One database is one database.** Every module shares `sunrise_dental`, so a slow report competes
with a booking. For one practice that is right; a chain would need read replicas, and `reporting`
is the module you would point at one — which is why it has its own repository rather than borrowing
`billing`'s.

**Module boundaries are not process boundaries.** They make extraction *possible*, not free. The
graph tells you `feedback` could become its own service with four inbound dependencies to replace
with API calls. That is a real estimate, which is more than most monoliths can offer, but it is not
a small number.

---

## 5. Onboarding a developer into a grown system

The property that matters when the system is larger than any one person's memory.

**A new developer owns one module on day one.** To be useful in `billing` they need
`billing/`, `platform/`, and the *interfaces* of `appointments` and `scheduling` — not their
internals, because the rule forbids reaching into a sibling's `web/` or `data/`. That is roughly 20
files instead of 125.

**The four questions a newcomer asks, and where each is answered:**

| Question | Answer |
|---|---|
| Where does feature X live? | One directory named after it |
| What can I break by changing this? | The module's own tier, plus anything with an inbound edge in the dependency graph |
| Where do I put the new thing? | The same four sub-packages, every time |
| How do I know I have not broken a rule? | Two greps and 48 tests |

**The documents are the map, and they are checkable against the code.** Each answers a different
question about the same system, and any drift shows up as a broken cross-reference:

| Document | Answers |
|---|---|
| [`../use-case-diagram.md`](../use-case-diagram.md) | Who wants what |
| [`../class-diagram.md`](../class-diagram.md) | What the system is made of |
| [`../sequence-diagrams.md`](../sequence-diagrams.md) | In what order, and what happens when it fails |
| [`../er-diagram.md`](../er-diagram.md) | Where the result is kept |
| [`../srs/srs.md`](../srs/srs.md) | Why any of it is required |

The consistency rule is what keeps them worth reading: an `<<include>>` with no corresponding method
call, or a sequence message with no method in the class diagram, means one of the two is wrong.

---

## 6. When this architecture stops being the right one

Stated so the design does not outlive its reasoning.

| Trigger | What it means | What to do |
|---|---|---|
| ~15 modules | `AppContext` and `web.xml` become edit bottlenecks | Split `AppContext` per module; move to `@WebServlet` |
| A second inbound channel — a mobile app with its own needs | The one-servlet assumption breaks | Put a port around the business tier, i.e. adopt (c) from [`1-alternatives.md`](1-alternatives.md) properly |
| A second clinic with its own database | The single-datasource assumption breaks | Tenant column plus a datasource per tenant, or extract per-tenant deployables |
| Reports too slow to run live | The shared-database assumption breaks | A read replica, pointed at by `reporting` only |
| One capability needing independent deployment | The single-deployable assumption breaks | Extract that module — the graph tells you the cost |

None of these is true today. Each would be a reason to change the architecture, and none is a reason
to have built differently now.
