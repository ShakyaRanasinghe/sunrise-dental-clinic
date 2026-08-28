<%--
    One appointment, as its treating dentist sees it — the visit and the patient's declared
    notes together (FR-NOTE-07).

    A patient with nothing declared produces an explicit "nothing declared", never a blank
    (FR-NOTE-12). A blank looks like a screen that failed to load, and a dentist cannot tell
    the difference between "no allergies" and "the allergies did not render".
--%>
<%@ include file="/WEB-INF/jsp/shared/taglibs.jspf" %>
<c:set var="pageTitle" value="Appointment" />
<%@ include file="/WEB-INF/jsp/shared/header.jspf" %>

<h1 class="page-title"><c:out value="${appointment.patientName()}" /></h1>
<p class="page-subtitle">
    ${appointment.date()} at ${appointment.time()} &mdash;
    <c:out value="${appointment.treatmentName()}" />
</p>

<c:if test="${appointment.hasCriticalNotes()}">
    <div class="notice error">
        <strong>This patient has declared something important.</strong> Read it before treating.
    </div>
</c:if>

<div class="card">
    <h2>Declared by the patient</h2>
    <c:choose>
        <c:when test="${appointment.hasNoNotes()}">
            <p class="page-subtitle">
                <strong>Nothing declared.</strong> This patient has not recorded any allergies,
                medication or conditions.
            </p>
        </c:when>
        <c:otherwise>
            <div class="table-wrap">
                <table>
                    <thead><tr><th>Kind</th><th>Detail</th><th></th></tr></thead>
                    <tbody>
                        <c:forEach var="note" items="${appointment.patientNotes()}">
                            <tr>
                                <td><c:out value="${note.categoryLabel()}" /></td>
                                <td><c:out value="${note.detail()}" /></td>
                                <td>
                                    <c:if test="${note.critical()}">
                                        <span class="pill error">Important</span>
                                    </c:if>
                                </td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
            <p class="page-subtitle">
                The patient declared these and only they can change them. If you disagree with
                one, record that as part of the treatment rather than editing what they said.
            </p>
        </c:otherwise>
    </c:choose>
</div>

<div class="card">
    <h2>Visit</h2>
    <div class="table-wrap">
        <table>
            <tbody>
                <tr><th>Appointment</th><td><code>${appointment.appointmentNo()}</code></td></tr>
                <tr><th>Status</th><td>${appointment.status()}</td></tr>
                <c:if test="${not empty appointment.diagnosis()}">
                    <tr><th>Recorded</th><td><c:out value="${appointment.diagnosis()}" /></td></tr>
                </c:if>
            </tbody>
        </table>
    </div>
    <p class="form-actions">
        <a class="btn btn-secondary" href="${ctx}/dentist/schedule?date=${appointment.date()}">
            Back to the day</a>
    </p>
</div>

<%@ include file="/WEB-INF/jsp/shared/footer.jspf" %>
