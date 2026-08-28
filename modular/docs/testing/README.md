# Testing

Three suites, deliberately different in kind. None of them replaces the others.

| Suite | What it is | Run it |
|---|---|---|
| **Unit tests** | 266 JUnit 5 tests below the web tier, against in-memory adapters | `cd modular && mvn test` |
| **Smoke test** | 53 checks driving the assembled application over HTTP | `./scripts/smoke.sh` |
| **[Scenarios](scenarios.md)** | 50 walkthroughs in plain language, followed by a person | by hand, in a browser |

## Why three

**The unit tests** are fast and precise, and they are where the rules live: the status machines,
the revenue split invariant across 49 combinations of its dials, the confidentiality gates, the
concurrency guard firing twelve simultaneous bookings at one slot. They need no database and
finish in seconds, so they run on every save.

They cannot see anything that only exists once the application is assembled. **Every defect
found late in this project was of that kind:**

| Defect | Only visible when |
|---|---|
| A `BEFORE INSERT` trigger made every appointment update fail | deployed against MySQL |
| A wrong view name turned every page error into a 404 | a page error actually occurred |
| Dates were computed in UTC for a clinic at +05:30 | a report was read after 18:30 local |
| A form posted to the JSP's own path | a browser submitted it |
| Seed data was double-encoded | the application rendered a note |
| Appointment numbers restarted from 1 | Tomcat restarted |

**The smoke test** exists for exactly those. It starts from a fresh database, signs in as all
four roles, and walks the clinic — publish, book, treat, bill, print — asserting the boundaries
at each step. It would have caught all six.

**The scenarios** exist because a script asserts a status code and a person notices that the
message does not answer the question, the important note is third in the list, or the page looks
broken. They are also the thing to hand somebody who has never seen the application and wants to
know what it does.

## When to run which

| After changing | Run |
|---|---|
| a service, a domain rule | the unit tests |
| a servlet, a JSP, a DAO, the schema, `web.xml` | the unit tests **and** the smoke test |
| anything a user would notice | the relevant scenario group as well |
| before submitting or releasing | all three, from a fresh `./scripts/dev-up.sh` |

Group **G** of the scenarios is the regression list — ten defects that were real, each with the
symptom to look for. Walk it after any change to a servlet, a JSP, a DAO or the schema.
