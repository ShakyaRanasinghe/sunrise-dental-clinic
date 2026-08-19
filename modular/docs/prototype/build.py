#!/usr/bin/env python3
"""
Generates the prototype, grouped by role.

    access/     sign-in, shared by everyone
    patient/    the patient's own screens
    reception/  front desk
    dentist/    the treating clinician
    admin/      clinic management
    shared/     help, errors

A generator rather than hand-written files, because the shared pieces here are the
JSP fragments: `header()` becomes shared/header.jspf, `login_form()` becomes
access/login-form.jspf. Translating the prototype is then mechanical.

Note the prototype groups by ROLE and the JSP views group by FEATURE MODULE. That
is deliberate: this tree answers "whose screen is this?", which is what a reviewer
asks; the JSP tree answers "which module owns it?", which is what a maintainer
asks. Every page's black bar names both its route and its target .jsp.

    python3 build.py
"""

import os
import pathlib

OUT = pathlib.Path(__file__).parent

# --------------------------------------------------------------------------
# Navigation per role — mirrors what RolePolicy will return.
# Paths are relative to the prototype root; rel() rewrites them per page.
# --------------------------------------------------------------------------
NAV = {
    "patient": [("patient/home.html", "My appointments"), ("patient/book.html", "Book"),
                ("patient/profile.html", "My profile"),
                ("patient/complaints.html", "Concerns"), ("shared/help.html", "Help")],
    "reception": [("reception/day.html", "Today"), ("patient/book.html", "Book"),
                  ("reception/availability.html", "Availability"),
                  ("reception/patients.html", "Patients"),
                  ("reception/billing.html", "Billing"), ("shared/help.html", "Help")],
    "dentist": [("dentist/schedule.html", "My schedule"), ("shared/help.html", "Help")],
    "admin": [("admin/reports.html", "Reports"), ("admin/accounts.html", "Accounts"),
              ("admin/complaints.html", "Complaints"), ("shared/help.html", "Help")],
}

WHO = {
    "patient":   ("Nimal Perera", "PATIENT"),
    "reception": ("Kumari Silva", "RECEPTIONIST"),
    "dentist":   ("Dr. Ranil Silva", "DENTIST"),
    "admin":     ("Anoma Fernando", "ADMIN"),
}

ROLE_DIR = {"patient": "patient", "reception": "reception",
            "dentist": "dentist", "admin": "admin"}


def rel(from_dir, target):
    """Path to `target` (root-relative) from a page living in `from_dir`."""
    if not from_dir:
        return target
    return os.path.relpath(target, from_dir).replace(os.sep, "/")


def proto_bar(from_dir, route, view, whose):
    return f"""<div class="proto-bar">
  <span class="tag">{whose}</span>
  <a href="{rel(from_dir, 'index.html')}">All screens</a>
  <span>{route}</span>
  <span>&rarr; {view}</span>
</div>"""


def header(role, active, from_dir, route, view):
    """Becomes shared/header.jspf."""
    whose = f"{role.upper()} SCREEN" if role else "PUBLIC"
    bar = proto_bar(from_dir, route, view, whose)
    if role is None:
        return bar
    name, tag = WHO[role]
    links = "".join(
        f'\n        <a href="{rel(from_dir, href)}"'
        f'{" class=\"active\"" if href == active else ""}>{label}</a>'
        for href, label in NAV[role])
    return f"""{bar}
<header class="site-header">
  <div class="bar">
    <a class="brand" href="{rel(from_dir, NAV[role][0][0])}">Sunrise <span>Dental</span></a>
    <nav class="site-nav">{links}
    </nav>
    <div class="who">
      {name} <span class="role-tag">{tag}</span>
      <a class="btn small secondary"
         href="{rel(from_dir, 'access/portal-chooser.html')}">Sign out</a>
    </div>
  </div>
</header>
<main>"""


FOOTER = """</main>
<footer class="site-footer">
  Sunrise Dental Clinic &mdash; Appointment &amp; Patient Management System
</footer>"""

DOC = """<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{title}</title>
<link rel="stylesheet" href="{css}">
</head>
<body>
{header}
{body}
{footer}
</body>
</html>
"""


def login_form():
    """Becomes access/login-form.jspf — included by all four portal views."""
    return """    <form method="post" action="#">
      <div class="field">
        <label for="email">Email</label>
        <input type="email" id="email" name="email" autocomplete="username" autofocus>
      </div>
      <div class="field">
        <label for="password">Password</label>
        <input type="password" id="password" name="password" autocomplete="current-password">
      </div>
      <div class="form-actions">
        <button type="submit" class="btn" style="width:100%">Sign in</button>
      </div>
    </form>"""


def portal_page(label, note, extra=""):
    """The four portal views. Identical but for the label, note and extras."""
    return f"""<div class="narrow">
  <div class="brand-block">
    <div class="name">Sunrise Dental Clinic</div>
    <div class="tagline">Appointments &amp; patient records</div>
  </div>
  <div class="card">
    <span class="portal-label">{label}</span>
    <p class="page-subtitle" style="margin-top:0">{note}</p>
{login_form()}
  </div>
  {extra}
  <div class="alt"><a href="../shared/help.html">Need help signing in?</a></div>
  <div class="alt"><a href="portal-chooser.html">&larr; All sign-in options</a></div>
</div>"""


# --------------------------------------------------------------------------
PAGES = []


def page(path, role, active, route, view, body):
    """path is root-relative, e.g. 'patient/home.html'."""
    PAGES.append((path, role, active, route, view, body))


# ======================= access/ — everyone ===============================
page("access/portal-chooser.html", None, None, "GET /login", "access/portal-chooser.jsp", """
<div class="narrow">
  <div class="brand-block">
    <div class="name">Sunrise Dental Clinic</div>
    <div class="tagline">Appointments &amp; patient records</div>
  </div>
  <div class="card">
    <h2>Sign in</h2>
    <p class="page-subtitle">Choose how you use the clinic.</p>
    <div class="portals">
      <a class="portal" href="login-patient.html">
        <div class="name">I am a patient</div>
        <div class="what">Book, view or cancel your own appointments and see your receipts.</div>
      </a>
      <a class="portal" href="login-reception.html">
        <div class="name">Reception</div>
        <div class="what">Register patients, book on their behalf, publish availability, issue bills.</div>
      </a>
      <a class="portal" href="login-dentist.html">
        <div class="name">Dentist</div>
        <div class="what">Your own schedule, and recording what you treated.</div>
      </a>
    </div>
  </div>
  <div class="alt">New patient? <a href="register.html">Create an account</a></div>
  <div class="alt"><a href="../shared/help.html">Need help?</a></div>
</div>""")

page("access/login-patient.html", None, None, "GET /login/patient",
     "access/login-patient.jsp",
     portal_page("Patient", "Sign in to manage your appointments.",
                 '<div class="alt">New patient? <a href="register.html">Create an account</a></div>'))
page("access/login-reception.html", None, None, "GET /login/reception",
     "access/login-reception.jsp", portal_page("Reception", "Front desk access."))
page("access/login-dentist.html", None, None, "GET /login/dentist",
     "access/login-dentist.jsp", portal_page("Dentist", "Your schedule and treatment records."))
page("access/login-admin.html", None, None, "GET /login/admin",
     "access/login-admin.jsp", portal_page("Administrator", "Clinic reports and accounts."))

page("access/register.html", None, None, "GET /register", "access/register.jsp", """
<div class="narrow">
  <div class="brand-block">
    <div class="name">Sunrise Dental Clinic</div>
    <div class="tagline">Create a patient account</div>
  </div>
  <div class="card">
    <div class="notice info">Only patients create their own account. Reception, dentist and
      administrator accounts are issued by the clinic administrator.</div>
    <form method="post" action="#">
      <div class="field"><label>Full name</label><input type="text"></div>
      <div class="form-row">
        <div class="field"><label>Email</label><input type="email"></div>
        <div class="field"><label>Contact number</label><input type="tel" placeholder="0771234567"></div>
      </div>
      <div class="form-row">
        <div class="field"><label>Address <span class="page-subtitle">(optional)</span></label><input type="text"></div>
        <div class="field"><label>Date of birth <span class="page-subtitle">(optional)</span></label><input type="date"></div>
      </div>
      <div class="field"><label>Password</label><input type="password">
        <div class="page-subtitle">At least 8 characters.</div></div>
      <div class="notice">Nothing medical is asked here. You can add allergies and
        medications from your profile once you have signed in.</div>
      <div class="form-actions"><button class="btn" style="width:100%">Create account</button></div>
    </form>
  </div>
  <div class="alt">Already registered? <a href="login-patient.html">Sign in</a></div>
</div>""")

# ======================= patient/ =========================================
page("patient/home.html", "patient", "patient/home.html",
     "GET /patient/home", "appointments/patient-home.jsp", """
<h1 class="page-title">My appointments</h1>
<p class="page-subtitle">Everything booked, and what you have already been treated for.</p>

<div class="card">
  <h2>Upcoming <span class="count">1</span></h2>
  <div class="table-wrap">
    <table>
      <thead><tr><th>Reference</th><th>When</th><th>Dentist</th><th>Treatment</th><th>Status</th><th></th></tr></thead>
      <tbody>
        <tr><td>APT-20260901-0001</td><td>1 Sep 2026, 09:00</td><td>Dr. Ranil Silva</td>
            <td>Scaling &amp; polishing</td><td><span class="pill confirmed">Confirmed</span></td>
            <td><a class="btn small secondary" href="#">Cancel</a></td></tr>
      </tbody>
    </table>
  </div>
</div>

<div class="card">
  <h2>Past visits <span class="count">1</span></h2>
  <div class="table-wrap">
    <table>
      <thead><tr><th>Reference</th><th>When</th><th>Treatment</th><th>Status</th><th></th></tr></thead>
      <tbody>
        <tr><td>APT-20260714-0003</td><td>14 Jul 2026, 10:30</td><td>Routine check-up</td>
            <td><span class="pill billed">Billed</span></td>
            <td><a class="btn small secondary" href="../reception/billing.html">Receipt</a></td></tr>
      </tbody>
    </table>
  </div>
  <div class="clinical">
    <div class="label">Your dentist's note &mdash; 14 Jul 2026</div>
    Mild plaque on lower incisors. Advised twice-daily brushing; review in six months.
  </div>
</div>

<div class="card">
  <h2>How was your visit? <span class="count">14 Jul 2026, Dr. Malini Jayasuriya</span></h2>
  <div class="slots">
    <div class="slot"><input type="radio" name="stars" id="r1"><label for="r1">1</label></div>
    <div class="slot"><input type="radio" name="stars" id="r2"><label for="r2">2</label></div>
    <div class="slot"><input type="radio" name="stars" id="r3"><label for="r3">3</label></div>
    <div class="slot"><input type="radio" name="stars" id="r4" checked><label for="r4">4</label></div>
    <div class="slot"><input type="radio" name="stars" id="r5"><label for="r5">5</label></div>
  </div>
  <div class="field"><label>Anything to add? <span class="page-subtitle">(optional)</span></label>
    <textarea rows="2" placeholder="Only if you want to&hellip;"></textarea></div>
  <div class="notice">Your dentist sees an average across all their patients, never your
    individual rating or comment. The clinic manager can see what you wrote.</div>
  <div class="form-actions">
    <button class="btn">Save rating</button>
    <button class="btn secondary">Skip</button>
  </div>
  <p class="page-subtitle">You can change this for 30 days after the visit. Something gone
    wrong rather than just middling? <a href="complaints.html">Raise a concern</a> instead.</p>
</div>

<div class="form-actions"><a class="btn" href="book.html">Book an appointment</a></div>""")

page("patient/book.html", "patient", "patient/book.html",
     "GET /patient/book", "appointments/book.jsp", """
<h1 class="page-title">Book an appointment</h1>
<p class="page-subtitle">Choose a dentist and a day, then pick a time that suits you.</p>

<div class="card">
  <h2>1 &nbsp;Choose a dentist and date</h2>
  <div class="form-row">
    <div class="field"><label>Dentist</label>
      <select>
        <option>Dr. Ranil Silva &mdash; General Dentistry &mdash; Rs 1,500</option>
        <option>Dr. Malini Jayasuriya &mdash; Orthodontics &mdash; Rs 2,500</option>
      </select></div>
    <div class="field"><label>Date</label><input type="date" value="2026-09-01"></div>
  </div>
</div>

<div class="card">
  <h2>2 &nbsp;Pick a time <span class="count">4 open</span></h2>
  <div class="slots">
    <div class="slot"><input type="radio" name="slot" id="s1" checked><label for="s1">09:00</label></div>
    <div class="slot"><input type="radio" name="slot" id="s2"><label for="s2">09:30</label></div>
    <div class="slot"><input type="radio" name="slot" id="s3"><label for="s3">11:00</label></div>
    <div class="slot"><input type="radio" name="slot" id="s4"><label for="s4">11:30</label></div>
  </div>
  <p class="page-subtitle">Times already booked are not shown.</p>
</div>

<div class="card">
  <h2>3 &nbsp;What do you need?</h2>
  <div class="field"><label>Treatment</label>
    <select>
      <option>Routine check-up &mdash; Rs 1,000</option>
      <option selected>Scaling &amp; polishing &mdash; Rs 3,500</option>
      <option>Composite filling &mdash; Rs 4,500</option>
      <option>Extraction &mdash; Rs 5,000</option>
    </select></div>
  <div class="notice info">Total on your bill after treatment:
    consultation Rs 1,500 + treatment Rs 3,500 + service Rs 200.</div>
  <div class="form-actions"><button class="btn">Confirm booking</button></div>
</div>""")

page("patient/profile.html", "patient", "patient/profile.html",
     "GET /patient/profile", "patients/profile.jsp", """
<h1 class="page-title">My profile</h1>
<p class="page-subtitle">Your contact details, and anything a dentist should know before treating you.</p>

<div class="card">
  <h2>Contact details</h2>
  <div class="form-row">
    <div class="field"><label>Full name</label><input type="text" value="Nimal Perera"></div>
    <div class="field"><label>Contact number</label><input type="tel" value="0771234567"></div>
  </div>
  <div class="form-row">
    <div class="field"><label>Email</label><input type="email" value="nimal@example.lk"></div>
    <div class="field"><label>Date of birth</label><input type="date" value="1988-04-12"></div>
  </div>
  <div class="form-actions"><button class="btn">Save</button></div>
</div>

<div class="card">
  <h2>Medical notes <span class="count">2</span></h2>
  <div class="notice info">These notes go to the dentist who treats you, so they know before
    they start. Reception and clinic staff cannot see them. Add anything a dentist should
    know &mdash; medicines you react badly to, medicines you take, conditions you have.</div>

  <div class="note-list">
    <div class="note-item critical">
      <span class="note-cat allergy">Allergy</span>
      <strong>Critical</strong>
      <div>Penicillin &mdash; rash and swelling. Confirmed by my GP.</div>
      <div class="meta">Added 2 Jul 2026 &middot; last changed 2 Jul 2026</div>
      <div class="actions">
        <a class="btn small secondary" href="#">Edit</a>
        <a class="btn small secondary" href="#">Delete</a>
      </div>
    </div>
    <div class="note-item critical">
      <span class="note-cat medication">Medication</span>
      <strong>Critical</strong>
      <div>Warfarin, 3mg daily.</div>
      <div class="meta">Added 2 Jul 2026 &middot; last changed 2 Jul 2026</div>
      <div class="actions">
        <a class="btn small secondary" href="#">Edit</a>
        <a class="btn small secondary" href="#">Delete</a>
      </div>
    </div>
  </div>
</div>

<div class="card">
  <h2>Add a note</h2>
  <div class="form-row">
    <div class="field"><label>What kind of note is this?</label>
      <select>
        <option>Allergy &mdash; something I react badly to</option>
        <option>Medication &mdash; something I take</option>
        <option>Condition &mdash; something I have</option>
        <option>Other</option>
      </select></div>
    <div class="field"><label>&nbsp;</label>
      <label style="font-weight:400"><input type="checkbox"> A dentist must see this before treating me</label>
    </div>
  </div>
  <div class="field"><label>Details</label>
    <textarea rows="3" placeholder="What a dentist should know, and why&hellip;"></textarea></div>
  <div class="form-actions"><button class="btn">Add note</button></div>
</div>""")

page("patient/complaints.html", "patient", "patient/complaints.html",
     "GET /patient/complaints", "complaints/patient-complaints.jsp", """
<h1 class="page-title">Raise a concern</h1>
<p class="page-subtitle">Tell the clinic about a problem with your care.</p>

<div class="card">
  <h2>New concern</h2>
  <div class="notice info">This goes to the clinic administrator. The dentist you name will
    not see it, and it will not affect your appointments or your care. Tell us what happened,
    with dates if you remember them.</div>
  <div class="form-row">
    <div class="field"><label>Which dentist?</label>
      <select>
        <option>Dr. Ranil Silva &mdash; treated you 1 Sep 2026</option>
        <option>Dr. Ranil Silva &mdash; treated you 14 Jul 2026</option>
      </select>
      <div class="page-subtitle">Only dentists who have treated you are listed.</div>
    </div>
    <div class="field"><label>What is this about?</label>
      <select>
        <option>Conduct</option>
        <option>A concern about my treatment</option>
        <option>Waiting time</option>
        <option>A charge on my bill</option>
        <option>Something else</option>
      </select></div>
  </div>
  <div class="field"><label>Which appointment? <span class="page-subtitle">(optional)</span></label>
    <select>
      <option>Not about a specific appointment</option>
      <option>APT-20260901-0001 &mdash; 1 Sep 2026, 09:00</option>
      <option>APT-20260714-0003 &mdash; 14 Jul 2026, 10:30</option>
    </select></div>
  <div class="field"><label>What happened?</label>
    <textarea rows="5" placeholder="Please describe what happened&hellip;"></textarea>
    <div class="page-subtitle">At least a couple of sentences, so the clinic can look into it.</div></div>
  <div class="form-actions"><button class="btn">Submit concern</button></div>
</div>

<div class="card">
  <h2>Concerns you have raised <span class="count">2</span></h2>
  <div class="table-wrap">
    <table>
      <thead><tr><th>Raised</th><th>About</th><th>Dentist</th><th>State</th></tr></thead>
      <tbody>
        <tr><td>2 Sep 2026</td><td>Conduct</td><td>Dr. Ranil Silva</td>
            <td><span class="pill review">Under review</span></td></tr>
        <tr><td>18 Jul 2026</td><td>Waiting time</td><td>Dr. Malini Jayasuriya</td>
            <td><span class="pill resolved">Resolved</span></td></tr>
      </tbody>
    </table>
  </div>
  <div class="notice success"><strong>Resolved 22 Jul 2026.</strong> Afternoon clinics have
    been re-scheduled with longer gaps. Thank you for telling us.</div>
  <p class="page-subtitle">A concern cannot be changed or withdrawn once submitted.</p>
</div>""")

# ======================= reception/ =======================================
page("reception/day.html", "reception", "reception/day.html",
     "GET /reception/home", "appointments/reception-day.jsp", """
<h1 class="page-title">Today</h1>
<p class="page-subtitle">Everything booked for the selected day.</p>

<div class="card">
  <div class="form-row">
    <div class="field"><label>Showing</label><input type="date" value="2026-09-01"></div>
    <div class="field" style="align-self:end"><a class="btn" href="../patient/book.html">New booking</a></div>
  </div>
</div>

<div class="grid stats">
  <div class="stat"><div class="label">Booked</div><div class="value">6</div></div>
  <div class="stat"><div class="label">Completed</div><div class="value">2</div></div>
  <div class="stat"><div class="label">To bill</div><div class="value">2</div></div>
  <div class="stat"><div class="label">Cancelled</div><div class="value">1</div></div>
</div>

<div class="card">
  <h2>1 September 2026 <span class="count">6 appointments</span></h2>
  <div class="table-wrap">
    <table>
      <thead><tr><th>Time</th><th>Reference</th><th>Patient</th><th>Dentist</th><th>Treatment</th><th>Status</th><th></th></tr></thead>
      <tbody>
        <tr><td>09:00</td><td>APT-20260901-0001</td><td>Nimal Perera</td><td>Dr. Silva</td>
            <td>Scaling &amp; polishing</td><td><span class="pill completed">Completed</span></td>
            <td><a class="btn small" href="billing.html">Bill</a></td></tr>
        <tr><td>09:30</td><td>APT-20260901-0002</td><td>Sanduni Rathnayake</td><td>Dr. Silva</td>
            <td>Routine check-up</td><td><span class="pill confirmed">Confirmed</span></td>
            <td><a class="btn small secondary" href="#">Cancel</a></td></tr>
        <tr><td>10:00</td><td>APT-20260901-0003</td><td>Arun Wickrama</td><td>Dr. Jayasuriya</td>
            <td>Composite filling</td><td><span class="pill billed">Billed</span></td>
            <td><a class="btn small secondary" href="billing.html">Receipt</a></td></tr>
        <tr><td>11:00</td><td>APT-20260901-0004</td><td>Kamal Fernando</td><td>Dr. Silva</td>
            <td>Extraction</td><td><span class="pill cancelled">Cancelled</span></td><td></td></tr>
      </tbody>
    </table>
  </div>
  <p class="page-subtitle">Diagnosis notes and patients' medical notes are not shown to
    reception &mdash; only the treating dentist and the patient can see them.</p>
</div>""")

page("reception/patients.html", "reception", "reception/patients.html",
     "GET /reception/patients", "patients/records.jsp", """
<h1 class="page-title">Patient records</h1>
<p class="page-subtitle">Search the register, or add someone who has just walked in.</p>

<div class="grid two">
  <div class="card">
    <h2>Search</h2>
    <div class="field"><label>Name, contact number or email</label>
      <input type="search" placeholder="e.g. Perera or 077&hellip;"></div>
    <div class="form-actions"><button class="btn">Search</button></div>
    <p class="page-subtitle">One field covers all three &mdash; type whatever the patient gives you.</p>
  </div>
  <div class="card">
    <h2>Add a walk-in</h2>
    <div class="field"><label>Full name</label><input type="text"></div>
    <div class="form-row">
      <div class="field"><label>Contact number</label><input type="tel"></div>
      <div class="field"><label>Email <span class="page-subtitle">(optional)</span></label><input type="email"></div>
    </div>
    <div class="notice">This creates a patient record, not a login. The patient can create
      their own account later if they want online booking.</div>
    <div class="form-actions"><button class="btn">Add patient</button></div>
  </div>
</div>

<div class="card">
  <h2>All patients <span class="count">3</span></h2>
  <div class="table-wrap">
    <table>
      <thead><tr><th>Name</th><th>Contact</th><th>Email</th><th>Date of birth</th><th>Portal account</th><th></th></tr></thead>
      <tbody>
        <tr><td>Nimal Perera</td><td>0771234567</td><td>nimal@example.lk</td><td>1988-04-12</td>
            <td><span class="pill open">Yes</span></td><td><a class="btn small" href="../patient/book.html">Book</a></td></tr>
        <tr><td>Sanduni Rathnayake</td><td>0759876543</td><td>sanduni@example.lk</td><td>1995-11-02</td>
            <td><span class="pill">Walk-in</span></td><td><a class="btn small" href="../patient/book.html">Book</a></td></tr>
        <tr><td>Arun Wickrama</td><td>0712223334</td><td></td><td>1972-01-25</td>
            <td><span class="pill">Walk-in</span></td><td><a class="btn small" href="../patient/book.html">Book</a></td></tr>
      </tbody>
    </table>
  </div>
</div>""")

page("reception/availability.html", "reception", "reception/availability.html",
     "GET /reception/availability", "scheduling/availability.jsp", """
<h1 class="page-title">Publish availability</h1>
<p class="page-subtitle">Set the hours a dentist works; the bookable times are generated for you.</p>

<div class="card">
  <h2>New session</h2>
  <div class="form-row">
    <div class="field"><label>Dentist</label>
      <select><option>Dr. Ranil Silva</option><option>Dr. Malini Jayasuriya</option></select></div>
    <div class="field"><label>Date</label><input type="date" value="2026-09-02"></div>
  </div>
  <div class="form-row">
    <div class="field"><label>From</label><input type="time" value="09:00"></div>
    <div class="field"><label>To</label><input type="time" value="12:00"></div>
    <div class="field"><label>Each appointment</label>
      <select><option>30 minutes</option><option>20 minutes</option><option>45 minutes</option></select></div>
  </div>
  <div class="notice info">09:00 to 12:00 in 30-minute appointments will create
    <strong>6 bookable times</strong>.</div>
  <div class="form-actions"><button class="btn">Publish</button></div>
</div>

<div class="card">
  <h2>Published <span class="count">Dr. Silva, 2 Sep 2026</span></h2>
  <div class="slots">
    <div class="slot"><input type="checkbox" id="p1" checked disabled><label for="p1">09:00</label></div>
    <div class="slot"><input type="checkbox" id="p2" checked disabled><label for="p2">09:30</label></div>
    <div class="slot"><input type="checkbox" id="p3" disabled><label for="p3">10:00</label></div>
    <div class="slot"><input type="checkbox" id="p4" disabled><label for="p4">10:30</label></div>
    <div class="slot"><input type="checkbox" id="p5" disabled><label for="p5">11:00</label></div>
    <div class="slot"><input type="checkbox" id="p6" disabled><label for="p6">11:30</label></div>
  </div>
  <p class="page-subtitle">Ticked times are already booked.</p>
</div>""")

page("reception/billing.html", "reception", "reception/billing.html",
     "GET /reception/billing", "billing/billing.jsp", """
<h1 class="page-title">Billing</h1>
<p class="page-subtitle">Look up an appointment to issue or reprint its receipt.</p>

<div class="card">
  <div class="form-row">
    <div class="field"><label>Appointment reference</label>
      <input type="text" value="APT-20260901-0001"></div>
    <div class="field" style="align-self:end"><button class="btn">Look up</button></div>
  </div>
</div>

<div class="card">
  <h2>Bill</h2>
  <div class="receipt">
    <table>
      <tbody>
        <tr><td>Patient</td><td>Nimal Perera</td></tr>
        <tr><td>Appointment</td><td>APT-20260901-0001 &mdash; 1 Sep 2026, 09:00</td></tr>
        <tr><td>Dentist</td><td>Dr. Ranil Silva</td></tr>
        <tr><td>Treatment</td><td>Scaling &amp; polishing</td></tr>
        <tr><td>Consultation fee</td><td>Rs 1,500.00</td></tr>
        <tr><td>Treatment cost</td><td>Rs 3,500.00</td></tr>
        <tr><td>Service charge</td><td>Rs 200.00</td></tr>
        <tr><td><strong>Total</strong></td><td><strong>Rs 5,200.00</strong></td></tr>
      </tbody>
    </table>
  </div>
  <div class="form-actions">
    <button class="btn">Issue bill</button>
    <button class="btn secondary">Print</button>
  </div>
  <p class="page-subtitle">A bill can only be issued once the dentist has marked the
    appointment complete, and only once per appointment.</p>
</div>""")

# ======================= dentist/ =========================================
page("dentist/schedule.html", "dentist", "dentist/schedule.html",
     "GET /dentist/schedule", "appointments/dentist-schedule.jsp", """
<h1 class="page-title">My schedule</h1>
<p class="page-subtitle">Your own appointments. Nobody else's.</p>

<div class="card">
  <div class="field" style="max-width:220px"><label>Showing</label>
    <input type="date" value="2026-09-01"></div>
</div>

<div class="card">
  <h2>1 September 2026 <span class="count">3 patients</span></h2>
  <div class="table-wrap">
    <table>
      <thead><tr><th>Time</th><th>Reference</th><th>Patient</th><th>Treatment</th><th>Notes</th><th>Status</th><th></th></tr></thead>
      <tbody>
        <tr><td>09:00</td><td>APT-20260901-0001</td><td>Nimal Perera</td>
            <td>Scaling &amp; polishing</td>
            <td><span class="pill cancelled">2 critical</span></td>
            <td><span class="pill confirmed">Confirmed</span></td>
            <td><a class="btn small" href="#open">Open</a></td></tr>
        <tr><td>09:30</td><td>APT-20260901-0002</td><td>Sanduni Rathnayake</td>
            <td>Routine check-up</td>
            <td><span class="page-subtitle">None declared</span></td>
            <td><span class="pill confirmed">Confirmed</span></td>
            <td><a class="btn small secondary" href="#">Open</a></td></tr>
        <tr><td>11:00</td><td>APT-20260901-0005</td><td>Kamal Fernando</td>
            <td>Root canal therapy</td>
            <td><span class="pill booked">1 note</span></td>
            <td><span class="pill completed">Completed</span></td>
            <td><a class="btn small secondary" href="#">Open</a></td></tr>
      </tbody>
    </table>
  </div>
  <p class="page-subtitle">The Notes column warns you before you open a record. "None declared"
    means the patient has told us nothing &mdash; not that nothing was asked.</p>
</div>

<div class="card" id="open">
  <h2>APT-20260901-0001 &nbsp;<span class="count">Nimal Perera, 09:00</span></h2>

  <div class="notice error"><strong>2 critical notes from this patient.</strong>
    Read before treating.</div>

  <div class="note-list" style="margin-bottom:16px">
    <div class="note-item critical">
      <span class="note-cat allergy">Allergy</span>
      Penicillin &mdash; rash and swelling. Confirmed by GP.
      <div class="meta">Declared by the patient &middot; last changed 2 Jul 2026</div>
    </div>
    <div class="note-item critical">
      <span class="note-cat medication">Medication</span>
      Warfarin, 3mg daily.
      <div class="meta">Declared by the patient &middot; last changed 2 Jul 2026</div>
    </div>
  </div>

  <div class="table-wrap">
    <table>
      <tbody>
        <tr><td>Contact</td><td>0771234567</td></tr>
        <tr><td>Date of birth</td><td>1988-04-12</td></tr>
        <tr><td>Treatment booked</td><td>Scaling &amp; polishing</td></tr>
      </tbody>
    </table>
  </div>

  <div class="clinical">
    <div class="label">Diagnosis &mdash; visible only to you and the patient</div>
    <textarea placeholder="What you found and what you did&hellip;"></textarea>
  </div>
  <div class="form-actions">
    <button class="btn">Save &amp; mark complete</button>
    <button class="btn secondary">Save note only</button>
  </div>
  <p class="page-subtitle">You can read the patient's medical notes but not change them &mdash;
    they belong to the patient. Your clinical findings go in the diagnosis.</p>
</div>""")

# ======================= admin/ ===========================================
page("admin/reports.html", "admin", "admin/reports.html",
     "GET /admin/reports", "reporting/reports.jsp", """
<h1 class="page-title">Clinic reports</h1>
<p class="page-subtitle">Income, revenue split and footfall for the selected period.</p>

<div class="card">
  <div class="form-row">
    <div class="field"><label>From</label><input type="date" value="2026-08-01"></div>
    <div class="field"><label>To</label><input type="date" value="2026-08-31"></div>
    <div class="field" style="align-self:end">
      <button class="btn">Apply</button>
      <button class="btn secondary">Export CSV</button>
    </div>
  </div>
</div>

<div class="grid stats">
  <div class="stat"><div class="label">Gross takings</div><div class="value money">184,600.00</div></div>
  <div class="stat"><div class="label">Bills issued</div><div class="value">37</div></div>
  <div class="stat"><div class="label">Patients seen</div><div class="value">31</div></div>
  <div class="stat"><div class="label">No-show rate</div><div class="value">8%</div></div>
</div>

<div class="card">
  <h2>Where the money went</h2>
  <div class="grid stats">
    <div class="stat"><div class="label">Dentists</div><div class="value money">102,300.00</div></div>
    <div class="stat"><div class="label">Clinic</div><div class="value money">74,900.00</div></div>
    <div class="stat"><div class="label">Reception</div><div class="value money">7,400.00</div></div>
  </div>
  <p class="page-subtitle"><strong>Supports:</strong> fee negotiation and how much the clinic
    retains for materials and facilities.</p>
</div>

<div class="grid two">
  <div class="card">
    <h2>Earnings by dentist</h2>
    <div class="table-wrap">
      <table>
        <thead><tr><th>Dentist</th><th>Appointments</th><th>Earned</th><th>Rating</th><th>Concerns</th></tr></thead>
        <tbody>
          <tr><td>Dr. Ranil Silva</td><td>23</td><td>Rs 61,400.00</td><td>4.4 <span class="page-subtitle">(37)</span></td><td>1</td></tr>
          <tr><td>Dr. Malini Jayasuriya</td><td>14</td><td>Rs 40,900.00</td><td>3.9 <span class="page-subtitle">(21)</span></td><td>0</td></tr>
        </tbody>
      </table>
    </div>
    <p class="page-subtitle"><strong>Supports:</strong> rostering, retention, and spotting a
      dentist earning well on a falling rating. The Concerns column is a count only &mdash; read
      them under <a href="complaints.html">Complaints</a>. Dentists see their own rating average
      but never an individual review.</p>
  </div>
  <div class="card">
    <h2>Footfall</h2>
    <div class="table-wrap">
      <table>
        <thead><tr><th>Week</th><th>Attended</th><th>No-shows</th></tr></thead>
        <tbody>
          <tr><td>3&ndash;9 Aug</td><td>9</td><td>1</td></tr>
          <tr><td>10&ndash;16 Aug</td><td>11</td><td>0</td></tr>
          <tr><td>17&ndash;23 Aug</td><td>7</td><td>2</td></tr>
          <tr><td>24&ndash;31 Aug</td><td>4</td><td>0</td></tr>
        </tbody>
      </table>
    </div>
    <p class="page-subtitle"><strong>Supports:</strong> staffing levels and opening hours.</p>
  </div>
</div>

<div class="notice">Account locking and deactivation are on the
  <a href="accounts.html">Accounts</a> screen, not here.</div>""")

page("admin/accounts.html", "admin", "admin/accounts.html",
     "GET /admin/accounts", "reporting/accounts.jsp", """
<h1 class="page-title">Accounts</h1>
<p class="page-subtitle">Create a staff account, or unlock and deactivate any account.</p>

<div class="notice info"><strong>Create</strong> covers reception, dentist and administrator
  only &mdash; patients create their own. <strong>Unlock</strong> and
  <strong>deactivate</strong> cover every account, patients included.</div>

<div class="grid two">
  <div class="card">
    <h2>New staff account</h2>
    <div class="field"><label>Full name</label><input type="text"></div>
    <div class="field"><label>Email</label><input type="email"></div>
    <div class="field"><label>Role</label>
      <select><option>Receptionist</option><option>Dentist</option><option>Administrator</option></select>
      <div class="page-subtitle">Patient is not an option here.</div></div>
    <div class="form-row">
      <div class="field"><label>Specialization <span class="page-subtitle">(dentist only)</span></label><input type="text"></div>
      <div class="field"><label>Consultation fee <span class="page-subtitle">(dentist only)</span></label><input type="number" value="1500"></div>
    </div>
    <div class="notice">A one-time password is issued to the account holder, who must change it
      on first sign-in. You never see or set it.</div>
    <div class="form-actions"><button class="btn">Create account</button></div>
  </div>

  <div class="card">
    <h2>All accounts <span class="count">5</span></h2>
    <div class="table-wrap">
      <table>
        <thead><tr><th>Name</th><th>Role</th><th>State</th><th></th></tr></thead>
        <tbody>
          <tr><td>Kumari Silva</td><td>Receptionist</td><td><span class="pill open">Active</span></td>
              <td><a class="btn small secondary" href="#">Deactivate</a></td></tr>
          <tr><td>Dr. Ranil Silva</td><td>Dentist</td><td><span class="pill open">Active</span></td>
              <td><a class="btn small secondary" href="#">Deactivate</a></td></tr>
          <tr><td>Dr. Malini Jayasuriya</td><td>Dentist</td><td><span class="pill open">Active</span></td>
              <td><a class="btn small secondary" href="#">Deactivate</a></td></tr>
          <tr><td>Nimal Perera</td><td>Patient</td><td><span class="pill cancelled">Locked</span></td>
              <td><a class="btn small" href="#">Unlock</a>
                  <a class="btn small secondary" href="#">Deactivate</a></td></tr>
          <tr><td>Anoma Fernando</td><td>Administrator</td><td><span class="pill open">Active</span></td>
              <td><span class="page-subtitle">You</span></td></tr>
        </tbody>
      </table>
    </div>
    <p class="page-subtitle">You cannot deactivate or lock your own account &mdash; the clinic
      must keep an administrator. Deactivating a patient ends their portal access only; they
      stay bookable at the desk.</p>
  </div>
</div>""")

page("admin/complaints.html", "admin", "admin/complaints.html",
     "GET /admin/complaints", "complaints/admin-complaints.jsp", """
<h1 class="page-title">Complaints</h1>
<p class="page-subtitle">Concerns patients have raised. You are the only role who sees these.</p>

<div class="notice info">The dentist named in a complaint never sees it, and neither does
  reception. Every time you open one, that is written to the audit trail.</div>

<div class="card">
  <div class="form-row">
    <div class="field"><label>State</label>
      <select><option>Awaiting review</option><option>Under review</option>
        <option>Resolved</option><option>Dismissed</option><option>All</option></select></div>
    <div class="field"><label>Dentist</label>
      <select><option>All dentists</option><option>Dr. Ranil Silva</option>
        <option>Dr. Malini Jayasuriya</option></select></div>
    <div class="field" style="align-self:end"><button class="btn">Apply</button></div>
  </div>
</div>

<div class="grid stats">
  <div class="stat"><div class="label">Awaiting review</div><div class="value">1</div></div>
  <div class="stat"><div class="label">Under review</div><div class="value">1</div></div>
  <div class="stat"><div class="label">Resolved this month</div><div class="value">3</div></div>
</div>

<div class="card">
  <h2>Queue <span class="count">open first</span></h2>
  <div class="table-wrap">
    <table>
      <thead><tr><th>Raised</th><th>Patient</th><th>Dentist</th><th>About</th><th>State</th><th></th></tr></thead>
      <tbody>
        <tr><td>3 Sep 2026</td><td>Sanduni Rathnayake</td><td>Dr. Malini Jayasuriya</td>
            <td>A concern about treatment</td><td><span class="pill submitted">Awaiting review</span></td>
            <td><a class="btn small" href="#open">Open</a></td></tr>
        <tr><td>2 Sep 2026</td><td>Nimal Perera</td><td>Dr. Ranil Silva</td>
            <td>Conduct</td><td><span class="pill review">Under review</span></td>
            <td><a class="btn small secondary" href="#">Open</a></td></tr>
        <tr><td>18 Jul 2026</td><td>Nimal Perera</td><td>Dr. Malini Jayasuriya</td>
            <td>Waiting time</td><td><span class="pill resolved">Resolved</span></td>
            <td><a class="btn small secondary" href="#">Open</a></td></tr>
      </tbody>
    </table>
  </div>
</div>

<div class="card" id="open">
  <h2>Conduct &nbsp;<span class="count">raised 2 Sep 2026 by Nimal Perera</span></h2>
  <div class="table-wrap">
    <table>
      <tbody>
        <tr><td>Dentist named</td><td>Dr. Ranil Silva</td></tr>
        <tr><td>Appointment</td><td>APT-20260901-0001 &mdash; 1 Sep 2026, 09:00</td></tr>
        <tr><td>State</td><td><span class="pill review">Under review</span></td></tr>
      </tbody>
    </table>
  </div>

  <div class="clinical" style="border-left-color:var(--ink-faint);background:var(--canvas)">
    <div class="label" style="color:var(--ink-soft)">What the patient wrote &mdash; read only</div>
    I asked twice for an explanation of the treatment and was told there was no time.
  </div>

  <div class="notice warning">This is a conduct concern, so you can act on it directly.
    A <strong>clinical</strong> concern must be referred to another dentist &mdash; you cannot
    see diagnoses or patients' medical notes.</div>

  <div class="field"><label>Resolution</label>
    <textarea rows="3" placeholder="What you did about it&hellip;"></textarea>
    <div class="page-subtitle">Required before a concern can be closed.</div></div>
  <div class="form-actions">
    <button class="btn">Mark resolved</button>
    <button class="btn secondary">Dismiss</button>
  </div>
</div>""")

# ======================= shared/ ==========================================
page("shared/help.html", None, None, "GET /help", "shared/help.jsp", """
<main>
<h1 class="page-title">How to use the system</h1>
<p class="page-subtitle">Step by step, for anyone new to the front desk. No sign-in needed.</p>

<div class="card">
  <h2>Register a walk-in and book them</h2>
  <ol>
    <li>Open <strong>Patients</strong>.</li>
    <li>Search their contact number first &mdash; they may already be on the register.</li>
    <li>If not, fill in <strong>Add a walk-in</strong>. Name and contact number are enough.</li>
    <li>Press <strong>Book</strong> on their row, choose a dentist, day and time.</li>
    <li>Read the appointment reference back to the patient. It is how you find the visit again.</li>
  </ol>
</div>

<div class="card">
  <h2>Issue a bill</h2>
  <ol>
    <li>Open <strong>Billing</strong> and enter the appointment reference.</li>
    <li>Check the itemised total, then press <strong>Issue bill</strong>.</li>
    <li>Press <strong>Print</strong> for the patient's copy.</li>
  </ol>
  <div class="notice warning">A bill can only be issued after the dentist marks the
    appointment complete, and only once.</div>
</div>

<div class="card">
  <h2>Something went wrong</h2>
  <div class="table-wrap">
    <table>
      <thead><tr><th>What you see</th><th>What to do</th></tr></thead>
      <tbody>
        <tr><td>"That time has just been taken"</td><td>Another booking took the slot. Pick another time.</td></tr>
        <tr><td>"This account is temporarily locked"</td><td>Five wrong passwords. Ask the administrator to unlock it.</td></tr>
        <tr><td>Signed out unexpectedly</td><td>Sessions end after 30 minutes idle. Sign in again.</td></tr>
      </tbody>
    </table>
  </div>
  <p class="page-subtitle">Sunrise Dental Clinic &mdash; +94 11 234 5678 &mdash; hello@sunrisedental.lk</p>
</div>
</main>""")

# ======================= contact sheet ====================================
SHEET = [
    ("access/ &mdash; everyone", "Sign-in, shared by all four roles", [
        ("access/portal-chooser.html", "GET /login", "The four doors"),
        ("access/login-patient.html", "GET /login/patient", "Patient portal"),
        ("access/login-reception.html", "GET /login/reception", "Reception portal"),
        ("access/login-dentist.html", "GET /login/dentist", "Dentist portal"),
        ("access/login-admin.html", "GET /login/admin", "Administrator portal — not linked publicly"),
        ("access/register.html", "GET /register", "Patient self-registration"),
    ]),
    ("patient/ &mdash; the patient's dashboard", "Nimal Perera · role PATIENT", [
        ("patient/home.html", "GET /patient/home", "My appointments and past visits"),
        ("patient/book.html", "GET /patient/book", "Three-step booking"),
        ("patient/profile.html", "GET /patient/profile", "Contact details and medical notes"),
        ("patient/complaints.html", "GET /patient/complaints", "Raise a concern about a dentist"),
    ]),
    ("reception/ &mdash; the front desk", "Kumari Silva · role RECEPTIONIST", [
        ("reception/day.html", "GET /reception/home", "The day's appointments"),
        ("reception/patients.html", "GET /reception/patients", "Register: search and walk-in"),
        ("reception/availability.html", "GET /reception/availability", "Publish a dentist's session"),
        ("reception/billing.html", "GET /reception/billing", "Look up, issue, print"),
    ]),
    ("dentist/ &mdash; the treating clinician", "Dr. Ranil Silva · role DENTIST", [
        ("dentist/schedule.html", "GET /dentist/schedule", "Own schedule, patient notes, diagnosis"),
    ]),
    ("admin/ &mdash; clinic management", "Anoma Fernando · role ADMIN", [
        ("admin/reports.html", "GET /admin/reports", "Income, split, footfall"),
        ("admin/accounts.html", "GET /admin/accounts", "Create staff; unlock or deactivate anyone"),
        ("admin/complaints.html", "GET /admin/complaints", "Review and resolve concerns"),
    ]),
    ("shared/ &mdash; no sign-in", "Reachable by anyone", [
        ("shared/help.html", "GET /help", "Step-by-step guidance"),
    ]),
]


def write_index():
    groups = ""
    for title, who, rows in SHEET:
        cards = "".join(
            f'\n      <a href="{href}"><div class="route">{route}</div>'
            f'<div class="desc">{desc}</div></a>'
            for href, route, desc in rows)
        groups += f"""
  <div class="card">
    <h2>{title} <span class="count">{len(rows)}</span></h2>
    <p class="page-subtitle" style="margin-top:-6px">{who}</p>
    <div class="sheet">{cards}
    </div>
  </div>"""

    body = f"""<main>
  <h1 class="page-title">UI prototype</h1>
  <p class="page-subtitle">Eighteen static screens, grouped by whose dashboard they are.
    Same stylesheet the application uses, so a screen here is what a JSP will render.
    Nothing is wired up &mdash; forms do not submit and no data is real.</p>
{groups}
</main>"""
    (OUT / "index.html").write_text(
        DOC.format(title="UI prototype — Sunrise Dental Clinic",
                   css="css/app.css",
                   header=proto_bar("", "prototype", "contact sheet", "INDEX"),
                   body=body, footer=FOOTER),
        encoding="utf-8")


def main():
    for path, role, active, route, view, body in PAGES:
        d = os.path.dirname(path)
        (OUT / d).mkdir(parents=True, exist_ok=True)
        title = view.split("/")[-1].replace(".jsp", "").replace("-", " ").title()
        (OUT / path).write_text(
            DOC.format(title=f"{title} — Sunrise Dental Clinic",
                       css=rel(d, "css/app.css"),
                       header=header(role, active, d, route, view),
                       body=body,
                       footer=FOOTER if role else ""),
            encoding="utf-8")
        print(f"  {path:34} {route:30} {view}")
    write_index()
    print(f"  {'index.html':34} {'—':30} contact sheet")
    print(f"\n{len(PAGES) + 1} pages in 6 groups")


if __name__ == "__main__":
    main()
