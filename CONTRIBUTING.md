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

Scopes are the eight feature modules, plus the cross-cutting ones:
`platform`, `access`, `patients`, `scheduling`, `appointments`, `billing`, `reporting`,
`feedback`, and `docs`, `uml`, `ci`, `dev`.

A commit should touch one module. Where it cannot — a change to a shared response record,
say — that is worth a sentence in the message explaining why, because a commit spanning
three modules is usually a sign the change belongs in the platform.

## Versioning

Semantic versioning via git tags. `version.json` at the repo root holds the current version;
each release is tagged (`vMAJOR.MINOR.PATCH`) with a matching `CHANGELOG.md` entry.

## Running tests

```bash
cd modular && mvn test    # 266 tests, no database required (in-memory adapters)
```

And against a running instance:

```bash
./scripts/dev-up.sh       # database, schema, demo data, build, deploy
./scripts/smoke.sh        # 53 end-to-end checks
```

Run the second one before opening a pull request that touches a servlet, a JSP, a DAO or the
schema. The unit tests sit below the web tier and cannot see a servlet mapping, a JSP that
will not compile, a statement a trigger refuses, a timezone or a charset — and every defect
found late in this project was one of those.

## The two folders

Work in **`modular/`**. `layered/` is the earlier arrangement, kept as the source the
restructure copied from and as the git history; it is not run, maintained or deployed, and CI
does not build it.

## The boundaries CI enforces

Three checks run on every pull request, and each one exists because it was violated:

```bash
# the web tier must not reach the data tier — both greps must print nothing
grep -rl  'import com.sunrise.clinic.[a-z]*\.data' --include='*.java' \
  modular/src/main/java/com/sunrise/clinic/*/web/
grep -rnE 'app\(\)\.[a-z]+(Repository|Dao)\(\)'  --include='*.java' \
  modular/src/main/java/com/sunrise/clinic/*/web/

# patients must not import appointments — see TreatmentRelationship for why
grep -rn 'import com.sunrise.clinic.appointments' \
  modular/src/main/java/com/sunrise/clinic/patients/
```

The second grep is the one that matters: an import check alone found two of the twelve
violations the earlier arrangement had, because `app().patients().search(term)` names no type
to import.
