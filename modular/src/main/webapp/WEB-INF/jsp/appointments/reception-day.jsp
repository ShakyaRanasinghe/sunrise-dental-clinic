<%--
    The front desk's day view: everything booked on one date, across every dentist.

    There is no diagnosis column, and it is not omitted by choice of markup -
    AppointmentResponse has no diagnosis field, so this page could not show one if it
    tried. Reception books and bills; why the patient came is not theirs to read.
--%>
<%@ include file="/WEB-INF/jsp/shared/taglibs.jspf" %>
<c:set var="pageTitle" value="Today at the clinic" />
<%@ include file="/WEB-INF/jsp/shared/header.jspf" %>

<h1 class="page-title">Today at the clinic</h1>
<p class="page-subtitle">Everything booked on one day, across all dentists.</p>

<%-- GAP-FTB-14: cancelling confirms in a sub-window; Close keeps the viewed day. --%>
<c:if test="${not empty cancelled}">
    <div class="sub-window open" role="dialog" aria-modal="true" aria-labelledby="day-cancelled-title">
        <div class="dialog">
            <span class="success-tick" aria-hidden="true">&#10003;</span>
            <h3 id="day-cancelled-title">Successfully cancelled</h3>
            <p>Appointment <strong><c:out value="${cancelled}" /></strong> is cancelled, and the
                time is open again.</p>
            <div class="form-actions">
                <a class="btn" href="${ctx}/reception/home?date=${date}">Close</a>
            </div>
        </div>
    </div>
</c:if>

<div class="card">
    <form method="get" action="${ctx}/reception/home" class="form-row">
        <div class="field">
            <label for="date">Day</label>
            <input type="date" id="date" name="date" value="${date}">
        </div>
        <div class="form-actions"><button type="submit" class="btn">Show</button></div>
    </form>
</div>

<div class="card">
    <h2>${date} <span class="count">${fn:length(appointments)}</span></h2>

    <c:choose>
        <c:when test="${empty appointments}">
            <p class="page-subtitle">
                Nothing booked on ${date}.
                <a href="${ctx}/reception/availability">Publish availability</a> if the diary is empty.
            </p>
        </c:when>
        <c:otherwise>
            <div class="table-wrap">
                <table>
                    <thead>
                        <tr>
                            <th>Time</th><th>Patient</th><th>Dentist</th>
                            <th>Treatment</th><th>Number</th><th>Status</th><th></th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="a" items="${appointments}">
                            <tr>
                                <td>${a.time()}</td>
                                <td><c:out value="${a.patientName()}" /></td>
                                <td><c:out value="${a.dentistName()}" /></td>
                                <%-- GAP-REC-13: an "Other" visit names the patient's stated reason. --%>
                                <td><c:out value="${not empty a.treatmentName() ? a.treatmentName() : a.patientReason()}" /></td>
                                <td><code>${a.appointmentNo()}</code></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${a.status() eq 'CONFIRMED'}">
                                            <span class="pill open">Waiting</span>
                                        </c:when>
                                        <c:when test="${a.status() eq 'COMPLETED'}">
                                            <span class="pill" style="background:var(--sunrise);color:#fff;border-color:var(--sunrise);">Ready to bill</span>
                                        </c:when>
                                        <c:when test="${a.status() eq 'BILLED'}">
                                            <span class="pill">Paid &amp; done</span>
                                        </c:when>
                                        <c:when test="${a.status() eq 'CANCELLED'}">
                                            <span class="pill error">Cancelled</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="pill">${a.status()}</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <c:if test="${a.status() eq 'COMPLETED'}">
                                        <a href="${ctx}/reception/billing?appointmentNo=${a.appointmentNo()}"
                                           class="btn small">Bill</a>
                                    </c:if>
                                    <c:if test="${a.isCancellable()}">
                                        <details class="confirm-cancel">
                                            <summary class="btn small secondary">Cancel</summary>
                                            <div class="confirm-cancel__panel">
                                                <p class="page-subtitle">Cancel ${a.appointmentNo()}? The slot opens again.</p>
                                                <form method="post" action="${ctx}/reception/home">
                                                    <input type="hidden" name="appointmentNo"
                                                           value="${a.appointmentNo()}">
                                                    <input type="hidden" name="date" value="${date}">
                                                    <button type="submit" class="btn small">Yes, cancel</button>
                                                </form>
                                            </div>
                                        </details>
                                    </c:if>
                                </td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:otherwise>
    </c:choose>
</div>

<%@ include file="/WEB-INF/jsp/shared/footer.jspf" %>
