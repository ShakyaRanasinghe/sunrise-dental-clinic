<%--
    The patient's profile.

    Shows the contact details the clinic holds for the patient, and lets them
    edit everything except the email address, which is the identity they sign
    in with.
--%>
<%@ include file="/WEB-INF/jsp/shared/taglibs.jspf" %>
<c:set var="pageTitle" value="My details" />
<%@ include file="/WEB-INF/jsp/shared/header.jspf" %>

<h1 class="page-title">My details</h1>
<p class="page-subtitle">Your contact details.</p>

<c:if test="${not empty saved}">
    <div class="notice">Saved.</div>
</c:if>

<c:if test="${not empty profile}">
    <div class="card">
        <h2>Contact details</h2>
        <c:choose>
            <c:when test="${not empty editing}">
                <form method="post" action="${ctx}/patient/profile">
                    <input type="hidden" name="action" value="update">
                    <div class="form-row">
                        <div class="field">
                            <label for="name">Name</label>
                            <input type="text" id="name" name="name" required
                                   value="<c:out value='${profile.name()}' />">
                        </div>
                        <div class="field">
                            <label for="contactNumber">Contact number</label>
                            <input type="tel" id="contactNumber" name="contactNumber" required
                                   pattern="[0-9+() -]{10,20}"
                                   title="A Sri Lankan number: 10 digits starting with 0, or +94 international."
                                   value="<c:out value='${profile.contactNumber()}' />">
                        </div>
                    </div>
                    <div class="field">
                        <label for="address">Address</label>
                        <input type="text" id="address" name="address"
                               value="<c:out value='${profile.address()}' />">
                    </div>
                    <div class="form-row">
                        <div class="field">
                            <label for="dob">Date of birth</label>
                            <input type="date" id="dob" name="dob"
                                   value="${profile.dob()}" placeholder="yyyy-mm-dd">
                        </div>
                        <div class="field">
                            <label for="email">Email</label>
                            <input type="text" id="email" value="<c:out value='${profile.email()}' />" disabled>
                            <p class="hint">Your email is your sign-in identity and cannot be changed.</p>
                        </div>
                    </div>
                    <div class="form-actions">
                        <button type="submit" class="btn">Save changes</button>
                        <a class="btn secondary" href="${ctx}/patient/profile">Cancel</a>
                    </div>
                </form>
            </c:when>
            <c:otherwise>
                <div class="table-wrap">
                    <table>
                        <tbody>
                            <tr><th>Name</th><td><c:out value="${profile.name()}" /></td></tr>
                            <tr><th>Contact number</th><td><c:out value="${profile.contactNumber()}" /></td></tr>
                            <tr><th>Address</th><td><c:out value="${profile.address()}" /></td></tr>
                            <tr><th>Email</th><td><c:out value="${profile.email()}" /></td></tr>
                            <tr><th>Date of birth</th><td>${profile.dob()}</td></tr>
                        </tbody>
                    </table>
                </div>
                <p class="form-actions"><a class="btn" href="${ctx}/patient/profile?edit=1">Edit details</a></p>
            </c:otherwise>
        </c:choose>
    </div>
</c:if>

<%@ include file="/WEB-INF/jsp/shared/footer.jspf" %>
