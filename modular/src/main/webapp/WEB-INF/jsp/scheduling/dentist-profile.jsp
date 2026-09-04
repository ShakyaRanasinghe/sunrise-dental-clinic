<%--
    The dentist's own profile (GAP-DEN-14).

    Read-only by default with an Edit control revealing the editable subset
    (GAP-FTB-14); a saved change confirms in a sub-window. The consultation fee
    is shown but disabled — it prices every bill, so only the administrator
    sets it. `editing` is a Boolean, so the gate tests its value (cf. the old
    profile bug where `not empty` on a Boolean rendered the form every visit).
--%>
<%@ include file="/WEB-INF/jsp/shared/taglibs.jspf" %>
<c:set var="pageTitle" value="My details" />
<%@ include file="/WEB-INF/jsp/shared/header.jspf" %>

<h1 class="page-title">My details</h1>
<p class="page-subtitle">How patients see you on the public page.</p>

<c:if test="${not empty saved}">
    <div class="sub-window open" role="dialog" aria-modal="true" aria-labelledby="dentist-saved-title">
        <div class="dialog">
            <span class="success-tick" aria-hidden="true">&#10003;</span>
            <h3 id="dentist-saved-title">Successfully updated</h3>
            <p class="page-subtitle">Your details were saved.</p>
            <div class="form-actions">
                <a class="btn" href="${ctx}/dentist/profile">Close</a>
            </div>
        </div>
    </div>
</c:if>

<c:if test="${not empty profile}">
    <div class="card">
        <h2>Profile details</h2>
        <c:choose>
            <c:when test="${editing}">
                <form method="post" action="${ctx}/dentist/profile">
                    <input type="hidden" name="action" value="update">
                    <div class="form-row">
                        <div class="field">
                            <label for="name">Name</label>
                            <input type="text" id="name" name="name" required
                                   value="<c:out value='${profile.name()}' />">
                        </div>
                        <div class="field">
                            <label for="specialization">Specialisation</label>
                            <input type="text" id="specialization" name="specialization"
                                   value="<c:out value='${profile.specialization()}' />">
                        </div>
                    </div>
                    <div class="form-row">
                        <div class="field">
                            <label for="phone">Phone number</label>
                            <input type="tel" id="phone" name="phone"
                                   pattern="[0-9+() -]{10,20}"
                                   title="A Sri Lankan number: 10 digits starting with 0, or +94 international."
                                   value="<c:out value='${profile.phone()}' />">
                            <p class="hint">Shown to patients on your public card. Blank hides it.</p>
                        </div>
                        <div class="field">
                            <label for="fee">Consultation fee (Rs)</label>
                            <input type="text" id="fee" value="${profile.consultationFee()}" disabled>
                            <p class="hint">Set by the administrator; it prices every bill.</p>
                        </div>
                    </div>
                    <div class="form-actions">
                        <button type="submit" class="btn">Save changes</button>
                        <a class="btn secondary" href="${ctx}/dentist/profile">Cancel</a>
                    </div>
                </form>
            </c:when>
            <c:otherwise>
                <div class="table-wrap">
                    <table>
                        <tbody>
                            <tr><th>Name</th><td><c:out value="${profile.name()}" /></td></tr>
                            <tr><th>Specialisation</th><td><c:out value="${profile.specialization()}" /></td></tr>
                            <tr><th>Phone number</th><td><c:out value="${profile.phone()}" /></td></tr>
                            <tr><th>Consultation fee</th><td>Rs <fmt:formatNumber value="${profile.consultationFee()}"
                                    minFractionDigits="2" maxFractionDigits="2" /></td></tr>
                        </tbody>
                    </table>
                </div>
                <p class="form-actions"><a class="btn" href="${ctx}/dentist/profile?edit=1">Edit details</a></p>
            </c:otherwise>
        </c:choose>
    </div>
</c:if>

<%@ include file="/WEB-INF/jsp/shared/footer.jspf" %>
