<%--
    Reception screen for editing the clinic phone number. Only the phone
    is editable here; name, email and address require the administrator
    (GAP-ADM-06, MANAGE_PHONE vs MANAGE_CLINIC_SETTINGS).
--%>
<%@ include file="/WEB-INF/jsp/shared/taglibs.jspf" %>
<c:set var="pageTitle" value="Clinic phone" />
<%@ include file="/WEB-INF/jsp/shared/header.jspf" %>

<h1 class="page-title">Clinic phone number</h1>
<p class="page-subtitle">The number shown on the landing page, help page and appointment slips.</p>

<c:if test="${param.saved == '1'}">
    <div class="notice">Phone number updated.</div>
</c:if>

<div class="card">
    <form method="post" action="${ctx}/reception/phone" class="form-row">
        <div class="field">
            <label for="clinic.phone">Phone number</label>
            <input type="text" id="clinic.phone" name="clinic.phone"
                   value="<c:out value="${clinicPhone}" />" required>
        </div>
        <div class="form-actions">
            <button type="submit" class="btn">Save</button>
        </div>
    </form>
</div>

<%@ include file="/WEB-INF/jsp/shared/footer.jspf" %>
