# Authentication

Why a session and not a token, and what the session already gives us.

The four role portals and the class hierarchy behind them are in
[`../class-diagram.md`](../class-diagram.md) §3; the behaviour is drawn in
[`../sequence-diagrams.md`](../sequence-diagrams.md) §1.

---

## Session only

**`HttpSession` from the Servlet API. No tokens, no JWT, no extra tables.**

I proposed a JWT layer earlier and I was overcomplicating it. For this application a session is
both simpler and better:

| | Session | JWT |
|---|---|---|
| Sent by the browser on a plain link or form | automatically | only if JavaScript attaches it — and there is no client JavaScript |
| Sign-out takes effect | immediately | not until the token expires, unless you add a revocation table |
| Locking an account cuts it off | immediately | same problem |
| Extra code | none — it is in the Servlet API | signer, token service, two tables |

JWT is the right answer when a third-party client calls your API without a cookie jar. Nothing
does that here, so it would be machinery serving a hypothetical. If that changes, it is one
filter and one endpoint added later — the design below does not have to move.

What the session gives us, all already required by the SRS:

| Property | Requirement |
|---|---|
| PBKDF2 hash, 120,000 iterations, per-user salt | NFR-SEC-01 |
| Constant-time comparison | NFR-SEC-02 |
| Lock after 5 failed attempts | NFR-SEC-03 |
| `HttpOnly` cookie, `SameSite=Strict` | NFR-SEC-04 |
| 30-minute idle timeout | NFR-SEC-05 |
| CSRF token per form, held in the session | NFR-SEC-07 |

`AuthenticationFilter` maps to `/*`, reads the session, and puts a `ClinicPrincipal` on the
request. One class, and every servlet downstream reads the principal without knowing how it got
there.

---
