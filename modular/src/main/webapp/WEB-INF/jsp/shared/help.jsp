<%@ include file="/WEB-INF/jsp/shared/taglibs.jspf" %>
<c:set var="pageTitle" value="Help" />
<%@ include file="/WEB-INF/jsp/shared/header.jspf" %>

<h1 class="page-title">How to use the system</h1>
<p class="page-subtitle">Step by step, for anyone new to the clinic. No sign-in needed.</p>

<div class="card">
    <h2>Signing in</h2>
    <ol>
        <li>Open <a href="${ctx}/login">the sign-in page</a> and choose how you use the clinic.</li>
        <li>Enter the email and password the clinic issued you.</li>
        <li>You land on the screen for your role.</li>
    </ol>
    <div class="notice warning">
        Five wrong passwords lock the account. Only an administrator can unlock it.
    </div>
</div>

<div class="card">
    <h2>Something went wrong</h2>
    <div class="table-wrap">
        <table>
            <thead><tr><th>What you see</th><th>What to do</th></tr></thead>
            <tbody>
                <tr><td>"Incorrect email or password."</td>
                    <td>Check both. If you are sure they are right, you may be using the wrong
                        sign-in page for your role.</td></tr>
                <tr><td>"This account is locked."</td>
                    <td>Five wrong passwords. Ask the administrator to unlock it.</td></tr>
                <tr><td>Signed out unexpectedly</td>
                    <td>Sessions end after 30 minutes of inactivity. Sign in again.</td></tr>
            </tbody>
        </table>
    </div>
    <p class="page-subtitle">
        Sunrise Dental Clinic &mdash; <c:out value="${clinicPhone}" />
        &mdash; <c:out value="${clinicEmail}" />
    </p>
</div>
<%@ include file="/WEB-INF/jsp/shared/footer.jspf" %>
