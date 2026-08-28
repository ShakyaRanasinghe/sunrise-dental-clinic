<%--
    The Patient portal. Identical to the other three but for the label and the
    note - everything else comes from login-form.jspf.
--%>
<%@ include file="/WEB-INF/jsp/shared/taglibs.jspf" %>
<c:set var="pageTitle" value="Patient sign in" />
<%@ include file="/WEB-INF/jsp/shared/header.jspf" %>

<div class="narrow">
    <div class="brand-block">
        <div class="name">Sunrise Dental Clinic</div>
        <div class="tagline">Appointments &amp; patient records</div>
    </div>

    <div class="card">
        <p class="page-subtitle" style="margin-top:0">Sign in to manage your appointments.</p>
        <%@ include file="/WEB-INF/jsp/access/login-form.jspf" %>
    </div>

    <div class="alt"><a href="${ctx}/help">Need help signing in?</a></div>
</div>

<%@ include file="/WEB-INF/jsp/shared/footer.jspf" %>
