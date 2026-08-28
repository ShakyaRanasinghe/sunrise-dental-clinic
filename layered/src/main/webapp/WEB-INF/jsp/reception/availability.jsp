<%-- Publishing a dentist's availability window. --%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Availability" />
<c:set var="nav" value="availability" />
<%@ include file="/WEB-INF/jsp/layout/header.jspf" %>

<h1 class="page-title">Dentist availability</h1>
<p class="page-subtitle">
    Publish a window and the system divides it into bookable slots automatically.
</p>

<c:if test="${not empty param.published}">
    <div class="notice success">
        Published — <strong>${param.published}</strong> slot(s) are now open for booking.
    </div>
</c:if>

<div class="grid two">
    <div class="card">
        <h2>Publish a window</h2>
        <form method="post" action="${ctx}/reception/availability">
            <div class="field">
                <label for="dentistId">Dentist</label>
                <select id="dentistId" name="dentistId" required>
                    <option value="">Select a dentist…</option>
                    <c:forEach var="d" items="${dentists}">
                        <option value="${d.id}"><c:out value="${d.name}" /></option>
                    </c:forEach>
                </select>
            </div>

            <div class="field">
                <label for="date">Date</label>
                <input type="date" id="date" name="date" value="${date}" required>
            </div>

            <div class="form-row">
                <div class="field">
                    <label for="startTime">From</label>
                    <input type="time" id="startTime" name="startTime" value="16:00" required>
                </div>
                <div class="field">
                    <label for="endTime">To</label>
                    <input type="time" id="endTime" name="endTime" value="18:00" required>
                </div>
                <div class="field">
                    <label for="slotMinutes">Slot length</label>
                    <input type="number" id="slotMinutes" name="slotMinutes" value="30"
                           min="5" max="240" step="5">
                    <div class="hint">Minutes per appointment.</div>
                </div>
            </div>

            <div class="form-actions">
                <button type="submit" class="btn">Publish availability</button>
            </div>
        </form>
    </div>

    <div class="card">
        <h2>Published for ${date}</h2>

        <form method="get" action="${ctx}/reception/availability" style="margin-bottom:16px">
            <div class="field" style="margin:0">
                <label for="viewDate">Show another day</label>
                <input type="date" id="viewDate" name="date" value="${date}"
                       onchange="this.form.submit()">
            </div>
        </form>

        <c:choose>
            <c:when test="${empty sessions}">
                <div class="empty"><p>No availability published for this day yet.</p></div>
            </c:when>
            <c:otherwise>
                <div class="table-wrap">
                    <table>
                        <thead>
                        <tr>
                            <th>Dentist</th>
                            <th>From</th>
                            <th>To</th>
                            <th class="num">Slot</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="s" items="${sessions}">
                            <tr>
                                <td>
                                    <c:forEach var="d" items="${dentists}">
                                        <c:if test="${d.id == s.dentistId}"><c:out value="${d.name}" /></c:if>
                                    </c:forEach>
                                </td>
                                <td>${s.startTime}</td>
                                <td>${s.endTime}</td>
                                <td class="num">${s.slotDurationMinutes} min</td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/layout/footer.jspf" %>
