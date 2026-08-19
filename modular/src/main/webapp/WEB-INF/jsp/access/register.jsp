<%@ include file="/WEB-INF/jsp/shared/taglibs.jspf" %>
<c:set var="pageTitle" value="Create an account" />
<%@ include file="/WEB-INF/jsp/shared/header.jspf" %>

<div class="narrow">
    <div class="brand-block">
        <div class="name">Sunrise Dental Clinic</div>
        <div class="tagline">Create a patient account</div>
    </div>

    <div class="card">
        <div class="notice info">
            Only patients create their own account. Reception, dentist and
            administrator accounts are issued by the clinic administrator.
        </div>

        <c:if test="${not empty error}">
            <div class="notice error"><c:out value="${error}" /></div>
        </c:if>

        <form method="post" action="${ctx}/register">
            <div class="field">
                <label for="name">Full name</label>
                <input type="text" id="name" name="name" value="<c:out value='${param.name}' />" autofocus>
            </div>
            <div class="field">
                <label for="email">Email</label>
                <input type="email" id="email" name="email" value="<c:out value='${param.email}' />">
            </div>
            <div class="field">
                <label for="password">Password</label>
                <input type="password" id="password" name="password">
                <div class="page-subtitle">At least 8 characters.</div>
            </div>
            <div class="field">
                <label for="confirmPassword">Confirm password</label>
                <input type="password" id="confirmPassword" name="confirmPassword">
            </div>
            <div class="notice">
                Nothing medical is asked here. Allergies and medications are added from
                your profile once you have signed in.
            </div>
            <div class="form-actions">
                <button type="submit" class="btn" style="width:100%">Create account</button>
            </div>
        </form>
    </div>

    <div class="alt">Already registered? <a href="${ctx}/login/patient">Sign in</a></div>
</div>
<%@ include file="/WEB-INF/jsp/shared/footer.jspf" %>
