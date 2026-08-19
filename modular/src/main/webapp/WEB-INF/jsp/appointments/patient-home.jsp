<%--
    The patient's own dashboard.

    No patient id anywhere on this page. Their appointments are resolved from the
    signed-in account, so there is no parameter to change to see someone else's.
--%>
<%@ include file="/WEB-INF/jsp/shared/taglibs.jspf" %>
<c:set var="pageTitle" value="My appointments" />
<%@ include file="/WEB-INF/jsp/shared/header.jspf" %>

<h1 class="page-title">My appointments</h1>
<p class="page-subtitle">Book a visit, or cancel one you can no longer make.</p>

<c:if test="${not empty booked}">
    <div class="notice">
        Booked. Your appointment number is <strong><c:out value="${booked}" /></strong> &mdash;
        quote it if you telephone the clinic.
    </div>
</c:if>
<c:if test="${not empty cancelled}">
    <div class="notice">Cancelled <strong><c:out value="${cancelled}" /></strong>.</div>
</c:if>

<div class="card">
    <h2>Upcoming and past <span class="count">${fn:length(appointments)}</span></h2>

    <c:choose>
        <c:when test="${empty appointments}">
            <p class="page-subtitle">You have no appointments yet.</p>
            <p class="form-actions"><a class="btn" href="${ctx}/patient/book">Book an appointment</a></p>
        </c:when>
        <c:otherwise>
            <div class="table-wrap">
                <table>
                    <thead>
                        <tr>
                            <th>Number</th><th>Date</th><th>Time</th>
                            <th>Dentist</th><th>Treatment</th><th>Status</th><th></th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="a" items="${appointments}">
                            <tr>
                                <td><code>${a.appointmentNo()}</code></td>
                                <td>${a.date()}</td>
                                <td>${a.time()}</td>
                                <td><c:out value="${a.dentistName()}" /></td>
                                <td><c:out value="${a.treatmentName()}" /></td>
                                <td>
                                    <span class="pill <c:choose>
                                        <c:when test="${a.status() eq 'CONFIRMED'}">open</c:when>
                                        <c:when test="${a.status() eq 'CANCELLED'}">error</c:when>
                                    </c:choose>">${a.status()}</span>
                                </td>
                                <td>
                                    <%-- Shown only when the status machine permits it, so the
                                         page never offers a move the service would refuse. --%>
                                    <c:if test="${a.isCancellable()}">
                                        <form method="post" action="${ctx}/patient/home"
                                              style="display:inline">
                                            <input type="hidden" name="appointmentNo"
                                                   value="${a.appointmentNo()}">
                                            <button type="submit" class="btn small secondary">Cancel</button>
                                        </form>
                                    </c:if>
                                </td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
            <p class="form-actions"><a class="btn" href="${ctx}/patient/book">Book another</a></p>
        </c:otherwise>
    </c:choose>
</div>

<%@ include file="/WEB-INF/jsp/shared/footer.jspf" %>
