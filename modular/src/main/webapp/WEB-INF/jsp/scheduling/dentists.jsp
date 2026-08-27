<%@ include file="/WEB-INF/jsp/shared/taglibs.jspf" %>
<c:set var="pageTitle" value="Our dentists" />
<%@ include file="/WEB-INF/jsp/shared/header.jspf" %>

<h1 class="page-title">Our dentists</h1>
<p class="page-subtitle">All currently practising dentists — for advising walk-in patients on who to see.</p>

<div class="card">
    <div class="table-wrap">
        <table>
            <thead>
                <tr>
                    <th>Name</th>
                    <th>Specialisation</th>
                    <th class="right">Consultation fee (Rs)</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="d" items="${dentists}">
                    <tr>
                        <td><strong><c:out value="${d.name()}" /></strong></td>
                        <td><c:out value="${empty d.specialization() ? '—' : d.specialization()}" /></td>
                        <td class="right">
                            <fmt:formatNumber value="${d.consultationFee()}"
                                             minFractionDigits="2" maxFractionDigits="2" />
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </div>
    <p class="page-subtitle">
        Consultation fee is charged per visit in addition to the treatment cost and the
        Rs <fmt:formatNumber value="${serviceCharge}" minFractionDigits="2" maxFractionDigits="2" />
        service charge.
    </p>
</div>

<%@ include file="/WEB-INF/jsp/shared/footer.jspf" %>
