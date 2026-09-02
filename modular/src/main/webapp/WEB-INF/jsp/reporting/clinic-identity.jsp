<%--
    GAP-ADM-06: administrator screen for clinic identity — name, phone,
    email and address. Previously these lived only in clinic.properties
    and could not be changed at runtime.
--%>
<%@ include file="/WEB-INF/jsp/shared/taglibs.jspf" %>
<c:set var="pageTitle" value="Clinic identity" />
<%@ include file="/WEB-INF/jsp/shared/header.jspf" %>

<h1 class="page-title">Clinic identity</h1>
<p class="page-subtitle">Name, phone, email and address shown on the landing page, help page, receipts and appointment slips.</p>

<c:if test="${param.saved == '1'}">
    <div class="notice">Saved.</div>
</c:if>

<div class="card">
    <form method="post" action="${ctx}/admin/clinic" class="form-row">
        <div class="field">
            <label for="clinic.name">Clinic name</label>
            <input type="text" id="clinic.name" name="clinic.name"
                   value="<c:out value="${values['clinic.name']}" />" required>
        </div>
        <div class="field">
            <label for="clinic.phone">Phone number</label>
            <input type="text" id="clinic.phone" name="clinic.phone"
                   value="<c:out value="${values['clinic.phone']}" />" required
                   pattern="[0-9+() -]{10,20}"
                   title="A Sri Lankan number: 10 digits starting with 0, or +94 international.">
        </div>
        <div class="field">
            <label for="clinic.email">Email address</label>
            <input type="email" id="clinic.email" name="clinic.email"
                   value="<c:out value="${values['clinic.email']}" />" required>
        </div>
        <div class="field">
            <label for="clinic.address">Address</label>
            <input type="text" id="clinic.address" name="clinic.address"
                   value="<c:out value="${values['clinic.address']}" />" required>
        </div>
        <div class="form-actions">
            <button type="submit" class="btn">Save</button>
        </div>
    </form>
</div>

<%@ include file="/WEB-INF/jsp/shared/footer.jspf" %>
