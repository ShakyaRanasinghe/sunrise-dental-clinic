<%--
    403 - signed in, but this page belongs to another role.

    Deliberately does not name what lives at the path. The user knows they are
    signed in and knows their own role; telling them which role owns the address
    only maps the parts of the system they cannot reach.
--%>
<%@ include file="/WEB-INF/jsp/shared/taglibs.jspf" %>
<c:set var="pageTitle" value="Not permitted" />
<%@ include file="/WEB-INF/jsp/shared/header.jspf" %>

<h1 class="page-title">Not permitted</h1>
<p class="page-subtitle">
    Your account does not have access to this page.
</p>

<div class="card">
    <p>
        You are signed in as <strong><c:out value="${user.displayName}" /></strong>
        (<c:out value="${user.role}" />). This page belongs to a different role.
    </p>
    <p class="form-actions">
        <a class="btn" href="${ctx}${user.policy().homePath()}">Back to your pages</a>
        <a class="btn btn-secondary" href="${ctx}/help">Help</a>
    </p>
</div>

<%@ include file="/WEB-INF/jsp/shared/footer.jspf" %>
