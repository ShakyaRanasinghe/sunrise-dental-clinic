<%--
    The audit trail — read-only, and there is no route that edits or deletes a record
    (FR-ADM-31).

    Filtering by target is what answers "who changed this appointment, and when" for any
    appointment number (FR-ADM-32). The trail records that a diagnosis was written, never
    what it said (FR-ADM-33) — a property of what AuditEvent carries, so no view can leak
    it even by accident.
--%>
<%@ include file="/WEB-INF/jsp/shared/taglibs.jspf" %>
<c:set var="pageTitle" value="Audit trail" />
<%@ include file="/WEB-INF/jsp/shared/header.jspf" %>

<h1 class="page-title">Audit trail</h1>
<p class="page-subtitle">Who did what, and when. Append-only.</p>

<div class="card">
    <form method="get" action="${ctx}/admin/audit">
        <div class="form-row">
            <div class="field">
                <label for="targetId">Appointment or account</label>
                <input type="text" id="targetId" name="targetId"
                       value="<c:out value='${targetId}' />" placeholder="APT-20260820-0001">
            </div>
            <div class="field">
                <label for="actorUid">Actor</label>
                <input type="text" id="actorUid" name="actorUid"
                       value="<c:out value='${actorUid}' />" placeholder="u-recep">
            </div>
            <div class="field">
                <label for="from">From</label>
                <input type="date" id="from" name="from" value="<c:out value='${from}' />">
            </div>
            <div class="field">
                <label for="to">To</label>
                <input type="date" id="to" name="to" value="<c:out value='${to}' />">
            </div>
            <div class="form-actions">
                <button type="submit" class="btn">Search</button>
                <a class="btn btn-secondary" href="${ctx}/admin/audit">Clear</a>
            </div>
        </div>
    </form>
</div>

<div class="card">
    <h2>Records <span class="count">${fn:length(events)}</span></h2>

    <c:choose>
        <c:when test="${empty events}">
            <p class="page-subtitle">Nothing matches. Every action leaves a record, so an
                empty result means the filter is too narrow.</p>
        </c:when>
        <c:otherwise>
            <div class="table-wrap">
                <table>
                    <thead>
                        <tr><th>When</th><th>Actor</th><th>Role</th><th>Action</th><th>Target</th></tr>
                    </thead>
                    <tbody>
                        <c:forEach var="e" items="${events}">
                            <tr>
                                <td>${e.timestamp}</td>
                                <td><code><c:out value="${e.actorUid}" /></code></td>
                                <td><c:out value="${e.actorRole}" /></td>
                                <td><c:out value="${e.action}" /></td>
                                <td>
                                    <c:out value="${e.targetType}" />
                                    <code><c:out value="${e.targetId}" /></code>
                                </td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
            <p class="page-subtitle">
                Newest first, at most ${pageSize} records. Narrow the range to see further
                back.
            </p>
        </c:otherwise>
    </c:choose>
</div>

<%@ include file="/WEB-INF/jsp/shared/footer.jspf" %>
