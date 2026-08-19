# Implementation Tracker — temporary

> **Delete this directory when the implementation is finished.** It is a working board, not
> documentation. Everything durable belongs in [`../migration-plan.md`](../migration-plan.md),
> [`../servlets.md`](../servlets.md) and [`../architecture/`](../architecture/).

The checklist is in [`tasks.md`](tasks.md). This page is the status board. The environment is
[`../local-setup.md`](../local-setup.md) — one command, `./scripts/dev-up.sh`.

---

## Current position

| | |
|---|---|
| **Active step** | **6 — `notifications`**, or **8 — feedback**, or **9 — verify and report** |
| Steps complete | **6 of 9** — through `billing`, plus `reporting`. **All six of the brief's functions work, and every role lands on a real screen** |
| First deployable | **done** — sign in as any of the four roles at http://localhost:8080 |
| First real screen | **done** — the patient register and publish-availability |
| Core journey working | **done** — publish, book, day view, schedule, diagnosis |
| `layered/` | **out of scope** — source and history only, not run or shipped |
| `modular/` | 135 Java files, 25 views, **207 tests green**, deploys and answers |

```
 0  decide what ships          ◻  ← START HERE, blocks everything
 1  platform                   ✅  18 classes · Json fix · 15 tests
 2  access + stub landings     ✅  30 classes · 4 portals · 15 views · 50 tests · 5 defects found
 3  patients + scheduling      ✅  55 classes · 2 real screens · 48 tests · 7 defects fixed
 4  appointments               ✅  22 classes · 3 dashboards · 37 tests · 7 defects fixed
 5  billing                    ✅  14 classes · receipt · 34 tests · 4 defects fixed
 6  notifications              ◻   email and SMS actually send
 7  reporting                  ✅  14 classes · 3 admin screens · 23 tests · no stubs left
 8  feedback + medical notes   ◻   complaints, reviews, notes
 9  verify and report          ◻
```

✱ = deployable and demonstrable · ✱✱ = a good stopping point if time runs out

### What step 2 proved against the running application

Verified by driving the deployed WAR, not by reading it:

| Check | Result |
|---|---|
| Four portals, four seeded accounts | each signs in and lands on its own page |
| Wrong password / wrong portal / unknown address | one message, byte-identical bodies — FR-AUTH-03 |
| Five failures | account locked; the correct password is then refused |
| Lock survives a restart | yes — the counters are in `user_account`, not a map |
| Administrator unlock | `POST /api/auth/unlock` clears it; a receptionist gets 403 |
| Role isolation | a clean diagonal — each role reaches only its own prefix, 403 otherwise |
| Anonymous protected path | redirected to `/login?next=…`; API paths get 401 JSON |
| Sign-out | session invalidated, protected paths redirect again |
| Signing in **from a browser**, by clicking the button | works — and did not, until the form's action was fixed |

Running it also found five defects that reading it had not — see the end of step 2 in
[`tasks.md`](tasks.md). The most serious was that a signed-in patient could open every role's
landing page; the one only a browser could find was that the sign-in form posted to the view's own
path, so clicking **Sign in** answered 404 while `curl` against the same route succeeded.

---

## The order was wrong, and this is the fix

The first version of this plan migrated modules in pure dependency order:
`platform → access → patients → scheduling → appointments → …`. Checked against the route
inventory, that order gives **nothing you can navigate until step 5**:

| After | Pages resolving | Role landing pages |
|---|---|---|
| step 1 platform | 1 of 24 | **0 of 4** |
| step 2 access | 9 of 24 | **0 of 4** |
| step 3 patients | 11 of 24 | **0 of 4** |
| step 4 scheduling | 12 of 24 | **0 of 4** |
| step 5 appointments | 16 of 24 | 3 of 4 |

`HomeServlet` redirects a signed-in receptionist to `/reception/home`, which the `appointments`
module owns. So for four steps you could sign in and land on a **404**. Worse, the step-0 advice
below originally said "one to two weeks → do steps 1–2 and stop", which would have shipped a login
page leading nowhere.

**Two changes fix it, and neither breaks the dependency order:**

1. **Step 2 adds four stub landing pages** — one per role, each the real header and navigation with
   "signed in as …" and nothing else. Four small JSPs. The moment sign-in works, it goes somewhere,
   and the four portals plus role routing plus lock-out become demonstrable on their own.
2. **`patients` and `scheduling` merge into one step.** Neither owns a landing page, so as separate
   steps they were two stretches with no visible change. Together they are the reference data
   `appointments` needs, and they are independent of each other, so the step is two commits.

`appointments` then becomes step 4 rather than 5, and it is the step where the application starts
looking like the prototype: three dashboards on real data, and booking working end to end.

**The revised stopping points are honest ones.** Stop after step 2 and you can demonstrate the
brief's requirements 1 and 6. Stop after step 4 and you have 1, 2 and 3. Stop after step 5 and all
six work.

---

## Why step 0 comes before any code

**One question decides the whole sequence, and it has not been answered: when is the Moodle
deadline?**

`modular/` is the only code path, so the deadline sets **how far through the steps to go**, not
which implementation to ship:

| If the deadline is | Aim for | Because |
|---|---|---|
| **more than ~2 weeks** | all 9 steps | 7–9 hours fits, and the features beyond the brief are worth the marks |
| **1–2 weeks** | **step 5**, then step 9 | Step 5 is the first point at which all six of the brief's functions work. Steps 6–8 are extensions |
| **under a week** | **step 4**, then step 9 | Requirements 1, 2, 3 and 6 working, with billing as the first thing to cut |

Skip step 9 under no circumstances — it is where the SRS statuses are corrected and the report gets
its evidence.

Step 0 also has one task that is worth doing **under every branch above**, which is why it is a step
and not just a question. See [`tasks.md`](tasks.md).

---

## Rules while this board exists

1. **`layered/` is frozen.** The only edits it accepts are the two in step 0 — defects that would
   block submission.
2. **One commit per step**, named for its module: `refactor(billing): move to feature module`.
3. **A step is not done until its gate passes.** The gates are in
   [`../migration-plan.md`](../migration-plan.md) §5, and each step below repeats its own.
4. **Tick a box only when it is committed**, not when it compiles locally.
5. **Update the board above** when a step closes, so this page always answers "where are we".

---

## The gates, once

Applied at the end of every step from 1 onward:

```bash
mvn -f modular/pom.xml compile     # gate 1 — must never fail for a missing class
mvn -f modular/pom.xml test        # gate 2 — all moved tests green

# gate 3 — the tier boundary, both checks (an import grep alone finds 2 of 12)
grep -rl  'import com.sunrise.clinic.[a-z]*\.data' --include='*.java' \
  modular/src/main/java/com/sunrise/clinic/*/web/
grep -rnE 'app\(\)\.[a-z]+(Repository|Dao)\(\)'    --include='*.java' \
  modular/src/main/java/com/sunrise/clinic/*/web/
```

Both greps must print nothing. Gate 4 — deploy both WARs and diff the HTML — runs once, in step 9.
