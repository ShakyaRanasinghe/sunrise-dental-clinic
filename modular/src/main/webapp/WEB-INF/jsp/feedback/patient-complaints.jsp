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
    <h2>Your visits</h2>

    <c:choose>
        <c:when test="${empty dentistsTreated}">
            <p class="page-subtitle">
                You have not been treated at the clinic yet, so there is nothing to raise a
                concern about. Telephone us if something else is wrong.
            </p>
        </c:when>
        <c:otherwise>
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
                <%-- GAP-FTB-08: a compact, expandable list instead of a stack of full
                     tables. Each concern is one row; open it to read the detail and the
                     clinic's reply. <details>/<summary> keeps it script-free. --%>
                <details class="concern-item">
                    <summary>
                        <span class="concern-item__who">
                            <c:out value="${c.categoryLabel()}" />
                            <c:if test="${not empty c.dentistName()}">
                                &mdash; <c:out value="${c.dentistName()}" />
                            </c:if>
                        </span>
                        <span class="concern-item__meta">
                            <span class="pill <c:if test='${c.isOpen()}'>open</c:if>">
                                <c:out value="${c.statusLabel()}" /></span>
                            <span class="page-subtitle">${c.submittedAt()}</span>
                        </span>
                    </summary>
                    <div class="concern-item__body">
                        <p class="page-subtitle"><strong>What you told us</strong></p>
                        <p><c:out value="${c.detail()}" /></p>
                        <c:if test="${not empty c.resolution()}">
                            <%-- The clinic's answer sits beside the patient's account,
                                 never over it — FR-ADM-55. --%>
                            <p class="page-subtitle"><strong>What we did</strong></p>
                            <p><c:out value="${c.resolution()}" /></p>
                        </c:if>
                    </div>
                </details>
            </c:forEach>
            <p class="page-subtitle">
                A concern cannot be withdrawn once sent &mdash; a record of something that
                worried you is worth keeping even if you change your mind about it.
            </p>
        </c:otherwise>
    </c:choose>
</div>

<%@ include file="/WEB-INF/jsp/shared/footer.jspf" %>
