# Patients API

The patient register — searching it, reading one record, adding a walk-in. Conventions, the
error envelope and the role matrix are in [`README.md`](README.md).

| | |
|---|---|
| Servlet | `PatientApiServlet` |
| Mapping | `/api/patients/*` |
| Requirements | FR-REC-20…22, FR-PAT-04, FR-PAT-06, ASM-03, ASM-04 |

| Method | Path | Roles | Purpose |
|---|---|---|---|
| `GET` | `/api/patients?q=` | `RECEPTIONIST` `ADMIN` | Search or list the register |
| `GET` | `/api/patients/{id}` | self, `RECEPTIONIST` `ADMIN` `DENTIST` | One patient record |
| `POST` | `/api/patients` | **any signed in** *(defect — see below)* | Add a patient |
| `GET` | `/api/patients/{id}/notes` | the patient themselves; a dentist treating them | Medical notes |
| `POST` | `/api/patients/{id}/notes` | the patient themselves only | Add a note |
| `PUT` | `/api/patients/{id}/notes/{noteId}` | the patient themselves only | Edit a note |
| `DELETE` | `/api/patients/{id}/notes/{noteId}` | the patient themselves only | Delete a note |

> ## ⚠ This resource does not currently return usable JSON
>
> All three endpoints serialise the `Patient` **domain object** rather than a response record,
> and the hand-written serialiser has no case for a plain class. It falls through to
> `String.valueOf(value)`, so every response is a quoted `toString()`.
>
> Verified against a running instance:
>
> ```text
> GET /api/patients?q=Perera   200  ["Patient{id=p-nimal}"]
> GET /api/patients/p-nimal    200  "Patient{id=p-nimal}"
> POST /api/patients           201  "Patient{id=af980578-9186-438c-9d2d-1e8a8e878306}"
> ```
>
> Not one field of patient data crosses the wire. No name, no contact number, no address —
> exactly the fields **FR-REC-20** exists to return. The endpoints answer `200`, so nothing
> looks broken to a monitor, and a client receives a string it cannot parse into anything.
>
> **Why the other resources are fine.** `Json.write` has a branch for
> `value.getClass().isRecord()`, which reflects over the record's components. Appointments,
> bills and slots all serialise correctly because they are mapped to DTO **records** first.
> `Patient` is a mutable class with a builder, and `Patient.toString()` returns only
> `"Patient{id=" + id + "}"`.
>
> **The fix** is a `PatientResponse` record and a `toPatientResponse` mapper method, matching
> how every other resource is handled. Documented below is the shape it should return, marked
> as such — everything else in this file describes behaviour that does work.

---

## The intended response shape

Not yet implemented. This is what a `PatientResponse` record should carry, from the columns
that exist:

```json
{
  "id": "p-nimal",
  "name": "Nimal Perera",
  "address": "14 Galle Road, Colombo 03",
  "contactNumber": "0771234567",
  "email": "nimal@example.lk",
  "dob": "1988-04-12",
  "hasPortalAccount": true
}
```

`hasPortalAccount` is derived from whether `user_uid` is set, not exposed raw. Reception needs
to know whether a patient can self-serve (**FR-REC-22**); it does not need the uid itself, and
publishing internal account identifiers to a screen invites their use as a parameter.

---

## Medical notes

**Specified, not yet implemented.** A patient records their own medical background — allergies,
medications, conditions — from their profile screen after signing in. Never at registration
(**FR-NOTE-02**).

### PatientNoteResponse

```json
{
  "id": "n-8c31",
  "category": "ALLERGY",
  "detail": "Penicillin — rash and swelling. Confirmed by GP.",
  "critical": true,
  "createdAt": "2026-07-02T09:14:00Z",
  "updatedAt": "2026-07-02T09:14:00Z"
}
```

`category` is one of `ALLERGY`, `MEDICATION`, `CONDITION`, `OTHER`. `critical` means a dentist
must see it before treating (**FR-NOTE-04**).

Note what the shape does **not** carry: no `patientId`, because it is already in the path, and no
author field — the owning patient *is* the author. A dentist's clinical opinion is
`appointment.diagnosis`, a different field on a different entity.

### GET /api/patients/{id}/notes

```bash
curl -s -b jar.txt http://localhost:8080/api/patients/p-nimal/notes
```

**`200 OK`** — an array, criticals first:

```json
[
  { "id": "n-8c31", "category": "ALLERGY", "detail": "Penicillin — rash and swelling.",
    "critical": true, "createdAt": "2026-07-02T09:14:00Z", "updatedAt": "2026-07-02T09:14:00Z" },
  { "id": "n-2a55", "category": "CONDITION", "detail": "Type 2 diabetes, diet controlled.",
    "critical": false, "createdAt": "2026-07-02T09:20:00Z", "updatedAt": "2026-07-02T09:20:00Z" }
]
```

**Who may call it.** The patient whose notes they are, and a dentist **with that patient on their
own schedule** (**FR-NOTE-09**). Not reception, not the administrator, and not a dentist who has
never treated them.

**`404`** — for a patient the caller is not treating. Not `403`, because a `403` would confirm
the patient exists and has an id worth guessing.

**`200` with `[]`** — the patient has declared nothing. A client must say so explicitly
(**FR-NOTE-12**).

### POST /api/patients/{id}/notes

| Field | Type | Required | Notes |
|---|---|---|---|
| `category` | string | yes | One of the four values |
| `detail` | string | yes | 3–1000 characters |
| `critical` | boolean | no | Defaults to `false` |

```bash
curl -s -b patient-jar.txt -X POST http://localhost:8080/api/patients/p-nimal/notes \
  -H 'Content-Type: application/json' \
  -d '{"category":"ALLERGY","detail":"Penicillin — rash and swelling.","critical":true}'
```

**`201 Created`** — the created `PatientNoteResponse`.

**`403`** — a dentist attempting to write. Dentists read notes; they do not author them
(**FR-NOTE-10**, **FR-DEN-45**). The patient owns their own record, and keeping the two authors
apart is what makes each trustworthy.

**`400`** — validation:

```json
{ "errorCode": "bad_request", "message": "category must be one of ALLERGY, MEDICATION, CONDITION, OTHER" }
```

### PUT /api/patients/{id}/notes/{noteId}

Same body as `POST`. Patient only. **`200 OK`** with the updated note; `updatedAt` moves, which is
what the dentist's screen shows so a five-year-old declaration is distinguishable from last
week's (**FR-DEN-46**).

### DELETE /api/patients/{id}/notes/{noteId}

Patient only. **`204 No Content`**. Deleting a note removes it from every future appointment view;
past `audit_event` rows record that it existed and was read, without reproducing its content.

---

## GET /api/patients

Searches the register, or lists all of it when `q` is absent.

| Parameter | Type | Required | Notes |
|---|---|---|---|
| `q` | query | no | Partial match on name, contact number or email. Blank or absent lists everyone |

```bash
curl -s -b jar.txt 'http://localhost:8080/api/patients?q=Perera'
curl -s -b jar.txt 'http://localhost:8080/api/patients'          # everyone
```

**`200 OK`** — a JSON array. One search field covers all three columns, so the front desk types
whatever the patient offers — a surname, a phone number — without choosing a field first
(**FR-REC-20**).

**`403`** — verified for a patient caller:

```json
{ "errorCode": "forbidden", "message": "Your role (PATIENT) does not permit this action." }
```

**No pagination.** An empty `q` returns every patient in one response. Fine for a single
practice; it will not stay fine, and there is no limit parameter to reach for when it stops
being fine.

---

## GET /api/patients/{id}

One patient by id.

```bash
curl -s -b jar.txt http://localhost:8080/api/patients/p-nimal
```

**`200 OK`** — the patient record.

**Access rule.** `AccessControl.requireSelfOr(user, patient.getUserUid(), RECEPTIONIST, ADMIN,
DENTIST)` — the caller passes if they *are* this patient, or hold one of the three staff roles.
A patient reading their own record is permitted; a patient reading another's is not.

Note that **any** dentist may read **any** patient record here, not only patients on their own
schedule. That is looser than [`srs-dentist.md`](../srs/srs-dentist.md) intends, where a dentist
reaches patient details through their own appointments.

**`404`**

```json
{ "errorCode": "not_found", "message": "Patient not found: p-nobody" }
```

**`403`** — a patient requesting another patient's record. Note this differs from the
appointment read path, which returns `404` to avoid confirming existence (**FR-PAT-23**). Here
a `403` confirms the id is real.

---

## POST /api/patients

Adds a patient. Intended for reception registering a walk-in (**FR-REC-21**) — someone treated
without a portal account (**ASM-04**).

**Request**

| Field | Type | Required | Notes |
|---|---|---|---|
| `name` | string | yes | |
| `contactNumber` | string | yes | |
| `address` | string | no | |
| `email` | string | no | Validated for form if present |
| `dob` | string | no | `yyyy-MM-dd` |

```bash
curl -s -b jar.txt -X POST http://localhost:8080/api/patients \
  -H 'Content-Type: application/json' \
  -d '{"name":"Sunil Bandara","contactNumber":"0761112223","address":"5 Lake Road, Kandy"}'
```

**`201 Created`** — the created patient, `id` a fresh UUID.

**`400`** — a missing required field:

```json
{ "errorCode": "bad_request", "message": "contactNumber is required" }
```

### Two verified defects

**1. No authorisation check at all.** `doPost` calls `register` without
`AccessControl.require`. Any authenticated caller can create a patient record. Verified by
signing in as the seeded patient and calling it:

```text
POST /api/patients   201   "Patient{id=af980578-9186-438c-9d2d-1e8a8e878306}"
```

This contradicts **FR-REC-21** and **NFR-SEC-08**. Compare `GET /api/patients?q=`, four lines
away in the same class, which correctly requires `RECEPTIONIST` or `ADMIN`. It reads as an
omission rather than a decision.

**2. The new record is linked to the caller's own account.** `register` sets
`userUid(user != null ? user.uid() : null)`, so a patient calling this ends up with a **second**
`patient` row pointing at their own uid:

```text
mysql> SELECT id, name, user_uid FROM patient WHERE user_uid='u-pat1';
af980578-9186-438c-9d2d-1e8a8e878306 | Created By A Patient | u-pat1
p-nimal                              | Nimal Perera         | u-pat1
```

That breaks **ASM-03** and makes `patients.findByUserUid` ambiguous — which matters, because
patient booking resolves `patientId` through exactly that call. A patient could give themselves
a second profile and make their own future bookings land unpredictably.

Two consequences of the same line: when *reception* calls this endpoint for a walk-in, the
walk-in is silently linked to the **receptionist's** account, so the patient appears to own a
portal account they have never had, and `hasPortalAccount` (**FR-REC-22**) reports the wrong
thing. The uid should come from the request for a self-registration and be null for a walk-in,
never be inherited from whoever happens to be signed in.

**The fix** is three changes: require `RECEPTIONIST` or `ADMIN`; never inherit the caller's uid;
and leave patient self-registration to `POST /api/auth/register`, which owns account creation
and correctly fixes the role to `PATIENT`.

### Also missing

**Nothing prevents a duplicate patient.** **FR-REC-25** requires a warning when a contact number
already exists; there is no such check, so registering the same walk-in twice creates two
records with the same number and nothing to reconcile them.

**No update endpoint.** **FR-REC-24** requires a patient's details to be correctable — a
mistyped phone number is the commonest error at a busy desk — and there is no `PUT` or `PATCH`.
A wrong number can only be fixed in the database.

**Registration does not create the profile row.** **FR-PAT-04** requires
`POST /api/auth/register` to create the `user_account` and `patient` rows in one transaction.
It creates only the account, which is why a freshly registered patient booking an appointment
can hit `"No patient profile for user …"` — see [`appointments.md`](appointments.md).

**No linking of a walk-in to a later account.** **FR-PAT-06** asks that self-registration with
an email matching an existing walk-in link the two rather than duplicate. Nothing does.
