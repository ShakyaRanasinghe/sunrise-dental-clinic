<%@ include file="/WEB-INF/jsp/shared/taglibs.jspf" %>
<c:set var="pageTitle" value="Dentist help" />
<%@ include file="/WEB-INF/jsp/shared/header.jspf" %>

<%--
    GAP-FTB-12: the dentist's own help page — schedule, availability publishing,
    the public phone number, offered treatments and recording treatment,
    grounded in srs-dentist.md. Reached from the dentist sign-in screen and the
    dentist nav; never advertised on a public page.
--%>

<h1 class="page-title">Dentist help</h1>
<p class="page-subtitle">How to run your practice in the system, step by step.</p>

<div class="card help-toc">
    <h2>Help topics</h2>
    <div class="help-toc__links">
        <a class="help-toc__item" href="#schedule">Your schedule</a>
        <a class="help-toc__item" href="#availability">Publishing availability</a>
        <a class="help-toc__item" href="#offered">Treatments you offer</a>
        <a class="help-toc__item" href="#treating">Recording treatment</a>
        <a class="help-toc__item" href="#trouble">Something went wrong</a>
    </div>
</div>

<div class="card" id="schedule">
    <h2>Your schedule</h2>
    <p>Open <a href="${ctx}/dentist/schedule">Schedule</a> to see today's patients first, finished visits folded below, and the week ahead last. An empty day says plainly that nothing is booked. Open an appointment to record the visit.</p>
</div>

<div class="card" id="availability">
    <h2>Publishing availability</h2>
    <ol class="help-steps">
        <li class="help-steps__item">
            <span class="help-steps__no" aria-hidden="true">1</span>
            <span class="help-steps__text">Open <a href="${ctx}/dentist/availability">Availability</a> and publish the windows you will see patients in. Only published, still-open times are ever offered for booking.</span>
        </li>
        <li class="help-steps__item">
            <span class="help-steps__no" aria-hidden="true">2</span>
            <span class="help-steps__text">Set the <strong>phone number</strong> on the same screen to publish a public number on your "Our dentists" card, so patients can reach you directly.</span>
        </li>
        <li class="help-steps__item">
            <span class="help-steps__no" aria-hidden="true">3</span>
            <span class="help-steps__text">After saving, a confirmation window tells you the change is stored.</span>
        </li>
    </ol>
</div>

<div class="card" id="offered">
    <h2>Treatments you offer</h2>
    <p>On the availability screen, toggle which treatments you perform. A patient booking with you is offered only the treatments you leave on — an "Other" booking stays possible regardless, for needs no listed procedure names.</p>
</div>

<div class="card" id="treating">
    <h2>Recording treatment</h2>
    <p>Open the visit from your schedule, record the diagnosis, and mark the treatment complete. That moves the visit to <strong>ready to bill</strong> so reception can issue the bill. Treatment can only be recorded on the day of the appointment. Your note to the patient appears on their dashboard once the visit is finished.</p>
    <div class="notice warning">
        A patient's allergy and medication notes are shown to you before treatment — read the critical ones first.
    </div>
</div>

<div class="card" id="trouble">
    <h2>Something went wrong?</h2>
    <div class="help-faq">
        <details class="help-faq__item">
            <summary>A patient booked a treatment I do not do</summary>
            <p>Check your offered-treatments toggles on the availability screen — the booking list follows them. An "Other" booking can still name anything, so read the patient's written reason on the visit.</p>
        </details>
        <details class="help-faq__item">
            <summary>Signed out unexpectedly</summary>
            <p>Sessions end after 30 minutes of inactivity. Sign in again — your schedule, availability and treatment notes are all still there.</p>
        </details>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/shared/footer.jspf" %>
