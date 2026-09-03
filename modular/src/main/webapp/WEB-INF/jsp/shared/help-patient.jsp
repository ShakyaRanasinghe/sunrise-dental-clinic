<%@ include file="/WEB-INF/jsp/shared/taglibs.jspf" %>
<c:set var="pageTitle" value="Patient help" />
<%@ include file="/WEB-INF/jsp/shared/header.jspf" %>

<%--
    GAP-FTB-12: the patient's own help page. The shared /help page is for visitors;
    this one is for a signed-in patient (and for someone creating an account), so it
    covers the dashboard, booking, details, notes, concerns and ratings — the screens
    a patient actually touches (srs-patient.md).
--%>

<h1 class="page-title">Patient help</h1>
<p class="page-subtitle">How to use your patient account, step by step.</p>

<div class="card help-toc">
    <h2>Help topics</h2>
    <div class="help-toc__links">
        <a class="help-toc__item" href="#dashboard">Your dashboard</a>
        <a class="help-toc__item" href="#booking">Booking a visit</a>
        <a class="help-toc__item" href="#details">Your details</a>
        <a class="help-toc__item" href="#notes">Allergies and medications</a>
        <a class="help-toc__item" href="#concern">Raising a concern</a>
        <a class="help-toc__item" href="#rating">Rating a visit</a>
        <a class="help-toc__item" href="#trouble">Something went wrong</a>
    </div>
</div>

<div class="card" id="dashboard">
    <h2>Your dashboard</h2>
    <p class="page-subtitle">Everything lives under <strong>My appointments</strong>.</p>
    <ol class="help-steps">
        <li class="help-steps__item">
            <span class="help-steps__no" aria-hidden="true">1</span>
            <span class="help-steps__text">Open <a href="${ctx}/patient/home">My appointments</a> to see your upcoming and past visits, each with its number, dentist, treatment, date, time and status.</span>
        </li>
        <li class="help-steps__item">
            <span class="help-steps__no" aria-hidden="true">2</span>
            <span class="help-steps__text">A visit you can no longer make is cancelled from the same row: open <strong>Cancel</strong>, then confirm with <strong>Yes, cancel it</strong>. One click alone never cancels.</span>
        </li>
        <li class="help-steps__item">
            <span class="help-steps__no" aria-hidden="true">3</span>
            <span class="help-steps__text">A visit you have paid for shows a <strong>BILLED</strong> pill — open it to see the receipt for that visit.</span>
        </li>
    </ol>
</div>

<div class="card" id="booking">
    <h2>Booking a visit</h2>
    <ol class="help-steps">
        <li class="help-steps__item">
            <span class="help-steps__no" aria-hidden="true">1</span>
            <span class="help-steps__text">Open <a href="${ctx}/patient/book">Book</a>. If you have no dentist in mind, the <strong>open days</strong> overview shows who has free appointments soon — clicking a date jumps straight there.</span>
        </li>
        <li class="help-steps__item">
            <span class="help-steps__no" aria-hidden="true">2</span>
            <span class="help-steps__text">Pick the dentist's <strong>available time</strong> first, then the <strong>treatment</strong> below it. Use the info mark beside a treatment to read what it covers, or choose <strong>Other</strong> and describe the visit in your own words.</span>
        </li>
        <li class="help-steps__item">
            <span class="help-steps__no" aria-hidden="true">3</span>
            <span class="help-steps__text">Confirm with the button naming your dentist. Your appointment number is shown at once — quote it if you telephone the clinic.</span>
        </li>
    </ol>
</div>

<div class="card" id="details">
    <h2>Your details</h2>
    <p>Open <a href="${ctx}/patient/profile">My details</a> from the account menu. Your contact details and diagnosis details are shown read-only; choose <strong>Edit details</strong> to change what you may, then save. A confirmation window tells you the change is stored.</p>
</div>

<div class="card" id="notes">
    <h2>Allergies and medications</h2>
    <p>Declare them from <a href="${ctx}/patient/profile">My details</a> so your dentist sees them before treating you. Mark anything urgent as <strong>critical</strong>. Only you and the dentist treating you can see these notes — never reception, never the administrator.</p>
</div>

<div class="card" id="concern">
    <h2>Raising a concern</h2>
    <p>Open <a href="${ctx}/patient/complaints">Raise a concern</a>, choose the dentist from your own visit history, and write what happened. It is read by the clinic's administrator only — the dentist you name never sees it, and it never affects your appointments or your ability to book.</p>
</div>

<div class="card" id="rating">
    <h2>Rating a visit</h2>
    <p>After a finished visit you can leave a star rating from the dashboard, and change it within 30 days. Your dentist sees only an overall average, never your individual comment.</p>
</div>

<div class="card" id="trouble">
    <h2>Something went wrong?</h2>
    <p class="page-subtitle">Tap the message you saw to read what to do about it.</p>
    <div class="help-faq">
        <details class="help-faq__item">
            <summary>&ldquo;Incorrect email or password.&rdquo;</summary>
            <p>Check both. If you are sure they are right, you may be on the wrong sign-in page for your role — or the account may be locked (below).</p>
        </details>
        <details class="help-faq__item">
            <summary>&ldquo;This account is locked.&rdquo;</summary>
            <p>Five wrong passwords lock the account and it does not unlock itself. Ask the clinic administrator to clear it.</p>
        </details>
        <details class="help-faq__item">
            <summary>Signed out unexpectedly</summary>
            <p>Sessions end after 30 minutes of inactivity. Sign in again &mdash; your appointments and record are all still there.</p>
        </details>
    </div>
</div>

<div class="card" id="contact">
    <h2>Still stuck? Talk to the clinic</h2>
    <p class="page-subtitle">Prefer a person? Call or write to us &mdash; a question needs no appointment.</p>
    <div class="help-contact">
        <a class="help-contact__item" href="tel:${clinicPhone}">
            <span class="help-contact__label">Call us</span>
            <span class="help-contact__value"><c:out value="${clinicPhone}" /></span>
        </a>
        <a class="help-contact__item" href="mailto:${clinicEmail}">
            <span class="help-contact__label">Email us</span>
            <span class="help-contact__value"><c:out value="${clinicEmail}" /></span>
        </a>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/shared/footer.jspf" %>
