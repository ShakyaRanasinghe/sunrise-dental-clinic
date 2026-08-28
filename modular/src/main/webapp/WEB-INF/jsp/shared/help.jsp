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
    <h2>Appointment statuses</h2>
    <p class="page-subtitle">What each status means at every stage of a patient's visit.</p>
    <div class="table-wrap">
        <table>
            <thead>
                <tr><th>Status</th><th>What it means</th><th>Who sets it</th><th>What happens next</th></tr>
            </thead>
            <tbody>
                <tr>
                    <td><span class="pill open">Waiting</span></td>
                    <td>Appointment is booked and confirmed. Patient is expected or currently with the dentist.</td>
                    <td>Set automatically on booking</td>
                    <td>Dentist records the diagnosis and marks treatment complete</td>
                </tr>
                <tr>
                    <td><span class="pill" style="background:var(--sunrise);color:#fff;border-color:var(--sunrise);">Ready to bill</span></td>
                    <td>Dentist has finished the treatment and recorded a diagnosis. Patient is waiting to pay.</td>
                    <td>Dentist marks treatment complete</td>
                    <td>Reception issues the bill — use the <strong>Bill</strong> button on the day view</td>
                </tr>
                <tr>
                    <td><span class="pill">Paid &amp; done</span></td>
                    <td>Bill has been issued and the visit is fully complete.</td>
                    <td>Reception issues bill</td>
                    <td>Nothing — this is the end of the journey</td>
                </tr>
                <tr>
                    <td><span class="pill error">Cancelled</span></td>
                    <td>Appointment was called off. The time slot is released for another patient.</td>
                    <td>Patient or reception cancels</td>
                    <td>Nothing — slot is free again</td>
                </tr>
            </tbody>
        </table>
    </div>
</div>

<div class="card">
    <h2>The patient journey</h2>
    <div class="table-wrap">
        <table>
            <thead><tr><th>Step</th><th>Who acts</th><th>What happens</th></tr></thead>
            <tbody>
                <tr><td>1. Book</td><td>Patient (online) or Reception (walk-in)</td><td>Appointment created — status <strong>Waiting</strong></td></tr>
                <tr><td>2. Arrive</td><td>Patient arrives at clinic</td><td>Reception sees them on the day view</td></tr>
                <tr><td>3. Treat</td><td>Dentist</td><td>Dentist records diagnosis and marks complete — status becomes <strong>Ready to bill</strong></td></tr>
                <tr><td>4. Pay</td><td>Reception</td><td>Reception issues bill from day view — status becomes <strong>Paid &amp; done</strong></td></tr>
            </tbody>
        </table>
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
