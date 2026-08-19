# Reviews API

A patient rating the dentist after a completed visit, and the aggregate that rating feeds.
Conventions, the error envelope and the role matrix are in [`README.md`](README.md).

| | |
|---|---|
| Servlet | `ReviewApiServlet` |
| Mappings | `/api/appointments/{no}/review` and `/api/dentists/{id}/rating` |
| Module | `feedback` — alongside complaints |
| Requirements | FR-RVW-01…12, FR-PAT-60…67, FR-DEN-60…63, FR-ADM-59…61, NFR-SEC-13 |
| Status | **Specified, not implemented** |

| Method | Path | Roles | Purpose |
|---|---|---|---|
| `POST` | `/api/appointments/{no}/review` | `PATIENT` (own) | Rate the visit |
| `GET` | `/api/appointments/{no}/review` | `PATIENT` (own) · `ADMIN` | The review left |
| `PUT` | `/api/appointments/{no}/review` | `PATIENT` (own, ≤30 days) | Change it |
| `GET` | `/api/dentists/{id}/rating` | `DENTIST` (own) · `ADMIN` | Mean and count |
| `GET` | `/api/reviews?dentistId=` | `ADMIN` | Individual reviews with comments |

---

## One table, read at three resolutions

This is the part to get right. `dentist_review` is served through **two different response
types**, and which one you get is decided by role — not by filtering fields out of a shared
object.

| Caller | Type | Carries |
|---|---|---|
| `PATIENT` (own) | `ReviewResponse` | rating, comment, dates |
| `ADMIN` | `ReviewResponse` | the same, for any review, plus the author |
| `DENTIST` (own) | `RatingSummary` | **mean and count only** |
| `RECEPTIONIST` | — | `403`, nothing |

**Why the dentist gets a separate type rather than a nulled-out one.** A field set to `null`
can be un-nulled by a later edit, a copy-paste, or a mapper someone reuses. A field that does
not exist on the type cannot leak. `RatingSummary` has no `comment` and no `patientId`, so
there is nothing there to expose (**NFR-SEC-13**).

That is the same technique as `AppointmentResponse` vs `AppointmentDetailResponse` in
[`appointments.md`](appointments.md) — the third place in the system where a confidentiality
boundary is a type rather than a condition.

---

## ReviewResponse

```json
{
  "appointmentNo": "APT-20260901-0001",
  "dentistId": "d-silva",
  "dentistName": "Dr. Ranil Silva",
  "rating": 4,
  "comment": "Explained everything clearly. Slight wait.",
  "submittedAt": "2026-09-01T11:02:00Z",
  "updatedAt": "2026-09-01T11:02:00Z",
  "editableUntil": "2026-10-01T11:02:00Z"
}
```

`editableUntil` is served rather than left for the client to compute, so a client cannot get the
30-day window wrong (**FR-PAT-63**). When an administrator reads it, a `patientName` field is
added; a patient reading their own does not need to be told who they are.

## RatingSummary — what a dentist gets

```json
{
  "dentistId": "d-silva",
  "mean": 4.4,
  "count": 37,
  "publishable": true
}
```

Four fields, none of them traceable to a person. `publishable` is `false` while `count` is under
five (**FR-RVW-12**), and a client must show nothing rather than a mean, so one poor visit cannot
define a dentist.

---

## POST /api/appointments/{no}/review

| Field | Type | Required | Notes |
|---|---|---|---|
| `rating` | integer | yes | 1–5 |
| `comment` | string | no | Up to 1000 characters |

```bash
curl -s -b patient-jar.txt -X POST \
  http://localhost:8080/api/appointments/APT-20260901-0001/review \
  -H 'Content-Type: application/json' \
  -d '{"rating":4,"comment":"Explained everything clearly. Slight wait."}'
```

**`201 Created`** — the `ReviewResponse`.

**`409 Conflict`** — already reviewed. One review per appointment, and the constraint is the
database's: `UNIQUE (dentist_review.appointment_no)`. A client should treat this as "use `PUT`
instead" rather than an error to show the patient (**FR-DAT-07**).

**`400`** — the appointment has not happened:

```json
{ "errorCode": "bad_request", "message": "You can rate a visit once it has happened." }
```

Accepted only for `COMPLETED` or `BILLED`. A `CONFIRMED` or `CANCELLED` appointment is refused
(**FR-RVW-04**) — you cannot rate care you have not received.

**`403`** — any role but `PATIENT`, and only for their own appointment. Reception cannot rate on a
patient's behalf; a rating entered by the person who booked it is not feedback.

**A comment is optional and that is deliberate** (**FR-RVW-02**). Requiring words suppresses
ratings, and a star with no comment is still a signal. `{"rating":4}` is a complete request.

---

## GET /api/appointments/{no}/review

**`200 OK`** — the `ReviewResponse`. The patient who wrote it, or an administrator.

**`404`** — not reviewed, or another patient's appointment. The two are indistinguishable on
purpose.

---

## PUT /api/appointments/{no}/review

Same body as `POST`. Replaces the rating and comment; `updatedAt` moves, `submittedAt` does not.

**`200 OK`** — the updated review.

**`403`** — past the 30-day window:

```json
{ "errorCode": "forbidden", "message": "Reviews can be changed for 30 days after the visit." }
```

The window is enforced on the entity — `DentistReview.isEditable(now)` — not by a date comparison
in the servlet, so every path gets the same answer (**FR-PAT-63**).

There is no `DELETE`. A patient may change a rating but not withdraw it; otherwise a dentist's
mean could be edited after the fact by anyone who changed their mind about being counted.

---

## GET /api/dentists/{id}/rating

The aggregate. A dentist may read **only their own**; an administrator may read any.

```bash
curl -s -b dentist-jar.txt http://localhost:8080/api/dentists/d-silva/rating
curl -s -b dentist-jar.txt http://localhost:8080/api/dentists/me/rating     # equivalent
```

**`200 OK`** — a `RatingSummary`.

**`403`** — a dentist requesting another dentist's rating (**FR-DEN-63**), or any receptionist
(**FR-RVW-10**).

**`200` with `publishable: false`** — fewer than five reviews. The `mean` is still present for an
administrator, who has a legitimate need to see a thin signal; a client serving a dentist should
render "not enough reviews yet".

---

## GET /api/reviews?dentistId=

Individual reviews with comments and authors. **Administrator only** (**FR-ADM-59**).

| Parameter | Type | Required |
|---|---|---|
| `dentistId` | query | no — omit for every dentist |
| `maxRating` | query | no — e.g. `2`, to read the poor ones first |

**`200 OK`** — an array of `ReviewResponse` with `patientName` included, newest first.

**`403`** for every other role, including the dentist reviewed. This is the endpoint FR-RVW-08
exists to keep away from them.

There is no `PATCH` and no `DELETE`. An administrator cannot edit or remove a review
(**FR-ADM-61**) — curating the feedback would make the average meaningless, and an average
nobody trusts is worse than none.

---

## Not shown to patients in this release

**FR-RVW-11.** No rating, mean or star appears anywhere a patient can see it — including the
dentist picker on the booking screen, where it would be most tempting.

That is a decision, not an oversight. The clinic wants the data accumulating from day one so a
future decision about publishing ratings has real history behind it; building the display now
would mean guessing at a presentation nobody has asked for. Storing the data cannot be done
retrospectively, and showing it can be added whenever the clinic wants — the endpoints above
already return everything such a screen would need.
