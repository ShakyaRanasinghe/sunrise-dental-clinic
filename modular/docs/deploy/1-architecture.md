# Deployment architecture

## What runs

```
                        ┌──────────────────────────────┐
   a browser  ────────► │  Tomcat 10.1  ·  JDK 17      │
   HTTP/HTTPS           │                              │
                        │  ROOT.war                    │
                        │  ├── servlets   (web tier)   │
                        │  ├── services   (business)   │
                        │  ├── DAOs       (data)       │
                        │  └── JSP + one CSS file      │
                        │                              │
                        │  8 connections, pooled       │
                        └───────────────┬──────────────┘
                                        │  JDBC
                                        │  utf8mb4
                                        ▼
                        ┌──────────────────────────────┐
                        │  MySQL 8.0                   │
                        │                              │
                        │  14 tables · 24 foreign keys │
                        │  3 routines · 5 triggers     │
                        │  1 check constraint          │
                        └──────────────────────────────┘
```

Two processes. That is the entire runtime.

## What is deliberately absent

| Not here | Why not |
|---|---|
| A front-end build, a CDN, a hosting channel | The server renders the pages. There is no second artefact, so there is nothing that can be a version behind the WAR. This replaced three Firebase Hosting workflows that ran `npm ci` in a directory the application never used |
| A load balancer | One practice, a few hundred requests a day. A second Tomcat would need session replication or sticky sessions, and neither buys anything at this size |
| A cache | The expensive queries are the reports, and they are read a few times a day by one person. Caching them would add a staleness question to answer for no measurable gain |
| A message broker | The one asynchronous thing in the design — notifications — is an in-process observer. A broker would be a second thing to run so that an email can be late |
| Kubernetes, or any orchestrator | Two containers. `docker run` twice is the whole deployment, and a control plane to manage two containers is more moving parts than it removes |
| An application framework | The point of the exercise, and the reason the dependency list is five entries. See the root README |

**Five dependencies, and that is the whole list:** `jakarta.servlet-api`, `jakarta.servlet.jsp-api`,
`jakarta.servlet.jsp.jstl`, `mysql-connector-j`, `junit-jupiter`. The first three are provided by
the container, so exactly two are shipped.

## The three tiers, and where the boundary is enforced

The tiers are not a diagram — they are a rule the build checks:

```
web/       servlets and JSP. May call service/. May not touch data/.
service/   the rules. Calls data/ through interfaces only.
data/      repository interfaces, JDBC adapters, in-memory adapters.
domain/    entities and the records that leave the module.
```

Two greps run on every pull request, and both must print nothing:

```bash
grep -rl  'import com.sunrise.clinic.[a-z]*\.data' --include='*.java' \
  modular/src/main/java/com/sunrise/clinic/*/web/
grep -rnE 'app\(\)\.[a-z]+(Repository|Dao)\(\)'    --include='*.java' \
  modular/src/main/java/com/sunrise/clinic/*/web/
```

The second matters more than the first. An import check alone found **two of the twelve**
violations the earlier arrangement had, because `app().patients().search(term)` names no type to
import — which is also why `AppContext` stopped exposing repositories at all and exposes only
services.

## Where a request goes

```
  GET /reception/billing
        │
        ▼
  AuthenticationFilter        who is this?      → 302 to /login if nobody
        │                     may they be here? → 403 if the prefix is another role's
        ▼
  BillingPageServlet          reads the request, nothing else
        │
        ▼
  BillingService              the rules: may they bill? has it been billed? is it treated?
        │
        ▼
  BillRepository (interface)  ── JDBC in production, in-memory in tests
        │
        ▼
  MySQL                       and the constraints and triggers underneath
```

Every rule is stated once, in the service. The servlet cannot enforce one the next servlet
would forget, and the database still holds the last line — a unique key, a check constraint, a
trigger — for anything that reaches the table another way.

## Defence in depth, on purpose

Three of the rules exist in two places, and the duplication is deliberate:

| Rule | In the application | In the database |
|---|---|---|
| A slot is booked once | `SELECT … FOR UPDATE` inside the booking transaction | `UNIQUE KEY uq_slot_appointment`, and `trg_prevent_double_booking` |
| A bill needs a treated appointment | `BillingService` refuses it in plain language | `trg_bill_requires_completion` |
| A rating is 1 to 5 | `DentistReview.setRating` | `CHECK (rating BETWEEN 1 AND 5)` |

The application's version exists so a person gets a sentence they can act on. The database's
version exists because a migration script, a support query or a future service will eventually
write that table without going through the service — and on that day the constraint is the only
thing standing there.

The audit trail is the same idea from the other direction: `AuditObserver` records **who** acted,
and `trg_audit_appointment_status` records **that** a status changed even when the application was
not involved. Its null actor is the signal that the change did not come through the application.

## Sessions, and what that means for scaling

Authentication is an `HttpSession` holding a `ClinicPrincipal`. No JWT, no token store — the
stack has no client-side JavaScript to hold a token, and a cookie the container already manages
is the simpler correct answer.

The consequence is worth stating plainly: **sessions are in Tomcat's memory**, so a restart signs
everyone out, and a second Tomcat would need session replication or sticky sessions before it
worked at all. At one practice that is a fair trade. It is also the first thing that would have
to change if the clinic became a chain, and it is the reason the section below draws that line
where it does.

## When this stops being the right shape

| If | Then |
|---|---|
| A second branch opens | Sessions become the problem first. Either sticky sessions at a proxy, or move the session to the database |
| Reports get slow | They are already `SUM` and `GROUP BY` in SQL. Add indexes on `bill.issued_at` and `appointment.appointment_date` before considering a cache |
| Notifications are wanted | Register `NotificationObserver` in `AppContext`. The seam is built: the event carries the recipient and the publisher isolates its observers |
| A mobile app is wanted | The JSON API already exists beside every screen and is documented in [`../api/`](../api/). It was built for exactly this, and is the reason the confidentiality rules live in the services rather than the views |
| The clinic wants online payment | Out of scope by design (SRS §9). The `bill` table records what was charged, not what was paid — adding payment means a new table, not a new column |
