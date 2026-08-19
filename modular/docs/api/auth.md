# Auth API

Sessions, self-registration and account lock-out. Conventions, the error envelope and the
role matrix are in [`README.md`](README.md).

| | |
|---|---|
| Servlet | `AuthApiServlet` |
| Mapping | `/api/auth/*` |
| Public prefix | Yes — `/api/auth/` bypasses the authentication filter, so sign-in is reachable |
| Requirements | FR-AUTH-01…06, NFR-SEC-01…05, FR-ADM-23 |

| Method | Path | Roles | Purpose |
|---|---|---|---|
| `POST` | `/api/auth/login` | any | Open a session |
| `POST` | `/api/auth/logout` | any signed in | Invalidate the session |
| `POST` | `/api/auth/register` | any | Create a patient account |
| `POST` | `/api/auth/unlock` | `ADMIN` | Clear a lock-out |
| `GET` | `/api/auth/me` | any signed in | The current principal |
| `GET` | `/api/auth/lock-status` | any *(gap — see README §5)* | Lock state for an email |

---

## POST /api/auth/login

Authenticates and establishes a session. The `Set-Cookie` on the response is what every
later call must send back.

**Request**

| Field | Type | Required | Notes |
|---|---|---|---|
| `email` | string | yes | Case-insensitive; lower-cased before lookup |
| `password` | string | yes | Compared against a PBKDF2 hash in constant time (NFR-SEC-02) |

```bash
curl -i -c jar.txt -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"reception@sunrisedental.lk","password":"Password123"}'
```

**`200 OK`**

```json
{
  "uid": "u-recep",
  "displayName": "Kumari Silva",
  "role": "RECEPTIONIST",
  "home": "/reception/home"
}
```

`home` is the route for the caller's role, so a client need not hold its own role-to-route
table (**FR-AUTH-04**). The four values are `/patient/home`, `/reception/home`,
`/dentist/schedule` and `/admin/reports`.

**`401` — wrong password**

```json
{
  "errorCode": "invalid_credentials",
  "message": "Incorrect email or password.",
  "lockStatus": { "locked": false, "attemptsRemaining": 4 }
}
```

**`401` — locked after five failures** (NFR-SEC-03)

```json
{
  "errorCode": "account_locked",
  "message": "This account is locked. Please contact the clinic to have it unlocked.",
  "lockStatus": { "locked": true, "attemptsRemaining": 0 }
}
```

The lock does **not** expire. It stands until an administrator clears it with
`POST /api/auth/unlock` — NFR-SEC-03. An earlier draft auto-unlocked after 24 hours and reported
`retryAfterSeconds`; both were dropped, because an account that quietly unlocks itself is a weaker
guarantee than one that does not, and a countdown told the caller to wait for something that never
arrives. `attemptsRemaining` counts down from 5 and resets on any successful sign-in.

**`400`** — a missing field:

```json
{ "errorCode": "bad_request", "message": "password is required" }
```

> **Note on the role-specific portals.** FR-AUTH-01 to FR-AUTH-03 specify four sign-in pages,
> one per role, each rejecting accounts of other roles. This endpoint is role-agnostic: it
> authenticates against the single credential store and reports the role it found. When the
> portals are built, the role check belongs in the page servlets, and a rejection for wrong
> role must be indistinguishable from a wrong password (**FR-AUTH-03**). Nothing about this
> endpoint changes.

---

## POST /api/auth/logout

Invalidates the session. Takes no body. Idempotent — calling it without a session succeeds.

```bash
curl -s -b jar.txt -X POST http://localhost:8080/api/auth/logout
```

**`200 OK`**

```json
{ "message": "Signed out" }
```

---

## POST /api/auth/register

Creates an account. **Always with role `PATIENT`**, whatever the request says — this is the
patient self-registration path (**FR-PAT-03**), and hardcoding the role is what keeps it safe.
A submitted `role` field is ignored, so this endpoint cannot be used to mint a `RECEPTIONIST`.

**Request**

| Field | Type | Required | Notes |
|---|---|---|---|
| `email` | string | yes | Must not already be registered |
| `password` | string | yes | Stored as PBKDF2, 120,000 iterations, per-user salt |
| `displayName` | string | yes | The person's name |

```bash
curl -s -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"kamal@example.lk","password":"CorrectHorse9","displayName":"Kamal Fernando"}'
```

**`201 Created`**

```json
{ "uid": "3f2a…", "email": "kamal@example.lk", "role": "PATIENT" }
```

**`400`** — already registered:

```json
{ "errorCode": "bad_request", "message": "An account already exists for kamal@example.lk" }
```

**One gap against the requirements.**

The hardcoded role is correct here and must stay. What is missing is the *other* path: there is
no endpoint an administrator can call to create a receptionist, dentist or administrator
account (**FR-ADM-20**), so those three exist only because `demo-data.sql` inserted them. That
belongs at `POST /api/accounts`, admin-only, with an explicit role, built on the same
`UserAccountFactory` (**FR-ADM-22**).

This endpoint also creates the `user_account` row alone. **FR-PAT-04** requires the `patient` profile
row in the same transaction — a registered patient currently has an account with no profile
until one is created separately, which is why `POST /api/appointments` can fail with
`"No patient profile for user …"`.

---

## POST /api/auth/unlock

Clears the failed-attempt counter and the lock. Administrator only (**FR-ADM-23**).

**Request**

| Field | Type | Required |
|---|---|---|
| `email` | string | yes |

```bash
curl -s -b admin-jar.txt -X POST http://localhost:8080/api/auth/unlock \
  -H 'Content-Type: application/json' -d '{"email":"nimal@example.lk"}'
```

**`200 OK`** — the resulting state, so a client need not re-query:

```json
{ "locked": false, "attemptsRemaining": 5 }
```

Unlocking an account that is not locked succeeds and returns the same shape — the operation
is idempotent by intent, since "make sure this account is usable" is the actual request.

**`403`** for any non-administrator.

---

## GET /api/auth/me

The current principal. The cheapest way for a client to discover whether its cookie is still
valid.

```bash
curl -s -b jar.txt http://localhost:8080/api/auth/me
```

**`200 OK`**

```json
{ "uid": "u-recep", "displayName": "Kumari Silva", "role": "RECEPTIONIST" }
```

**`401`** when the session has expired (30 minutes idle, NFR-SEC-05):

```json
{ "errorCode": "unauthenticated", "message": "Not signed in." }
```

---

## GET /api/auth/lock-status

Reports the lock state for an email address.

| Parameter | Type | Required |
|---|---|---|
| `email` | query string | yes |

```bash
curl -s -b jar.txt 'http://localhost:8080/api/auth/lock-status?email=nimal@example.lk'
```

**`200 OK`**

```json
{ "locked": true, "attemptsRemaining": 0 }
```

**Gap.** No role check. Any authenticated caller can probe any address and learn whether it
is a registered account under attack. It should require `ADMIN`.

---

## Security behaviour worth knowing

| Behaviour | Requirement |
|---|---|
| Passwords stored only as PBKDF2, 120,000 iterations, per-user salt | NFR-SEC-01 |
| Hash comparison is constant-time | NFR-SEC-02 |
| Lock after 5 consecutive failures, until an administrator unlocks | NFR-SEC-03 |
| Session cookie is `HttpOnly` | NFR-SEC-04 |
| Session expires after 30 minutes idle | NFR-SEC-05 |
| `password_hash` is never serialised by any endpoint | FR-ADM-25 |
| Failed sign-in is logged server-side with the email and attempts remaining | — |

Lock state is held **in memory**, not in the database, despite `user_account` carrying
`failed_attempts` and `locked` columns. A server restart therefore clears every lock. Worth
knowing before relying on lock-out as a defence.
