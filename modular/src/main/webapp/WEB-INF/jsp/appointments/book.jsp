<%--
    Booking, in three steps on one page.

    Driven by the query string, not by script - the stack has no client-side JavaScript.
    Choosing a dentist reloads with that dentist's open times; choosing a time posts the
    booking. Each step is a real address, so the back button works.

    Only OPEN slots are offered. A patient must never be shown a time they cannot have.

    GAP-PAT-15: once a dentist and date are chosen, the open TIMES are the first card
    (they are the thing the patient is deciding). GAP-PAT-28: the times head the
    single card under a heading naming the doctor, the treatment picker sits below
    them, the submit names the dentist ("Proceed appointment with …"), and the old
    "2. Dentist and date" picker card is gone. GAP-PAT-29: each treatment has an
    info mark opening its description in a sub-window. The doctor's fee details are
    not a card on the page at all: the dentist's name beside the times opens them in
    a sub-window - a CSS-only :target modal, so no script is needed. Closing a modal
    links to #top, which clears the :target and hides it again.
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
        GAP-PAT-15/28: the times are the card now. The patient chose a dentist and
        date on their way here (or jumped from the availability overview), so the first
        thing they should see is which times are actually open, then the treatment
        picker below. No dentist/date picker card: a link returns to the overview.
    --%>
    <%--
        GAP-PAT-28: the available TIMES come first under a heading naming the doctor,
        then the treatment picker below them. The old "2. Dentist and date" picker
        card is gone — the patient chose both to get here, and a small link returns
        to the overview to pick differently.
    --%>
    <div class="card">
        <c:choose>
            <c:when test="${empty slots}">
                <h2>Available times</h2>
                <p class="page-subtitle">
                    No times are open on ${date}. <a href="${ctx}/patient/book">Choose a different dentist or date</a>, or telephone the clinic.
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
                <h2>Available times with <c:out value="${slots[0].dentistName()}" /> on ${date}</h2>
                <p class="page-subtitle">
                    <a href="${ctx}/patient/book">Choose a different dentist or date</a>
                    &middot;
                    <a href="#dentist-details">About <c:out value="${slots[0].dentistName()}" /></a>
                </p>
                <form method="post" action="${ctx}/patient/book">
                    <c:if test="${not empty patientId}">
                        <input type="hidden" name="patientId" value="<c:out value='${patientId}' />">
                    </c:if>
                    <div class="slots">
                        <c:forEach var="slot" items="${slots}">
                            <div class="slot">
                                <input type="radio" id="slot-${slot.id()}"
                                       name="slotId" value="${slot.id()}" required>
                                <label for="slot-${slot.id()}">${slot.startTime()}</label>
                            </div>
                        </c:forEach>
                    </div>
                    <%--
                        GAP-FTB-06: a single-treatment radio checklist replaces the old
                        dropdown, and an "Other (describe…)" option lets a patient book a
                        visit whose need no listed procedure names — the free text is
                        captured as the booking reason. CSS-only, no script.
                        GAP-PAT-29: each treatment carries an info mark opening the
                        administrator-written description in a sub-window below.
                    --%>
                    <fieldset class="treatment-picker">
                        <legend class="field__label">Treatment for this visit</legend>
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
                                <a class="treatment-info" href="#treatment-${t.id()}"
                                   title="About <c:out value='${t.name()}' />" aria-label="About <c:out value='${t.name()}' />">i</a>
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

                    <div class="form-actions" style="margin-top:1rem;">
                        <button type="submit" class="btn">Proceed appointment with <c:out value="${slots[0].dentistName()}" /></button>
                    </div>
                </form>
                <%--
                    GAP-PAT-29 sub-windows: one per offered treatment, showing the
                    administrator-written description. CSS-only via :target, like the
                    dentist-details dialog; Close returns to #top.
                --%>
                <c:forEach var="t" items="${treatments}">
                    <div class="sub-window" id="treatment-${t.id()}" role="dialog"
                         aria-modal="true" aria-labelledby="treatment-title-${t.id()}">
                        <div class="dialog">
                            <h3 id="treatment-title-${t.id()}"><c:out value="${t.name()}" /></h3>
                            <p><c:out value="${t.description()}" /></p>
                            <p class="page-subtitle">Price:
                                Rs <fmt:formatNumber value="${t.baseCost()}"
                                                      minFractionDigits="2" maxFractionDigits="2" /></p>
                            <div class="form-actions">
                                <a class="btn secondary" href="#top">Close</a>
                            </div>
                        </div>
                    </div>
                </c:forEach>
            </c:otherwise>
        </c:choose>
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