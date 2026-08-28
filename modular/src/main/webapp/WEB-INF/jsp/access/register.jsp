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
                <div class="pw-wrap">
                    <input type="password" id="password" name="password"
                           class="pw-field" autocomplete="new-password">
                    <button type="button" class="pw-toggle" data-password-toggle data-for="password"
                            aria-controls="password" aria-pressed="false"
                            aria-label="Show password" title="Show password">
                        <svg class="eye-on" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"
                             fill="none" stroke="currentColor" stroke-width="2"
                             stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                            <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                            <circle cx="12" cy="12" r="3"/>
                        </svg>
                        <svg class="eye-off" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"
                             fill="none" stroke="currentColor" stroke-width="2"
                             stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                            <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94"/>
                            <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19"/>
                            <line x1="1" y1="1" x2="23" y2="23"/>
                        </svg>
                    </button>
                </div>
                <div class="page-subtitle">At least 8 characters.</div>
            </div>
            <div class="field">
                <label for="confirmPassword">Confirm password</label>
                <div class="pw-wrap">
                    <input type="password" id="confirmPassword" name="confirmPassword"
                           class="pw-field" autocomplete="new-password">
                    <button type="button" class="pw-toggle" data-password-toggle
                            data-for="confirmPassword"
                            aria-controls="confirmPassword" aria-pressed="false"
                            aria-label="Show password" title="Show password">
                        <svg class="eye-on" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"
                             fill="none" stroke="currentColor" stroke-width="2"
                             stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                            <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                            <circle cx="12" cy="12" r="3"/>
                        </svg>
                        <svg class="eye-off" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"
                             fill="none" stroke="currentColor" stroke-width="2"
                             stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                            <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94"/>
                            <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19"/>
                            <line x1="1" y1="1" x2="23" y2="23"/>
                        </svg>
                    </button>
                </div>
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

<%--
    Eye-icon toggle inside each password field. The app is otherwise
    server-rendered with no script of its own; this is the one place a
    form gains an in-page behaviour, kept tiny and safe: it only flips
    input[type] and swaps which eye icon is shown, it never touches the
    values, and a browser with no JavaScript simply keeps the fields as
    password type, which is the secure default.
--%>
<script>
(function () {
    var toggles = document.querySelectorAll('[data-password-toggle]');
    toggles.forEach(function (toggle) {
        toggle.addEventListener('click', function () {
            var field = document.getElementById(toggle.getAttribute('data-for'));
            if (!field) {
                return;
            }
            var showing = field.type === 'text';
            field.type = showing ? 'password' : 'text';
            toggle.classList.toggle('is-visible', !showing);
            toggle.setAttribute('aria-pressed', showing ? 'false' : 'true');
            toggle.setAttribute('aria-label', showing ? 'Show password' : 'Hide password');
            toggle.setAttribute('title', showing ? 'Show password' : 'Hide password');
            field.focus();
        });
    });
})();
</script>
<%@ include file="/WEB-INF/jsp/shared/footer.jspf" %>
