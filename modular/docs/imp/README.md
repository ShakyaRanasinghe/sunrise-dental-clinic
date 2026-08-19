# Implementation Tracker — temporary

> **Delete this directory when the implementation is finished.** It is a working board, not
> documentation. Everything durable belongs in [`../migration-plan.md`](../migration-plan.md),
> [`../servlets.md`](../servlets.md) and [`../architecture/`](../architecture/).

The checklist is in [`tasks.md`](tasks.md). This page is the status board.

---

## Current position

| | |
|---|---|
| **Active step** | **0 — decide what ships** |
| Steps complete | 0 of 9 |
| First deployable | **end of step 2** |
| First real screen | **end of step 3** |
| Core journey working | **end of step 4** |
| `layered/` | frozen, 48 tests passing, deployable |
| `modular/` | scaffolding complete, 0 Java classes |

```
 0  decide what ships          ◻  ← START HERE, blocks everything
 1  platform                   ◻   help page renders
 2  access + stub landings     ◻   ✱ sign in as all four roles — FIRST DEPLOY
 3  patients + scheduling      ◻   ✱ patient register, dentist and treatment lists
 4  appointments               ◻   ✱✱ three dashboards live, booking works
 5  billing                    ◻   ✱✱ all six of the brief's functions work
 6  notifications              ◻   email and SMS actually send
 7  reporting                  ◻   admin reports, accounts, audit
 8  feedback + medical notes   ◻   complaints, reviews, notes
 9  verify and report          ◻
```

✱ = deployable and demonstrable · ✱✱ = a good stopping point if time runs out

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

| If the deadline is | Then | Because |
|---|---|---|
| **more than ~2 weeks** | Do steps 1–8. `modular/` ships | 7–9 hours of migration fits, and the modular structure is what the report describes |
| **1–2 weeks** | Do steps 1–4, stop, then decide | Step 4 is the first point where `modular/` demonstrates the brief's requirements 1, 2 and 3. Below that, `layered/` remains the better submission |
| **under a week** | Skip the migration entirely. Fix `layered/`'s defects and write the report | A half-migrated tree is the one outcome that costs marks rather than earning them |

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
