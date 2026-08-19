<%--
    Account administration.

    The one-time password is shown once, here, on the response that created the account.
    It is never stored and cannot be looked up (FR-ADM-25), which is why this screen
    renders after the POST instead of redirecting: a redirect would either lose it or
    carry it in a URL, where it would sit in the browser history and the access log.
--%>
<%@ include file="/WEB-INF/jsp/shared/taglibs.jspf" %>
<c:set var="pageTitle" value="Accounts" />
<%@ include file="/WEB-INF/jsp/shared/header.jspf" %>

<h1 class="page-title">Accounts</h1>
<p class="page-subtitle">Create staff accounts, unlock a locked one, or take one out of use.</p>

<c:if test="${not empty created}">
    <div class="card">
        <h2>Hand this over now</h2>
        <div class="notice error">
            This password is shown <strong>once</strong>. It is not stored anywhere and
            cannot be looked up. If it is lost, reset it below and issue a new one.
        </div>
        <div class="table-wrap">
            <table>
                <tbody>
                    <tr><th>Name</th><td><c:out value="${created.account().displayName()}" /></td></tr>
                    <tr><th>Email</th><td><c:out value="${created.account().email()}" /></td></tr>
                    <tr><th>Role</th><td>${created.account().role()}</td></tr>
                    <tr><th>One-time password</th>
                        <td><code class="otp"><c:out value="${created.oneTimePassword()}" /></code></td></tr>
                </tbody>
            </table>
        </div>
        <p class="page-subtitle">
            Ask them to sign in at
            <code>/login/<c:out value="${fn:toLowerCase(created.account().role())}" /></code>
            and change it.
        </p>
    </div>
</c:if>

<div class="card">
    <h2>New staff account</h2>
    <form method="post" action="${ctx}/admin/accounts">
        <input type="hidden" name="action" value="create">
        <div class="form-row">
            <div class="field">
                <label for="displayName">Full name</label>
                <input type="text" id="displayName" name="displayName" required>
            </div>
            <div class="field">
                <label for="email">Email</label>
                <input type="email" id="email" name="email" required>
            </div>
            <div class="field">
                <label for="role">Role</label>
                <select id="role" name="role" required>
                    <c:forEach var="r" items="${roles}">
                        <option value="${r}">${r}</option>
                    </c:forEach>
                </select>
            </div>
        </div>
        <%--
            Only meaningful for a dentist, and harmless otherwise — the service ignores
            them for other roles. Hiding them would need script, and a label saying who
            they are for costs nothing.
        --%>
        <div class="form-row">
            <div class="field">
                <label for="specialization">Specialisation <span class="page-subtitle">(dentists)</span></label>
                <input type="text" id="specialization" name="specialization"
                       placeholder="General Dentistry">
            </div>
            <div class="field">
                <label for="consultationFee">Consultation fee <span class="page-subtitle">(dentists, Rs)</span></label>
                <input type="text" id="consultationFee" name="consultationFee" placeholder="1500.00">
            </div>
        </div>
        <div class="notice">
            Patients register themselves, so there is no option for one here. Creating a
            dentist also creates their profile, so their schedule works from the first
            sign-in.
        </div>
        <div class="form-actions">
            <button type="submit" class="btn">Create and issue a password</button>
        </div>
    </form>
</div>

<div class="card">
    <h2>All accounts <span class="count">${fn:length(accounts)}</span></h2>
    <div class="table-wrap">
        <table>
            <thead>
                <tr><th>Name</th><th>Email</th><th>Role</th><th>State</th><th></th></tr>
            </thead>
            <tbody>
                <c:forEach var="a" items="${accounts}">
                    <tr>
                        <td><c:out value="${a.displayName()}" /></td>
                        <td><c:out value="${a.email()}" /></td>
                        <td>${a.role()}</td>
                        <td>
                            <c:choose>
                                <c:when test="${a.locked()}">
                                    <span class="pill error">Locked</span>
                                </c:when>
                                <c:when test="${not a.active()}">
                                    <span class="pill">Inactive</span>
                                </c:when>
                                <c:otherwise><span class="pill open">Active</span></c:otherwise>
                            </c:choose>
                            <c:if test="${a.failedAttempts() gt 0 and not a.locked()}">
                                <span class="page-subtitle">${a.failedAttempts()} failed</span>
                            </c:if>
                        </td>
                        <td>
                            <c:if test="${a.locked()}">
                                <form method="post" action="${ctx}/admin/accounts" style="display:inline">
                                    <input type="hidden" name="action" value="unlock">
                                    <input type="hidden" name="uid" value="${a.uid()}">
                                    <button type="submit" class="btn small">Unlock</button>
                                </form>
                            </c:if>
                            <form method="post" action="${ctx}/admin/accounts" style="display:inline">
                                <input type="hidden" name="action" value="reset">
                                <input type="hidden" name="uid" value="${a.uid()}">
                                <button type="submit" class="btn small secondary">Reset password</button>
                            </form>
                            <%--
                                FR-ADM-26: no control at all against your own account.
                                The service refuses it too; offering a button that always
                                fails would be worse than not offering one.
                            --%>
                            <c:if test="${a.uid() ne user.uid}">
                                <c:choose>
                                    <c:when test="${a.active()}">
                                        <form method="post" action="${ctx}/admin/accounts" style="display:inline">
                                            <input type="hidden" name="action" value="deactivate">
                                            <input type="hidden" name="uid" value="${a.uid()}">
                                            <button type="submit" class="btn small secondary">Deactivate</button>
                                        </form>
                                    </c:when>
                                    <c:otherwise>
                                        <form method="post" action="${ctx}/admin/accounts" style="display:inline">
                                            <input type="hidden" name="action" value="reactivate">
                                            <input type="hidden" name="uid" value="${a.uid()}">
                                            <button type="submit" class="btn small">Reactivate</button>
                                        </form>
                                    </c:otherwise>
                                </c:choose>
                            </c:if>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </div>
    <p class="page-subtitle">
        Accounts are deactivated, never deleted: appointments, bills and audit records refer
        to them, and deleting one would take the history with it.
    </p>
</div>

<%@ include file="/WEB-INF/jsp/shared/footer.jspf" %>
