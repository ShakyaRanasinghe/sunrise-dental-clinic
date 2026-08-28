# Reference API

Lookup data a client needs before it can book: who the dentists are, and what treatments the
clinic offers. Conventions, the error envelope and the role matrix are in
[`README.md`](README.md).

| | |
|---|---|
| Servlet | `ReferenceApiServlet` |
| Mappings | `/api/dentists` and `/api/treatments` |
| Requirements | FR-PAT-10, FR-PAT-12, FR-BIL-01, FR-DAT-02 |

| Method | Path | Roles | Purpose |
|---|---|---|---|
| `GET` | `/api/dentists` | any signed in | Active dentists |
| `GET` | `/api/treatments` | any signed in | Active treatments and their prices |

Both are exact-path mappings, not prefixes — there is no `/api/dentists/{id}`. Both return
**only active** rows, so a deactivated dentist or a withdrawn treatment disappears from every
booking screen without affecting the appointments that already reference it
(**FR-ADM-42**).

Neither declares a role check. They rely on `AuthenticationFilter` for authentication, which
is adequate: this is the clinic's public-facing catalogue, and every role legitimately needs
it to book or to price.

---

> ## ✅ The serialisation defect is fixed — step 3b
>
> Both endpoints returned `Dentist` and `Treatment` **domain objects**, so the serialiser fell
> through to `toString()`:
>
> ```text
> GET /api/dentists    200  ["Dentist{id=d-jayasuriya}","Dentist{id=d-silva}"]
> GET /api/treatments  200  ["Treatment{id=t-filling}", … ]
> ```
>
> Ids and nothing else — no name, no specialisation, no fee, no cost. That made the API
> unusable for its main purpose: a booking client is meant to show the patient a dentist to
> choose and a priced treatment to pick (**FR-PAT-10**, **FR-PAT-12**), and there was no call it
> could make to turn `"Dentist{id=d-silva}"` into "Dr. Ranil Silva, General Dentistry, Rs 1,500".
>
> It went unnoticed because the JSP screens read the repositories directly rather than calling
> the API — nothing in the application consumed its own web service (see README §2).
>
> **Two changes closed it.** `Json` learned to write beans and records rather than falling back
> to `toString()`, in step 1; and these endpoints now return `DentistResponse` and
> `TreatmentResponse` through `ReferenceService`, in step 3b. Verified against a running
> instance:
>
> ```text
> GET /api/dentists  200  [{"id":"d-jayasuriya","name":"Dr. Malini Jayasuriya",
>                           "specialization":"Orthodontics","consultationFee":2500.00,"active":true}, … ]
> ```
>
> Note `2500.00`, not `2500.0`. Money is `BigDecimal`, matching the `DECIMAL(10,2)` column —
> `double` cannot represent 0.01 exactly, and a total assembled from such values drifts from
> the one the database computes for the same inputs.

---

## The shapes

**Implemented** in step 3b. These are the records the endpoints return, from columns that
already exist.

### DentistResponse

```json
[
  {
    "id": "d-silva",
    "name": "Dr. Ranil Silva",
    "specialization": "General Dentistry",
    "consultationFee": 1500.00
  },
  {
    "id": "d-jayasuriya",
    "name": "Dr. Malini Jayasuriya",
    "specialization": "Orthodontics",
    "consultationFee": 2500.00
  }
]
```

`user_uid` must **not** be included. It is an internal account identifier with no use to a
client, and exposing it invites its use as a parameter.

`active` need not be included either — every row returned is active by definition.

### TreatmentResponse

```json
[
  { "id": "t-checkup",    "name": "Routine check-up",    "description": "Examination and advice",              "baseCost": 1000.00 },
  { "id": "t-scaling",    "name": "Scaling & polishing", "description": "Removal of plaque and stains",        "baseCost": 3500.00 },
  { "id": "t-filling",    "name": "Composite filling",   "description": "Tooth-coloured restoration",          "baseCost": 4500.00 },
  { "id": "t-extraction", "name": "Extraction",          "description": "Simple tooth extraction",             "baseCost": 5000.00 },
  { "id": "t-whitening",  "name": "Teeth whitening",     "description": "In-clinic whitening session",         "baseCost": 12000.00 },
  { "id": "t-rootcanal",  "name": "Root canal therapy",  "description": "Endodontic treatment, single visit",  "baseCost": 18000.00 }
]
```

---

## GET /api/dentists

```bash
curl -s -b jar.txt http://localhost:8080/api/dentists
```

**`200 OK`** — an array of active dentists.

**`401`** — unauthenticated, verified:

```json
{ "errorCode": "unauthenticated", "message": "Authentication is required." }
```

**Ordering is not specified.** The observed order was `d-jayasuriya` then `d-silva`, which is
neither insertion order nor an obvious sort. A client wanting a predictable list must sort it
itself. An `ORDER BY name` in the query would be the smaller fix.

---

## GET /api/treatments

```bash
curl -s -b jar.txt http://localhost:8080/api/treatments
```

**`200 OK`** — an array of active treatments.

**Why this is reference data and not free text.** The brief calls this field "treatment type".
Storing it as a priced catalogue entry rather than a typed string is what makes
`POST /api/appointments/{no}/bill` able to calculate a total at all (**FR-DAT-02**,
**FR-BIL-01**) — you cannot price "cleaning, I think" written into a text box.

**Ordering is unspecified here too**, and the observed order is not by name or by cost.

---

## How a booking client is meant to use these

The intended sequence, and where it currently fails:

```bash
BASE=http://localhost:8080
curl -s -c jar.txt -o /dev/null -X POST $BASE/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"nimal@example.lk","password":"Password123"}'

# 1. Offer the patient a dentist        -> returns ids only, cannot render a choice
curl -s -b jar.txt $BASE/api/dentists

# 2. Offer a treatment with its price   -> returns ids only, cannot render a price
curl -s -b jar.txt $BASE/api/treatments

# 3. Show the open times                -> works correctly, SlotResponse is a record
curl -s -b jar.txt "$BASE/api/availability?dentistId=d-silva&date=2026-09-01"

# 4. Book                               -> works correctly
curl -s -b jar.txt -X POST $BASE/api/appointments \
  -H 'Content-Type: application/json' \
  -d '{"slotId":"d-silva_2026-09-01_09:30","treatmentId":"t-checkup"}'
```

Steps 3 and 4 work. Steps 1 and 2 return data a client cannot display. The whole booking flow
is therefore blocked on two response records that do not exist — roughly thirty lines of
code, and the difference between a web-service tier that demonstrably works and one that only
appears to.
