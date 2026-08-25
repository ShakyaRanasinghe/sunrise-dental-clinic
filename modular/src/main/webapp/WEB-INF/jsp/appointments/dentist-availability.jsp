<%--
    FR-DEN-18: the dentist's own published availability. Reception publishes these
    windows, but the dentist previously had no screen showing them — they could only
    see already-booked appointments on the schedule, not the open windows patients
    can still book. GAP-DEN-05.
--%>
<%@ include file="/WEB-INF/jsp/shared/taglibs.jspf" %>
<c:set var="pageTitle" value="My availability" />
<%@ include file="/WEB-INF/jsp/shared/header.jspf" %>

<h1 class="page-title">My availability</h1>
<p class="page-subtitle">Time windows reception published for you. Patients can book the open slots shown here.</p>

<c:choose>
    <c:when test="${not hasAvailability}">
        <div class="card">
            <p class="page-subtitle">No published availability yet. Reception will create time windows for you.</p>
        </div>
    </c:when>
    <c:otherwise>
        <c:forEach var="entry" items="${byDate}">
            <div class="card">
                <h2>${entry.key}</h2>
                <div class="table-wrap">
                    <table>
                        <thead>
                            <tr><th>From</th><th>To</th><th>Open</th><th>Booked</th><th>Total</th></tr>
                        </thead>
                        <tbody>
                            <c:forEach var="info" items="${entry.value}">
                                <tr>
                                    <td>${info.startTime()}</td>
                                    <td>${info.endTime()}</td>
                                    <td><span class="count">${info.open()}</span></td>
                                    <td><span class="count">${info.booked()}</span></td>
                                    <td><span class="count">${info.total()}</span></td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </div>
        </c:forEach>
    </c:otherwise>
</c:choose>

<%@ include file="/WEB-INF/jsp/shared/footer.jspf" %>
