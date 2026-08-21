# Patient Journey — Online Path

**Who this is:** A patient who has some technological awareness — uses a smartphone, is
familiar with booking things online, and first becomes aware of Sunrise Dental Clinic
through its website or through a social channel such as Facebook, Instagram or a Google
search result. They have a dental symptom or a routine need and want to get it sorted.

This is one of two patient journeys. The other — the patient who walks directly into the
physical clinic without any prior online contact — is documented in
[patient-walkin-journey.md](patient-walkin-journey.md).

---

## The journey, step by step

### 1. First contact — reading the website

The patient finds the clinic online. Before doing anything, they read.

They want to know:
- What services the clinic offers
- Who the dentists are and what they specialise in
- Where the clinic is physically located
- How to contact the front desk (telephone number, email)
- Whether the clinic is accepting new patients

They form an impression of the clinic from what they read. If they are satisfied, they
move forward. If the information is not there or is unclear, many will leave and look
elsewhere.

> **[GAP]** The current system has no public-facing website with this information. The
> home page (`/login`) is a role chooser — a page for people who already have an account.
> A first-time visitor landing on it sees three login portals and nothing about the clinic
> itself: no services list, no dentist profiles, no location, no contact number.
>
> This is a significant real-world gap. A clinic website and the appointment booking system
> are two different things. In production, the public website would typically exist
> separately (a WordPress site, a landing page, or similar) and link to this system's
> `/register` page when a visitor is ready to book. Until that exists, patients who find
> the system URL directly have no way to learn about the clinic before being asked to
> create an account.

---

### 2. Deciding to book — calling reception first (optional)

Some patients, even tech-aware ones, prefer to speak to a person before committing.
They telephone the clinic using the number from the website, ask about availability,
describe their symptom, and ask which dentist would be right for them. Reception answers
their questions. The patient may then go online to book, or ask reception to book on their
behalf.

> **[GAP]** The clinic telephone number and email are referenced in the help page
> (`/help`) via `${clinicPhone}` and `${clinicEmail}` attributes — but these are never
> populated. They show blank. The contact details of the clinic are not configurable in
> `clinic.properties` and not shown anywhere in the system.

---

### 3. Creating an account

The patient navigates to the registration page (`/register`). They fill in:
- Full name
- Contact number
- Email address
- Password
- Date of birth (optional)
- Address (optional)

On submission, both their login account and their patient profile are created in one
step. They are signed in immediately and land on their appointments page.

**What the patient sees first:** an empty appointments list with a message telling them
nothing is booked yet and inviting them to book.

---

### 4. Returning patient — signing in

If the patient already has an account from a previous visit, they go to `/login`, choose
**I am a patient**, and sign in with their email and password. They land on their
appointments page showing their history.

> Note: the patient portal (`/login/patient`) is the only one linked from the main login
> chooser. The staff portals (reception, dentist, administrator) are not shown to the
> public. The administrator portal is not shown at all on the chooser — it is reached
> directly at `/login/admin` only.

---

### 5. Appointments page — the patient's home screen

After signing in, the patient lands on `/patient/home`. For a new patient this is empty.
For a returning patient it shows every appointment — upcoming and past — with:

- Appointment number (e.g. `APT-20260821-0001`)
- Dentist name
- Treatment type
- Date and time
- Status (Waiting / Ready to bill / Paid & done / Cancelled)

From here they can cancel an upcoming appointment or view the bill for a completed one.

---

### 6. Booking an appointment — Step 1: Dentist and date

The patient clicks **Book** in the navigation. The booking page has two cards.

The first card is **Dentist and date**.

The patient chooses a dentist from the dropdown. The dropdown shows the dentist's name
only — clean and readable. After selecting a dentist and clicking **Show times**, a
details panel appears below showing:

- The dentist's specialisation (e.g. Orthodontics)
- The consultation fee (e.g. Rs 2,500.00) — with an explanation that this is charged for
  every visit with that dentist
- The service charge (e.g. Rs 200.00) — with an explanation that this is a fixed clinic
  fee per appointment
- A pointer to the treatment cost in the dropdown below
- An estimated total formula: *treatment cost + consultation fee + service charge*
- A note that this is an estimate and the final bill is issued by reception after the
  appointment

The patient also chooses a date. Today or any future date is allowed.

> **[GAP]** The patient cannot see a dentist's rating or any profile detail (photo,
> biography, years of experience) when choosing. They see name, specialisation and fee
> only. FR-PAT-67 defers showing ratings at booking by decision; a biography/profile
> page for each dentist does not exist.

---

### 7. Booking — Step 2: Treatment and time

The second card appears — **Treatment and time** — once the dentist and date are selected.

The patient sees:
- A **treatment dropdown** showing treatment names only (no prices in the option)
- Below the dropdown, a reference table listing every available treatment with its name,
  description and price — so the patient can compare before choosing
- The available time slots for that dentist on that date — only times that are not already
  booked by another patient and that the dentist has made available

> The time slots shown are only the genuinely open ones. A patient is never shown a time
> they cannot have. Slots already taken by other patients simply do not appear.

The patient selects a treatment from the dropdown and selects a time slot by clicking it
(the slot is highlighted in orange when selected). They then click **Confirm booking**.

> **[GAP]** The treatment dropdown and the time slots are independent — changing the
> treatment does not reload the page, so the estimated total in the dentist card above
> does not update dynamically when a different treatment is selected. Because the system
> uses no client-side JavaScript, the total updates only when the dentist or date is
> changed (which triggers a page reload via Show times). A patient must mentally calculate
> their total using the treatment price reference table.

---

### 8. Booking confirmed

On confirmation the patient is returned to their appointments page. A confirmation
message shows the appointment number (e.g. `APT-20260821-0001`) and tells them to quote
it if they telephone the clinic. The appointment appears in their list with status
**Waiting**.

> **[GAP]** No confirmation email or SMS is sent to the patient. FR-PAT-16 specifies that
> booking must trigger a confirmation message to the patient's recorded address. The event
> is published internally (the observer pattern is wired) but no notification channel
> implementation exists. The patient receives nothing outside the system.

---

### 9. Waiting for the appointment day

The patient waits. They can log in at any time to see their upcoming appointment.

> **[GAP]** No reminder is sent before the appointment day. In a real clinic, a reminder
> SMS or email the day before significantly reduces no-shows — which the system's own
> report tracks as a metric (no-show rate). The reminder infrastructure is not built.
> The patient must remember the appointment on their own.

---

### 10. Arriving at the clinic

On the appointment day the patient travels to the physical location of the clinic and
walks in.

They approach the reception desk and tell the receptionist:
- Their name
- Which dentist they have an appointment with
- Their appointment number (if they have it)

Reception looks them up on the day view (`/reception/home`) and confirms the appointment.

> **[GAP]** There is currently no room or location information in the system. A clinic
> with multiple dentists working in different rooms has no way to tell the patient which
> room their doctor is in. The patient must ask reception verbally — "Which room is
> Dr. Silva in?" — and reception must know this outside the system.

---

### 11. With the dentist

The dentist is already aware the patient is coming. When the dentist logs in and views
their schedule (`/dentist/schedule`), they see the patient listed with:

- Appointment time
- Patient name
- Treatment booked
- Treatment base cost
- A warning if the patient has declared anything critical in their medical notes (an
  allergy, a medication, a condition)

The dentist opens the appointment to read the patient's full declared medical notes before
treating. They give the treatment.

> The dentist cannot see the patient's bill or revenue figures. The patient's diagnosis
> is confidential to the dentist and the patient — reception and the administrator cannot
> read it.

After treating the patient, the dentist records the diagnosis (what was found and what
was done) in a free-text field and clicks **Record and complete**. The appointment status
moves to **Ready to bill**.

The dentist tells the patient verbally: *"Please go to reception to pay."*

> **[GAP]** There is no in-system notification from the dentist to the patient or to
> reception at the moment the appointment is marked complete. Reception only knows the
> appointment is ready to bill by checking the day view. In a busy clinic, the patient
> may reach the reception desk before the status has been refreshed on screen.

---

### 12. Payment at reception

The patient returns to the reception desk.

Reception looks at the day view. The appointment row for this patient now shows
**Ready to bill** in orange — visually distinct from waiting patients. Reception clicks
the **Bill** button directly on that row.

The billing screen shows the appointment details and the calculated bill:
- Treatment cost
- Consultation fee
- Service charge
- Any applicable discount
- Total

Reception confirms the amount with the patient, collects payment and issues the bill.
The appointment status moves to **Paid & done**.

The patient receives a printable receipt showing the full bill breakdown.

---

### 13. Journey complete

The patient's journey through the online path is now complete. Their appointment appears
in their portal as **Paid & done**. They can log in at any time to view or print their
receipt.

---

## Gaps summary — online patient path

| # | Gap | Impact | Status |
|---|---|---|---|
| 1 | No public-facing website — home page is a role chooser with no clinic information | New patients cannot learn about the clinic before being asked to register | Open |
| 2 | Clinic contact details (phone, email) not populated anywhere in the system | Patients cannot find the telephone number from within the system | Open |
| 3 | No dentist profile or biography page | Patient chooses a dentist by name and specialisation only | Open |
| 4 | Estimated total does not update dynamically when treatment is changed | Patient must calculate manually using the reference table | Open — requires JavaScript |
| 5 | No confirmation message (email/SMS) after booking | Patient has no record outside the system | Open — FR-PAT-16 |
| 6 | No appointment reminder before the appointment day | High no-show risk | Open — FR-NOT-01 |
| 7 | No room or location information for dentists | Patient must ask reception which room to go to | Open |
| 8 | No in-system notification when dentist marks treatment complete | Reception must manually check day view; patient may arrive before status refreshes | Open |
