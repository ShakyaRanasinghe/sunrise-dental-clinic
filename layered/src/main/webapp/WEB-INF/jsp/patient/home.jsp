<%-- A patient's own appointments. --%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="My appointments" />
<c:set var="nav" value="home" />
<%@ include file="/WEB-INF/jsp/layout/header.jspf" %>

<h1 class="page-title">Hello, <c:out value="${patient.name}" /></h1>
<p class="page-subtitle">Your appointments at Sunrise Dental Clinic.</p>

<c:if test="${not empty param.booked}">
    <div class="notice success">
        Your appointment <strong><c:out value="${param.booked}" /></strong> is confirmed.
        A confirmation has been recorded against your record.
    </div>
</c:if>
<c:if test="${not empty param.cancelled}">
    <div class="notice info">
        Appointment <strong><c:out value="${param.cancelled}" /></strong> has been cancelled
        and the time released.
    </div>
</c:if>

<div class="card">
    <h2>Upcoming <span class="count">${upcoming.size()}</span></h2>

    <c:choose>
        <c:when test="${empty upcoming}">
            <div class="empty">
                <p>You have no upcoming appointments.</p>
                <a class="btn" href="${ctx}/patient/book">Book an appointment</a>
            </div>
        </c:when>
        <c:otherwise>
            <div class="table-wrap">
                <table>
                    <thead>
                    <tr>
                        <th>Reference</th>
                        <th>Date</th>
                        <th>Time</th>
                        <th>Dentist</th>
                        <th>Treatment</th>
                        <th>Status</th>
                        <th></th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="a" items="${upcoming}">
                        <tr>
                            <td class="mono"><c:out value="${a.appointmentNo}" /></td>
                            <td>${a.date}</td>
                            <td>${a.time}</td>
                            <td><c:out value="${dentistNames[a.dentistId]}" /></td>
                            <td><c:out value="${treatmentNames[a.treatmentId]}" /></td>
                            <td><span class="pill ${fn:toLowerCase(a.status)}">${a.status}</span></td>
                            <td>
                                <c:if test="${a.status == 'CONFIRMED'}">
                                    <form method="post" action="${ctx}/patient/home"
                                          onsubmit="return confirm('Cancel this appointment?');">
                                        <input type="hidden" name="appointmentNo" value="${a.appointmentNo}">
                                        <button type="submit" class="btn danger small">Cancel</button>
                                    </form>
                                </c:if>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
            <div class="form-actions">
                <a class="btn" href="${ctx}/patient/book">Book another appointment</a>
            </div>
        </c:otherwise>
    </c:choose>
</div>

<c:if test="${not empty past}">
    <div class="card">
        <h2>Past <span class="count">${past.size()}</span></h2>
        <div class="table-wrap">
            <table>
                <thead>
                <tr>
                    <th>Reference</th>
                    <th>Date</th>
                    <th>Dentist</th>
                    <th>Treatment</th>
                    <th>Status</th>
                    <th>Bill</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="a" items="${past}">
                    <tr>
                        <td class="mono"><c:out value="${a.appointmentNo}" /></td>
                        <td>${a.date}</td>
                        <td><c:out value="${dentistNames[a.dentistId]}" /></td>
                        <td><c:out value="${treatmentNames[a.treatmentId]}" /></td>
                        <td><span class="pill ${fn:toLowerCase(a.status)}">${a.status}</span></td>
                        <td>
                            <c:choose>
                                <c:when test="${bills[a.appointmentNo]}">Issued</c:when>
                                <c:otherwise><span style="color:var(--ink-faint)">—</span></c:otherwise>
                            </c:choose>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </div>
    </div>
</c:if>

<%@ include file="/WEB-INF/jsp/layout/footer.jspf" %>
