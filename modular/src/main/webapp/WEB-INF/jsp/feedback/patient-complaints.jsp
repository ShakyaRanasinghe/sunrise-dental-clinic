<%--
    Where a patient raises a concern.

    The notice says who reads it and who does not, before they type — FR-CMP-12. Somebody
    deciding whether to complain about the person treating them next month needs to know
    that beforehand, not afterwards.
--%>
<%@ include file="/WEB-INF/jsp/shared/taglibs.jspf" %>
<c:set var="pageTitle" value="Raise a concern" />
<%@ include file="/WEB-INF/jsp/shared/header.jspf" %>

<h1 class="page-title">Raise a concern</h1>
<p class="page-subtitle">If something was not right, tell us. It is read by the clinic's
    administrator.</p>

<c:if test="${not empty raised}">
    <div class="notice">
        Thank you. Your concern has been received and will be looked at.
    </div>
</c:if>

<div class="card">
    <h2>What happened</h2>

    <div class="notice">
        <strong>Who reads this:</strong> the clinic's administrator, and nobody else.
        <strong>The dentist you name will not see it</strong> &mdash; not the concern, not that
        you raised one. Reception does not see it either. Raising a concern does not affect your
        appointments or your ability to book.
    </div>

    <c:choose>
        <c:when test="${empty dentistsTreated}">
            <p class="page-subtitle">
                You have not been treated at the clinic yet, so there is nothing to raise a
                concern about. Telephone us if something else is wrong.
            </p>
        </c:when>
        <c:otherwise>
            <p class="page-subtitle">
                Choose the dentist your concern is about. The form opens when you do.
            </p>
            <c:forEach var="dentist" items="${dentistsTreated}">
                <details class="concern-dentist">
                    <summary>
                        <span class="concern-dentist__name"><c:out value="${dentist.dentistName()}" /></span>
                        <span class="concern-dentist__count">${fn:length(dentist.visits())}
                            visit${fn:length(dentist.visits()) != 1 ? 's' : ''}</span>
                    </summary>
                    <form method="post" action="${ctx}/patient/complaints">
                        <input type="hidden" name="dentistId" value="${dentist.dentistId()}">
                        <c:choose>
                            <c:when test="${fn:length(dentist.visits()) > 1}">
                                <div class="field">
                                    <label for="appointmentNo-${dentist.dentistId()}">Which visit</label>
                                    <select id="appointmentNo-${dentist.dentistId()}" name="appointmentNo">
                                        <option value="${dentist.visits()[0].appointmentNo()}">
                                            Most recent &mdash; ${dentist.visits()[0].date()}</option>
                                        <c:forEach var="a" items="${dentist.visits()}">
                                            <option value="${a.appointmentNo()}">${a.date()} &mdash; <c:out value="${a.treatmentName()}" /></option>
                                        </c:forEach>
                                    </select>
                                </div>
                            </c:when>
                            <c:otherwise>
                                <input type="hidden" name="appointmentNo" value="${dentist.visits()[0].appointmentNo()}">
                            </c:otherwise>
                        </c:choose>
                        <div class="field">
                            <label for="category-${dentist.dentistId()}">What is it about</label>
                            <select id="category-${dentist.dentistId()}" name="category" required>
                                <c:forEach var="c" items="${categories}">
                                    <option value="${c}">${c.label()}</option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="field">
                            <label for="detail-${dentist.dentistId()}">Tell us what happened</label>
                            <textarea id="detail-${dentist.dentistId()}" name="detail" rows="6" required
                                      placeholder="In your own words. The more you can tell us, the more we can do."></textarea>
                        </div>
                        <div class="form-actions">
                            <button type="submit" class="btn">Send concern</button>
                        </div>
                    </form>
                </details>
            </c:forEach>
        </c:otherwise>
    </c:choose>
</div>

<div class="card">
    <h2>Concerns you have raised <span class="count">${fn:length(complaints)}</span></h2>
    <c:choose>
        <c:when test="${empty complaints}">
            <p class="page-subtitle">None yet.</p>
        </c:when>
        <c:otherwise>
            <c:forEach var="c" items="${complaints}">
                <div class="table-wrap">
                    <table>
                        <tbody>
                            <tr>
                                <th>Raised</th>
                                <td>${c.submittedAt()}
                                    <span class="pill <c:if test='${c.isOpen()}'>open</c:if>">
                                        <c:out value="${c.statusLabel()}" /></span>
                                </td>
                            </tr>
                            <tr><th>About</th>
                                <td><c:out value="${c.categoryLabel()}" /> &mdash;
                                    <c:out value="${c.dentistName()}" /></td></tr>
                            <tr><th>What you told us</th><td><c:out value="${c.detail()}" /></td></tr>
                            <c:if test="${not empty c.resolution()}">
                                <%-- The clinic's answer sits beside the patient's account,
                                     never over it — FR-ADM-55. --%>
                                <tr><th>What we did</th><td><c:out value="${c.resolution()}" /></td></tr>
                            </c:if>
                        </tbody>
                    </table>
                </div>
            </c:forEach>
            <p class="page-subtitle">
                A concern cannot be withdrawn once sent &mdash; a record of something that
                worried you is worth keeping even if you change your mind about it.
            </p>
        </c:otherwise>
    </c:choose>
</div>

<%@ include file="/WEB-INF/jsp/shared/footer.jspf" %>
