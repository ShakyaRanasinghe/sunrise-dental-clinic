<%--
    The dentist's diary.

    The diagnosis field is clinical data: it is captured here and shown back to
    the treating dentist, and it is never rendered on any receptionist or
    administrator screen.
--%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="My schedule" />
<c:set var="nav" value="schedule" />
<%@ include file="/WEB-INF/jsp/layout/header.jspf" %>

<h1 class="page-title">My schedule</h1>
<p class="page-subtitle"><c:out value="${dentist.name}" /> — appointments for the selected day.</p>

<c:if test="${not empty param.completed}">
    <div class="notice success">
        <strong><c:out value="${param.completed}" /></strong> marked as completed.
        The front desk can now issue the bill.
    </div>
</c:if>

<div class="card">
    <form method="get" action="${ctx}/dentist/schedule">
        <div class="field" style="max-width:220px; margin:0">
            <label for="date">Showing</label>
            <input type="date" id="date" name="date" value="${date}" onchange="this.form.submit()">
        </div>
    </form>
</div>

<c:choose>
    <c:when test="${empty appointments}">
        <div class="card">
            <div class="empty"><p>No appointments booked for ${date}.</p></div>
        </div>
    </c:when>
    <c:otherwise>
        <c:forEach var="a" items="${appointments}">
            <div class="card">
                <h2>
                    ${a.time} — <c:out value="${patientNames[a.patientId]}" />
                    <span class="pill ${fn:toLowerCase(a.status)}" style="margin-left:8px">${a.status}</span>
                </h2>

                <table style="margin-bottom:14px">
                    <tbody>
                    <tr><th style="width:150px">Reference</th><td class="mono">${a.appointmentNo}</td></tr>
                    <tr><th>Treatment</th><td><c:out value="${treatmentNames[a.treatmentId]}" /></td></tr>
                    <c:if test="${not empty a.diagnosis}">
                        <tr><th>Diagnosis</th><td><c:out value="${a.diagnosis}" /></td></tr>
                    </c:if>
                    </tbody>
                </table>

                <c:if test="${a.status == 'CONFIRMED'}">
                    <form method="post" action="${ctx}/dentist/schedule">
                        <input type="hidden" name="appointmentNo" value="${a.appointmentNo}">
                        <input type="hidden" name="date" value="${date}">
                        <div class="field">
                            <label for="diagnosis-${a.appointmentNo}">Diagnosis and treatment notes</label>
                            <textarea id="diagnosis-${a.appointmentNo}" name="diagnosis" required
                                      placeholder="What you found and what you did…"></textarea>
                            <div class="hint">
                                Visible only to you and to the patient.
                            </div>
                        </div>
                        <div class="form-actions">
                            <button type="submit" class="btn">Mark completed</button>
                        </div>
                    </form>
                </c:if>
            </div>
        </c:forEach>
    </c:otherwise>
</c:choose>

<%@ include file="/WEB-INF/jsp/layout/footer.jspf" %>
