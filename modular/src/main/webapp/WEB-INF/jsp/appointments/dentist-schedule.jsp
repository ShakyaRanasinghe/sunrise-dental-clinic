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

<c:choose>
    <c:when test="${empty appointments}">
        <div class="card">
            <p class="page-subtitle">Nothing booked with you on ${date}.</p>
        </div>
    </c:when>
    <c:otherwise>
        <c:forEach var="row" items="${appointments}">
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
                            <tr><th>Treatment</th><td><c:out value="${a.treatmentName()}" /></td></tr>
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
                    <c:when test="${a.status() eq 'CONFIRMED'}">
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
                    </c:when>
                    <c:otherwise>
                        <p class="page-subtitle">
                            <%-- The status machine says nothing further is possible, so the
                                 page offers nothing rather than a button that would fail. --%>
                            Recorded. Nothing further to do here.
                        </p>
                    </c:otherwise>
                </c:choose>
            </div>
        </c:forEach>
    </c:otherwise>
</c:choose>

<%@ include file="/WEB-INF/jsp/shared/footer.jspf" %>
