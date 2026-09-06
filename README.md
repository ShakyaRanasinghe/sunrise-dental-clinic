# Sunrise Dental Clinic — Appointment & Patient Management System

A role-based web application for a private dental clinic — patients book and pay for
visits online, reception runs the desk and billing, dentists work their schedule, and
the administrator reads reports and governs accounts. One shared source of information
instead of paper and spreadsheets.

> **Stack:** Java 17 · Jakarta Servlets & JSP · JDBC · MySQL 8 · HTML/CSS — no application
> framework. **Layout:** 3 tiers × 8 feature modules under [`modular/`](modular/).

| | |
|---|---|
| [Run it locally](modular/docs/local-setup.md) | One command, demo accounts, and what to check when it does not work |
| [What it does](modular/docs/testing/scenarios.md) | Plain-language walkthroughs — the fastest tour |
| [Architecture](modular/docs/architecture/) | Why the code is arranged this way, and the alternatives |
| [Requirements](modular/docs/srs/) | The SRS, with the verified status of every requirement |
| [API](modular/docs/api/) | Every JSON endpoint, with worked examples |
| [Deployment](modular/docs/deploy/) | Environments and the release runbook |

## Running it

**Prerequisites:** JDK 17, Maven 3.8+, Docker. No local MySQL or Tomcat needed.

```bash
./scripts/dev-up.sh          # database, schema, demo data, build, deploy
./scripts/smoke.sh           # end-to-end checks against the running instance
```

That is the whole thing: <http://localhost:8080>. See
[`modular/docs/local-setup.md`](modular/docs/local-setup.md) for details.

### Demo accounts

All use the password `Password123`. Staff sign in with usernames, the patient with email.

| Role | Sign in with |
|------|--------------|
| Administrator | `admin` |
| Receptionist | `reception` |
| Dentist | `silva` |
| Patient | `nimal@example.lk` |

## Testing

```bash
cd modular && mvn test      # JUnit 5 suite, no database required
./scripts/smoke.sh          # checks against a running instance
```
