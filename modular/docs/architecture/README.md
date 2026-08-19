# Architecture

How the **modular** implementation is put together, and why it is put together that way.

| Document | Answers |
|---|---|
| [`1-alternatives.md`](1-alternatives.md) | What other architectures exist, and why this one was chosen over each |
| [`2-structure.md`](2-structure.md) | The three tiers, the eight modules, and the directory layout |
| [`3-growth.md`](3-growth.md) | How a developer finds their way as features are added |
| [`4-database.md`](4-database.md) | Where MySQL sits and how it is reached |
| [`5-security.md`](5-security.md) | Authentication, and why it is a session rather than a token |

Implementation sequencing is in [`../migration-plan.md`](../migration-plan.md); the servlet and
route contract is [`../servlets.md`](../servlets.md).

Aligns with [`../srs/srs.md`](../srs/srs.md), [`../class-diagram.md`](../class-diagram.md),
[`../er-diagram.md`](../er-diagram.md), [`../use-case-diagram.md`](../use-case-diagram.md) and
[`../sequence-diagrams.md`](../sequence-diagrams.md).

---

## The shape, in one picture

**A grid: three tiers by eight modules. Every class has exactly one cell.**

```
            access  patients  scheduling  appointments  billing  notif.  reporting  feedback
  web/        ·        ·          ·            ·           ·        ·        ·         ·    ← Presentation
  service/    ·        ·          ·            ·           ·        ·        ·         ·    ← Business
  data/       ·        ·          ·            ·           ·        ·        ·         ·    ← Data
  domain/     ·        ·          ·            ·           ·        ·        ·         ·
```

Tiers run **horizontally** — that is the three-tier architecture the assessment asks for. Modules
run **vertically** — that is what makes one feature readable in one place. The design has both,
because neither substitutes for the other: tiers alone scatter a feature across six packages,
modules alone lose the separation of concerns.

Two coordinates locate any class: *which capability*, and *which tier*.

---

## The stack

| Layer | Technology | Why |
|---|---|---|
| Language | Java 17 | Records, sealed types, pattern matching — used, not decorative |
| Web interaction | **Jakarta Servlet 6.0** | CON-02. Every request enters through a servlet |
| Views | JSP 3.1 + JSTL 3.0 | Server-rendered. No client-side framework, no bundler, no client JS |
| Persistence | **Plain JDBC** | CON-03. No ORM |
| Database | **MySQL 8.0**, InnoDB | Transactions and row locks — the double-booking guard needs both |
| Container | Tomcat 10.1+ | Servlet 6.0 requires the Jakarta namespace |
| Build | Maven, packaged as a WAR | A build tool is not an application framework (CON-04) |
| Tests | JUnit 5 | Same reasoning |

---

## No frameworks

| Layer | What we use | What we are **not** using |
|---|---|---|
| Web | Jakarta Servlet + JSP + JSTL | Spring, Spring MVC, JAX-RS |
| Persistence | Plain JDBC, `PreparedStatement` | Hibernate, JPA, MyBatis, jOOQ |
| Views | Server-rendered JSP | React, Vue, Thymeleaf, any template engine |
| Wiring | `AppContext`, written by hand | Spring DI, CDI, Guice |
| JSON | `Json`, written by hand | Jackson, Gson |
| Security | `HttpSession` + PBKDF2, written by hand | Spring Security, Shiro, any JWT library |
| Cloud | none | Firebase, Firestore |

The complete dependency list — **five entries**, three of them supplied by the container or the
test runner:

```xml
jakarta.servlet-api          provided by Tomcat
jakarta.servlet.jsp-api      provided by Tomcat
jakarta.servlet.jsp.jstl     tag library, not a framework
mysql-connector-j            the JDBC driver
junit-jupiter                test scope only
```

Maven and JUnit are a build tool and a test library, not application frameworks. Everything the
application does at runtime is written in this project.

---

## The four properties this shape is chosen for

Each is a consequence of a structural decision, not of a framework. Argued in full in
[`3-growth.md`](3-growth.md); in summary:

| Property | What delivers it |
|---|---|
| **Clear to read** | Two coordinates locate any class. One feature lives in one directory |
| **Clear to find bugs** | Each tier fails in its own way, so a symptom names a tier before you open a file |
| **Clear to plug into** | Five named interfaces, plus "add a module" as the sixth extension point |
| **Clear to scale** | An acyclic module graph for code; a near-stateless WAR and a separate database for load |
