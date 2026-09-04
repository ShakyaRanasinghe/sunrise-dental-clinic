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

<%-- The phone number used to be edited here (GAP-FTB-04); it now lives on the
     dentist's profile (GAP-DEN-14), so this screen keeps only availability and
     the offered-treatments list. --%>

<%-- GAP-FTB-14: a flipped toggle confirms in a sub-window. --%>
<c:if test="${param.toggled == '1'}">
    <div class="sub-window open" role="dialog" aria-modal="true" aria-labelledby="toggled-title">
        <div class="dialog">
            <span class="success-tick" aria-hidden="true">&#10003;</span>
            <h3 id="toggled-title">Successfully updated</h3>
            <p class="page-subtitle">Your offered treatments were saved.</p>
            <div class="form-actions">
                <a class="btn" href="${ctx}/dentist/availability#treatments">Close</a>
            </div>
        </div>
    </div>
</c:if>
<%--
    GAP-DEN-16: the time windows come first — they are what the dentist checks daily —
    with the treatments list below. Each day headlines its booked/open totals so the
    state of the day reads at a glance, and a window with nothing left shows Full.
--%>
<c:choose>
    <c:when test="${not hasAvailability}">
        <div class="card">
            <p class="page-subtitle">No published availability yet. Reception will create time windows for you.</p>
        </div>
    </c:when>
    <c:otherwise>
        <c:forEach var="entry" items="${byDate}">
            <c:set var="dayBooked" value="0" />
            <c:set var="dayOpen" value="0" />
            <c:forEach var="info" items="${entry.value}">
                <c:set var="dayBooked" value="${dayBooked + info.booked()}" />
                <c:set var="dayOpen" value="${dayOpen + info.open()}" />
            </c:forEach>
            <div class="card">
                <h2>${entry.key}</h2>
                <p class="page-subtitle">${dayBooked} booked &middot; ${dayOpen} still open</p>
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
                                    <td>
                                        <c:choose>
                                            <c:when test="${info.open() gt 0}">
                                                <span class="pill open">${info.open()} open</span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="pill booked">Full</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
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

<%-- GAP-FTB-07: the treatments this dentist offers. A toggle list; patients booking
     with this dentist see only the checked treatments. "Other (describe…)" is always
     available, so disabling everything is never a dead end. --%>
<div class="card" id="treatments">
    <h2>Treatments I offer</h2>
    <p class="page-subtitle">
        Tick the treatments you perform. Patients booking with you will only see these
        (they can still choose "Other (describe&hellip;)" for anything else).
    </p>
    <c:forEach var="t" items="${toggles}">
        <form method="post" action="${ctx}/dentist/availability" class="treatment-toggle">
            <input type="hidden" name="action" value="toggle">
            <input type="hidden" name="treatmentId" value="<c:out value='${t.id()}' />" />
            <span class="treatment-toggle__name"><c:out value="${t.name()}" /></span>
            <span class="treatment-toggle__desc"><c:out value="${t.description()}" /></span>
            <span class="treatment-toggle__state ${t.offered() ? 'on' : 'off'}">
                ${t.offered() ? 'Offered' : 'Hidden from patients'}
            </span>
            <c:choose>
                <c:when test="${t.offered()}">
                    <input type="hidden" name="offered" value="off">
                    <button type="submit" class="btn secondary treatment-toggle__save">Remove</button>
                </c:when>
                <c:otherwise>
                    <input type="hidden" name="offered" value="on">
                    <button type="submit" class="btn treatment-toggle__save">Add</button>
                </c:otherwise>
            </c:choose>
        </form>
    </c:forEach>
</div>

<%@ include file="/WEB-INF/jsp/shared/footer.jspf" %>
