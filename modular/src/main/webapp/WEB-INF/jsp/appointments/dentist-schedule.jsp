<%--
    A dentist's own schedule, and where the diagnosis is recorded.

    The dentist comes from the signed-in account, never from a parameter, so this page
    cannot be pointed at a colleague's day. The service checks it again on the POST: any
    dentist holds COMPLETE_TREATMENT, but only one of them is treating this patient.

    Each row carries a critical-notes flag rather than the notes themselves — see below.
--%>
<%@ include file="/WEB-INF/jsp/shared/taglibs.jspf" %>
<c:set var="pageTitle" value="My schedule" />
<%@ include file="/WEB-INF/jsp/shared/header.jspf" %>

<h1 class="page-title">My schedule</h1>
<p class="page-subtitle">Your patients for the day, and where you record what you treated.</p>

<c:if test="${not empty completed}">
    <div class="notice">Recorded for <strong><c:out value="${completed}" /></strong>.</div>
</c:if>

<div class="card">
    <form method="get" action="${ctx}/dentist/schedule" class="form-row">
        <div class="field">
            <label for="date">Day</label>
            <input type="date" id="date" name="date" value="${date}">
        </div>
        <div class="form-actions"><button type="submit" class="btn">Show</button></div>
    </form>
</div>

<%--
    Today's due patients come first: the doctor opens the screen and sees the next
    patient to treat, not a week's overview. The diagnosis and "Record and complete"
    form sits right here, at the top of the page (GAP-DEN-08).
--%>
<h2 class="page-title">Today's patients</h2>
<c:choose>
    <c:when test="${empty pending}">
        <div class="card">
            <%-- GAP-DEN-11: when viewing today, say plainly that nothing is booked,
                 instead of echoing the date back (which read like an error). When a
                 specific other day is picked, name it. --%>
            <c:choose>
                <c:when test="${date == today}">
                    <p class="page-subtitle">No patients booked with you today.</p>
                </c:when>
                <c:otherwise>
                    <p class="page-subtitle">No patients booked with you on ${date}.</p>
                </c:otherwise>
            </c:choose>
        </div>
    </c:when>
    <c:otherwise>
        <c:forEach var="row" items="${pending}">
            <c:set var="a" value="${row.appointment()}" />
            <div class="card">
                <h2>
                    ${a.time()} &mdash; <c:out value="${a.patientName()}" />
                    <span class="count">${a.status()}</span>
                </h2>

                <%--
                    FR-NOTE-08: the warning comes before the appointment is opened. A flag
                    rather than the notes themselves — reading every patient's notes to
                    render a day would be a great deal of medical information fetched to
                    print one line.
                --%>
                <c:if test="${row.hasCriticalNotes()}">
                    <div class="notice error">
                        <strong>This patient has declared something important.</strong>
                        Open the appointment below to read it before treating.
                    </div>
                </c:if>
                <div class="table-wrap">
                    <table>
                        <tbody>
                            <tr>
                                <th>Treatment</th>
                                <td>
                                    <c:out value="${a.treatmentName()}" />
                                    <c:if test="${not empty row.treatmentCost()}">
                                        &mdash; Rs <fmt:formatNumber value="${row.treatmentCost()}"
                                                                     minFractionDigits="2"
                                                                     maxFractionDigits="2" />
                                    </c:if>
                                </td>
                            </tr>
                            <tr><th>Appointment</th><td><code>${a.appointmentNo()}</code></td></tr>
                            <tr>
                                <th>Declared by the patient</th>
                                <td>
                                    <a href="${ctx}/dentist/appointment?appointmentNo=${a.appointmentNo()}">
                                        See what they declared</a>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>

                <c:choose>
                    <c:when test="${a.status() eq 'CONFIRMED' and a.date() gt today}">
                        <%-- GAP-DEN-06: a visit that has not happened yet cannot be
                             completed. The service refuses it too; this simply does
                             not offer a button that would fail. --%>
                        <p class="page-subtitle">
                            Scheduled for ${a.date()}. Treatment is recorded on the day
                            of the appointment.
                        </p>
                    </c:when>
                    <c:otherwise>
                        <form method="post" action="${ctx}/dentist/schedule">
                            <input type="hidden" name="appointmentNo" value="${a.appointmentNo()}">
                            <input type="hidden" name="date" value="${date}">
                            <div class="field">
                                <label for="d-${a.appointmentNo()}">What you treated</label>
                                <input type="text" id="d-${a.appointmentNo()}" name="diagnosis"
                                       placeholder="Diagnosis and what was done" required>
                            </div>
                            <div class="form-actions">
                                <button type="submit" class="btn">Record and complete</button>
                            </div>
                        </form>
                    </c:otherwise>
                </c:choose>
            </div>
        </c:forEach>
    </c:otherwise>
</c:choose>

<%--
    What has already been handled on this day — recorded, billed or cancelled — is folded
    away behind an expander so a doctor opening the schedule sees what still needs attention
    first, and only opens the rest when they want the day's shape. <details> keeps it script-free.
--%>
<c:if test="${not empty done}">
    <details class="card done-list">
        <summary>
            Previous on ${date}
            <span class="count">${fn:length(done)}</span>
        </summary>
        <div class="table-wrap">
            <table>
                <thead>
                    <tr><th>Time</th><th>Patient</th><th>Treatment</th><th>Status</th></tr>
                </thead>
                <tbody>
                    <c:forEach var="row" items="${done}">
                        <c:set var="a" value="${row.appointment()}" />
                        <tr>
                            <td>${a.time()}</td>
                            <td><c:out value="${a.patientName()}" /></td>
                            <td><c:out value="${a.treatmentName()}" /></td>
                            <td><span class="count">${a.status()}</span></td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
        </div>
    </details>
</c:if>

<%--
    FR-DEN-15: the week ahead. The picker above answers "what is happening on a day I
    name"; this section answers "what is coming", which a one-day view never could -
    a booking made for tomorrow was invisible until tomorrow was picked.
--%>
<h2 class="page-title">The week ahead</h2>
<c:choose>
    <c:when test="${not hasWeekAppointments}">
        <div class="card">
            <p class="page-subtitle">Nothing booked with you in the seven days from ${date}.</p>
        </div>
    </c:when>
    <c:otherwise>
        <c:forEach var="day" items="${week}">
            <c:if test="${not empty day.appointments()}">
                <div class="card">
                    <h2>${day.date()}</h2>
                    <div class="table-wrap">
                        <table>
                            <thead>
                                <tr><th>Time</th><th>Patient</th><th>Treatment</th><th>Status</th><th></th></tr>
                            </thead>
                            <tbody>
                                <c:forEach var="row" items="${day.appointments()}">
                                    <tr>
                                        <td>${row.appointment().time()}</td>
                                        <td>
                                            <c:out value="${row.appointment().patientName()}" />
                                            <c:if test="${row.hasCriticalNotes()}">
                                                <span class="count">&#9888; notes</span>
                                            </c:if>
                                        </td>
                                        <td><c:out value="${row.appointment().treatmentName()}" /></td>
                                        <td><span class="count">${row.appointment().status()}</span></td>
                                        <td>
                                            <a href="${ctx}/dentist/appointment?appointmentNo=${row.appointment().appointmentNo()}">Open</a>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </div>
            </c:if>
        </c:forEach>
    </c:otherwise>
</c:choose>

<%@ include file="/WEB-INF/jsp/shared/footer.jspf" %>
