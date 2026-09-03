<%@ include file="/WEB-INF/jsp/shared/taglibs.jspf" %>
<c:set var="pageTitle" value="Walk-in booking" />
<%@ include file="/WEB-INF/jsp/shared/header.jspf" %>

<h1 class="page-title">Walk-in booking</h1>
<p class="page-subtitle">Find or register the patient, then pick an open slot for them. <a href="${ctx}/help/reception">How does this screen work?</a></p>

<%-- GAP-FTB-14: registering confirms in a sub-window; Close keeps the patient. --%>
<c:if test="${param.registered == '1' and not empty patientId}">
    <div class="sub-window open" role="dialog" aria-modal="true" aria-labelledby="walkin-registered-title">
        <div class="dialog">
            <span class="success-tick" aria-hidden="true">&#10003;</span>
            <h3 id="walkin-registered-title">Successfully registered</h3>
            <p class="page-subtitle">The walk-in patient was added. Pick an open slot for them below.</p>
            <div class="form-actions">
                <a class="btn" href="${ctx}/reception/walkin?patientId=<c:out value='${patientId}' />&date=${date}">Close</a>
            </div>
        </div>
    </div>
</c:if>

<%-- ── Step 1: Find or register patient ───────────────────────────── --%>
<div class="grid two">
    <div class="card">
        <h2>Step 1 &mdash; Find patient</h2>
        <form method="get" action="${ctx}/reception/walkin">
            <input type="hidden" name="date" value="${date}">
            <div class="field">
                <label for="q">Name, contact number or email</label>
                <input type="search" id="q" name="q"
                       value="<c:out value='${q}' />"
                       placeholder="e.g. Perera or 077&hellip;" autofocus>
            </div>
            <div class="form-actions">
                <button type="submit" class="btn">Search</button>
            </div>
        </form>

        <c:if test="${not empty results}">
            <div class="table-wrap" style="margin-top:1rem;">
                <table>
                    <thead>
                        <tr><th>Patient ID</th><th>Name</th><th>Contact</th><th></th></tr>
                    </thead>
                    <tbody>
                        <c:forEach var="p" items="${results}">
                            <tr>
                                <td class="mono"><c:out value="${p.patientNumber()}" /></td>
                                <td><c:out value="${p.name()}" /></td>
                                <td><c:out value="${p.contactNumber()}" /></td>
                                <td>
                                    <a href="${ctx}/reception/walkin?patientId=<c:out value='${p.id()}' />&date=${date}"
                                       class="btn small">Select</a>
                                </td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:if>
        <c:if test="${not empty q and empty results}">
            <p class="page-subtitle" style="margin-top:0.75rem;">
                Nobody found. Register them as new below.
            </p>
        </c:if>
    </div>

    <div class="card">
        <h2>Step 1 &mdash; Or register new patient</h2>
        <form method="post" action="${ctx}/reception/walkin">
            <div class="field">
                <label for="name">Full name</label>
                <input type="text" id="name" name="name" required>
            </div>
            <div class="form-row">
                <div class="field">
                    <label for="contactNumber">Contact number</label>
                    <input type="tel" id="contactNumber" name="contactNumber" required>
                </div>
                <div class="field">
                    <label for="email">Email <span class="page-subtitle">(optional)</span></label>
                    <input type="email" id="email" name="email">
                </div>
            </div>
            <div class="form-actions">
                <button type="submit" class="btn">Register &amp; continue</button>
            </div>
        </form>
    </div>
</div>

<%-- ── Step 2: Date picker ─────────────────────────────────────────── --%>
<c:if test="${not empty patientId}">
    <div class="card">
        <h2>
            Step 2 &mdash; Choose a slot for
            <c:out value="${patient.name()}" />
        </h2>
        <form method="get" action="${ctx}/reception/walkin" class="form-row">
            <input type="hidden" name="patientId" value="<c:out value='${patientId}' />">
            <div class="field">
                <label for="date">Date</label>
                <input type="date" id="date" name="date" value="${date}">
            </div>
            <div class="field">
                <label for="filterDentistId">Doctor <span class="page-subtitle">(optional)</span></label>
                <select id="filterDentistId" name="filterDentistId">
                    <option value="">All doctors</option>
                    <c:forEach var="d" items="${dentists}">
                        <option value="<c:out value='${d.id()}' />"
                            <c:if test="${d.id() eq filterDentistId}">selected</c:if>>
                            <c:out value="${d.name()}" />
                        </option>
                    </c:forEach>
                </select>
            </div>
            <div class="form-actions">
                <button type="submit" class="btn">Show slots</button>
            </div>
        </form>

        <%-- Cost reference for quoting patient before booking --%>
        <div class="notice" style="margin-top:0.75rem;">
            Service charge: <strong>Rs <fmt:formatNumber value="${serviceCharge}"
                minFractionDigits="2" maxFractionDigits="2" /></strong> per appointment.
            Estimated total = treatment cost + consultation fee + service charge.
            <a href="${ctx}/reception/dentists">See all consultation fees</a>.
        </div>

        <c:choose>
            <c:when test="${empty slots}">
                <p class="page-subtitle" style="margin-top:1rem;">
                    No open slots on ${date}. Try another date or publish availability first.
                </p>
            </c:when>
            <c:otherwise>
                <div class="table-wrap" style="margin-top:1rem;">
                    <table>
                        <thead>
                            <tr>
                                <th>Time</th>
                                <th>Dentist</th>
                                <th class="right">Consultation fee</th>
                                <th></th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="slot" items="${slots}">
                                <tr>
                                    <td><strong>${slot.startTime()}</strong></td>
                                    <td><c:out value="${slot.dentistName()}" /></td>
                                    <td class="right">
                                        <%-- Look up this dentist's fee from the dentists list --%>
                                        <c:forEach var="d" items="${dentists}">
                                            <c:if test="${d.id() eq slot.dentistId()}">
                                                Rs <fmt:formatNumber value="${d.consultationFee()}"
                                                    minFractionDigits="2" maxFractionDigits="2" />
                                            </c:if>
                                        </c:forEach>
                                    </td>
                                    <td>
                                        <a href="${ctx}/patient/book?patientId=<c:out value='${patientId}' />&dentistId=<c:out value='${slot.dentistId()}' />&date=${date}"
                                           class="btn small">Book this slot</a>
                                    </td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
                <p class="page-subtitle">
                    ${fn:length(slots)} open slot<c:if test="${fn:length(slots) ne 1}">s</c:if>
                    on ${date}. Clicking a slot opens the booking page for
                    <c:out value="${patient.name()}" />.
                </p>
            </c:otherwise>
        </c:choose>
    </div>
</c:if>

<%@ include file="/WEB-INF/jsp/shared/footer.jspf" %>
