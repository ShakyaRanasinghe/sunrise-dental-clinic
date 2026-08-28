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

`prod` must stay code-only. When a `qa_vX.Y.Z` candidate passes QA, its code is
merged into `prod` (documentation excluded), the `vX.Y.Z` tag is placed on that
code-only commit, and the tag is pushed. The `Release` workflow
(`.github/workflows/release.yml`) reacts to any `v*` tag: it builds the WAR and
attaches it to the GitHub release with generated notes.

## Cutting a release (checklist)

1. On `dev`, make sure the working tree is clean and the branch is pushed.
2. Tag the candidate: `git tag -a dev_vX.Y.Z -m "Candidate deployment"` and
   `git push origin dev_vX.Y.Z`.
3. Bring `qa` up to the candidate (fast-forward or merge from `dev`), push `qa` —
   pushing `qa` triggers the **QA workflow** (build, tests, module boundaries,
   smoke test) automatically.
4. Tag the same commit on `qa`: `git tag -a qa_vX.Y.Z` and push the tag.
5. Open GitHub → Actions → the QA workflow and **confirm the run is green** on
   `qa` at that exact commit. If it is red: fix on `dev`, bump the number, and
   repeat from step 2.
6. Only when QA is green: merge `qa` into `prod`, but **exclude
   documentation** (docs never go to `prod`). Tag the code-only commit `vX.Y.Z`,
   push the tag — the Release workflow builds and attaches `clinic.war` to the
   GitHub release.
7. Write `vX.Y.Z.md` in this folder listing the features in the release, and add
   a row to the release table on this page. Commit and push on `dev` (and carry
   to `main`).
8. Deploy goes live on Render automatically when `prod` is pushed.

## Releases

| Version | Date | Candidate (QA) | Features |
|---------|------|----------------|----------|
| `v1.0.0` | 2026-08-28 | `qa_v1.0.0` | See `v1.0.0.md` |