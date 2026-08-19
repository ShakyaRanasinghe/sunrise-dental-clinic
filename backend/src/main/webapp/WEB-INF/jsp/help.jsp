<%-- Help page — public, since someone unable to sign in most needs it. --%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Help" />
<c:set var="nav" value="help" />
<%@ include file="/WEB-INF/jsp/layout/header.jspf" %>

<h1 class="page-title">Help</h1>
<p class="page-subtitle">How to use the system, and how to reach us.</p>

<div class="grid two">
    <div class="card">
        <h2>For patients</h2>
        <p><strong>Booking.</strong> Sign in, choose <em>Book</em>, pick your dentist and a
            date, then choose any open time and the treatment you need.</p>
        <p><strong>Cancelling.</strong> Open <em>My appointments</em> and use Cancel next to
            the booking. The time is released immediately for someone else.</p>
        <p><strong>Your records.</strong> You can see your own appointments and diagnoses.
            Nobody at the front desk can read your clinical notes.</p>
    </div>

    <div class="card">
        <h2>For clinic staff</h2>
        <p><strong>Availability.</strong> Publish a window for a dentist and the system
            creates the individual bookable slots for you.</p>
        <p><strong>Walk-ins.</strong> Add the patient under <em>Patients</em>, then book for
            them from the same screen.</p>
        <p><strong>Billing.</strong> Once a dentist marks an appointment completed, look up
            its reference under <em>Billing</em> to issue the receipt.</p>
    </div>
</div>

<div class="card">
    <h2>Trouble signing in</h2>
    <p>After five incorrect attempts an account is locked for 24 hours. An administrator
        can unlock it sooner — call the clinic and ask.</p>
    <p style="margin-bottom:0">
        <strong>Telephone:</strong> <c:out value="${clinicPhone}" /><br>
        <strong>Email:</strong> <c:out value="${clinicEmail}" />
    </p>
</div>

<%@ include file="/WEB-INF/jsp/layout/footer.jspf" %>
