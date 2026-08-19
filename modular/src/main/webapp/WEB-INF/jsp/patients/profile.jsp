<%--
    The patient's profile, and where they declare what a dentist should know.

    Registration collects none of this (FR-NOTE-02). It is declared here, afterwards, by
    someone who has already decided to use the clinic.

    The form says plainly who reads these and who does not. A patient asked for medical
    information deserves to know where it goes before they type it.
--%>
<%@ include file="/WEB-INF/jsp/shared/taglibs.jspf" %>
<c:set var="pageTitle" value="My details" />
<%@ include file="/WEB-INF/jsp/shared/header.jspf" %>

<h1 class="page-title">My details</h1>
<p class="page-subtitle">Your contact details, and what you would like your dentist to know.</p>

<c:if test="${not empty saved}">
    <div class="notice">Saved.</div>
</c:if>

<c:if test="${not empty profile}">
    <div class="card">
        <h2>Contact details</h2>
        <div class="table-wrap">
            <table>
                <tbody>
                    <tr><th>Name</th><td><c:out value="${profile.name()}" /></td></tr>
                    <tr><th>Contact number</th><td><c:out value="${profile.contactNumber()}" /></td></tr>
                    <tr><th>Email</th><td><c:out value="${profile.email()}" /></td></tr>
                    <tr><th>Date of birth</th><td>${profile.dob()}</td></tr>
                </tbody>
            </table>
        </div>
        <p class="page-subtitle">
            Telephone the clinic to correct any of these.
        </p>
    </div>
</c:if>

<div class="card">
    <h2>What my dentist should know <span class="count">${fn:length(notes)}</span></h2>

    <div class="notice">
        <strong>Who sees this:</strong> you, and the dentist treating you. Not the front desk,
        and not the administrator. Your dentist sees it beside your appointment, and anything
        you mark as important is flagged on their schedule before you arrive.
    </div>

    <c:choose>
        <c:when test="${empty notes}">
            <p class="page-subtitle">
                You have not declared anything yet. Allergies, medication you take and
                ongoing conditions are the useful ones.
            </p>
        </c:when>
        <c:otherwise>
            <div class="table-wrap">
                <table>
                    <thead>
                        <tr><th>Kind</th><th>Detail</th><th>Important</th><th>Updated</th><th></th></tr>
                    </thead>
                    <tbody>
                        <c:forEach var="note" items="${notes}">
                            <tr>
                                <td><c:out value="${note.categoryLabel()}" /></td>
                                <td><c:out value="${note.detail()}" /></td>
                                <td>
                                    <c:if test="${note.critical()}">
                                        <span class="pill error">Important</span>
                                    </c:if>
                                </td>
                                <td>${note.updatedAt()}</td>
                                <td>
                                    <%-- Deleted, not archived: a fact that is no longer true
                                         and cannot be removed is worse than no record. --%>
                                    <form method="post" action="${ctx}/patient/profile"
                                          style="display:inline">
                                        <input type="hidden" name="action" value="withdraw">
                                        <input type="hidden" name="noteId" value="${note.id()}">
                                        <button type="submit" class="btn small secondary">Remove</button>
                                    </form>
                                </td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:otherwise>
    </c:choose>
</div>

<div class="card">
    <h2>Add something</h2>
    <form method="post" action="${ctx}/patient/profile">
        <input type="hidden" name="action" value="declare">
        <div class="form-row">
            <div class="field">
                <label for="category">Kind</label>
                <select id="category" name="category" required>
                    <c:forEach var="c" items="${categories}">
                        <option value="${c}">${c.label()}</option>
                    </c:forEach>
                </select>
            </div>
            <div class="field">
                <label for="detail">Detail</label>
                <input type="text" id="detail" name="detail" required
                       placeholder="e.g. Allergic to penicillin">
            </div>
        </div>
        <div class="field">
            <label>
                <input type="checkbox" name="critical" value="1">
                Important &mdash; my dentist should see this before treating me
            </label>
        </div>
        <div class="form-actions">
            <button type="submit" class="btn">Add</button>
        </div>
    </form>
</div>

<%@ include file="/WEB-INF/jsp/shared/footer.jspf" %>
