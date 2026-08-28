<%--
    Two callers, one page.

    PageServlet.fail forwards here with errorHeading and errorMessage set - "Not
    permitted", "That is no longer possible", "Please check the form". The container
    forwards here for an unhandled 500, with neither.

    It used to ignore both attributes and always print "Something went wrong … it is
    safe to try again". For a 403 or a 409 that is wrong twice over: it hides what
    happened, and trying again will never work. So the specific message wins when there
    is one, and the generic text is the fallback rather than the only option.
--%>
<%@ include file="/WEB-INF/jsp/shared/taglibs.jspf" %>
<c:set var="pageTitle"
       value="${empty errorHeading ? 'Something went wrong' : errorHeading}" />
<%@ include file="/WEB-INF/jsp/shared/header.jspf" %>

<div class="narrow">
    <div class="card">
        <h2><c:out value="${empty errorHeading ? 'Something went wrong' : errorHeading}" /></h2>

        <c:choose>
            <c:when test="${not empty errorMessage}">
                <p class="page-subtitle"><c:out value="${errorMessage}" /></p>
            </c:when>
            <c:otherwise>
                <p class="page-subtitle">
                    The clinic has been told. Nothing you were doing has been saved, so it is
                    safe to try again.
                </p>
            </c:otherwise>
        </c:choose>

        <%-- Deliberately no stack trace and no SQL: NFR-USE-02. The messages that reach
             here are ones the services wrote for a person to read. --%>
        <div class="form-actions">
            <c:choose>
                <c:when test="${not empty user}">
                    <a class="btn" href="${ctx}${user.policy().homePath()}">Back to your pages</a>
                </c:when>
                <c:otherwise>
                    <a class="btn" href="${ctx}/">Start again</a>
                </c:otherwise>
            </c:choose>
            <a class="btn btn-secondary" href="${ctx}/help">Help</a>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/shared/footer.jspf" %>
