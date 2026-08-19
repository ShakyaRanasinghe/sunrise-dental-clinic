<%--
    The administrator's complaints screen.

    Open ones first — a concern submitted last week and untouched matters more than one
    resolved this morning (FR-ADM-51). Closing needs words (FR-ADM-53), and the patient's
    own account has no edit control anywhere on this page, because nothing in the
    application can rewrite it (FR-ADM-55).
--%>
<%@ include file="/WEB-INF/jsp/shared/taglibs.jspf" %>
<c:set var="pageTitle" value="Complaints" />
<%@ include file="/WEB-INF/jsp/shared/header.jspf" %>

<h1 class="page-title">Complaints</h1>
<p class="page-subtitle">Concerns raised by patients. The dentist named in one never sees it.</p>

<c:if test="${not empty done}">
    <div class="notice">Updated.</div>
</c:if>

<div class="card">
    <form method="get" action="${ctx}/admin/complaints" class="form-row">
        <div class="field">
            <label for="status">State</label>
            <select id="status" name="status">
                <option value="">All</option>
                <c:forEach var="s" items="${statuses}">
                    <option value="${s}" <c:if test="${s eq status}">selected</c:if>>
                        ${s.label()}</option>
                </c:forEach>
            </select>
        </div>
        <div class="field">
            <label for="dentistId">Dentist</label>
            <select id="dentistId" name="dentistId">
                <option value="">Any</option>
                <c:forEach var="d" items="${dentists}">
                    <option value="${d.id()}" <c:if test="${d.id() eq dentistId}">selected</c:if>>
                        <c:out value="${d.name()}" /></option>
                </c:forEach>
            </select>
        </div>
        <div class="field">
            <label for="from">From</label>
            <input type="date" id="from" name="from">
        </div>
        <div class="field">
            <label for="to">To</label>
            <input type="date" id="to" name="to">
        </div>
        <div class="form-actions">
            <button type="submit" class="btn">Filter</button>
            <a class="btn btn-secondary" href="${ctx}/admin/complaints">Clear</a>
        </div>
    </form>
</div>

<c:choose>
    <c:when test="${empty complaints}">
        <div class="card">
            <p class="page-subtitle">Nothing matches.</p>
        </div>
    </c:when>
    <c:otherwise>
        <c:forEach var="c" items="${complaints}">
            <div class="card">
                <h2>
                    <c:out value="${c.categoryLabel()}" />
                    <span class="count">
                        <span class="pill <c:if test='${c.isOpen()}'>error</c:if>">
                            <c:out value="${c.statusLabel()}" /></span>
                    </span>
                </h2>
                <div class="table-wrap">
                    <table>
                        <tbody>
                            <tr><th>Patient</th><td><c:out value="${c.patientName()}" /></td></tr>
                            <tr><th>Dentist named</th><td><c:out value="${c.dentistName()}" /></td></tr>
                            <c:if test="${not empty c.appointmentNo()}">
                                <tr><th>Appointment</th>
                                    <td><code>${c.appointmentNo()}</code></td></tr>
                            </c:if>
                            <tr><th>Raised</th><td>${c.submittedAt()}</td></tr>
                            <%--
                                Read-only, with no control beside it. FR-ADM-55: the
                                administrator must not be able to edit the patient's account
                                of what happened, and ComplaintDao's update statement cannot
                                write this column at all.
                            --%>
                            <tr><th>What the patient said</th><td><c:out value="${c.detail()}" /></td></tr>
                            <c:if test="${not empty c.resolution()}">
                                <tr><th>Resolution</th><td><c:out value="${c.resolution()}" /></td></tr>
                            </c:if>
                        </tbody>
                    </table>
                </div>

                <c:choose>
                    <c:when test="${c.status() eq 'SUBMITTED'}">
                        <form method="post" action="${ctx}/admin/complaints">
                            <input type="hidden" name="action" value="review">
                            <input type="hidden" name="complaintId" value="${c.id()}">
                            <div class="form-actions">
                                <button type="submit" class="btn">Start reviewing</button>
                            </div>
                        </form>
                    </c:when>
                    <c:when test="${c.status() eq 'UNDER_REVIEW'}">
                        <form method="post" action="${ctx}/admin/complaints">
                            <input type="hidden" name="complaintId" value="${c.id()}">
                            <div class="field">
                                <label for="r-${c.id()}">What was done about it</label>
                                <textarea id="r-${c.id()}" name="resolution" rows="3" required
                                          placeholder="Required — a concern closed with no explanation is a concern ignored."></textarea>
                            </div>
                            <div class="form-actions">
                                <button type="submit" class="btn" name="action" value="resolve">
                                    Resolve</button>
                                <button type="submit" class="btn btn-secondary" name="action"
                                        value="dismiss">Close without action</button>
                            </div>
                        </form>
                    </c:when>
                    <c:otherwise>
                        <p class="page-subtitle">Closed. Nothing further to do.</p>
                    </c:otherwise>
                </c:choose>
            </div>
        </c:forEach>
    </c:otherwise>
</c:choose>

<%@ include file="/WEB-INF/jsp/shared/footer.jspf" %>
