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
| `layered/` | frozen, 48 tests passing, deployable |
| `modular/` | scaffolding complete, 0 Java classes |

```
 0  decide what ships          ◻  ← START HERE, blocks everything
 1  platform                   ◻
 2  access                     ◻
 3  patients                   ◻
 4  scheduling                 ◻
 5  appointments               ◻
 6  billing                    ◻
 7  notifications              ◻
 8  reporting + feedback       ◻
 9  verify and report          ◻
```

---

## Why step 0 comes before any code

**One question decides the whole sequence, and it has not been answered: when is the Moodle
deadline?**

| If the deadline is | Then | Because |
|---|---|---|
| **more than ~2 weeks** | Do steps 1–8. `modular/` ships | 7–9 hours of migration fits, and the modular structure is what the report describes |
| **1–2 weeks** | Do steps 1–2, stop, ship `layered/` | `platform` and `access` deliver the two things worth having — the `Json` fix and the four portals — and `layered/` stays the submission |
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
