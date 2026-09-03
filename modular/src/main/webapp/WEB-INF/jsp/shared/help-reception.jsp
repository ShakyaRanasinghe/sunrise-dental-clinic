<%@ include file="/WEB-INF/jsp/shared/taglibs.jspf" %>
<c:set var="pageTitle" value="Reception help" />
<%@ include file="/WEB-INF/jsp/shared/header.jspf" %>

<%--
    GAP-FTB-12: the receptionist's own help page — the front-desk screens
    (day view, patients register, walk-in booking, availability, billing),
    grounded in srs-reception.md. Reached from the reception sign-in screen
    and the reception nav; never advertised on a public page.
--%>

<h1 class="page-title">Reception help</h1>
<p class="page-subtitle">How to run the front desk, step by step.</p>

<div class="card help-toc">
    <h2>Help topics</h2>
    <div class="help-toc__links">
        <a class="help-toc__item" href="#day">The day view</a>
        <a class="help-toc__item" href="#patients">Patients and walk-ins</a>
        <a class="help-toc__item" href="#booking">Booking for a patient</a>
        <a class="help-toc__item" href="#availability">Availability</a>
        <a class="help-toc__item" href="#billing">Billing</a>
        <a class="help-toc__item" href="#trouble">Something went wrong</a>
    </div>
</div>

<div class="card" id="day">
    <h2>The day view</h2>
    <ol class="help-steps">
        <li class="help-steps__item">
            <span class="help-steps__no" aria-hidden="true">1</span>
            <span class="help-steps__text">Open <a href="${ctx}/reception/home">Home</a> to see today's appointments per dentist. Pick another date to look ahead or behind.</span>
        </li>
        <li class="help-steps__item">
            <span class="help-steps__no" aria-hidden="true">2</span>
            <span class="help-steps__text">Cancel is offered only while a visit is still upcoming — a treated visit cannot be cancelled. Cancelling asks for an explicit confirmation first.</span>
        </li>
    </ol>
</div>

<div class="card" id="patients">
    <h2>Patients and walk-ins</h2>
    <ol class="help-steps">
        <li class="help-steps__item">
            <span class="help-steps__no" aria-hidden="true">1</span>
            <span class="help-steps__text">Open <a href="${ctx}/reception/patients">Patients</a> to search the register. Same-named patients are told apart by the <strong>Patient ID</strong> column on the left.</span>
        </li>
        <li class="help-steps__item">
            <span class="help-steps__no" aria-hidden="true">2</span>
            <span class="help-steps__text">Register a walk-in from the same screen: name, contact number, email and date of birth. A contact number is required and must be a valid Sri Lankan number. If the number matches an existing record, the screen warns you about a probable duplicate instead of silently doubling the patient.</span>
        </li>
        <li class="help-steps__item">
            <span class="help-steps__no" aria-hidden="true">3</span>
            <span class="help-steps__text">Stuck signing a patient up? The patient help page at <a href="${ctx}/help/patient">Help for patients</a> describes the self-service screens you are guiding them through.</span>
        </li>
    </ol>
</div>

<div class="card" id="booking">
    <h2>Booking for a patient</h2>
    <p>Book on a walk-in's behalf from the patient booking screen: choose the patient, then a dentist and date, then an open time and the treatment (or <strong>Other</strong> with a written reason). Only genuinely open slots are ever offered.</p>
</div>

<div class="card" id="availability">
    <h2>Availability</h2>
    <p>Open <a href="${ctx}/reception/availability">Availability</a> to publish or close the clinic's bookable windows. Dentists publish their own availability too; what a patient sees when booking is the overlap that is actually open.</p>
</div>

<div class="card" id="billing">
    <h2>Billing</h2>
    <p>Open <a href="${ctx}/reception/billing">Billing</a> to issue the bill for a visit the dentist has marked complete. The bill carries consultation, treatment and service charges; the patient can later open the same receipt from their own dashboard.</p>
</div>

<div class="card" id="trouble">
    <h2>Something went wrong?</h2>
    <div class="help-faq">
        <details class="help-faq__item">
            <summary>A slot I could see just vanished</summary>
            <p>Another booking took it first. Reload the day view — only open slots are offered, so a taken slot disappears rather than failing at submit.</p>
        </details>
        <details class="help-faq__item">
            <summary>Signed out unexpectedly</summary>
            <p>Sessions end after 30 minutes of inactivity. Sign in again — the register, the day view and every bill are all still there.</p>
        </details>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/shared/footer.jspf" %>
