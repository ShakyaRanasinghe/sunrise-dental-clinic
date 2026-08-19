<%--
    The front desk's billing screen.

    Shows the day's treated appointments in both states - billed and not. Hiding the
    billed ones would make "have I billed this yet?" answerable only by absence, and
    absence is also what a cancelled or non-existent appointment looks like.
--%>
<%@ include file="/WEB-INF/jsp/shared/taglibs.jspf" %>
<c:set var="pageTitle" value="Billing" />
<%@ include file="/WEB-INF/jsp/shared/header.jspf" %>

<h1 class="page-title">Billing</h1>
<p class="page-subtitle">Treated appointments for the day, and the receipts issued for them.</p>

<div class="card">
    <form method="get" action="${ctx}/reception/billing" class="form-row">
        <div class="field">
            <label for="date">Day</label>
            <input type="date" id="date" name="date" value="${date}">
        </div>
        <div class="form-actions"><button type="submit" class="btn">Show</button></div>
    </form>
</div>

<div class="card">
    <h2>${date} <span class="count">${fn:length(billables)}</span></h2>

    <c:choose>
        <c:when test="${empty billables}">
            <p class="page-subtitle">
                Nothing treated on ${date} yet. An appointment can be billed once the
                dentist has recorded the treatment.
            </p>
        </c:when>
        <c:otherwise>
            <div class="table-wrap">
                <table>
                    <thead>
                        <tr>
                            <th>Time</th><th>Patient</th><th>Dentist</th><th>Treatment</th>
                            <th>Number</th><th class="right">Total</th><th></th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="b" items="${billables}">
                            <c:set var="a" value="${b.appointment()}" />
                            <tr>
                                <td>${a.time()}</td>
                                <td><c:out value="${a.patientName()}" /></td>
                                <td><c:out value="${a.dentistName()}" /></td>
                                <td><c:out value="${a.treatmentName()}" /></td>
                                <td><code>${a.appointmentNo()}</code></td>
                                <td class="right">
                                    <c:choose>
                                        <c:when test="${b.isBilled()}">
                                            Rs <fmt:formatNumber value="${b.bill().total()}"
                                                    minFractionDigits="2" maxFractionDigits="2" />
                                        </c:when>
                                        <c:otherwise><span class="page-subtitle">&mdash;</span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${b.isBilled()}">
                                            <a class="btn small secondary"
                                               href="${ctx}/reception/receipt?appointmentNo=${a.appointmentNo()}">Receipt</a>
                                        </c:when>
                                        <c:otherwise>
                                            <%-- Only offered for a COMPLETED appointment, so the
                                                 button never proposes something the service
                                                 would refuse. --%>
                                            <form method="post" action="${ctx}/reception/billing"
                                                  style="display:inline">
                                                <input type="hidden" name="appointmentNo"
                                                       value="${a.appointmentNo()}">
                                                <input type="hidden" name="date" value="${date}">
                                                <button type="submit" class="btn small">Bill</button>
                                            </form>
                                        </c:otherwise>
                                    </c:choose>
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
