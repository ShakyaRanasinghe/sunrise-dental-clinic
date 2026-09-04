<%--
    GAP-ADM-10: administrator screen for pricing — the dentist's share of each
    treatment price, and the flat service charge per bill. Stored in
    clinic_setting and read live on every bill, so a save applies to the next
    bill with no restart. Bills already issued keep the split they went out with.
--%>
<%@ include file="/WEB-INF/jsp/shared/taglibs.jspf" %>
<c:set var="pageTitle" value="Pricing" />
<%@ include file="/WEB-INF/jsp/shared/header.jspf" %>

<h1 class="page-title">Pricing</h1>
<p class="page-subtitle">What dentists earn from each bill, and the flat charge on every bill.</p>

<c:if test="${not empty saved}">
    <div class="sub-window open" role="dialog" aria-modal="true" aria-labelledby="pricing-saved-title">
        <div class="dialog">
            <span class="success-tick" aria-hidden="true">&#10003;</span>
            <h3 id="pricing-saved-title">Successfully updated</h3>
            <p class="page-subtitle">The pricing applies to the next bill. Bills already issued are unchanged.</p>
            <div class="form-actions">
                <a class="btn" href="${ctx}/admin/pricing">Close</a>
            </div>
        </div>
    </div>
</c:if>

<div class="card">
    <form method="post" action="${ctx}/admin/pricing">
        <div class="form-row">
            <div class="field">
                <label for="dentistSharePercent">Dentist share of treatment price (%)</label>
                <input type="text" id="dentistSharePercent" name="dentistSharePercent"
                       value="<c:out value='${dentistSharePercent}' />" required
                       pattern="[0-9]+(\.[0-9]+)?"
                       title="A percent between 0 and 100.">
                <p class="hint">Each dentist receives this share of every treatment price —
                    catalog and dentist-entered alike — plus their full consultation fee.
                    The clinic keeps the rest.</p>
            </div>
            <div class="field">
                <label for="serviceCharge">Service charge per bill (Rs)</label>
                <input type="text" id="serviceCharge" name="serviceCharge"
                       value="<c:out value='${serviceCharge}' />" required
                       pattern="[0-9]+(\.[0-9]{1,2})?"
                       title="An amount in rupees, like 200.">
                <p class="hint">Flat charge on every bill. It belongs to the clinic in full.</p>
            </div>
        </div>
        <div class="form-actions">
            <button type="submit" class="btn">Save pricing</button>
        </div>
    </form>
</div>

<%@ include file="/WEB-INF/jsp/shared/footer.jspf" %>
