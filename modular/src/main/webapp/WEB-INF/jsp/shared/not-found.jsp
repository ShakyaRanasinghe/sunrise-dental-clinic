<%@ include file="/WEB-INF/jsp/shared/taglibs.jspf" %>
<c:set var="pageTitle" value="Not found" />
<%@ include file="/WEB-INF/jsp/shared/header.jspf" %>
<div class="narrow">
    <div class="card">
        <h2>Not found</h2>
        <p class="page-subtitle">That address does not exist, or is not yours to see.</p>
        <div class="form-actions"><a class="btn" href="${ctx}/">Start again</a></div>
    </div>
</div>
<%@ include file="/WEB-INF/jsp/shared/footer.jspf" %>
