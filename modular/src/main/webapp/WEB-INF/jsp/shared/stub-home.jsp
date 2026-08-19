<%--
    TEMPORARY - delete with StubHomeServlet when the reporting module lands.

    Only the administrator still sees this. The patient, reception and dentist landing
    pages became real screens when the appointments module was migrated.
--%>
<%@ include file="/WEB-INF/jsp/shared/taglibs.jspf" %>
<c:set var="pageTitle" value="Signed in" />
<%@ include file="/WEB-INF/jsp/shared/header.jspf" %>

<h1 class="page-title">Signed in</h1>
<p class="page-subtitle"><c:out value="${user.displayName}" />, as <c:out value="${user.role}" />.</p>

<div class="card">
    <div class="notice">
        <strong>This screen is a placeholder.</strong> Sign-in, the four portals, role routing
        and lock-out all work. Reports, account management and the audit trail arrive with the
        reporting module.
    </div>

    <h2>What already works</h2>
    <p class="page-subtitle">
        The administrator holds every action the front-desk screens perform, so these are
        reachable now - from the addresses below rather than from a dashboard of its own:
    </p>
    <div class="table-wrap">
        <table>
            <tbody>
                <tr><th>The patient register</th>
                    <td><a href="${ctx}/reception/patients">/reception/patients</a></td></tr>
                <tr><th>Publish availability</th>
                    <td><a href="${ctx}/reception/availability">/reception/availability</a></td></tr>
                <tr><th>The day view</th>
                    <td><a href="${ctx}/reception/home">/reception/home</a></td></tr>
            </tbody>
        </table>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/shared/footer.jspf" %>
