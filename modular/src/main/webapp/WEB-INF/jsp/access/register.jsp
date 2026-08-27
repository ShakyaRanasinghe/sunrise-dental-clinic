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

            <%--
                Optional, all three, and labelled as such.

                The contact number used to be required by the servlet and absent from this
                form, so registration refused everybody with "Contact number is required" —
                a field nobody could fill in. It is offered now rather than demanded: the
                column is nullable, reception can add it at the desk, and asking for a
                telephone number at sign-up turns away somebody who would otherwise have
                become a patient.
            --%>
            <div class="field">
                <label for="contactNumber">
                    Contact number <span class="page-subtitle">(optional)</span>
                </label>
                <input type="tel" id="contactNumber" name="contactNumber"
                       value="<c:out value='${param.contactNumber}' />"
                       pattern="[0-9+() -]+" maxlength="20"
                       title="Digits, with + ( ) and - allowed — no letters.">
                <div class="page-subtitle">So the clinic can reach you about an appointment.</div>
            </div>
            <div class="form-row">
                <div class="field">
                    <label for="dob">Date of birth <span class="page-subtitle">(optional)</span></label>
                    <input type="date" id="dob" name="dob" value="<c:out value='${param.dob}' />">
                </div>
                <div class="field">
                    <label for="address">Address <span class="page-subtitle">(optional)</span></label>
                    <input type="text" id="address" name="address"
                           value="<c:out value='${param.address}' />">
                </div>
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
