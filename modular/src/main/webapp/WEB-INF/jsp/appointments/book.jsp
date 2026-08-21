<%--
    Booking, in three steps on one page.

    Driven by the query string, not by script - the stack has no client-side JavaScript.
    Choosing a dentist reloads with that dentist's open times; choosing a time posts the
    booking. Each step is a real address, so the back button works.

    Only OPEN slots are offered. A patient must never be shown a time they cannot have.
--%>
<%@ include file="/WEB-INF/jsp/shared/taglibs.jspf" %>
<c:set var="pageTitle" value="Book an appointment" />
<%@ include file="/WEB-INF/jsp/shared/header.jspf" %>

<h1 class="page-title">Book an appointment</h1>
<p class="page-subtitle">Choose a dentist and a date, then pick a time that suits you.</p>

<div class="card">
    <h2>1. Dentist and date</h2>
    <form method="get" action="${ctx}/patient/book">
        <div class="form-row">
            <div class="field">
                <label for="dentistId">Dentist</label>
                <select id="dentistId" name="dentistId" required>
                    <option value="">Choose&hellip;</option>
                    <c:forEach var="d" items="${dentists}">
                        <option value="<c:out value='${d.id()}' />"
                            <c:if test="${d.id() eq dentistId}">selected</c:if>>
                            <c:out value="${d.name()}" />
                            <c:if test="${not empty d.specialization()}">
                                &mdash; <c:out value="${d.specialization()}" />
                            </c:if>
                            (Rs <fmt:formatNumber value="${d.consultationFee()}"
                                                  minFractionDigits="2" maxFractionDigits="2" />)
                        </option>
                    </c:forEach>
                </select>
            </div>
            <div class="field">
                <label for="date">Date</label>
                <input type="date" id="date" name="date" value="${date}" required>
            </div>
            <div class="form-actions"><button type="submit" class="btn">Show times</button></div>
        </div>
    </form>
</div>

<c:if test="${not empty dentistId}">
    <div class="card">
        <h2>2. Treatment and time</h2>
        <c:choose>
            <c:when test="${empty slots}">
                <p class="page-subtitle">
                    No times are open on ${date}. Try another date, or telephone the clinic.
                </p>
            </c:when>
            <c:otherwise>
                <%--
                    One form per slot rather than a radio group, so the chosen time is
                    unambiguous in the POST and no JavaScript is needed to enable a
                    submit button. The treatment is chosen once, above the times.
                --%>
                <form method="post" action="${ctx}/patient/book">
                    <div class="field">
                        <label for="treatmentId">Treatment</label>
                        <select id="treatmentId" name="treatmentId" required>
                            <c:forEach var="t" items="${treatments}">
                                <option value="<c:out value='${t.id()}' />">
                                    <c:out value="${t.name()}" />
                                    &mdash; Rs <fmt:formatNumber value="${t.baseCost()}"
                                                                 minFractionDigits="2"
                                                                 maxFractionDigits="2" />
                                </option>
                            </c:forEach>
                        </select>
                    </div>

                    <p class="page-subtitle">
                        ${fn:length(slots)} times open with
                        <c:out value="${slots[0].dentistName()}" /> on ${date}.
                    </p>
                    <div class="slots">
                        <c:forEach var="slot" items="${slots}">
                            <div class="slot">
                                <input type="radio" id="slot-${slot.id()}"
                                       name="slotId" value="${slot.id()}" required>
                                <label for="slot-${slot.id()}">${slot.startTime()}</label>
                            </div>
                        </c:forEach>
                    </div>
                    <div class="form-actions" style="margin-top:1rem;">
                        <button type="submit" class="btn">Confirm booking</button>
                    </div>
                </form>
            </c:otherwise>
        </c:choose>
    </div>
</c:if>

<%@ include file="/WEB-INF/jsp/shared/footer.jspf" %>
