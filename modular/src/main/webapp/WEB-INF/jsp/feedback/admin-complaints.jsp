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

<%-- GAP-FTB-14: resolving confirms in a sub-window. --%>
<c:if test="${not empty done}">
    <div class="sub-window open" role="dialog" aria-modal="true" aria-labelledby="complaint-done-title">
        <div class="dialog">
            <span class="success-tick" aria-hidden="true">&#10003;</span>
            <h3 id="complaint-done-title">Successfully updated</h3>
            <p class="page-subtitle">The concern was reviewed.</p>
            <div class="form-actions">
                <a class="btn" href="${ctx}/admin/complaints">Close</a>
            </div>
        </div>
    </div>
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
        <%-- GAP-ADM-13: newest first by default, reversible to oldest. --%>
        <div class="field">
            <label for="sort">Order</label>
            <select id="sort" name="sort">
                <option value="newest"<c:if test="${sort ne 'oldest'}"> selected</c:if>>Newest first</option>
                <option value="oldest"<c:if test="${sort eq 'oldest'}"> selected</c:if>>Oldest first</option>
            </select>
        </div>
        <div class="form-actions">
            <button type="submit" class="btn">Filter</button>
            <a class="btn btn-secondary" href="${ctx}/admin/complaints">Clear</a>
        </div>
    </form>
</div>

<c:choose>
    <c:when test="${empty open and empty closed}">
        <div class="card">
            <p class="page-subtitle">Nothing matches.</p>
        </div>
    </c:when>
    <c:otherwise>

    <%--
        Open complaints first, each as a compact card. The review controls sit inside a
        <details> so a growing queue stays scannable — you see who, when and what at a
        glance, and open the form only for the one you are acting on (GAP-ADM-09).
    --%>
    <h2 class="page-title">To act on</h2>
    <c:if test="${empty open}">
        <div class="card">
            <p class="page-subtitle">No complaints waiting on you.</p>
        </div>
    </c:if>
    <c:forEach var="c" items="${open}">
        <details class="card complaint-card" <c:if test="${c.status() eq 'UNDER_REVIEW'}">open</c:if>>
            <summary>
                <span class="complaint-card__title">
                    <c:out value="${c.categoryLabel()}" />
                </span>
                <span class="complaint-card__who">
                    <%-- GAP-PAT-32: general concerns arrive with neither identity. --%>
                    <c:choose><c:when test="${empty c.patientName()}">Anonymous</c:when><c:otherwise><c:out value="${c.patientName()}" /></c:otherwise></c:choose>
                    <c:if test="${not empty c.dentistName()}"> &middot; named <c:out value="${c.dentistName()}" /></c:if>
                    <c:if test="${empty c.dentistName()}"> &middot; general concern</c:if>
                </span>
                <span class="complaint-card__status pill <c:if test='${c.isOpen()}'>error</c:if>">
                    <c:out value="${c.statusLabel()}" /></span>
            </summary>
            <div class="table-wrap">
                <table>
                    <tbody>
                        <c:if test="${not empty c.appointmentNo()}">
                            <tr><th>Appointment</th>
                                <td><code>${c.appointmentNo()}</code></td></tr>
                        </c:if>
                        <tr><th>Raised</th><td>${c.submittedAt()}</td></tr>
                        <tr>
                            <%-- FR-ADM-55: the patient's account of what happened is
                                 read-only, and cannot be rewritten from here or anywhere. --%>
                            <th>What the patient said</th>
                            <td><c:out value="${c.detail()}" /></td>
                        </tr>
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
            </c:choose>
        </details>
    </c:forEach>

    <%--
        Complaints already reviewed or closed are folded away so they never crowd the
        queue. Open on demand with <details>, no script (GAP-ADM-09).
    --%>
    <c:if test="${not empty closed}">
        <details class="card done-list">
            <summary>
                Reviewed
                <span class="count">${fn:length(closed)}</span>
            </summary>
            <c:forEach var="c" items="${closed}">
                <div class="complaint-card faded">
                    <div class="complaint-card__head">
                        <span class="complaint-card__title"><c:out value="${c.categoryLabel()}" /></span>
                        <span class="pill"><c:out value="${c.statusLabel()}" /></span>
                    </div>
                    <div class="complaint-card__who">
                        <c:choose><c:when test="${empty c.patientName()}">Anonymous &middot; general concern</c:when><c:otherwise><c:out value="${c.patientName()}" /></c:otherwise></c:choose>
                        <c:if test="${not empty c.resolution()}">
                            <span class="complaint-card__resolution"><c:out value="${c.resolution()}" /></span>
                        </c:if>
                    </div>
                    <div class="table-wrap">
                        <table>
                            <tbody>
                                <tr><th>Raised</th><td>${c.submittedAt()}</td></tr>
                                <tr><th>What the patient said</th><td><c:out value="${c.detail()}" /></td></tr>
                                <c:if test="${not empty c.resolution()}">
                                    <tr><th>Resolution</th><td><c:out value="${c.resolution()}" /></td></tr>
                                </c:if>
                            </tbody>
                        </table>
                    </div>
                </div>
            </c:forEach>
        </details>
    </c:if>

    </c:otherwise>
</c:choose>

<%@ include file="/WEB-INF/jsp/shared/footer.jspf" %>
