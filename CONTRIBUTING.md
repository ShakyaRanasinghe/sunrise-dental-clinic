# Contributing & Development Workflow

This project uses a lightweight, professional Git workflow so that changes are small,
reviewed, tested, and traceable.

## Branch strategy

```
feature/*   →   develop   →   qa   →   main
   │              │            │         │
 work on a      dev env      qa env    production
 section                     (full     (deployed
                             test run)  release)
```

- **`main`** — production. Always green. Tagged releases (`v0.1.0` … `v1.0.0`) are cut here.
- **`qa`** — release-candidate integration; the full test suite runs here.
- **`develop`** — day-to-day integration branch.
- **`feature/*`, `chore/*`, `fix/*`** — one branch per issue/section.

## Working on an issue

1. **File an issue** describing the goal, scope, and acceptance criteria.
2. **Branch** from `develop` (or `main` for infra): `git checkout -b feature/<short-name>`.
3. **Commit** in small, conventional commits: `feat(scope): …`, `fix(scope): …`, `docs: …`.
4. **Open a PR** into `develop` (or `main`), referencing the issue with `Closes #N`.
5. **CI** builds and runs the tests automatically. It must be green to merge.
6. **Merge** once reviewed and green; delete the branch.

## Commit convention

[Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <description>
```

Types: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `ci`.
Scopes: `backend`, `frontend`, `functions`, `uml`, `docs`, `ci`.

## Versioning

Semantic versioning via git tags. `version.json` at the repo root holds the current version;
each release is tagged (`vMAJOR.MINOR.PATCH`) with a matching `CHANGELOG.md` entry.

## Running tests

```bash
cd backend && mvn test    # 15 tests, no cloud account required (in-memory adapter)
```
