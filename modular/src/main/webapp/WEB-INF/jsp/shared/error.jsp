<%@ include file="/WEB-INF/jsp/shared/taglibs.jspf" %>
<c:set var="pageTitle" value="Something went wrong" />
<%@ include file="/WEB-INF/jsp/shared/header.jspf" %>
<div class="narrow">
    <div class="card">
        <h2>Something went wrong</h2>
        <p class="page-subtitle">
            The clinic has been told. Nothing you were doing has been saved, so it is
            safe to try again.
        </p>
        <%-- Deliberately no stack trace and no SQL: NFR-USE-02. --%>
        <div class="form-actions"><a class="btn" href="${ctx}/">Start again</a></div>
    </div>
</div>
<%@ include file="/WEB-INF/jsp/shared/footer.jspf" %>
