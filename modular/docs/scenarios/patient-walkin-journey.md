# Patient Journey — Walk-in Physical Path

**Who this is:** A patient who has little or no digital awareness. They may not have an
email address. They may not own a smartphone or know how to use online booking. They know
the clinic exists — from word of mouth, a signboard, or having visited before — and they
know the physical location. They may also have the front desk telephone number written
down or saved in a basic phone.

This is the second of two patient journeys. The first — the tech-aware patient who books
online — is documented in [patient-online-journey.md](patient-online-journey.md).

This path is **more dependent on the receptionist** than any other in the system. The
patient relies entirely on the front desk to guide them, answer their questions, check
availability, register them, book on their behalf and give them something to take away
so they do not forget their appointment.

---

## The journey, step by step

### 1. First contact — arriving at the front desk or telephoning

The patient either walks directly into the clinic or calls the front desk telephone
number.

**If they walk in:** They approach the reception desk and say they need an appointment.
They may describe their symptom or say what kind of treatment they think they need. They
may not know which dentist to ask for.

**If they call:** Reception answers. The patient explains what they need. Reception
handles the entire booking conversation over the phone and books on their behalf. The
patient is then told to come in at the booked time.

> **[GAP]** The clinic telephone number and email are not surfaced anywhere in the system.
> The help page (`/help`) has placeholders (`${clinicPhone}`, `${clinicEmail}`) that are
> never populated. There is no configuration in `clinic.properties` for clinic contact
> details. A patient who needs to call cannot find the number from within the system.

---

### 2. Reception explains the available doctors

The patient may not know which doctor to see. Reception explains:

- Which dentists are currently practising at the clinic
- What each dentist specialises in
- What the consultation fee is per dentist
- What treatment types are available and what each costs

> **[GAP]** There is no screen in the system that gives reception a quick overview of all
> active dentists with their specialisations and fees in one place for this purpose.
> Reception currently has to recall this information from memory or look it up through
> the admin accounts screen. A simple "Our dentists" reference card within the reception
> portal would close this gap.

The patient decides which doctor they want, or reception recommends one based on the
symptom described.

---

### 3. Reception checks whether the patient is already registered

Before booking, reception checks whether this patient is already in the system.

Reception goes to **Patients** (`/reception/patients`) and searches by name or contact
number. Two outcomes:

**Already registered:** The patient's record appears in the search results. Reception
selects them and proceeds to check availability.

**Not registered:** No record is found. Reception tells the patient they need to be
registered first and asks for their details:

- Full name *(required)*
- Contact number *(required)*
- Email address *(optional — this patient may not have one)*
- Date of birth *(optional)*
- Address *(optional)*

> A contact number is enough to register. An email address is not required for a
> walk-in patient record. If the patient does not have an email, reception leaves that
> field blank.

Reception goes to **Walk-in** (`/reception/walkin`) and fills in the registration form.
On submission the patient record is created and reception is taken directly to the slot
selection screen for that patient — no need to navigate back to Patients.

---

### 4. Checking doctor availability

With the patient identified, reception checks whether the chosen doctor has open slots
on a suitable date.

On the Walk-in screen, reception selects today's date (or the patient's preferred date)
and the system shows all open time slots across all active dentists for that day in a
table — time and dentist name, one row per slot.

If the chosen dentist has open slots, reception tells the patient:
*"Dr. Silva has 10:00, 10:30 and 11:00 available today."*

**If no slots are available for that doctor on that date:**
Reception tells the patient the doctor is not available and offers alternatives:
- A different date for the same doctor
- A different doctor who has availability today

> **[GAP]** The Walk-in screen shows all open slots across all dentists but does not
> filter by dentist directly. Reception must scan the table to find the chosen dentist's
> rows. A filter by dentist on the Walk-in screen would speed this up at a busy desk.

---

### 5. Calculating the charge before booking

Before confirming, reception tells the patient what the appointment will cost.

Reception can calculate this mentally or on paper:
- Treatment cost (from the treatment catalogue)
- Dentist consultation fee (shown on the booking page when a dentist is selected)
- Service charge (fixed clinic fee, Rs 200 by default)
- **Estimated total = treatment + consultation fee + service charge**

The patient is told the estimated amount before the booking is confirmed so there are
no surprises at payment time.

> **[GAP]** There is no "calculate charge for this patient" screen in the reception portal.
> The cost breakdown is shown on the patient-facing booking page (`/patient/book`) when
> a dentist is selected — but a receptionist using the Walk-in screen does not see this
> breakdown before clicking through to the booking page. Reception must calculate manually
> or navigate to the booking page first just to see the numbers.

---

### 6. Booking the appointment

Reception clicks **Book this slot** on the Walk-in screen next to the chosen time.

This takes reception to the booking page (`/patient/book`) pre-filled with the patient
and dentist. Reception selects the treatment type and clicks **Confirm booking**.

The appointment is confirmed. The system returns to the patient's appointments page
showing the new booking with its appointment number (e.g. `APT-20260821-0003`),
the dentist, the treatment, the date and the time.

---

### 7. Giving the patient something to take away

This is critical for the walk-in patient. Unlike the online patient who can log in and
check their appointment at any time, the walk-in patient has no portal account and no
way to look up their booking later. If they leave the clinic without a written record,
they may forget the time, the date or the doctor's name.

Reception should give the patient a printed or handwritten slip containing:
- The appointment number
- The dentist's name
- The date and time
- The clinic address
- The clinic telephone number (in case they need to cancel or ask questions)

> **[GAP]** The system does not generate a printed appointment confirmation slip for
> walk-in patients. The receipt (`/reception/receipt`) is a billing document issued
> after payment — it is not an appointment reminder. There is no "print appointment
> card" function in the system. Reception must write this information by hand or print
> the screen, which is not a reliable process at a busy desk. A dedicated printable
> appointment confirmation for walk-in patients is a real-world need that is not met.

---

### 8. The patient waits — and may leave

After booking, the walk-in patient may:

- **Wait in the clinic** if their slot is soon
- **Leave and return later** if the appointment is later in the day or on another day

If they leave, they rely entirely on the paper slip reception gave them (or their memory
if no slip was given) to return at the right time.

> **[GAP]** There is no SMS reminder system. For a patient without email or portal access,
> there is no automated way the system can remind them of their appointment. This is
> especially important for walk-in patients who may have registered without an email
> address. An SMS reminder sent to the contact number on the day of the appointment would
> close this gap. This requires an SMS integration that does not currently exist.

---

### 9. Understanding that immediate treatment is not guaranteed

A walk-in patient may arrive at the clinic expecting to be seen immediately. This is
not how the system works and reception must explain it clearly at the time of booking.

**Why the patient cannot be seen immediately even if they are physically present:**

- The dentist's available time slots have already been published by reception in advance
- Other patients — including online patients — may have already booked those slots
- A slot that appears open now may be taken by another patient between the moment
  reception checks and the moment the booking is confirmed
- Even if a slot is open, the dentist may be with another patient in that slot

**What this means in practice:** The patient must wait for their specific booked time
slot, not just for the dentist to become free. Being physically present does not move
them ahead of a patient who has a booked slot.

> Reception must communicate this clearly: *"Your appointment is at 10:30. Dr. Silva is
> with another patient until then. Please take a seat and we will call you at 10:30."*

> **[GAP]** There is no waiting queue or "call patient" function in the system. Reception
> has no way to formally signal to the patient that the doctor is ready for them. This
> is managed verbally. In a larger clinic with a waiting area separate from the front
> desk, a digital queue display or notification would be needed. This does not exist.

---

### 10. Doctor's room — finding the right place

When the patient's time comes, they need to know which room to go to.

Reception tells them verbally: *"Dr. Silva is in Room 2, down the corridor on the left."*

> **[GAP]** There is no room or location information stored in the system. The dentist
> record holds name, specialisation and consultation fee — no room number, no floor, no
> building. In a clinic with multiple dentists working simultaneously in different rooms,
> this information must be communicated entirely outside the system. A room or location
> field on the dentist profile would allow this to be shown on the appointment confirmation
> slip, on the day view and potentially on a waiting-area display.

---

### 11. Treatment — same as the online patient from here

Once the patient is with the dentist, the journey is identical to the online patient path:

- Dentist already sees the patient on their schedule
- Dentist reads any declared medical notes before treating
- Dentist gives treatment, records diagnosis, marks appointment complete
- Appointment status moves to **Ready to bill**
- Dentist tells the patient to go to reception to pay

---

### 12. Payment at reception

The patient returns to the front desk. Reception sees the appointment row highlighted
**Ready to bill** in orange on the day view and clicks **Bill**.

The billing screen shows the full cost breakdown. Reception tells the patient the amount,
collects payment and issues the bill. The appointment moves to **Paid & done**.

The patient receives a printed receipt showing exactly what was charged and what was paid.

> For the walk-in patient, this printed receipt is the only physical document they leave
> with that proves the visit happened and shows the amount paid. It is more important to
> them than to the online patient who can log in and view it later.

---

### 13. Journey complete

The walk-in patient's journey is complete. They leave with their printed receipt.

They have no portal account unless they choose to register for one later. If they return
to the clinic in the future, reception can find their record by searching for their name
or contact number.

---

## Gaps summary — walk-in physical patient path

| # | Gap | Impact | Real-world need |
|---|---|---|---|
| 1 | Clinic telephone number and email not stored or shown anywhere in the system | Patient who wants to call cannot find the number from within the system | Configure `clinic.phone` and `clinic.email` in `clinic.properties`; show on help page and appointment slip |
| 2 | No "Our dentists" quick-reference screen for reception | Reception must recall doctor details from memory when advising a walk-in patient | A read-only dentist overview screen in the reception portal |
| 3 | Walk-in screen shows all slots but has no filter by dentist | Reception must scan the full slot table to find a specific doctor's availability | Add dentist filter to the Walk-in screen |
| 4 | No cost breakdown visible from the Walk-in screen before booking | Reception must navigate away or calculate manually before quoting the patient | Show consultation fee + service charge + treatment price on the Walk-in slot table |
| 5 | No printable appointment confirmation slip for walk-in patients | Patient may forget their appointment time, date or doctor | A "Print appointment card" function after booking — separate from the billing receipt |
| 6 | No SMS reminder for patients without email or portal access | Walk-in patients with no email get no reminder before their appointment | SMS integration using the patient's contact number |
| 7 | No waiting queue or "call patient" function | Reception manages the waiting room verbally | A queue display or notification system — larger scope enhancement |
| 8 | No room or location information on the dentist record | Patient must be told verbally which room to go to | Add `room` field to the dentist profile; show on appointment slip and day view |
| 9 | No in-system notification when dentist marks treatment complete | Reception must watch the day view; patient may arrive at desk before status updates | Push notification or auto-refresh on the reception day view |
