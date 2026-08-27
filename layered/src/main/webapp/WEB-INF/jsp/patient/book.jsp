<%--
    Booking screen, used by patients for themselves and by the front desk on a
    patient's behalf.

    Two forms: choosing a dentist and a day reloads the page with that day's open
    slots (a GET, so the result can be bookmarked and refreshed safely); picking a
    slot and a treatment submits the booking itself.
--%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Book an appointment" />
<c:set var="nav" value="book" />
<%@ include file="/WEB-INF/jsp/layout/header.jspf" %>

<h1 class="page-title">Book an appointment</h1>
<p class="page-subtitle">Choose a dentist and a day, then pick a time that suits you.</p>

<div class="card">
    <h2>1. Choose a dentist and date</h2>
    <form method="get" action="${ctx}/patient/book">
        <div class="form-row">
            <div class="field">
                <label for="dentistId">Dentist</label>
                <select id="dentistId" name="dentistId" required onchange="this.form.submit()">
                    <option value="">Select a dentist…</option>
                    <c:forEach var="d" items="${dentists}">
                        <option value="${d.id}" ${d.id == dentistId ? 'selected' : ''}>
                            <c:out value="${d.name}" />
                            <c:if test="${not empty d.specialization}">
                                — <c:out value="${d.specialization}" />
                            </c:if>
                        </option>
                    </c:forEach>
                </select>
            </div>
            <div class="field">
                <label for="date">Date</label>
                <input type="date" id="date" name="date" value="${date}" onchange="this.form.submit()">
            </div>
        </div>
        <noscript>
            <div class="form-actions">
                <button type="submit" class="btn secondary">Show available times</button>
            </div>
        </noscript>
    </form>
</div>

<c:if test="${not empty dentistId}">
    <div class="card">
        <h2>2. Choose a time on ${date}</h2>

        <c:choose>
            <c:when test="${noSlots}">
                <div class="empty">
                    <p>No open slots for this dentist on ${date}.</p>
                    <p style="font-size:13px">Try another date, or another dentist.</p>
                </div>
            </c:when>
            <c:otherwise>
                <form method="post" action="${ctx}/patient/book">
                    <div class="slots">
                        <c:forEach var="s" items="${slots}" varStatus="loop">
                            <div class="slot">
                                <input type="radio" id="slot-${loop.index}" name="slotId"
                                       value="${s.id}" required
                                       ${loop.first ? 'checked' : ''}>
                                <label for="slot-${loop.index}">${s.startTime}</label>
                            </div>
                        </c:forEach>
                    </div>

                    <div style="margin-top:22px" class="form-row">
                        <div class="field">
                            <label for="treatmentId">Treatment</label>
                            <select id="treatmentId" name="treatmentId" required>
                                <option value="">Select a treatment…</option>
                                <c:forEach var="t" items="${treatments}">
                                    <option value="${t.id}">
                                        <c:out value="${t.name}" /> — Rs <fmt:formatNumber value="${t.baseCost}" pattern="#,##0.00" />
                                    </option>
                                </c:forEach>
                            </select>
                        </div>

                        <%-- Staff book on someone else's behalf, so they choose the patient. --%>
                        <c:if test="${not empty patients}">
                            <div class="field">
                                <label for="patientId">Patient</label>
                                <select id="patientId" name="patientId" required>
                                    <option value="">Select a patient…</option>
                                    <c:forEach var="p" items="${patients}">
                                        <option value="${p.id}">
                                            <c:out value="${p.name}" />
                                            <c:if test="${not empty p.contactNumber}">
                                                (<c:out value="${p.contactNumber}" />)
                                            </c:if>
                                        </option>
                                    </c:forEach>
                                </select>
                            </div>
                        </c:if>
                    </div>

                    <div class="form-actions">
                        <button type="submit" class="btn">Confirm booking</button>
                        <a class="btn secondary" href="${ctx}/">Cancel</a>
                    </div>
                </form>
            </c:otherwise>
        </c:choose>
    </div>
</c:if>

<%@ include file="/WEB-INF/jsp/layout/footer.jspf" %>
