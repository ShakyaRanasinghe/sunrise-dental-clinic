# Deployment

How the application is put in front of users, and why it is shaped this way.

| Document | What it answers |
|---|---|
| [`1-architecture.md`](1-architecture.md) | What runs where, and what talks to what |
| [`2-environments.md`](2-environments.md) | What differs between a developer's machine and a clinic |
| [`3-runbook.md`](3-runbook.md) | Deploy it, roll it back, and what to check afterwards |

---

## The shape of it in one paragraph

One WAR on one Tomcat, talking to one MySQL. No load balancer, no cache, no message broker, no
container orchestrator, no front-end build. The application renders its own pages on the server,
so there is nothing to deploy to a CDN and no second thing that can be a version behind the
first. A single dental practice with a handful of staff generates a few hundred requests a day;
anything more than this would be infrastructure bought to look serious.

That is a decision, not an omission, and [`1-architecture.md`](1-architecture.md) sets out what
it costs and when it stops being the right answer.
