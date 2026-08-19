# Availability API

Publishing a dentist's working session, and reading the slots it produced. Conventions, the
error envelope and the role matrix are in [`README.md`](README.md).

| | |
|---|---|
| Servlet | `AvailabilityApiServlet` |
| Mappings | `/api/availability/*` and `/api/sessions` |
| Requirements | FR-AVL-01…04, FR-REC-40…44, ASM-06 |

| Method | Path | Roles | Purpose |
|---|---|---|---|
| `GET` | `/api/availability?dentistId&date` | any signed in | Open slots for one dentist on one day |
| `GET` | `/api/availability/week?from&to` | any signed in | Open slots across a date range |
| `POST` | `/api/sessions` | `RECEPTIONIST` `ADMIN` | Publish a session and generate its slots |

---

## The session and slot model

Two levels, and the distinction matters:

- A **session** is a statement of intent — *Dr Silva works 09:00 to 12:00 on 20 August, in
  30-minute appointments*. One row in `dentist_session`.
- A **slot** is one bookable interval cut from that session. Publishing a three-hour session
  in 30-minute intervals produces six slots, each `OPEN` until booked.

Reception publishes; the dentist does not (**ASM-06**). Slots are generated automatically —
reception never enters them individually (**FR-AVL-02**, **FR-REC-41**).

**Slot ids are composed, not random**: `{dentistId}_{date}_{startTime}`, for example
`d-silva_2026-08-20_09:00`. Two consequences worth knowing. A client can construct a slot id
without having read it, and the same dentist, date and time can only ever be one slot — the
id itself prevents a duplicate.

### SlotResponse

```json
{
  "id": "d-silva_2026-08-20_09:00",
  "dentistId": "d-silva",
  "date": "2026-08-20",
  "startTime": "09:00",
  "durationMinutes": 30,
  "status": "OPEN"
}
```

`status` is `OPEN` or `BOOKED`. Both read endpoints return **only open slots**, so in practice
every slot you receive from this API reads `OPEN`. That is **FR-AVL-03** and **FR-PAT-11**:
booked times are not offered and not shown as unavailable — they are absent.

---

## GET /api/availability

Open slots for one dentist on one date. This is what a booking screen calls after the patient
has chosen a dentist and a day.

| Parameter | Type | Required | Notes |
|---|---|---|---|
| `dentistId` | query | yes | From `GET /api/dentists` |
| `date` | query | yes | `yyyy-MM-dd` |

```bash
curl -s -b jar.txt 'http://localhost:8080/api/availability?dentistId=d-silva&date=2026-08-20'
```

**`200 OK`** — a JSON array, ordered by start time. An empty array is a valid answer and means
either nothing was published or everything is booked; the API does not distinguish the two, so
a client cannot tell "the dentist is not working" from "the dentist is full". Worth adding if
a screen needs to say which.

```json
[
  { "id": "d-silva_2026-08-20_09:00", "dentistId": "d-silva", "date": "2026-08-20",
    "startTime": "09:00", "durationMinutes": 30, "status": "OPEN" },
  { "id": "d-silva_2026-08-20_09:30", "dentistId": "d-silva", "date": "2026-08-20",
    "startTime": "09:30", "durationMinutes": 30, "status": "OPEN" }
]
```

**`400`** — missing or malformed parameter:

```json
{ "errorCode": "bad_request", "message": "dentistId is required" }
```

```json
{ "errorCode": "bad_request", "message": "date must be a date in yyyy-MM-dd format" }
```

Note there is **no check that `dentistId` exists**. An unknown dentist returns an empty array
rather than `404`, which is indistinguishable from a real dentist with nothing free.

---

## GET /api/availability/week

Open slots across a date range, for every dentist. Despite the name the range is arbitrary —
`week` describes the intended use, not a limit.

| Parameter | Type | Required | Notes |
|---|---|---|---|
| `from` | query | yes | `yyyy-MM-dd`, inclusive |
| `to` | query | yes | `yyyy-MM-dd`, inclusive, not before `from` |

```bash
curl -s -b jar.txt 'http://localhost:8080/api/availability/week?from=2026-08-20&to=2026-08-26'
```

**`200 OK`** — an array of `SlotResponse` spanning every dentist in the range. Clients group
by `dentistId` themselves.

**`400`** — inverted range, rejected before any query runs:

```json
{ "errorCode": "bad_request", "message": "'to' must not be before 'from'" }
```

**No upper bound on the range.** `from=2020-01-01&to=2030-12-31` is accepted and will attempt
to return every open slot in a decade. **NFR-PRF-01** expects a two-second response; a cap
comparable to the 366-day limit on reports (**FR-ADM-10**) belongs here.

---

## POST /api/sessions

Publishes a session and generates its slots in one call. Reception or admin only.

**Request**

| Field | Type | Required | Notes |
|---|---|---|---|
| `dentistId` | string | yes | From `GET /api/dentists` |
| `date` | string | yes | `yyyy-MM-dd` |
| `startTime` | string | yes | `HH:mm` |
| `endTime` | string | yes | `HH:mm`, must be after `startTime` |
| `slotMinutes` | integer | no | Defaults to **30**. Must be greater than zero |

```bash
curl -s -b jar.txt -X POST http://localhost:8080/api/sessions \
  -H 'Content-Type: application/json' \
  -d '{"dentistId":"d-silva","date":"2026-08-20","startTime":"09:00","endTime":"12:00","slotMinutes":30}'
```

**`201 Created`** — the session id and every slot it produced, so a client need not follow up
with a read:

```json
{
  "sessionId": "096a4864-f3b2-4c77-9d81-2ab5e7c40e19",
  "slots": [
    { "id": "d-silva_2026-08-20_09:00", "dentistId": "d-silva", "date": "2026-08-20",
      "startTime": "09:00", "durationMinutes": 30, "status": "OPEN" },
    { "id": "d-silva_2026-08-20_09:30", "dentistId": "d-silva", "date": "2026-08-20",
      "startTime": "09:30", "durationMinutes": 30, "status": "OPEN" }
  ]
}
```

The `slots` array is the **currently open** slots for that dentist and date, not strictly the
ones this call created. Publishing a second session on a day that already has free slots
returns both sets together.

**`400`** — validation failures, each checked before anything is written:

```json
{ "errorCode": "bad_request", "message": "endTime must be after startTime" }
```

```json
{ "errorCode": "bad_request", "message": "slotMinutes must be greater than zero" }
```

```json
{ "errorCode": "bad_request", "message": "startTime must be a time in HH:mm format" }
```

**`403`** for a patient or a dentist — publishing is a front-desk task (**ASM-06**).

### Gaps against the requirements

All four were confirmed against a running instance.

**Overlapping sessions are accepted** (**FR-REC-43**, *Specified*). Publishing 09:00–11:00 and
then 10:00–12:00 for the same dentist on the same date both return `201`:

```text
SELECT COUNT(*) FROM dentist_session WHERE session_date='2026-09-07';  -> 2
SELECT COUNT(*) FROM slot           WHERE slot_date='2026-09-07';     -> 6
```

Four slots plus four slots gives six, not eight, because the composed ids make the shared
10:00 and 10:30 slots collide and the DAO upserts them. There is a subtler effect: those two
slots now carry the **second** session's `session_id`, so a slot from the first session has
been silently reparented. The dentist is recorded as working two contradictory sessions.

**A ragged span is accepted** (**FR-REC-42**, *Partial*). 09:00–10:20 in 30-minute slots
produces slots at 09:00 and 09:30 and silently discards the trailing 20 minutes — the loop
condition is `!cursor.plusMinutes(slotMinutes).isAfter(end)`. Nothing warns that the span does
not divide evenly.

**Past dates are accepted.** `{"date":"2020-01-15"}` returned `201` with a slot created for a
day six years gone. **FR-REC-40**'s validation table requires today or later; nothing enforces
it.

**An unknown dentist produces a `500`, not a `4xx`.** The database catches it — the
`fk_session_dentist` foreign key rejects the insert — but the resulting `DataAccessException`
maps to a generic server error:

```json
{ "errorCode": "internal_error", "message": "Something went wrong. Please try again." }
```

So integrity holds and no orphan session is written, but the caller is told nothing useful and
the fault reads as a server bug rather than a bad request. A `dentists.findById` check before
building the session would return `404` with the dentist named. This is the clearest example
in the API of a validation belonging in the service tier rather than being left to the
schema.

---

## Worked sequence

Publish, read, book, then confirm the slot has left the open list:

```bash
BASE=http://localhost:8080
curl -s -c r.txt -o /dev/null -X POST $BASE/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"reception@sunrisedental.lk","password":"Password123"}'

# Publish one hour, two slots
curl -s -b r.txt -X POST $BASE/api/sessions -H 'Content-Type: application/json' \
  -d '{"dentistId":"d-silva","date":"2026-09-01","startTime":"09:00","endTime":"10:00","slotMinutes":30}'

# Both are open
curl -s -b r.txt "$BASE/api/availability?dentistId=d-silva&date=2026-09-01"

# Book the first
curl -s -b r.txt -X POST $BASE/api/appointments -H 'Content-Type: application/json' \
  -d '{"patientId":"p-nimal","slotId":"d-silva_2026-09-01_09:00","treatmentId":"t-scaling"}'

# Now only 09:30 comes back — the booked slot is absent, not marked
curl -s -b r.txt "$BASE/api/availability?dentistId=d-silva&date=2026-09-01"
```

The last step is the observable form of **FR-AVL-03**, and the reason a patient cannot book a
taken time by guessing its slot id: the booking endpoint re-checks the slot under a row lock,
so a constructed id for a booked slot returns `409` rather than succeeding.
