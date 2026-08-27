# Complaints API

A patient raising a concern about a dentist, and an administrator reviewing it. Conventions, the
error envelope and the role matrix are in [`README.md`](README.md).

| | |
|---|---|
| Servlet | `ComplaintApiServlet` |
| Mapping | `/api/complaints/*` |
| Module | `complaints` |
| Requirements | FR-CMP-01…12, FR-PAT-50…57, FR-ADM-50…58, NFR-SEC-12 |
| Status | **Specified, not implemented** |

| Method | Path | Roles | Purpose |
|---|---|---|---|
| `POST` | `/api/complaints` | `PATIENT` | Raise a complaint |
| `GET` | `/api/complaints` | `PATIENT` (own) · `ADMIN` (all) | List |
| `GET` | `/api/complaints/{id}` | `PATIENT` (own) · `ADMIN` | One complaint |
| `PATCH` | `/api/complaints/{id}` | `ADMIN` | State and resolution only |

---

## The confidentiality rule, first

**A dentist can never reach this resource.** Not the content, not a count, not a `404` that
differs from any other `404`. Every path returns `403` for `DENTIST` and `RECEPTIONIST` before
any lookup happens, so response timing cannot reveal whether a complaint exists
(**FR-CMP-08**, **FR-CMP-09**).

This is the **opposite** direction to [`patients.md`](patients.md)'s medical notes, and the
contrast is the design point:

| Resource | Dentist | Reception | Admin | Governed by |
|---|:--:|:--:|:--:|---|
| `/api/patients/{id}/notes` | reads | ✘ | ✘ | clinical need |
| `/api/complaints` | ✘ | ✘ | reads | independence |

Access follows purpose, not rank. The administrator is the most privileged account in the system
and cannot read a diagnosis; the dentist is the only role that can, and cannot read a complaint
naming them.

**Every read is audited** (**FR-CMP-11**), including a `GET` that returns nothing. Unusual —
reads are not normally audited here — and justified because with data this sensitive, who looked
matters as much as who changed it.

---

## ComplaintResponse

```json
{
  "id": "c-91af",
  "dentistId": "d-silva",
  "dentistName": "Dr. Ranil Silva",
  "appointmentNo": "APT-20260901-0001",
  "category": "CONDUCT",
  "detail": "I asked twice for an explanation of the treatment and was told there was no time.",
  "status": "UNDER_REVIEW",
  "submittedAt": "2026-09-02T14:20:00Z",
  "resolution": null,
  "resolvedAt": null
}
```

`dentistName` is resolved server-side, because a patient reading their own complaint should not
have to call `/api/dentists` to find out who they complained about.

**What the shape omits, deliberately:** no `patientId` — a patient reading their own needs no
reminder, and an administrator gets it from the list context — and no `reviewedByUid`. Which
administrator handled a complaint is in the audit trail, not in a response the complainant reads.

`status` is `SUBMITTED`, `UNDER_REVIEW`, `RESOLVED` or `DISMISSED`. `resolution` and `resolvedAt`
are `null` until closed.

---

## POST /api/complaints

Raises a complaint. Patient only.

| Field | Type | Required | Notes |
|---|---|---|---|
| `dentistId` | string | yes | Must be a dentist who has **treated this patient** |
| `category` | string | yes | One of the five values |
| `detail` | string | yes | 20–4000 characters |
| `appointmentNo` | string | no | Must be one of the caller's own appointments |

```bash
curl -s -b patient-jar.txt -X POST http://localhost:8080/api/complaints \
  -H 'Content-Type: application/json' \
  -d '{"dentistId":"d-silva","appointmentNo":"APT-20260901-0001","category":"CONDUCT",
       "detail":"I asked twice for an explanation of the treatment and was told there was no time."}'
```

**`201 Created`** — the `ComplaintResponse`, `status` `SUBMITTED`.

**`400`** — the named dentist has never treated this patient:

```json
{ "errorCode": "bad_request", "message": "Choose the dentist this concerns." }
```

The check matters for two reasons: it keeps the complaint investigable, and it stops the endpoint
being used to discover which dentists exist. The client should populate the field from the
patient's own appointment history rather than `/api/dentists` (**FR-PAT-50**).

**`400`** — detail too short. The 20-character floor is deliberate: a complaint the clinic cannot
act on wastes the patient's time as well as the administrator's.

**`403`** — any role other than `PATIENT`. A receptionist cannot raise a complaint on a patient's
behalf; if they could, the front desk would sit between the patient and the process, which is
what this exists to avoid.

---

## GET /api/complaints

Scope depends on the caller's role — the same endpoint, two answers.

| Caller | Returns |
|---|---|
| `PATIENT` | only complaints they raised |
| `ADMIN` | every complaint |

| Parameter | Type | Applies to | Notes |
|---|---|---|---|
| `status` | query | both | Filter by state |
| `dentistId` | query | **admin only** | Ignored for a patient, who has no need to filter their own few |

```bash
curl -s -b patient-jar.txt http://localhost:8080/api/complaints
curl -s -b admin-jar.txt   'http://localhost:8080/api/complaints?status=SUBMITTED'
```

**`200 OK`** — an array, newest first, with open complaints before closed ones so an
administrator's queue reads top-down (**FR-ADM-51**).

**`403`** for `DENTIST` and `RECEPTIONIST`, returned before any query runs.

---

## GET /api/complaints/{id}

**`200 OK`** — one `ComplaintResponse`. The patient who raised it, or an administrator.

**`404`** — another patient's complaint. Not `403`, which would confirm the id is real.

---

## PATCH /api/complaints/{id}

Moves a complaint through its states and records the outcome. Administrator only.

| Field | Type | Required | Notes |
|---|---|---|---|
| `status` | string | yes | `UNDER_REVIEW`, `RESOLVED` or `DISMISSED` |
| `resolution` | string | conditional | **Required** when moving to `RESOLVED` or `DISMISSED`, 10–2000 characters |

```bash
curl -s -b admin-jar.txt -X PATCH http://localhost:8080/api/complaints/c-91af \
  -H 'Content-Type: application/json' \
  -d '{"status":"RESOLVED","resolution":"Spoke with Dr Silva; consultation slots extended to 40 minutes for restorative work."}'
```

**`200 OK`** — the updated complaint.

**`400`** — closing without a resolution:

```json
{ "errorCode": "bad_request", "message": "Say how this was resolved." }
```

Enforced on the entity, not the servlet: `Complaint.resolve(note, adminUid)` takes both together,
so a complaint cannot be closed silently (**FR-ADM-53**).

**`400`** — an illegal transition, e.g. `SUBMITTED → RESOLVED` with no review:

```json
{ "errorCode": "bad_request", "message": "A complaint must be reviewed before it is closed." }
```

**`detail` is not accepted by this endpoint.** The patient's account of what happened has no
setter anywhere in the system (**FR-ADM-55**). An administrator writes the outcome, never the
allegation — otherwise the record stops being evidence of anything.

---

## The limitation worth reading

An administrator reviewing a **clinical** complaint cannot see the clinical record: `diagnosis`
and `patient_note` remain closed to them (**FR-ADM-58**). So a complaint of the form *"the
treatment was wrong"* is not judgeable from this API alone.

That is deliberate rather than overlooked, and the resolution is procedural: such a complaint is
referred to a dentist other than the one named, who reads the record in their own right as a
treating clinician. Widening the administrator's access would be the easier fix and the wrong
one — it would make every administrator a reader of every patient's clinical history in order to
handle the rare clinical complaint.
