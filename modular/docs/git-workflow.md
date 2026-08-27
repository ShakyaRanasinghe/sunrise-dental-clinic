# Working with git on this project

Everything a developer needs to commit, push, and get changes onto `main` — with no tooling
beyond git and the `gh` CLI, and no help from anybody.

---

## Where things stand

| Branch | State |
|---|---|
| `develop` | Current. Everything described in these docs is here |
| `main` | Behind. Still holds the original Spring Boot / React / Firebase version |
| `qa` | Unused so far |

So the first substantial job is getting `develop` onto `main`, and there is a
[recommended order](#getting-onto-main) for that below.

---

## First, a fact about the contribution graph

**GitHub colours a day by the commit's author date, not by when you pushed it.**

That is worth knowing before planning anything around it. If you commit ten times tonight and
push one commit a day for the next ten days, the graph still shows one busy day — tonight —
and nine empty ones. Pushing does not move a commit's date.

The only way to change the date on a commit that already exists is to rewrite it, and that is
not something to do here. **The git history of a coursework repository is evidence about how the
work was done**, and a marker reading it is entitled to assume it is true. A rewritten history
is a claim about your process that is not accurate — and it is also the kind of thing that shows
up plainly under inspection, because a rewrite changes every commit hash after it and leaves a
reflog that does not match.

The honest way to a graph that shows steady daily work is to **do steady daily work.** There is
genuinely a fortnight of it left, and the plan below lays it out one day at a time. Each day is
a real change, with real tests, producing a real commit — which is exactly the graph you want,
and it stands up to somebody reading it.

---

## The daily loop

Four commands, in this order, every time.

```bash
# 1. Start from the latest develop
git checkout develop
git pull

# 2. Branch for the piece of work
git checkout -b feat/patient-details-editable

# 3. ... do the work ...
cd modular && mvn test          # must be green before you commit
cd .. && ./scripts/smoke.sh     # if you touched a servlet, JSP, DAO or the schema

# 4. Commit and push
git add -A
git commit                      # write a real message; see below
git push -u origin feat/patient-details-editable
```

Then open a pull request into `develop`:

```bash
gh pr create --base develop --fill
```

Review your own diff before merging — reading it back catches more than you expect:

```bash
gh pr diff
gh pr merge --squash --delete-branch
```

**Why squash.** One branch becomes one commit on `develop`, so the history reads as a list of
changes rather than a list of keystrokes. It also means "work in progress" commits on your branch
cost nothing.

### Commit messages

```
<type>(<scope>): <what changed, in the imperative>

<why it changed, and anything the next person would otherwise have to work out>
```

Types: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `ci`.
Scopes: the eight modules — `platform`, `access`, `patients`, `scheduling`, `appointments`,
`billing`, `reporting`, `feedback` — plus `docs`, `uml`, `ci`, `dev`.

The body is the valuable part. "Fixed the bug" tells the next person nothing; "the trigger fires
on the upsert before MySQL discovers the duplicate key, so every update failed" saves them the
afternoon you just spent.

---

## Getting onto `main`

`main` is a long way behind and still holds a different application, so this is one large
integration rather than a normal merge. Do it in this order.

### Step 1 — see the size of it

```bash
git fetch origin
git log --oneline origin/main..origin/develop | wc -l      # how many commits
git diff --stat origin/main origin/develop | tail -1       # how many files
```

### Step 2 — decide what `main` should contain

`main` currently has files this project no longer uses: a `frontend/` directory with
`node_modules` committed, and Firebase configuration for a hosting product nothing deploys to.
Merging `develop` will not remove them, because nothing on `develop` deleted them.

Two honest options, and the choice is yours:

| Option | Consequence |
|---|---|
| **Leave them.** Merge `develop` and let the dead files sit there | `main` stays cluttered, and a marker reading the repository sees a React app that is not part of the submission |
| **Remove them in their own commit on `develop` first** | One reviewable commit — `chore: remove the superseded frontend and Firebase configuration` — and `main` ends up containing only what ships. Nothing is lost: it is all in the history |

If you take the second, do it as its own commit and say why in the message. A deletion mixed into
a feature commit is the kind of thing nobody can review.

### Step 3 — through `qa` if you want a checkpoint, or straight in

The branch strategy in `CONTRIBUTING.md` is `develop → qa → main`. For one integration of this
size, `qa` is a useful place to run the full check once with nothing else moving:

```bash
git checkout qa && git pull
git merge --no-ff develop
git push
# then run everything against it
cd modular && mvn clean package && cd ..
./scripts/dev-up.sh && ./scripts/smoke.sh
```

### Step 4 — the pull request

```bash
gh pr create --base main --head develop \
  --title "The framework-free rebuild: modular structure, all six brief functions" \
  --body-file .github/pr-body.md
```

Write the body properly — this is the one pull request somebody will actually read. Cover:

- what the application now is, in three sentences;
- that `modular/` is the code path and `layered/` is kept as history;
- the six brief functions and where each is demonstrated;
- the test position: 266 unit tests, 53 smoke checks, and what each kind catches;
- the defects found by running it rather than reading it — that list is the most convincing
  thing in the repository;
- what is **not** built, and why. Being straight about the notifications module reads far
  better than hoping nobody checks.

### Step 5 — merge, tag, and check

```bash
gh pr merge --merge                      # a merge commit, not a squash — keep the history
git checkout main && git pull

git tag -a v1.0.0 -m "Sunrise Dental Clinic — all six brief functions, 266 tests"
git push origin v1.0.0
```

Use `--merge` here and not `--squash`. Squashing this one would flatten every commit into a
single "merge develop" — throwing away exactly the record of incremental work that makes the
history worth reading.

The tag triggers `.github/workflows/release.yml`, which builds the WAR and attaches it. Check it:

```bash
gh run list --workflow=release.yml --limit 1
gh release view v1.0.0
```

---

## The plan for the remaining work

Twenty-two requirements are outstanding, and they are recorded honestly in
[`srs/srs.md`](srs/srs.md#verification--what-is-built-and-how-it-was-checked). Here they are as
one day's work each.

**How to use this:** one line, one branch, one commit, one pull request into `develop`. Tick it
when it is merged. If a day gets away from you, the day moves — the order matters, the dates do
not.

Each day ends with a real commit because it contains real work. That is the whole trick.

### Week one — the missing module and the small gaps

| # | Do | Requirements | Commit |
|---|---|---|---|
| 1 | Move `notifications` from `layered/`: `Notification`, `ChannelType`, `NotificationStatus`, the repository trio | FR-NOT-03 | `refactor(notifications): move the dispatch record` |
| 2 | Move `NotificationChannel`, `NotificationChannelFactory`, the email and SMS channels. Move `NotificationChannelFactoryTest` | FR-NOT-05 | `refactor(notifications): move the channel factory` |
| 3 | Move `NotificationObserver` and register it in `AppContext`. Confirm a booking writes a dispatch row and that a channel failure does not fail the booking | FR-NOT-01, FR-PAT-16 | `feat(notifications): send the booking confirmation` |
| 4 | The reminder: a route or a scheduled sweep that sends before the appointment. Decide and document which — a servlet a cron hits is honest and needs no new dependency | FR-NOT-02 | `feat(notifications): the appointment reminder` |
| 5 | `PUT /api/patients/{id}` and an edit form on the register. The oldest outstanding item — carried since step 3 | FR-REC-24 | `feat(patients): correct a patient's details` |
| 6 | Registration matches an existing walk-in on email or contact number and links it rather than creating a second record | FR-PAT-06 | `feat(access): link registration to an existing walk-in` |

### Week two — the screens that exist as services

| # | Do | Requirements | Commit |
|---|---|---|---|
| 7 | Complaint count and mean rating per dentist on the reports screen. Both services already exist and are permission-gated | FR-ADM-57, FR-ADM-60 | `feat(reporting): complaint volume and ratings per dentist` |
| 8 | A dentist's own rating on their schedule — count and mean, withheld below five | FR-DEN-60 | `feat(feedback): the dentist's own rating` |
| 9 | Show a note's last-updated date on the dentist's view, and tell the patient on their profile that the dentist sees only an average | FR-DEN-46, FR-PAT-65 | `feat(patients): note dates and what the dentist sees` |
| 10 | Write a failed administrator sign-in to the audit trail rather than the log. Put it behind a service method so the web tier does not touch the repository | FR-ADM-04 | `fix(access): audit a failed administrator sign-in` |
| 11 | A discount field on the billing screen, refused if it exceeds the total. Extend `BillBreakdown`; the split invariant test will tell you if the shares stop summing | FR-REC-56 | `feat(billing): a discount on a bill` |
| 12 | The treatment catalogue: add a treatment, set a cost, deactivate rather than delete | FR-ADM-40, FR-ADM-42 | `feat(reporting): manage the treatment catalogue` |
| 13 | Edit a dentist's consultation fee. Confirm a bill already issued does not change | FR-ADM-41, FR-ADM-43 | `feat(reporting): change a dentist's fee` |

### Week three — the last requirements and the submission

| # | Do | Requirements | Commit |
|---|---|---|---|
| 14 | The dentist's week view — seven days rather than one | FR-DEN-15 | `feat(appointments): the dentist's week` |
| 15 | Completion without a diagnosis, if you decide it is right. **Read the note in the SRS first** — an appointment marked treated with nothing recorded is what the no-show derivation reads as an absence | FR-DEN-34 | `feat(appointments): completion without a diagnosis` |
| 16 | Confirmation before a destructive action. This needs the second line of client-side script in the application — decide deliberately, and record the decision either way | FR-UI-04, FR-PAT-33 | `feat(web): confirm before cancelling` |
| 17 | Walk all 50 scenarios in [`testing/scenarios.md`](testing/scenarios.md) from a fresh `dev-up.sh`. Fix what you find | — | `fix: what the scenario walkthrough found` |
| 18 | Update the SRS statuses again for everything above, and regenerate the diagrams | — | `docs(srs): statuses after the remaining work` |
| 19 | The report — Tasks B–D in `docs/`. The defects-found-by-running-it list is the strongest material you have | — | `docs: the assessment report` |
| 20 | Remove the superseded `frontend/` and Firebase configuration, if you decided to | — | `chore: remove the superseded frontend` |
| 21 | `develop → qa`, run everything, then `qa → main`, tag `v1.0.0` | — | the pull request |

**Do not skip day 17.** Everything else on this list adds code. That day is the only one that
checks the whole thing still works together, and it is the day that finds what the others broke.

---

## Command reference

Everything you need, in one place.

### Every day

```bash
git checkout develop && git pull            # start from current
git checkout -b <type>/<short-name>         # branch
git status                                  # what have I changed
git diff                                    # what exactly
git add -A && git commit                    # commit (an editor opens for the message)
git push -u origin HEAD                     # push the branch
gh pr create --base develop --fill          # open the PR
gh pr diff                                  # read your own diff
gh pr merge --squash --delete-branch        # merge and tidy up
```

### Before you commit

```bash
cd modular && mvn test                      # 266 tests. Must be green
cd .. && ./scripts/dev-up.sh                # rebuild and redeploy
./scripts/smoke.sh                          # 53 checks. Must be 0 failed
```

### Finding your way around history

```bash
git log --oneline -20                                   # recent commits
git log --format='%ad %h %s' --date=short -20            # with dates
git log --oneline origin/main..origin/develop            # what main is missing
git log -p --follow -- modular/src/.../Thing.java        # one file's whole history
git blame modular/src/.../Thing.java                     # who last touched each line
git show <hash>                                          # one commit in full
```

### Undoing things

```bash
git restore <file>                          # discard changes to a file
git restore --staged <file>                 # unstage, keep the changes
git commit --amend                          # fix the last commit — only before pushing
git revert <hash>                           # undo a pushed commit with a new commit
git reset --hard origin/develop             # throw away everything local. Destructive
```

`git revert` is the one to reach for on a shared branch. `reset --hard` and `--amend` rewrite
history, which is fine on your own unpushed branch and a problem on `develop`.

### If you get stuck

```bash
git stash                                   # park your changes
git stash pop                               # get them back
git checkout -- .                           # discard everything uncommitted
git reflog                                  # every HEAD you have been at — recovers most mistakes
```

`git reflog` is the safety net. Almost nothing is truly lost for about ninety days.

### The pull request, without the CLI

If `gh` is not installed: push the branch, then open the repository on GitHub. It offers
"Compare & pull request" for a branch you have just pushed. Set the base branch yourself —
GitHub defaults to `main`, and day-to-day work goes into `develop`.

```bash
gh auth login                               # if you want the CLI
```

---

## Two rules worth keeping

**Never commit a credential.** `clinic.properties` ships with an empty password for exactly this
reason. If one does go in, changing it is not enough — it is in the history from then on, and it
has to be rotated at the provider.

**Never force-push a shared branch.** `--force` on `develop` or `main` rewrites what everybody
else has. On your own feature branch, before anybody has pulled it, use `--force-with-lease` and
it will refuse if somebody has.
