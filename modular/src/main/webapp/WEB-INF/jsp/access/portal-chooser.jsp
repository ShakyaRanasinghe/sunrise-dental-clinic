<%@ include file="/WEB-INF/jsp/shared/taglibs.jspf" %>
<c:set var="pageTitle" value="Sign in" />
<%@ include file="/WEB-INF/jsp/shared/header.jspf" %>

<div class="narrow">
    <div class="brand-block">
        <div class="name">Sunrise Dental Clinic</div>
        <div class="tagline">Appointments &amp; patient records</div>
    </div>

    <div class="card">
        <h2>Sign in</h2>
        <p class="page-subtitle">Choose how you use the clinic.</p>
        <%-- One door, not four. /login/reception, /login/dentist and
             /login/admin all work and are deliberately unlisted: no member of
             the public has business there, and a patient should not even learn
             that the staff doors exist (FR-ADM-03, GAP-PAT-14). --%>
        <div class="portals">
            <a class="portal" href="${ctx}/login/patient">
                <div class="name">I am a patient</div>
                <div class="what">Book, view or cancel your own appointments and see your receipts.</div>
            </a>
        </div>
    </div>

    <div class="alt">New patient? <a href="${ctx}/register">Create an account</a></div>
    <div class="alt"><a href="${ctx}/help">Need help?</a></div>
</div>

<%@ include file="/WEB-INF/jsp/shared/footer.jspf" %>
