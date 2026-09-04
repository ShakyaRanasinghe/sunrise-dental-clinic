# Appointments API

Booking, cancellation, completion and bills. Conventions, the error envelope and the role
matrix are in [`README.md`](README.md).

| | |
|---|---|
| Servlet | `AppointmentApiServlet` |
| Mapping | `/api/appointments/*` |
| Requirements | FR-APT-01…08, FR-BIL-01…06, NFR-SEC-06, NFR-REL-01 |

| Method | Path | Roles | Purpose |
|---|---|---|---|
| `POST` | `/api/appointments` | `PATIENT` `RECEPTIONIST` `ADMIN` | Book into an open slot |
| `GET` | `/api/appointments/{no}` | any signed in, response narrows by role | One appointment |
| `POST` | `/api/appointments/{no}/cancel` | `PATIENT` (own) `RECEPTIONIST` `ADMIN` | Release the slot |
| `POST` | `/api/appointments/{no}/complete` | `DENTIST` | Record diagnosis, mark complete |
| `POST` | `/api/appointments/{no}/bill` | `RECEPTIONIST` `ADMIN` | Calculate and issue the bill |
| `GET` | `/api/appointments/{no}/bill` | any signed in *(gap — README §6)* | Fetch an issued bill |
| `GET` | `/api/appointments` | — | **Not implemented** — README §2 |

---

## The two response shapes

An appointment serialises one of two ways depending on whether the caller may see clinical
detail. This is **NFR-SEC-06** made structural: the confidential field is absent from the
object, not hidden by the view, so it cannot leak through an oversight.

**`AppointmentResponse`** — reception, admin, and any dentist other than the treating one:

```json
{
  "appointmentNo": "APT-20260820-0001",
  "patientId": "p-nimal",
  "dentistId": "d-silva",
  "slotId": "d-silva_2026-08-20_09:00",
  "treatmentId": "t-scaling",
  "date": "2026-08-20",
  "time": "09:00",
  "status": "CONFIRMED"
}
```

**`AppointmentDetailResponse`** — the treating dentist and the patient themselves. Identical,
plus the two clinical fields:

```json
{
  "appointmentNo": "APT-20260820-0001",
  "patientId": "p-nimal",
  "dentistId": "d-silva",
  "slotId": "d-silva_2026-08-20_09:00",
  "treatmentId": "t-scaling",
  "date": "2026-08-20",
  "time": "09:00",
  "status": "COMPLETED",
  "diagnosis": "Moderate calculus on lower incisors. Scaling completed.",
  "hasCriticalNotes": true,
  "patientNotes": [
    {
      "id": "n-8c31",
      "category": "ALLERGY",
      "detail": "Penicillin — rash and swelling. Confirmed by GP.",
      "critical": true,
      "updatedAt": "2026-07-02T09:14:00Z"
    },
    {
      "id": "n-4f07",
      "category": "MEDICATION",
      "detail": "Warfarin, 3mg daily.",
      "critical": true,
      "updatedAt": "2026-07-02T09:16:00Z"
    }
  ]
}
```

`patientNotes` is what the patient declared about themselves from their own profile
(**FR-NOTE-01**), not something staff typed. It arrives with the appointment so the dentist needs
no second call (**FR-NOTE-07**), and `hasCriticalNotes` exists so a schedule listing can show a
warning without carrying every note (**FR-NOTE-08**).

An empty array means the patient has declared nothing. A client must render that as an explicit
"nothing declared" rather than blank space — silence must not be readable as safety
(**FR-NOTE-12**).

`ClinicAccess.canViewClinical` decides which one you get, and it gates **both** clinical fields
with one decision — there is no way to be authorised for the diagnosis but not the notes.
Reception and the administrator receive `AppointmentResponse`, which has a field for neither
(**FR-NOTE-11**, **NFR-SEC-11**).

The two fields come from different places and different authors:

| Field | Table | Written by | Scope |
|---|---|---|---|
| `diagnosis` | `appointment` | the dentist | one visit |
| `patientNotes` | `patient_note` | the **patient** | persistent, every visit |
 Note the shapes carry **ids, not
names** — resolve `dentistId` and `treatmentId` through [`reference.md`](reference.md), and
`patientId` through [`patients.md`](patients.md). That keeps the appointment record
normalised (**FR-DAT-01**) at the cost of extra calls for a display name.

### Status values

`CONFIRMED` → `COMPLETED` → `BILLED`, with `CANCELLED` reachable from `CONFIRMED`.

**FR-APT-08 is only partly enforced.** The transitions are not centrally guarded, so a
`CANCELLED` appointment can currently be completed and a `CONFIRMED` one billed without ever
being completed (**FR-BIL-05**, *Partial*).

---

## POST /api/appointments

Books an appointment into an open slot and returns it with its new number.

**Request**

| Field | Type | Required | Notes |
|---|---|---|---|
| `slotId` | string | yes | From [`availability.md`](availability.md) |
| `treatmentId` | string | yes | From `GET /api/treatments` |
| `patientId` | string | conditional | **Required** for reception and admin. **Ignored** for a patient — see below |

**Whose appointment is it?** When the caller's role is `PATIENT`, `patientId` is taken from
the session, resolved via `patients.findByUserUid`, and any value in the body is discarded.
That is **FR-PAT-15**: a patient cannot book for anyone else, because the identity never
comes from the request. Reception must supply `patientId` explicitly (**FR-REC-31**).

```bash
# Reception booking on a patient's behalf
curl -s -b jar.txt -X POST http://localhost:8080/api/appointments \
  -H 'Content-Type: application/json' \
  -d '{"patientId":"p-nimal","slotId":"d-silva_2026-08-20_09:00","treatmentId":"t-scaling"}'
```

```bash
# A patient booking for themselves — no patientId
curl -s -b patient-jar.txt -X POST http://localhost:8080/api/appointments \
  -H 'Content-Type: application/json' \
  -d '{"slotId":"d-silva_2026-08-20_09:30","treatmentId":"t-checkup"}'
```

**`201 Created`** — an `AppointmentResponse`. The `appointmentNo` is what the front desk reads
aloud, so clients should surface it immediately (**FR-PAT-13**, **FR-REC-33**).

**`409 Conflict`** — the slot was taken between the slot list being read and this call
(**FR-APT-05**):

```json
{ "errorCode": "slot_unavailable", "message": "Slot d-silva_2026-08-20_09:00 is no longer available." }
```

This is the endpoint the clinic's original problem lives in. The guard is a
`SELECT … FOR UPDATE` row lock inside the booking transaction, with
`UNIQUE (slot.appointment_no)` as a database-level backstop, so two simultaneous bookings
cannot both succeed (**FR-APT-04**). It holds under concurrency, not merely in sequence —
`BookingConcurrencyTest` covers exactly this.

**`400`** — reception omitted `patientId`:

```json
{ "errorCode": "bad_request", "message": "patientId is required when booking on behalf of a patient" }
```

**`404`** — the caller is a patient whose account has no profile row (the **FR-PAT-04** gap):

```json
{ "errorCode": "not_found", "message": "No patient profile for user 3f2a…" }
```

**Atomicity.** The appointment insert, the slot status change and the appointment-number
increment happen in one transaction (**NFR-REL-01**). A failure part-way leaves no
appointment and no consumed slot.

---

## GET /api/appointments/{no}

One appointment by its number — **FR-APT-03**, the brief's "display appointment details".

```bash
curl -s -b jar.txt http://localhost:8080/api/appointments/APT-20260820-0001
```

**`200 OK`** — `AppointmentDetailResponse` or `AppointmentResponse`, per the rules above.

**`404`**

```json
{ "errorCode": "not_found", "message": "Appointment not found: APT-20260820-9999" }
```

A patient requesting another patient's appointment number also gets `404`, never `403` — a
`403` would confirm the appointment exists (**FR-PAT-23**).

---

## POST /api/appointments/{no}/cancel

Cancels and returns the slot to bookable (**FR-APT-06**, **FR-PAT-31**). No body.

```bash
curl -s -b jar.txt -X POST http://localhost:8080/api/appointments/APT-20260820-0001/cancel
```

**`200 OK`** — the updated `AppointmentResponse` with `"status": "CANCELLED"`.

**`403`** — a patient attempting someone else's:

```json
{ "errorCode": "forbidden", "message": "You can only cancel your own appointments." }
```

Note the asymmetry with `GET`: cancel returns `403` here rather than `404`, because reaching
this point means the appointment was found and then rejected on ownership. It leaks slightly
more than the read path does.

**Known gap.** A `COMPLETED` or `BILLED` appointment can still be cancelled — **FR-PAT-32**
is *Partial*. Cancelling a billed appointment would release a slot for work already paid for.

---

## POST /api/appointments/{no}/complete

Records the diagnosis and marks the appointment complete. Dentist only.

**Request**

| Field | Type | Required | Notes |
|---|---|---|---|
| `diagnosis` | string | no | Up to 4,000 characters. Omit or `null` for a visit that produced none (**FR-DEN-34**) |

```bash
curl -s -b dentist-jar.txt -X POST \
  http://localhost:8080/api/appointments/APT-20260820-0001/complete \
  -H 'Content-Type: application/json' \
  -d '{"diagnosis":"Moderate calculus on lower incisors. Scaling completed."}'
```

**`200 OK`** — `AppointmentDetailResponse`, diagnosis included, since the caller is the
treating dentist.

**`403`** for every other role, including admin. Completion is a clinical judgement
(**FR-DEN-30**).

**Known gap.** The endpoint requires role `DENTIST` but does **not** verify the appointment
belongs to the calling dentist, so one dentist can complete and write a diagnosis on
another's appointment. **FR-DEN-22** requires that check; the read path enforces it but this
write path does not.

---

## POST /api/appointments/{no}/bill

Calculates the total and issues the bill — the brief's "calculate and print bill"
(**FR-BIL-01**). No body; every amount is derived server-side.

```bash
curl -s -b jar.txt -X POST http://localhost:8080/api/appointments/APT-20260820-0001/bill
```

**`201 Created`**

```json
{
  "id": "b-7c1e…",
  "appointmentNo": "APT-20260820-0001",
  "consultationFee": 1500,
  "treatmentCost": 3500,
  "serviceCharge": 200,
  "discount": 0,
  "tax": 0,
  "total": 5200
}
```

**How the total is built** — `consultationFee` from the treating dentist's
`dentist.consultation_fee`, `treatmentCost` from `treatment.base_cost`, `serviceCharge` from
`clinic.billing.service-charge`. Selected by strategy (**FR-BIL-06**), so a pricing change
means a new strategy rather than an edit to the billing service.

**The revenue policy** is two settings, described in [`../srs/srs.md`](../srs/srs.md#the-revenue-policy).
On the default policy the clinic takes the whole difference between what the patient pays and
what the dentist is paid, and the receptionist takes nothing.

**What the response omits.** The three-way revenue split — `dentistEarning`,
`clinicEarning`, `receptionistEarning` — is calculated and stored on the `bill` row
(**FR-BIL-03**) but is **not** in `BillResponse`. A patient reading their own bill has no
business seeing how the clinic divides it. The figures reach the administrator through the
reports screen, not this endpoint.

### Billing twice returns a phantom bill id — verified defect

There is **no duplicate check**, and the failure is silent. `generateBill` builds a fresh
`Bill` with a new UUID every time, and `BillDao.save` inserts with
`ON DUPLICATE KEY UPDATE`. The `UNIQUE (bill.appointment_no)` key therefore does not reject
the second call — it converts it into an update of the existing row, which keeps its
**original** id.

Observed against a running instance:

```text
POST /api/appointments/APT-20260901-0001/bill   201  id 30a48f91-9beb-4c1a-8bc9-f06aa36405dc
POST /api/appointments/APT-20260901-0001/bill   201  id 6e22c6dc-3551-4711-b475-ff9b2e3deb7d

mysql> SELECT id, total FROM bill WHERE appointment_no='APT-20260901-0001';
30a48f91-9beb-4c1a-8bc9-f06aa36405dc | 5200.00      <- only one row, the first id
```

The second call answered `201 Created` with an id **that does not exist in the database**. A
client storing it has a reference to nothing. Two further consequences:

- The `ON DUPLICATE KEY UPDATE` clause does not include `receptionist_uid` or
  `issued_by_uid`, so re-billing rewrites the amounts while leaving the original receptionist
  credited — the revenue split and the audit of who issued it silently disagree
- Because amounts *are* in the update list, a re-bill after a price change would rewrite an
  already-issued bill, contradicting **FR-ADM-43**

**FR-BIL-04** is therefore *Partial*, not satisfied: the schema prevents a duplicate row but
the application reports success as though it created one. The fix is to check for an existing
bill before building a new one and return `409` with a distinct `already_billed` code.

---

## GET /api/appointments/{no}/bill

Fetches an issued bill, for reprinting (**FR-REC-54**) or for a patient viewing their own
(**FR-PAT-22**).

```bash
curl -s -b jar.txt http://localhost:8080/api/appointments/APT-20260820-0001/bill
```

**`200 OK`** — the same `BillResponse` shape.

**`404`** — not billed yet:

```json
{ "errorCode": "not_found", "message": "No bill for appointment APT-20260820-0001" }
```

**Gap.** No ownership check. Any authenticated caller who knows an appointment number can
read its bill (README §6). It should be limited to the patient it belongs to, reception and
admin.

---

## Worked sequence

The full life of one appointment, from booking to bill:

```bash
BASE=http://localhost:8080

# Reception books
curl -s -c r.txt -X POST $BASE/api/auth/login -H 'Content-Type: application/json' \
  -d '{"identity":"reception","password":"Password123"}'
APT=$(curl -s -b r.txt -X POST $BASE/api/appointments -H 'Content-Type: application/json' \
  -d '{"patientId":"p-nimal","slotId":"d-silva_2026-08-20_09:00","treatmentId":"t-scaling"}' \
  | grep -o '"appointmentNo":"[^"]*"' | cut -d'"' -f4)

# The dentist completes it and records the diagnosis
curl -s -c d.txt -X POST $BASE/api/auth/login -H 'Content-Type: application/json' \
  -d '{"identity":"silva","password":"Password123"}'
curl -s -b d.txt -X POST $BASE/api/appointments/$APT/complete \
  -H 'Content-Type: application/json' -d '{"diagnosis":"Scaling completed."}'

# Reception issues the bill
curl -s -b r.txt -X POST $BASE/api/appointments/$APT/bill

# Reception reads it back — no diagnosis in this response
curl -s -b r.txt $BASE/api/appointments/$APT
```

The last two calls demonstrate the confidentiality boundary: the dentist's `complete` call
returns the diagnosis, and reception's read of the same appointment does not.
