<%--
    TEMPORARY - delete at step 4, with StubHomeServlet.

    Stands in for the four role landing pages so sign-in has somewhere to go.
    See StubHomeServlet for why this exists rather than accepting a 404.
--%>
<%@ include file="/WEB-INF/jsp/shared/taglibs.jspf" %>
<c:set var="pageTitle" value="Signed in" />
<%@ include file="/WEB-INF/jsp/shared/header.jspf" %>

<h1 class="page-title">Signed in</h1>
<p class="page-subtitle">
    <c:out value="${user.displayName}" />, as
    <c:out value="${user.role}" />.
</p>

<div class="card">
    <div class="notice info">
        <strong>This screen is a placeholder.</strong>
        Sign-in, the four portals, role routing, session timeout and lock-out all
        work. The dashboard that belongs here arrives with the module that owns it.
    </div>

    <h2>What this role may do</h2>
    <div class="table-wrap">
        <table>
            <thead><tr><th>Home path</th><th>Portal</th></tr></thead>
            <tbody>
                <tr>
                    <td><code><c:out value="${user.policy().homePath()}" /></code></td>
                    <td><code><c:out value="${user.policy().loginPath()}" /></code></td>
                </tr>
            </tbody>
        </table>
    </div>
    <p class="page-subtitle">
        Read from this role's own RolePolicy, not from a switch in a servlet.
    </p>
</div>
<%@ include file="/WEB-INF/jsp/shared/footer.jspf" %>
