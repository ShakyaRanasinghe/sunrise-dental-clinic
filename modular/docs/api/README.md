# Web Service API

JSON web service for the Sunrise Dental Clinic system. This is the tier that makes the
application distributed: the same operations the screens perform are available to a
programmatic caller over HTTP.

Satisfies **FR-WS-01** to **FR-WS-04** in [`../srs.md`](../srs/srs.md).

| | |
|---|---|
| Style | Resource-oriented JSON over HTTP |
| Base path | `/api` |
| Content type | `application/json;charset=UTF-8` |
| Authentication | Session cookie, established by `POST /api/auth/login` |
| Implementation | 5 Jakarta servlets, hand-written JSON — no framework (CON-01) |

## Resources

| Document | Resource | Endpoints |
|---|---|---|
| [`auth.md`](auth.md) | Sessions and accounts | 6 |
| [`appointments.md`](appointments.md) | Booking, cancellation, completion, bills | 6 |
| [`availability.md`](availability.md) | Sessions and open slots | 3 |
| [`patients.md`](patients.md) | The patient register and medical notes | 3 + 4 specified |
| [`reference.md`](reference.md) | Dentists and treatments | 2 |
| [`complaints.md`](complaints.md) | Patient concerns about a dentist | 4 specified |
| [`reviews.md`](reviews.md) | Dentist ratings, and the aggregate | 5 specified |

**20 endpoints implemented.** One further endpoint is specified but absent, and eight defects
were found while writing these documents — five of them verified against a running instance.
See [Known gaps](#known-gaps); the first one blocks the booking flow entirely.

---

## Conventions

### Authentication

Every endpoint except `POST /api/auth/login` and `POST /api/auth/register` requires an
authenticated session. `AuthenticationFilter` runs on `/*` and establishes the caller's
identity from the session cookie before any servlet sees the request.

Authenticate once, then reuse the cookie:

```bash
# Sign in and keep the session cookie
curl -s -c jar.txt -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"reception@sunrisedental.lk","password":"Password123"}'

# Every later call sends it back
curl -s -b jar.txt http://localhost:8080/api/treatments
```

An unauthenticated call to `/api/**` returns `401` with a JSON body rather than a redirect,
because a redirect to an HTML sign-in page is useless to a caller expecting JSON
(**FR-AUTH-05**):

```json
{ "errorCode": "unauthenticated", "message": "Authentication is required." }
```

### Authorisation

Role is checked per endpoint, after authentication. Each resource document states the roles
its endpoints accept. A caller authenticated as the wrong role gets `403`:

```json
{ "errorCode": "forbidden", "message": "Your role (PATIENT) does not permit this action." }
```

### Error envelope

Every failure returns the same two-field shape. There is no third form.

```json
{ "errorCode": "slot_unavailable", "message": "That slot has just been taken." }
```

| Status | `errorCode` | Cause |
|---|---|---|
| `400` | `bad_request` | Missing or malformed field, unknown endpoint |
| `401` | `unauthenticated` | No session, or the session expired |
| `401` | `invalid_credentials` | Sign-in failed |
| `401` | `account_locked` | Five consecutive failures; an administrator must unlock |
| `403` | `forbidden` | Authenticated, but the role does not permit the action |
| `404` | `not_found` | The resource does not exist, **or exists and is not yours** |
| `409` | `slot_unavailable` | The slot was taken between reading and writing |
| `500` | `internal_error` | Unexpected fault. The message is deliberately generic; details go to the server log only |

**`404` covers two cases on purpose.** Asking for another patient's appointment returns "not
found", never "exists but forbidden" — a distinction would confirm the appointment exists.

### Success codes

| Status | Used for |
|---|---|
| `200` | A successful read, or a state change that returns the updated resource |
| `201` | A resource was created — booking, registration, publishing a session, issuing a bill |

### Serialisation

Hand-written serialiser (`Json`), so the rules are worth stating exactly:

| Java type | JSON form | Example |
|---|---|---|
| `LocalDate` | ISO-8601 string | `"2026-08-20"` |
| `LocalTime` | ISO-8601 string | `"09:30"` |
| `enum` | its name, unquoted case preserved | `"CONFIRMED"` |
| `double` | number, no currency symbol or separator; a whole value carries no `.0` | `5200` |
| `null` | `null`, the field is present | `"diagnosis": null` |

Money crosses the wire as a bare number. Formatting — thousands separator, two decimal
places — belongs to the presentation tier (**FR-UI-06**), not the API.

### Dates and times

All dates are `yyyy-MM-dd`, all times `HH:mm`. No offset is carried: the clinic operates in
one time zone (**ASM-01**). A malformed date is rejected before any query runs:

```json
{ "errorCode": "bad_request", "message": "date must be a date in yyyy-MM-dd format" }
```

---

## Role access at a glance

| Endpoint | Patient | Reception | Dentist | Admin |
|---|:--:|:--:|:--:|:--:|
| `POST /api/auth/login` | ● | ● | ● | ● |
| `POST /api/auth/logout` | ● | ● | ● | ● |
| `POST /api/auth/register` | ● | ● | ● | ● |
| `GET /api/auth/me` | ● | ● | ● | ● |
| `GET /api/auth/lock-status` | ● | ● | ● | ● |
| `POST /api/auth/unlock` | | | | ● |
| `POST /api/appointments` | ● | ● | | ● |
| `GET /api/appointments/{no}` | own | ● | own | ● |
| `POST /api/appointments/{no}/cancel` | own | ● | | ● |
| `POST /api/appointments/{no}/complete` | | | ● | |
| `POST /api/appointments/{no}/bill` | | ● | | ● |
| `GET /api/appointments/{no}/bill` | ● | ● | ● | ● |
| `GET /api/availability` | ● | ● | ● | ● |
| `GET /api/availability/week` | ● | ● | ● | ● |
| `POST /api/sessions` | | ● | | ● |
| `GET /api/patients?q=` | | ● | | ● |
| `GET /api/patients/{id}` | own | ● | ● | ● |
| `POST /api/patients` | ● | ● | ● | ● |
| `GET /api/patients/{id}/notes` | own | | treating | |
| `POST /api/patients/{id}/notes` | own | | | |
| `PUT /api/patients/{id}/notes/{noteId}` | own | | | |
| `DELETE /api/patients/{id}/notes/{noteId}` | own | | | |
| `GET /api/complaints` | own | | | ● |
| `POST /api/complaints` | own | | | |
| `GET /api/complaints/{id}` | own | | | ● |
| `PATCH /api/complaints/{id}` | | | | ● |
| `POST /api/appointments/{no}/review` | own | | | |
| `GET /api/appointments/{no}/review` | own | | | ● |
| `PUT /api/appointments/{no}/review` | own | | | |
| `GET /api/dentists/{id}/rating` | | | own | ● |
| `GET /api/reviews?dentistId=` | | | | ● |
| `GET /api/dentists` | ● | ● | ● | ● |
| `GET /api/treatments` | ● | ● | ● | ● |

● = permitted · *own* = restricted to the caller's own records · *treating* = only for a patient
on the calling dentist's own schedule · blank = `403`

The rows shaded by their oddity are documented as gaps below: `POST /api/auth/register`,
`GET /api/auth/lock-status`, `GET /api/appointments/{no}/bill` and `POST /api/patients` are
open to more roles than the requirements intend.

---

## Known gaps

Recorded here rather than left for a reader to discover. Each is a defect against a stated
requirement, not a design choice.

### 1. Five endpoints return `toString()` instead of JSON

**The most severe defect found, and it blocks the API's main purpose.** Every endpoint that
returns a **domain object** rather than a DTO record serialises as a quoted `toString()`,
because `Json.write` has a branch for `isRecord()` and falls through to
`String.valueOf(value)` for everything else.

| Endpoint | Returns | Actual response |
|---|---|---|
| `GET /api/dentists` | `List<Dentist>` | `["Dentist{id=d-silva}", …]` |
| `GET /api/treatments` | `List<Treatment>` | `["Treatment{id=t-checkup}", …]` |
| `GET /api/patients?q=` | `List<Patient>` | `["Patient{id=p-nimal}"]` |
| `GET /api/patients/{id}` | `Patient` | `"Patient{id=p-nimal}"` |
| `POST /api/patients` | `Patient` | `"Patient{id=af980578-…}"` |

All five answer `200`/`201`, so nothing looks broken to a monitor. No name, no contact number,
no specialization, no consultation fee, no treatment cost crosses the wire — only ids.

A booking client cannot function: it must offer the patient a dentist and a priced treatment
(**FR-PAT-10**, **FR-PAT-12**) and there is no call that turns `"Dentist{id=d-silva}"` into
"Dr Ranil Silva, General Dentistry, Rs 1,500".

Appointments, bills and slots are unaffected — they are mapped to DTO records first, which is
exactly the fix these five need. Details in [`reference.md`](reference.md) and
[`patients.md`](patients.md).

### 2. `GET /api/appointments` does not exist

The role documents in [`../srs.md`](../srs/srs.md) list a scoped list endpoint. It is not
implemented: `AppointmentApiServlet.doGet` handles only a single appointment number and the
`/bill` sub-path, so a request with no path falls through to `IllegalArgumentException` and
returns:

```json
{ "errorCode": "bad_request", "message": "Unknown endpoint" }
```

A caller has no way to list appointments — every screen that shows a list reads the
repository directly instead. This is the single largest hole in the web-service tier: the
API cannot answer "what is booked?", which is the clinic's commonest question.

### 3. `POST /api/patients` has no role check

`PatientApiServlet.doPost` calls `register` with no `AccessControl.require`, so **any**
authenticated caller — a patient included — can create a patient record. Two consequences:

- It contradicts **FR-REC-21**, which places walk-in registration with reception
- The new record is linked to the caller's own uid, so a patient can create a second
  `patient` row pointing at their own account, breaking **ASM-03** and making
  `findByUserUid` ambiguous

Fix: require `RECEPTIONIST` or `ADMIN` on this endpoint. Patient self-registration already has
its own path at `POST /api/auth/register`, which is where a patient creating their own record
belongs.

### 4. No endpoint creates a staff account

`POST /api/auth/register` hardcodes `Role.PATIENT`, which is **correct** — it is the patient
self-registration path and a request-supplied role there would be a privilege-escalation hole
(**FR-PAT-03**).

The gap is that no second path exists. An administrator has no endpoint and no screen for
creating a receptionist, dentist or administrator account, so all three exist only because
`demo-data.sql` inserted them (**FR-ADM-20**). `POST /api/accounts`, admin-only, would close
it.

### 5. `GET /api/auth/lock-status` is readable by any role

It takes an arbitrary email and reports whether that account is locked and how many attempts
remain. Any authenticated caller can probe any address. **FR-ADM-23** intends this for
administrators.

### 6. `GET /api/appointments/{no}/bill` is not scoped

Unlike `GET /api/appointments/{no}`, which narrows the response for callers without clinical
access, the bill endpoint performs no ownership check. Any authenticated caller who knows an
appointment number can read its bill, including the amounts credited to each party. It should
be restricted to the patient it belongs to, reception and admin.

### 7. No CSRF protection

State-changing endpoints accept a request carrying only the session cookie, so a cross-site
form post from a page the user visits while signed in would succeed (**NFR-SEC-07**).

### 8. Requests and responses were undocumented

**FR-WS-04** required worked examples. These documents are that requirement being met.

---

## Trying it end to end

The seeded accounts all use the password `Password123`. Full walkthroughs are in each
resource document; the shortest useful sequence is:

```bash
BASE=http://localhost:8080

# 1. Reception signs in
curl -s -c jar.txt -X POST $BASE/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"reception@sunrisedental.lk","password":"Password123"}'

# 2. Publish tomorrow's availability for Dr Silva
curl -s -b jar.txt -X POST $BASE/api/sessions -H 'Content-Type: application/json' \
  -d '{"dentistId":"d-silva","date":"2026-08-20","startTime":"09:00","endTime":"12:00","slotMinutes":30}'

# 3. Book the first open slot for an existing patient
curl -s -b jar.txt -X POST $BASE/api/appointments -H 'Content-Type: application/json' \
  -d '{"patientId":"p-nimal","slotId":"d-silva_2026-08-20_09:00","treatmentId":"t-scaling"}'

# 4. Issue the bill
curl -s -b jar.txt -X POST $BASE/api/appointments/APT-20260820-0001/bill
```

Step 4 currently succeeds on a `CONFIRMED` appointment. **FR-BIL-05** requires completion
first; that is listed as *Partial* in the SRS.
