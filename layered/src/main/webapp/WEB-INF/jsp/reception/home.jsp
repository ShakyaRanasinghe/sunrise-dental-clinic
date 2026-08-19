<%-- The front desk's day view. --%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Today at the clinic" />
<c:set var="nav" value="home" />
<%@ include file="/WEB-INF/jsp/layout/header.jspf" %>

<h1 class="page-title">Appointments</h1>
<p class="page-subtitle">Everything booked for the selected day.</p>

<c:if test="${not empty param.booked}">
    <div class="notice success">
        Booked — reference <strong><c:out value="${param.booked}" /></strong>.
    </div>
</c:if>
<c:if test="${not empty param.cancelled}">
    <div class="notice info">
        <strong><c:out value="${param.cancelled}" /></strong> cancelled; the slot is open again.
    </div>
</c:if>

<div class="card">
    <form method="get" action="${ctx}/reception/home">
        <div class="form-row" style="max-width:420px">
            <div class="field" style="margin:0">
                <label for="date">Showing</label>
                <input type="date" id="date" name="date" value="${date}" onchange="this.form.submit()">
            </div>
            <div class="field" style="margin:0; align-self:end">
                <noscript><button type="submit" class="btn secondary">Show</button></noscript>
                <a class="btn" href="${ctx}/patient/book">New booking</a>
            </div>
        </div>
    </form>
</div>

<div class="card">
    <h2>${date} <span class="count">${appointments.size()} appointment(s)</span></h2>

    <c:choose>
        <c:when test="${empty appointments}">
            <div class="empty">
                <p>Nothing booked for this day.</p>
                <a class="btn" href="${ctx}/reception/availability">Publish availability</a>
            </div>
        </c:when>
        <c:otherwise>
            <div class="table-wrap">
                <table>
                    <thead>
                    <tr>
                        <th>Time</th>
                        <th>Reference</th>
                        <th>Patient</th>
                        <th>Dentist</th>
                        <th>Treatment</th>
                        <th>Status</th>
                        <th>Bill</th>
                        <th></th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="a" items="${appointments}">
                        <tr>
                            <td><strong>${a.time}</strong></td>
                            <td class="mono"><c:out value="${a.appointmentNo}" /></td>
                            <td><c:out value="${patientNames[a.patientId]}" /></td>
                            <td><c:out value="${dentistNames[a.dentistId]}" /></td>
                            <td><c:out value="${treatmentNames[a.treatmentId]}" /></td>
                            <td><span class="pill ${fn:toLowerCase(a.status)}">${a.status}</span></td>
                            <td>
                                <c:choose>
                                    <c:when test="${billed[a.appointmentNo]}">
                                        <a href="${ctx}/reception/billing?appointmentNo=${a.appointmentNo}">View</a>
                                    </c:when>
                                    <c:when test="${a.status == 'COMPLETED'}">
                                        <a href="${ctx}/reception/billing?appointmentNo=${a.appointmentNo}">Bill now</a>
                                    </c:when>
                                    <c:otherwise><span style="color:var(--ink-faint)">—</span></c:otherwise>
                                </c:choose>
                            </td>
                            <td>
                                <c:if test="${a.status == 'CONFIRMED'}">
                                    <form method="post" action="${ctx}/reception/home"
                                          onsubmit="return confirm('Cancel this appointment?');">
                                        <input type="hidden" name="appointmentNo" value="${a.appointmentNo}">
                                        <input type="hidden" name="date" value="${date}">
                                        <button type="submit" class="btn danger small">Cancel</button>
                                    </form>
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

<%@ include file="/WEB-INF/jsp/layout/footer.jspf" %>
