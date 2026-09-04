# Manual test scenarios

Written to be followed by a person, in a browser, in order. Every scenario is something that
actually happens at a dental practice, described in the words the practice would use — not in
the words the code uses.

There is an automated equivalent in `scripts/smoke.sh`, and these are not a substitute for it.
They exist because a script asserts a status code and a person notices that the button is in a
silly place, the message does not answer the question, or the page looks broken. Both find
things the other cannot.

---

## How to use this

**Before you start**

```bash
./scripts/dev-up.sh          # a clean database and a fresh deploy, every time
```

Run that between scenario groups. Several scenarios lock an account or use up a time slot, and
starting from a known state is faster than unpicking what the last one did.

**The accounts.** Every password is `Password123`.

| Who | Portal | Sign-in |
|---|---|---|
| Nimal Perera, a patient | `/login/patient` | `nimal@example.lk` |
| Kumari Silva, reception | `/login/reception` | username `reception` |
| Dr. Ranil Silva | `/login/dentist` | username `silva` |
| Dr. Malini Jayasuriya | `/login/dentist` | username `jayasuriya` |
| Anoma Fernando, administrator | `/login/admin` | username `admin` |

**How to read a scenario.** Each has a **Given** (what must already be true), numbered
**When** steps in plain language, and a **Then** listing what you should be able to see. A
scenario passes when every line of the Then is true *and* nothing else looks wrong.

**When one fails.** Note the scenario number, what you saw instead, and whether the page showed
an error or simply the wrong thing. Those two failures have different causes: an error means a
rule refused you, and the wrong thing means a rule was missing.

---

## Group A — Getting in

### A1. A patient signs in and lands somewhere useful
**Given** you are not signed in.
1. Open the clinic's address. You are sent to a page offering three ways in.
2. Choose **I am a patient**.
3. Enter Nimal's email and password, and sign in.

**Then**
- You are on your own appointments page, not a generic home page.
- The header greets you by name and says **PATIENT**.
- The navigation offers Book, My details and Raise a concern — and nothing belonging to staff.

### A2. The administrator's door is not advertised
**Given** you are not signed in.
1. Look at the three options on the sign-in page.
2. Now go directly to `/login/admin` and sign in as Anoma.

**Then**
- The chooser offered patient, reception and dentist — **not** administrator.
- The administrator's portal works perfectly well when you go to it directly.
- *Why:* nobody in the public needs to know the clinic has an administrator's login. It is not
  hidden from staff, only from the front page.

### A3. A wrong password and the wrong door look identical
**Given** you are on the patient sign-in page.
1. Enter Nimal's email with the password `Wrong999`. Read the message.
2. Now enter **Anoma's** email with her **correct** password — on the *patient* page. Read it.
3. Now enter `nobody@example.lk` with any password. Read it.

**Then**
- All three say exactly the same thing: *"Incorrect email or password."*
- *Why:* if the wrong-door message differed, anyone could type an address into the
  administrator's page and learn from the error whether that person is staff. Worse, on the
  patient page they could learn whether somebody is a patient here — which is itself medical
  information.

### A4. Five wrong passwords lock the account, and waiting does not help
**Given** nobody has failed a sign-in yet.
1. Enter Nimal's email with a wrong password. Do it **five times**.
2. Try a sixth time, with the wrong password. Read the message.
3. Now try the **correct** password.

**Then**
- The first five say *"Incorrect email or password."*
- The sixth says the account is locked, and tells you to contact the clinic.
- The correct password is refused too.
- The message does **not** say "try again later" — waiting will never help. Only an
  administrator can clear it (see E3).

### A5. Signing out really ends the session
**Given** you are signed in as anybody.
1. Note a page you can currently see.
2. Sign out from the header.
3. Use the browser's Back button, then reload the page.

**Then**
- You are returned to the sign-in page, not to the page you were on.
- Reloading does not get you back in. The session is gone, not hidden.

---

## Group B — A day at the clinic

This is the core journey, and it is worth walking in one sitting because each step depends on
the one before.

### B1. Reception opens the diary for tomorrow
**Given** you are signed in as Kumari at reception.
1. Go to **Availability**.
2. Choose Dr. Ranil Silva, tomorrow's date, 09:00 to 12:00, appointments of 30 minutes.
3. Publish.

**Then**
- The page reports **six bookable times published**.
- Below the form, the six times 09:00 to 11:30 are listed, none of them ticked.
- The caption explains that ticked times are already booked.

### B2. Reception is stopped from double-publishing
**Given** B1 has been done.
1. Try to publish Dr. Silva, the same day, 11:00 to 13:00.
2. Read the refusal.
3. Now publish 12:00 to 14:00 instead.

**Then**
- The first attempt is refused, and the message names the window already there:
  *"Dr. Ranil Silva already has 09:00-12:00 published on …"*
- The second attempt succeeds — 12:00 follows 11:30 without overlapping it, which is how a full
  day gets published.
- *Why this matters:* the times are identified by dentist, date and start, so an overlapping
  window would have silently replaced the first window's times — and any of them already booked
  would have quietly become free again, with a patient still holding an appointment for it.

### B3. Reception is warned about an uneven window
**Given** you are on the availability page.
1. Publish Dr. Malini Jayasuriya, tomorrow, 16:00 to 17:20, in 30-minute appointments.
2. Read both messages.

**Then**
- It publishes, and says **two** bookable times.
- A second message warns that the window is 80 minutes, which does not divide evenly, and that
  the last 20 minutes are not bookable.
- *Why:* publishing 16:00–17:20 is a perfectly reasonable thing to want. Silently discarding
  twenty minutes is not.

### B4. A patient books one of those times
**Given** B1 has been done. Sign in as Nimal.
1. Go to **Book**.
2. Choose Dr. Ranil Silva and tomorrow's date. Show the times.
3. Choose a treatment — Scaling & polishing.
4. Click **09:30**.

**Then**
- You are returned to your appointments page with a confirmation.
- It gives you an **appointment number** like `APT-20260821-0001`, and tells you to quote it if
  you telephone.
- The appointment is listed with the dentist, the treatment and the status **CONFIRMED**.
- The dentist dropdown showed a real name and fee — *"Dr. Ranil Silva — General Dentistry
  (Rs 1,500.00)"* — not a code.

### B5. The time disappears for everyone else
**Given** B4 has been done.
1. Still as Nimal, go back to **Book** and show Dr. Silva's times for tomorrow again.

**Then**
- **09:30 is no longer offered.** Five times remain.
- *Why:* a patient must never be shown a time they cannot have.

### B6. Reception sees the booking without being told
**Given** B4 has been done. Sign in as Kumari.
1. Go to the day view and set the date to tomorrow.

**Then**
- The 09:30 appointment is there, showing **Nimal Perera**, **Dr. Ranil Silva** and
  **Scaling & polishing**.
- Reception typed none of it. It arrived because the patient booked.
- There is **no diagnosis column** anywhere on this page.

### B7. The dentist sees only their own day
**Given** B4 has been done. Sign in as Dr. Ranil Silva.
1. Look at the schedule for tomorrow.
2. Sign out, sign in as Dr. Malini Jayasuriya, and look at tomorrow.

**Then**
- Dr. Silva sees Nimal's 09:30 appointment.
- Dr. Jayasuriya does **not** — she sees only her own 16:00 window from B3, with nothing booked.
- Neither of them can point the page at the other's day: there is no dentist to choose, because
  the schedule is resolved from who signed in.

### B8. The dentist records what they treated
**Given** you are Dr. Ranil Silva, looking at tomorrow.
1. In Nimal's appointment, type what you treated — *"Scaling done, no decay"*.
2. Record and complete.

**Then**
- The page confirms it, and the appointment now reads **COMPLETED**.
- The form is gone — there is nothing further to do on it.
- *Why the form disappears:* the appointment cannot be completed twice, so offering the button
  again would offer something that would be refused.

### B9. Reception bills the visit
**Given** B8 has been done. Sign in as Kumari.
1. Go to **Billing** and set the date to tomorrow.
2. Find Nimal's completed appointment and click **Bill**.

**Then**
- You are taken straight to a receipt.
- It shows the patient, the dentist, the treatment, and the charges: consultation
  **Rs 1,500.00**, treatment **Rs 3,500.00**, service charge **Rs 200.00**, total
  **Rs 5,200.00**.
- The totals have thousands separators and two decimal places — *Rs 5,200.00*, not *5200.0*.
- There is a **Print** button, and printing gives you the receipt without the site navigation.

### B10. The patient can see their own receipt
**Given** B9 has been done. Sign in as Nimal.
1. From your appointments, open the receipt for that visit.

**Then**
- You see the same figures the front desk saw.
- You do **not** see how the money was divided between the dentist and the clinic.

---

## Group C — Money

### C1. The owner can see where the money went
**Given** B9 has been done. Sign in as Anoma.
1. Open **Reports**. Leave the dates as they are.
2. Read the **Where the money went** panel.

**Then**
- Gross takings **Rs 5,200.00**.
- Dentists **Rs 3,600.00** — the whole consultation fee plus 60% of the treatment.
- The clinic **Rs 1,600.00** — the rest of the treatment plus the whole service charge.
- Reception **Rs 0.00**.
- The three add up to the gross exactly. **If they do not, the page says so and tells you not
  to use the figures** — check that warning is absent.
- *The clinic's figure is the owner's margin:* what the patient paid less what the dentist is
  paid.

### C2. Every report says what it is for
**Given** you are on the reports page with some activity in the period.
1. Read the small print under each panel.

**Then**
- Every panel has a **Supports:** line naming a decision — whether the practice is busy enough,
  which days are worth staffing, whether a reminder policy would pay for itself.
- *Why:* a figure with no decision attached is trivia, and a report full of trivia is one
  nobody opens twice.

### C3. A quiet period says so
**Given** you are on the reports page.
1. Set the dates to a range well in the past — a year ago, for a week.

**Then**
- The page says **nothing happened in this period** and suggests a wider range.
- It does **not** show an empty chart or a row of zeroes.
- *Why:* an empty chart looks like a broken chart, and you cannot tell which you are looking at.

### C4. The report can be taken away
**Given** you are on the reports page.
1. Click **Download CSV**.
2. Open it in a spreadsheet.

**Then**
- The file is named for the period, like `sunrise-report-2026-07-22-to-2026-08-20.csv`.
- It has sections: Summary, Earnings by dentist, Earnings by receptionist, Daily takings, Daily
  footfall.
- Any accented character or em dash reads correctly, not as `â€”`.
- Every figure matches what the screen showed.

### C5. A visit cannot be billed twice
**Given** B9 has been done.
1. As Kumari, go back to Billing for that day.
2. Look at the row for Nimal's appointment.

**Then**
- There is no **Bill** button. There is a **Receipt** link instead.
- *Why:* one bill per visit. The screen does not offer the second one, and if you reached it
  another way the clinic would refuse it and name the receipt that already covers it.

### C6. An untreated visit cannot be billed
**Given** B1 has been done. Sign in as Nimal and book **10:00**, then sign in as Kumari.
1. Go to Billing for that day.

**Then**
- The 10:00 appointment is **not listed**. Only treated appointments appear.
- *Why:* the dentist records the treatment first, and then it can be billed. Listing it would
  invite the desk to try something the clinic refuses.

---

## Group D — Who is allowed to see what

**This group is the most valuable in this document.** Three rules point in three different
directions, and none of them follows seniority.

### D1. A patient tells their dentist about an allergy
**Given** you are signed in as Nimal.
1. Go to **My details**.
2. Read the notice about who sees this.
3. Add a note: kind **Allergy**, detail *"Allergic to penicillin"*, and tick **Important**.
4. Add a second: kind **Condition**, detail *"Type 2 diabetes"*, not important.

**Then**
- The notice said plainly: you and the dentist treating you — **not** the front desk, and
  **not** the administrator.
- Both notes are listed, and the important one is **first**.
- The important one is marked as such.
- *Why the order:* a list that puts the allergy third is a list that gets skimmed past.

### D2. The dentist is warned before the patient arrives
**Given** D1 and B4 have been done. Sign in as Dr. Ranil Silva.
1. Look at the schedule for the day of Nimal's appointment.

**Then**
- Above Nimal's appointment: **"This patient has declared something important."**
- The warning is on the schedule, **before** you open anything.
- There is a link to see what they declared.

### D3. The dentist reads the notes beside the appointment
**Given** D2. 
1. Follow the link to see what Nimal declared.

**Then**
- Both notes are there, with their kinds, and the allergy marked important.
- The page says the patient declared these and only they can change them.
- **There is no way to edit or delete one.** No button, no field.
- *Why:* the patient owns their own record. A dentist who disagrees records that as part of the
  treatment, not by rewriting what the patient said.

### D4. Reception cannot see the notes at all
**Given** D1 has been done. Sign in as Kumari.
1. Look at the patient register and find Nimal.
2. Look at the day view and find his appointment.

**Then**
- **Nowhere** is there a medical note, an allergy, or a diagnosis.
- Not a redacted note. Not "hidden". Nothing — there is no place on any of reception's screens
  where one could appear.
- *Why:* reception books and bills. Why the patient came is not theirs to read.

### D5. The administrator cannot see them either
**Given** D1 has been done. Sign in as Anoma.
1. Look at Reports, Accounts, Audit and Complaints.

**Then**
- No medical note anywhere, and no diagnosis.
- The administrator reads totals, accounts and the audit trail. Not the clinical record.
- *Why this is worth checking:* the administrator can do more than reception in almost every
  other respect. Authority here follows **purpose**, not rank.

### D6. Another dentist cannot see them
**Given** D1 and B4. Sign in as Dr. Malini Jayasuriya.
1. Look at your schedule for the day of Nimal's appointment.

**Then**
- Nimal is not on it, and there is no route to his notes.
- *Why:* holding a dentist's login makes you clinical staff. It does not make you *this
  patient's* dentist.

### D7. A patient with nothing declared says so explicitly
**Given** reception has registered a walk-in who has declared nothing, and that patient has a
treated appointment with Dr. Silva.
1. As Dr. Silva, open that appointment.

**Then**
- It says **"Nothing declared"** and names what that means — no allergies, medication or
  conditions recorded.
- It is **not** a blank space.
- *Why:* a blank looks like a screen that failed to load, and a dentist cannot tell the
  difference between "no allergies" and "the allergies did not render".

### D8. A patient complains about a dentist
**Given** B8 has been done. Sign in as Nimal.
1. Go to **Raise a concern**.
2. Read the notice before typing anything.
3. Choose the visit, the dentist, the category **Waiting time**, and describe what happened in a
   sentence or two.
4. Send it.

**Then**
- The notice said, **before** you typed: the administrator reads this; the dentist you name will
  not see it, nor that you raised one; reception will not see it; and it does not affect your
  appointments.
- It is confirmed and appears in your list, marked **Received**.
- *Why the notice comes first:* somebody deciding whether to complain about the person treating
  them next month needs to know that beforehand, not afterwards.

### D9. The dentist named in it never learns of it
**Given** D8 has been done. Sign in as Dr. Ranil Silva.
1. Look at every page you can reach — the schedule, the appointment, everything.

**Then**
- There is **no complaint anywhere**, and no sign one exists. No count, no badge, no notice.
- There is no page in the dentist's navigation that could hold one.
- *Why:* a complaints process that the subject of the complaint can read is a process nobody
  uses.

### D10. Reception cannot see it either
**Given** D8. Sign in as Kumari.

**Then** nothing on any reception screen refers to it.

### D11. A patient rates the visit, and the dentist sees only an average
**Given** B8 has been done. Sign in as Nimal.
1. Rate the visit — 2 stars — and write *"The waiting room was filthy"*.
2. Sign out. Sign in as Dr. Ranil Silva.
3. Look for anything about ratings.

**Then**
- Dr. Silva can see **how many** reviews he has and that there is **no average yet**.
- He cannot see the 2, the comment, or the date.
- *Why:* one review, and he would know exactly who wrote it. A review the patient believes is
  confidential is not confidential.
- *And why no average:* it is withheld below five reviews. One bad visit should not define a
  career, and an average of two is not a measurement — it is two opinions with a decimal point.

### D12. The administrator can read the comment
**Given** D11. Sign in as Anoma.
1. Ask for the reviews of Dr. Silva — `/api/reviews/d-silva`.

**Then**
- The rating and the comment are both there.
- There is no way to change or delete either. *Why:* curating the feedback would make the
  average worthless.

---

## Group E — The administrator's work

### E1. A new dentist joins the practice
**Given** you are signed in as Anoma.
1. Go to **Accounts**.
2. Create an account: name *Dr. Priya Fernando*, an email, role **DENTIST**, specialisation
   *Periodontics*, fee *2000.00*.
3. **Write down the password it shows you.**
4. Reload the page.

**Then**
- A one-time password is shown once, large and readable, with a warning that it is not stored
  anywhere and cannot be looked up.
- After reloading, **it is gone**. There is no way to see it again.
- *Why shown only once:* the administrator must never be able to read anybody's password. If it
  is lost before the new dentist signs in, reset it and issue another.

### E2. The new dentist can work immediately
**Given** E1.
1. Sign out. Sign in at the dentist portal with the new email and the password you wrote down.
2. Look at the schedule.

**Then**
- It works, and the schedule loads — empty, but loading.
- *Why this is worth testing:* creating the login is not enough. A dentist also needs a dentist
  *record*, and creating one without the other gives you an account that signs in and then
  fails. Both are created in the same step.

### E3. The administrator unlocks a locked patient
**Given** A4 has been done, so Nimal is locked out.
1. As Anoma, go to **Accounts**.
2. Find Nimal. His state is **Locked**.
3. Unlock him.
4. Sign out and sign in as Nimal with the correct password.

**Then**
- The list showed him as Locked, distinctly from Active.
- After unlocking he signs in normally.

### E4. The administrator cannot lock themselves out
**Given** you are on the Accounts screen as Anoma.
1. Find your own row.

**Then**
- There is **no Deactivate button** on it — only on other people's rows.
- *Why:* deactivating the only account that can reactivate accounts leaves the clinic with no
  way in.

### E5. The trail answers who changed an appointment
**Given** B4 and B8 have been done. Sign in as Anoma.
1. Go to **Audit** and search for the appointment number.

**Then**
- The whole history is there, newest first: it was created by the patient, completed by the
  dentist, and — if C-group was done — billed by reception.
- Each row names **who**, their role, and when.
- Some rows have no actor. Those come from the database itself and record that a status changed
  even if the application was not involved — the empty actor is the signal.
- **There is no way to edit or delete a row.** No button anywhere.

### E6. The administrator reviews the complaint
**Given** D8 has been done. Sign in as Anoma.
1. Go to **Complaints**.
2. Start reviewing the one that is there.
3. Try to close it with the word *"sorted"*.
4. Close it properly, describing what was done.
5. Sign out, sign in as Nimal, and look at your concerns.

**Then**
- Open complaints are listed before closed ones, and marked distinctly.
- You could not resolve it without first starting to review it — somebody has to have looked
  at it.
- *"sorted"* was refused: it asks you to say what was done, because a concern closed with no
  explanation is a concern ignored with extra steps.
- **The patient's own words are shown and cannot be edited.** There is no field, no button.
- Nimal now sees the state has changed, and sees what the clinic did about it — beside what he
  wrote, not instead of it.

### E7. The administrator can cover the front desk
**Given** you are signed in as Anoma.
1. Go to `/reception/patients`. Then `/reception/home`.
2. Now try `/patient/home` and `/dentist/schedule`.

**Then**
- The two reception screens work.
- The patient's own pages and the dentist's schedule are **refused**.
- *Why:* the administrator holds every action the front desk performs, so barring the addresses
  while permitting the operations would have been an inconsistency. It is not a master key: a
  patient's own pages and the clinical record are still closed.

---

## Group F — Things that must be refused

Each of these is a real mistake somebody will make.

| # | Do this | It must |
|---|---|---|
| F1 | As a patient, open `/admin/reports` by typing it | Answer **Not permitted**, and say your account does not have access. Not a redirect — a redirect would suggest the address was wrong, when it exists and is not yours |
| F2 | As a dentist, open `/reception/billing` | Be refused the same way |
| F3 | As reception, publish availability for a date in the past | Be refused: *"date cannot be in the past"* |
| F4 | As reception, publish for a dentist who does not exist (edit the URL) | Answer **not found**, naming the dentist — not a server error |
| F5 | As reception, publish 09:00–09:20 in 30-minute appointments | Be refused, because no appointment would fit |
| F6 | As a patient, book a time somebody has already taken (two browsers, same time) | Exactly one succeeds. The other is told the time has just been taken and to choose another |
| F7 | As a dentist, try to complete a colleague's appointment | Be refused: *"That appointment is not yours to treat."* Holding a dentist's login is not enough |
| F8 | As a patient, try to cancel somebody else's appointment | Be refused |
| F9 | As reception, register a walk-in on a contact number already on file | **Succeed**, and warn that the number is on another record, naming it. Two people can share a telephone — a household, a parent and child — so this is advice, not a refusal |
| F10 | As reception, register a walk-in with a date of birth in the future | Be refused |
| F11 | As a patient, try to rate an appointment that has not happened | Be refused: you can rate a visit once it has happened |
| F12 | As a patient, try to rate a cancelled appointment | Be refused: there is nothing to rate |
| F13 | As a patient, raise a concern about a dentist who has never treated you | Be refused. A complaints channel open against anybody is a channel for abuse |
| F14 | As a patient, submit a concern of three words | Be refused, and asked to describe what happened — the administrator can only act on what you tell them |
| F15 | Try to cancel an appointment that has been billed | Be refused, and told why. The slot must stay taken while the bill stands |
| F16 | Ask for a page that does not exist, signed in | A **Not found** page in the clinic's own styling — not a Tomcat error page |
| F17 | Ask for a page that does not exist, signed **out** | The sign-in page. Which addresses exist is not information for somebody who has not signed in |

---

## Group G — Regressions

Every one of these was a real defect in this application. Each was invisible to the unit tests
and found only by using the thing. **Check these after any change to a servlet, a JSP, a DAO or
the schema.**

### G1. Every page has a doctype
1. View source on any page.

**Then** the very first line is `<!doctype html>`, with nothing before it — not even a blank
line. *Was:* nine blank lines and no doctype, so every screen rendered in quirks mode and the
stylesheet was computed against the wrong box model.

### G2. Clicking Sign in actually signs you in
1. Sign in by **clicking the button**, not by pressing Enter.

**Then** it works. *Was:* the form posted to the JSP's own path and answered 404. Every
automated check had posted straight to the route and passed.

### G3. An error says what went wrong
1. As Anoma, open `/reception/availability` — a page she may enter but an action she does not hold.

**Then** the page says **Not permitted** and *"Your role (ADMIN) does not permit this action."*
*Was:* every page-level error answered 404 with "That address does not exist" — a 403 looked
like a missing page, whatever had actually happened.

### G4. A refusal that cannot be retried says so
1. Bill a visit twice (through the API, since the screen does not offer it).

**Then** it is refused with a message naming the receipt that already covers it. *Was:*
"Something went wrong. Please try again" — and trying again never worked.

### G5. An appointment can be completed at all
1. Complete any appointment.

**Then** it works. *Was:* a database trigger refused every update to an appointment, so
completing and cancelling both failed with a message about the slot being booked. Booking worked,
which is why it went unnoticed.

### G6. Appointment numbers survive a restart
1. Book an appointment. Note its number.
2. `docker restart sunrise-tomcat`, wait for it to come back.
3. Book another.

**Then** the second number is the next one — `-0002`, not `-0001` again. *Was:* the counter
lived in memory, so the first booking after a restart re-used a number that already existed on
the primary key.

### G7. Dates are the clinic's, not the server's
1. As Anoma, open Reports and check the end of the default date range.

**Then** it is **today** in the clinic's timezone. Bills issued today appear in today's takings,
and "patients seen" is not zero beside real bills. *Was:* dates were computed in UTC, so for
five and a half hours every night takings landed on the previous day and disagreed with the
appointment book.

### G8. Accented characters and dashes render correctly
1. As Nimal, look at the seeded medical notes on **My details**.
2. Add a note containing an em dash and the word *café*.

**Then** both read correctly. No `â€”`, no `Ã©`. *Was:* the seed data was loaded with the MySQL
client's latin1 default and stored double-encoded — invisible until the application read it back.

### G9. Money keeps its decimal places
1. Look at a receipt, and at the reports.

**Then** every amount has two decimal places and a thousands separator: **Rs 5,200.00**. *Was:*
money was held as `double`, and a total assembled from several of them drifts from the figure the
database computes for the same inputs.

### G10. The revenue split adds up
1. As Anoma, read the **Where the money went** panel.

**Then** the three shares sum to the gross **exactly**, and there is no warning telling you not
to trust the figures. *Why this is checked by hand as well as automatically:* a split that misses
by a cent leaves a ledger permanently short with nobody able to say where.

---

## What is not covered here, and why

| Not tested | Because |
|---|---|
| Email and SMS | Not built. The notifications module was not migrated — see the verification section of [`../srs/srs.md`](../srs/srs.md) |
| Confirming before a destructive action | Not built. It would need client-side script, and the application deliberately has almost none |
| Correcting a patient's details | Not built. The register can add and search but not correct — recorded as outstanding |
| Managing the treatment catalogue | Not built. A "should" requirement, and the first thing cut |
| Payment | Out of scope by design. The clinic calculates and prints a bill; it does not record that money changed hands |
