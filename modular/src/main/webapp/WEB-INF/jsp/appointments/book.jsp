<%--
    Booking, in three steps on one page.

    Driven by the query string, not by script - the stack has no client-side JavaScript.
    Choosing a dentist reloads with that dentist's open times; choosing a time posts the
    booking. Each step is a real address, so the back button works.

    Only OPEN slots are offered. A patient must never be shown a time they cannot have.

    GAP-PAT-15: once a dentist and date are chosen, the open TIMES are the first card
    (they are the thing the patient is deciding), and the dentist and date picker sits
    below. The doctor's fee details are not a card on the page at all any more: the
    dentist's name beside the times opens them in a sub-window - a CSS-only :target
    modal, so no script is needed. Closing the modal links to #top, which clears the
    :target and hides it again.
--%>
<%@ include file="/WEB-INF/jsp/shared/taglibs.jspf" %>
<c:set var="pageTitle" value="Book an appointment" />
<%@ include file="/WEB-INF/jsp/shared/header.jspf" %>

<h1 class="page-title" id="top">Book an appointment</h1>
<p class="page-subtitle">Choose a dentist and a date, then pick a time that suits you.</p>

<%--
    FR-PAT-17: Availability overview. Shown on initial load (no dentist selected yet)
    so the patient can browse which dentists have open dates before picking one.
    Each dentist card shows their dates with open slots. Clicking a date pre-selects
    that dentist and date in the booking form below.
--%>
<c:if test="${not empty availabilityOverview}">
    <div class="card availability-overview">
        <h2>Open days in the next two weeks</h2>
        <p class="page-subtitle">The days each dentist has free appointments soon. Click a date to jump to their available times below.</p>
        <c:forEach var="dentist" items="${availabilityOverview}">
            <div class="dentist-row">
                <div class="dentist-row__info">
                    <span class="dentist-row__name"><c:out value="${dentist.dentistName()}" /></span>
                    <c:if test="${not empty dentist.specialization()}">
                        <span class="dentist-row__spec"><c:out value="${dentist.specialization()}" /></span>
                    </c:if>
                </div>
                <div class="dentist-row__dates">
                    <c:forEach var="dateAvail" items="${dentist.dates()}">
                        <a class="date-chip" href="${ctx}/patient/book?dentistId=${dentist.dentistId()}&date=${dateAvail.date()}">
                            <span class="date-chip__day"><c:out value="${dateAvail.date()}" /></span>
                            <span class="date-chip__slots">${dateAvail.openSlotCount()} slots</span>
                        </a>
                    </c:forEach>
                </div>
            </div>
        </c:forEach>
    </div>
</c:if>

<c:if test="${not empty dentistId}">
    <%--
        GAP-PAT-15: the times are the top card now. The patient chose a dentist and
        date on their way here (or jumped from the availability overview), so the first
        thing they should see is which times are actually open. Sidebar about the
        dentist's fees would only push the decision further down the page.
    --%>
    <div class="card">
        <h2>1. Treatment and time</h2>
        <c:choose>
            <c:when test="${empty slots}">
                <p class="page-subtitle">
                    No times are open on ${date}. Try another date, or telephone the clinic.
                </p>
                <%-- The doctor's card is still reachable even when no times are open. --%>
                <c:forEach var="d" items="${dentists}">
                    <c:if test="${d.id() eq dentistId}">
                        <p class="page-subtitle">
                            <a href="#dentist-details">View <c:out value="${d.name()}" />'s details</a>
                        </p>
                    </c:if>
                </c:forEach>
            </c:when>
            <c:otherwise>
                <%--
                    One form per slot rather than a radio group, so the chosen time is
                    unambiguous in the POST and no JavaScript is needed to enable a
                    submit button. The treatment is chosen once, above the times.
                --%>
                <form method="post" action="${ctx}/patient/book">
                    <c:if test="${not empty patientId}">
                        <input type="hidden" name="patientId" value="<c:out value='${patientId}' />">
                    </c:if>
                    <%--
                        GAP-FTB-06: a single-treatment radio checklist replaces the old
                        dropdown, and an "Other (describe…)" option lets a patient book a
                        visit whose need no listed procedure names — the free text is
                        captured as the booking reason. CSS-only, no script.
                    --%>
                    <fieldset class="treatment-picker">
                        <legend class="field__label">Treatment</legend>
                        <c:forEach var="t" items="${treatments}">
                            <div class="treatment-choice">
                                <input type="radio" id="t-${t.id()}" name="treatmentId"
                                       value="<c:out value='${t.id()}' />">
                                <label for="t-${t.id()}" class="treatment-choice__label">
                                    <span class="treatment-choice__name"><c:out value="${t.name()}" /></span>
                                    <span class="treatment-choice__desc"><c:out value="${t.description()}" /></span>
                                    <span class="treatment-choice__price">
                                        Rs <fmt:formatNumber value="${t.baseCost()}"
                                                              minFractionDigits="2" maxFractionDigits="2" />
                                    </span>
                                </label>
                            </div>
                        </c:forEach>
                        <div class="treatment-choice">
                            <input type="radio" id="t-other" name="treatmentId" value="">
                            <label for="t-other" class="treatment-choice__label">
                                <span class="treatment-choice__name">Other (describe&hellip;)</span>
                                <span class="treatment-choice__desc">A visit for something not listed above.</span>
                            </label>
                        </div>
                        <div class="treatment-other-note" id="other-note">
                            <label for="patientReason" class="field__label">What is it for?</label>
                            <textarea id="patientReason" name="patientReason" rows="2"
                                      placeholder="e.g. a sore wisdom tooth, a second opinion, a fitting…"></textarea>
                        </div>
                    </fieldset>

                    <p class="page-subtitle">
                        ${fn:length(slots)} times open with
                        <a href="#dentist-details">
                            <c:out value="${slots[0].dentistName()}" />
                        </a>
                        on ${date}.
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

    <%--
        The dentist and date picker, now the second card: a patient who wants a
        different dentist or date has a way to change their search without reloading.
        The fee details that used to live here moved into the sub-window below.
    --%>
    <div class="card">
        <h2>2. Dentist and date</h2>
        <form method="get" action="${ctx}/patient/book">
            <c:if test="${not empty patientId}">
                <input type="hidden" name="patientId" value="<c:out value='${patientId}' />">
            </c:if>
            <div class="form-row">
                <div class="field">
                    <label for="dentistId">Dentist</label>
                    <select id="dentistId" name="dentistId" required>
                        <option value="">Choose&hellip;</option>
                        <c:forEach var="d" items="${dentists}">
                            <option value="<c:out value='${d.id()}' />"
                                <c:if test="${d.id() eq dentistId}">selected</c:if>>
                                <c:out value="${d.name()}" />
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

    <%--
        GAP-PAT-15 sub-window: the doctor's fee details, shown only when the dentist's
        name in the times card is clicked. CSS-only via :target - the <a href="#dentist-details">
        link makes this the target, and the Close link returns to #top. No script involved,
        matching the rest of the stack.
    --%>
    <div class="sub-window" id="dentist-details" role="dialog" aria-modal="true"
         aria-labelledby="dentist-details-title">
        <div class="dialog">
            <h3 id="dentist-details-title">About this dentist</h3>
            <c:forEach var="d" items="${dentists}">
                <c:if test="${d.id() eq dentistId}">
                    <div class="table-wrap">
                        <table>
                            <tbody>
                                <tr>
                                    <th>Name</th>
                                    <td><c:out value="${d.name()}" /></td>
                                </tr>
                                <c:if test="${not empty d.specialization()}">
                                    <tr>
                                        <th>Specialisation</th>
                                        <td><c:out value="${d.specialization()}" /></td>
                                    </tr>
                                </c:if>
                                <tr>
                                    <th>Consultation fee</th>
                                    <td>
                                        Rs <fmt:formatNumber value="${d.consultationFee()}"
                                                             minFractionDigits="2" maxFractionDigits="2" />
                                        <span class="page-subtitle"> &mdash; charged for every visit with this dentist</span>
                                    </td>
                                </tr>
                                <tr>
                                    <th>Service charge</th>
                                    <td>
                                        Rs <fmt:formatNumber value="${serviceCharge}"
                                                             minFractionDigits="2" maxFractionDigits="2" />
                                        <span class="page-subtitle"> &mdash; fixed clinic fee per appointment</span>
                                    </td>
                                </tr>
                                <tr>
                                    <th>Treatment cost</th>
                                    <td>
                                        <span class="page-subtitle">Shown in the treatment dropdown above &mdash; varies by procedure</span>
                                    </td>
                                </tr>
                                <tr>
                                    <th>Your estimated total</th>
                                    <td>
                                        Treatment cost
                                        + Rs <fmt:formatNumber value="${d.consultationFee()}"
                                                               minFractionDigits="2" maxFractionDigits="2" />
                                        + Rs <fmt:formatNumber value="${serviceCharge}"
                                                               minFractionDigits="2" maxFractionDigits="2" />
                                        <br>
                                        <span class="page-subtitle">
                                            This is an estimate. The final bill is issued by reception after your appointment.
                                        </span>
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </c:if>
            </c:forEach>
            <div class="form-actions">
                <a class="btn secondary" href="#top">Close</a>
            </div>
        </div>
    </div>
</c:if>

<%@ include file="/WEB-INF/jsp/shared/footer.jspf" %>