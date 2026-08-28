<%@ include file="/WEB-INF/jsp/shared/taglibs.jspf" %>
<c:set var="pageTitle" value="Treatment catalogue" />
<%@ include file="/WEB-INF/jsp/shared/header.jspf" %>

<h1 class="page-title">Treatment catalogue</h1>
<p class="page-subtitle">Manage treatments and prices shown to patients at booking.</p>

<c:if test="${not empty notice}">
    <div class="notice"><c:out value="${notice}" /></div>
</c:if>

<%-- ── Add new treatment ───────────────────────────────────────────── --%>
<div class="card">
    <h2>Add treatment</h2>
    <form method="post" action="${ctx}/admin/treatments">
        <input type="hidden" name="action" value="add">
        <div class="form-row">
            <div class="field">
                <label for="name">Name</label>
                <input type="text" id="name" name="name" placeholder="e.g. Teeth whitening" required>
            </div>
            <div class="field">
                <label for="baseCost">Price (Rs)</label>
                <input type="text" id="baseCost" name="baseCost" placeholder="e.g. 4500.00" required>
            </div>
            <div class="field">
                <label for="description">Description <span class="page-subtitle">(optional)</span></label>
                <input type="text" id="description" name="description"
                       placeholder="Short description shown at booking">
            </div>
        </div>
        <div class="form-actions">
            <button type="submit" class="btn">Add treatment</button>
        </div>
    </form>
</div>

<%-- ── Catalogue ───────────────────────────────────────────────────── --%>
<div class="card">
    <h2>All treatments <span class="count">${fn:length(treatments)}</span></h2>
    <div class="table-wrap">
        <table>
            <thead>
                <tr>
                    <th>Name</th>
                    <th>Description</th>
                    <th>Price (Rs)</th>
                    <th>Status</th>
                    <th></th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="t" items="${treatments}">
                    <tr>
                        <td><c:out value="${t.getName()}" /></td>
                        <td><c:out value="${t.getDescription()}" /></td>
                        <td>
                            <fmt:formatNumber value="${t.getBaseCost()}"
                                             minFractionDigits="2" maxFractionDigits="2" />
                        </td>
                        <td>
                            <c:choose>
                                <c:when test="${t.isActive()}">
                                    <span class="pill open">Active</span>
                                </c:when>
                                <c:otherwise>
                                    <span class="pill">Inactive</span>
                                </c:otherwise>
                            </c:choose>
                        </td>
                        <td>
                            <%-- Edit form inline --%>
                            <form method="post" action="${ctx}/admin/treatments"
                                  style="display:inline-flex;gap:4px;align-items:center;flex-wrap:wrap;">
                                <input type="hidden" name="action" value="update">
                                <input type="hidden" name="id" value="<c:out value='${t.getId()}' />">
                                <input type="text" name="name" value="<c:out value='${t.getName()}' />"
                                       style="width:140px;" required>
                                <input type="text" name="baseCost"
                                       value="<fmt:formatNumber value='${t.getBaseCost()}' minFractionDigits='2' maxFractionDigits='2' />"
                                       style="width:90px;" required>
                                <input type="text" name="description"
                                       value="<c:out value='${t.getDescription()}' />"
                                       style="width:180px;">
                                <button type="submit" class="btn small">Save</button>
                            </form>

                            <%-- Activate / Deactivate --%>
                            <c:choose>
                                <c:when test="${t.isActive()}">
                                    <form method="post" action="${ctx}/admin/treatments"
                                          style="display:inline">
                                        <input type="hidden" name="action" value="deactivate">
                                        <input type="hidden" name="id"
                                               value="<c:out value='${t.getId()}' />">
                                        <button type="submit" class="btn small secondary">Deactivate</button>
                                    </form>
                                </c:when>
                                <c:otherwise>
                                    <form method="post" action="${ctx}/admin/treatments"
                                          style="display:inline">
                                        <input type="hidden" name="action" value="reactivate">
                                        <input type="hidden" name="id"
                                               value="<c:out value='${t.getId()}' />">
                                        <button type="submit" class="btn small">Reactivate</button>
                                    </form>
                                </c:otherwise>
                            </c:choose>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </div>
    <p class="page-subtitle">
        Inactive treatments are hidden from patient booking but kept in appointment history.
    </p>
</div>

<%@ include file="/WEB-INF/jsp/shared/footer.jspf" %>
