<%--
    The patient register: search the existing records, and add a walk-in.

    Two forms on one screen, deliberately. The front desk's two jobs are "find this
    person" and "this person is new", and which one applies is not known until the
    search comes back empty - so making them separate screens would mean navigating
    away and back at the moment the answer arrives.

    Search is a GET so the result is a shareable, refreshable address. Registration
    is a POST followed by a redirect, so a refresh does not create the patient twice.

    PatientResponse is a record, so its accessors are called as methods - ${p.name()}
    rather than ${p.name}. Jakarta EL 5.0 resolves a property by looking for getName()
    and does not understand a record's name().
--%>
<%@ include file="/WEB-INF/jsp/shared/taglibs.jspf" %>
<c:set var="pageTitle" value="Patient records" />
<%@ include file="/WEB-INF/jsp/shared/header.jspf" %>

<h1 class="page-title">Patient records</h1>
<p class="page-subtitle">Search the register, or add someone who has just walked in.</p>

<c:if test="${not empty registered}">
    <div class="notice">
        Registered <strong><c:out value="${registered.name()}" /></strong>
        on <c:out value="${registered.contactNumber()}" />.
    </div>
    <%--
        A warning, never a refusal. Two people can legitimately share a number - a
        household, a parent booking for a child - so the desk is told and decides
        (FR-REC-25).
    --%>
    <c:if test="${not empty possibleDuplicates}">
        <div class="notice error">
            That contact number is already on
            <strong>${fn:length(possibleDuplicates)}</strong>
            other record<c:if test="${fn:length(possibleDuplicates) > 1}">s</c:if>:
            <c:forEach var="other" items="${possibleDuplicates}" varStatus="loop">
                <c:out value="${other.name()}" /><c:if test="${not loop.last}">, </c:if>
            </c:forEach>.
            Check this is not the same person before booking.
        </div>
    </c:if>
</c:if>

<div class="grid two">
    <div class="card">
        <h2>Search</h2>
        <form method="get" action="${ctx}/reception/patients">
            <div class="field">
                <label for="q">Name, contact number or email</label>
                <input type="search" id="q" name="q" value="<c:out value='${q}' />"
                       placeholder="e.g. Perera or 077&hellip;">
            </div>
            <div class="form-actions">
                <button type="submit" class="btn">Search</button>
                <c:if test="${not empty q}">
                    <a class="btn btn-secondary" href="${ctx}/reception/patients">Clear</a>
                </c:if>
            </div>
        </form>
        <p class="page-subtitle">One field covers all three &mdash; type whatever the patient gives you.</p>
    </div>

    <div class="card">
        <h2>Add a walk-in</h2>
        <form method="post" action="${ctx}/reception/patients">
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
            <div class="form-row">
                <div class="field">
                    <label for="dob">Date of birth <span class="page-subtitle">(optional)</span></label>
                    <input type="date" id="dob" name="dob">
                </div>
                <div class="field">
                    <label for="address">Address <span class="page-subtitle">(optional)</span></label>
                    <input type="text" id="address" name="address">
                </div>
            </div>
            <div class="notice">
                This creates a patient record, not a login. The patient can create their own
                account later if they want online booking.
            </div>
            <div class="form-actions">
                <button type="submit" class="btn">Add patient</button>
            </div>
        </form>
    </div>
</div>

<div class="card">
    <h2>
        <c:choose>
            <c:when test="${empty q}">All patients</c:when>
            <c:otherwise>Matching &ldquo;<c:out value="${q}" />&rdquo;</c:otherwise>
        </c:choose>
        <span class="count">${fn:length(patients)}</span>
    </h2>

    <c:choose>
        <c:when test="${empty patients}">
            <p class="page-subtitle">
                <c:choose>
                    <c:when test="${empty q}">No patients on the register yet.</c:when>
                    <c:otherwise>
                        Nobody matches that. Add them as a walk-in above if they are new.
                    </c:otherwise>
                </c:choose>
            </p>
        </c:when>
        <c:otherwise>
            <div class="table-wrap">
                <table>
                    <thead>
                        <tr>
                            <th>Name</th><th>Contact</th><th>Email</th>
                            <th>Date of birth</th><th>Portal account</th><th></th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="p" items="${patients}">
                            <tr>
                                <td><c:out value="${p.name()}" /></td>
                                <td><c:out value="${p.contactNumber()}" /></td>
                                <td><c:out value="${p.email()}" /></td>
                                <%--
                                    dob is a LocalDate. Printed as it stands, which is ISO
                                    yyyy-MM-dd - unambiguous, and what the prototype shows.
                                    fmt:formatDate wants a java.util.Date and would need a
                                    conversion for no gain.
                                --%>
                                <td>${p.dob()}</td>
                                <%--
                                    hasPortalAccount, not the uid. Reception needs to know
                                    whether the patient can self-serve (FR-REC-22); the
                                    account identifier is internal and is not published.
                                --%>
                                <td>
                                    <c:choose>
                                        <c:when test="${p.hasPortalAccount()}">
                                            <span class="pill open">Yes</span>
                                        </c:when>
                                        <c:otherwise><span class="pill">Walk-in</span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <a href="${ctx}/patient/book?patientId=<c:out value='${p.id()}' />"
                                       class="btn small">Book</a>
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
