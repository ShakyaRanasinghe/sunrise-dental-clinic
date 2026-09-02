# Releases

This folder documents **every production release** of the Sunrise Dental Clinic
application. Each release has one notes file (`vX.Y.Z.md`) that lists the
features that went live with it, and this page explains the tagging structure so
any developer can cut the next release.

## Tagging structure

Every release candidate and release is marked with a git tag so the exact code
that passed QA is reproducible forever. There are three kinds of tag:

| Tag pattern  | Branches it appears on          | Meaning |
|--------------|---------------------------------|---------|
| `dev_vX.Y.Z` | `dev`                           | A release candidate snapshot. `dev` carries the full tree — code **and** documentation — which is why docs travel in the dev tag. |
| `qa_vX.Y.Z`  | `qa`                            | The same candidate promoted to `qa` for automated QA. `qa` is always brought up to the candidate that `qa_vX.Y.Z` points at and the QA workflow is run against exactly that commit. |
| `vX.Y.Z`     | `prod`                          | **The official production release.** Tagged only after `qa_vX.Y.Z` passed QA, on the code-only `prod` tree (no documentation, per the docs-on-dev rule). |

### Version numbering

- The `dev_` / `qa_` tags share one incrementing number and simply **track the
  candidate**. They do not mean a release was shipped — they mean "this candidate
  was staged".
- Every time a candidate moves to QA, the number is consumed. If QA fails, fix on
  `dev`, bump the number, and re-stage: `dev_v1.0.0` → fail → `dev_v1.0.1` →
  `qa_v1.0.1` → pass.
- Only a passing candidate becomes a production release, and the production
  number is **independent**: the first shipped release is `v1.0.0`, regardless of
  how many dev/qa iterations led to it. If `qa_v1.0.1` passes, the release is
  still `v1.0.0`; if a second release ships later it is `v1.0.1`, and so on.

### How production releases get their code

`prod` must stay code-only. A `qa_vX.Y.Z` candidate passes QA, its code is
verified identical to what `prod` already ships, the `vX.Y.Z` tag is placed on
the code-only `prod` tree, and the tag is pushed. The `Release` workflow
(`.github/workflows/release.yml`) reacts to any `v*` tag: it builds the WAR and
attaches it to the GitHub release with generated notes. `prod` is never merged
from `qa`/`dev` — see Step 4.

## Cutting a release — manual runbook

Run these commands in a fresh clone, or after `git fetch origin` on an existing
one. Replace `X.Y.Z` with the candidate number on steps 1–4 and with the
production number on steps 5–6 (they only coincide on the very first release).

### Step 0 — preconditions

```bash
git fetch origin
git status --short        # must print nothing — working tree clean
```

### Step 1 — tag the candidate on `dev`

`dev` carries the full tree (code **and** docs), so the dev tag is the release
candidate in full.

```bash
git checkout dev
git pull --ff-only origin dev
git tag   -a dev_vX.Y.Z -m "Release candidate dev_vX.Y.Z (code + docs)"
git push origin dev_vX.Y.Z
```

Verify it points at the dev tip:

```bash
git rev-parse dev_vX.Y.Z^{}    # should equal: git rev-parse origin/dev
```

### Step 2 — promote the same candidate to `qa`

Bring `qa` up to the candidate, then push the branch. **Pushing `qa` triggers
the QA workflow** (build + tests + module boundaries + smoke test). The
branch-off may not fast-forward if `qa` has its own merge commits — a normal
merge is fine.

```bash
git checkout qa
git pull --ff-only origin qa
git merge --no-ff -m "Promote candidate dev_vX.Y.Z to qa" dev_vX.Y.Z
git push origin qa                         # starts the QA workflow
```

Confirm `qa` now has exactly the candidate's content, then tag it:

```bash
git rev-parse -q --verify qa^{tree} && git rev-parse dev_vX.Y.Z^{}^{tree}
# the two hashes above must be IDENTICAL before you tag
git tag -a qa_vX.Y.Z -m "QA candidate qa_vX.Y.Z — awaiting workflow result"
git push origin qa_vX.Y.Z
```

### Step 3 — wait for QA and act on the result

Open **GitHub → Actions → QA** and inspect the run for the commit `qa_vX.Y.Z`
(there is also a **Run workflow** button to re-trigger by hand). Green = proceed
to Step 4. Red = the candidate is rejected:

```bash
git checkout dev
# ... make the fix and commit it ...
git push origin dev
git tag -a dev_v1.0.1 -m "Release candidate dev_v1.0.1 (fix after failed QA)"
git push origin dev_v1.0.1
# return to Step 2 with X.Y.Z = 1.0.1
```

Version numbers on `dev`/`qa` tags are iteration counters, not release numbers:
bump the patch number **every time** a candidate moves to QA, whether or not the
previous one passed. Only when one of them is **green** do you cut the release.

### Step 4 — verify prod carries the passing candidate, then release

`prod` must stay code-only and is **never merged from `qa`/`dev`** — a merge only
produces modify/delete conflicts on the files prod intentionally does not carry
(workflows and documentation), and prod's deployable code is already identical to
the candidate. Instead, verify that identity, then tag prod as it is.

```bash
git checkout prod
git pull --ff-only origin prod
# prod must contain exactly: .gitignore, .github/workflows/release.yml,
# deploy, modular (incl. src/test), render.yaml
git ls-tree --name-only HEAD
```

Then check that every file prod ships is byte-identical to the passing candidate:

```bash
for f in $(git ls-tree -r HEAD --name-only); do
  git diff --quiet qa_vX.Y.Z^{} HEAD -- "$f" || echo "DIFFERS: $f"
done
# no "DIFFERS" lines means prod === the passed candidate, code-wise
git diff --quiet qa_vX.Y.Z^{} HEAD -- modular/src && echo "modular/src identical"
```

If anything differs, stop and reconcile it on `dev` before releasing — never tag
`prod` with code that did not pass QA.

### Step 5 — tag the production release and push

```bash
git tag -a vX.Y.Z -m "Release vX.Y.Z"
git push origin prod vX.Y.Z
```

Pushing the tag runs the **Release** workflow (`.github/workflows/release.yml`,
which **also lives on `prod`** — GitHub resolves the workflow from the tagged
commit itself, so it must be present at the commit `vX.Y.Z` points at). The
workflow first verifies that every file prod ships is byte-identical to the
latest `qa_v*` tag (docs/dev-only files absent from prod are ignored), then
builds `modular/`, runs the tests, and attaches `clinic.war` to the GitHub
release with auto-generated notes. Pushing a branch deploys to Render
automatically (a plain `git push origin prod` when the code changed) — after a
tag-only release the already-deployed code is unchanged, so no new deploy fires.

### Step 6 — record the release in the docs

Releases are documented on `dev` (and carried to `main`), never on `prod`:

1. Write `vX.Y.Z.md` in this folder (copy an older one and update the header
   fields) listing the features in the release.
2. Add a row to the Releases table below: `| vX.Y.Z | <date> | qa_<candidate> | See vX.Y.Z.md |`.
3. Commit and push, then carry the folder to `main`:

```bash
git checkout dev
# edit vX.Y.Z.md and the Releases table, then:
git add modular/docs/releases && git commit -m "docs(releases): record vX.Y.Z"
git push origin dev
git checkout main
git pull --ff-only origin main
git merge --no-ff -m "docs(releases): record vX.Y.Z" dev
git push origin main
git checkout dev
```

### Summary of the tag names

| Step | Branch | Tag | When |
|------|--------|-----|------|
| 1 | `dev` | `dev_vX.Y.Z` | candidate staged (code + docs) |
| 2 | `qa` | `qa_vX.Y.Z` | same candidate, QA running |
| 4–5 | `prod` | `vX.Y.Z` | **after QA green**, code-only |

## Releases

| Version | Date | Candidate (QA) | Features |
|---------|------|----------------|----------|
| `v1.0.0` | 2026-08-28 | `qa_v1.0.0` | See `v1.0.0.md` |
| `v1.0.1` | 2026-09-02 | `qa_v1.0.1` | See `v1.0.1.md` |