<%--
    The patient register: search the existing records, and add a walk-in.

    The front desk's two jobs are "find this person" and "this person is new", and
    which one applies is not known until the search comes back empty. Search sits
    full width at the top with the register (the patient list) straight under it, so
    the desk sees results without scrolling. The walk-in registration form is behind
    the "Register a walk-in" toggle below the search, so the layout stays compact
    until it is needed.

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
<p class="page-subtitle">Search the register, or add someone who has just walked in. <a href="${ctx}/help/reception">How does this screen work?</a></p>

<%-- GAP-FTB-14: registering confirms in a sub-window. The duplicate warning
     below stays an inline notice — it is a warning, not the success. --%>
<c:if test="${not empty registered}">
    <div class="sub-window open" role="dialog" aria-modal="true" aria-labelledby="registered-title">
        <div class="dialog">
            <span class="success-tick" aria-hidden="true">&#10003;</span>
            <h3 id="registered-title">Successfully registered</h3>
            <p><strong><c:out value="${registered.name()}" /></strong>
                on <c:out value="${registered.contactNumber()}" />.</p>
            <div class="form-actions">
                <a class="btn" href="${ctx}/reception/patients">Close</a>
            </div>
        </div>
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

<div class="card">
    <h2>Search</h2>
    <form method="get" action="${ctx}/reception/patients" class="search-form">
        <div class="field grow">
            <label for="q">Name, contact number or email</label>
            <input type="search" id="q" name="q" value="<c:out value='${q}' />"
                   placeholder="e.g. Perera or 077&hellip;">
        </div>
        <div class="form-actions inline">
            <button type="submit" class="btn">Search</button>
            <c:if test="${not empty q}">
                <a class="btn btn-secondary" href="${ctx}/reception/patients">Clear</a>
            </c:if>
        </div>
    </form>
    <p class="page-subtitle">One field covers all three &mdash; type whatever the patient gives you.</p>
</div>

<details class="card walkin">
    <summary>Register a walk-in</summary>
    <form method="post" action="${ctx}/reception/patients">
        <div class="field">
            <label for="name">Full name</label>
            <input type="text" id="name" name="name" required>
        </div>
        <div class="form-row">
            <div class="field">
                <label for="contactNumber">Contact number</label>
                <input type="tel" id="contactNumber" name="contactNumber" required
                       pattern="[0-9+() -]{10,20}"
                       title="A Sri Lankan number: 10 digits starting with 0, or +94 international.">
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
</details>

<div class="card">
    <h2>
        <c:choose>
            <c:when test="${empty q}">All patients</c:when>
            <c:otherwise>Matching &ldquo;<c:out value="${q}" />&rdquo;</c:otherwise>
        </c:choose>
        <%--
            The honest size of the register, not the number of rows on this screen.
            The table shows one page; the count is everything that would match.
        --%>
        <span class="count">${total}</span>
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
                            <th>Patient ID</th><th>Name</th><th>Contact</th><th>Email</th>
                            <th>Date of birth</th><th>Portal account</th><th></th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="p" items="${patients}">
                            <tr>
                                <td class="mono"><c:out value="${p.patientNumber()}" /></td>
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
            <c:if test="${total > pageSize}">
                <%--
                    Q&A-free pager: the links are plain GET addresses that carry the
                    search term and the target page, so every page of a result is a
                    stable, refreshable, shareable URL - the same contract the search
                    box already follows. The controls are hand-built in the servlet,
                    clamped to the ends of the register so a stale link still draws a
                    real page.
                --%>
                <c:url var="pageUrl" value="${ctx}/reception/patients">
                    <c:param name="q" value="${q}" />
                </c:url>
                <div class="pager">
                    <p class="page-subtitle">
                        Showing ${firstOnPage}&ndash;${lastOnPage} of ${total}
                        patient<c:if test="${total != 1}">s</c:if>.
                    </p>
                    <nav class="pager__links" aria-label="Patient list pages">
                        <c:choose>
                            <c:when test="${not empty prevPage}">
                                <a class="pager__link" rel="prev" href="${pageUrl}&amp;page=${prevPage}">&larr; Prev</a>
                            </c:when>
                            <c:otherwise>
                                <span class="pager__link disabled" aria-disabled="true">&larr; Prev</span>
                            </c:otherwise>
                        </c:choose>
                        <c:forEach var="n" items="${pages}">
                            <c:choose>
                                <c:when test="${n == page}">
                                    <span class="pager__link current" aria-current="page">${n}</span>
                                </c:when>
                                <c:otherwise>
                                    <a class="pager__link" href="${pageUrl}&amp;page=${n}">${n}</a>
                                </c:otherwise>
                            </c:choose>
                        </c:forEach>
                        <c:choose>
                            <c:when test="${not empty nextPage}">
                                <a class="pager__link" rel="next" href="${pageUrl}&amp;page=${nextPage}">Next &rarr;</a>
                            </c:when>
                            <c:otherwise>
                                <span class="pager__link disabled" aria-disabled="true">Next &rarr;</span>
                            </c:otherwise>
                        </c:choose>
                    </nav>
                </div>
            </c:if>
        </c:otherwise>
    </c:choose>
</div>

<%@ include file="/WEB-INF/jsp/shared/footer.jspf" %>
