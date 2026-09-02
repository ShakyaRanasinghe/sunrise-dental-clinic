<%@ include file="/WEB-INF/jsp/shared/taglibs.jspf" %>
<c:set var="pageTitle" value="Help" />
<%@ include file="/WEB-INF/jsp/shared/header.jspf" %>

<h1 class="page-title">How to use the system</h1>
<p class="page-subtitle">Step by step, for anyone new to the clinic. No sign-in needed.</p>

<div class="card help-toc">
    <h2>Jump to a topic</h2>
    <div class="help-toc__links">
        <a class="help-toc__item" href="#signing-in">Signing in</a>
        <a class="help-toc__item" href="#statuses">Appointment statuses</a>
        <a class="help-toc__item" href="#journey">The patient journey</a>
        <a class="help-toc__item" href="#trouble">Something went wrong</a>
        <a class="help-toc__item" href="#contact">Talk to the clinic</a>
    </div>
</div>

<div class="card" id="signing-in">
    <h2>Signing in</h2>
    <p class="page-subtitle">Wherever you belong, the form is the same two fields.</p>
    <ol class="help-steps">
        <li class="help-steps__item">
            <span class="help-steps__no" aria-hidden="true">1</span>
            <span class="help-steps__text">Open <a href="${ctx}/login/patient">the patient sign-in page</a>.
                Staff use the portal address the clinic issued them (reception, dentist or
                administrator), and a brand-new patient starts with
                <a href="${ctx}/register">Create an account</a> instead.</span>
        </li>
        <li class="help-steps__item">
            <span class="help-steps__no" aria-hidden="true">2</span>
            <span class="help-steps__text">Enter the email and password the clinic issued you.</span>
        </li>
        <li class="help-steps__item">
            <span class="help-steps__no" aria-hidden="true">3</span>
            <span class="help-steps__text">You land on the screen for your role.</span>
        </li>
    </ol>
    <div class="notice warning">
        Five wrong passwords lock the account. Only an administrator can unlock it.
    </div>
</div>

<div class="card" id="statuses">
    <h2>Appointment statuses</h2>
    <p class="page-subtitle">Every appointment moves through these states as the visit happens.</p>
    <div class="help-statuses">
        <div class="status-card">
            <div class="status-card__head">
                <span class="pill open">Waiting</span>
                <span class="status-card__whom">Set automatically on booking</span>
            </div>
            <p class="status-card__mean">Appointment is booked and confirmed. The patient is
                expected, or already with the dentist.</p>
            <p class="status-card__next"><span>Next</span>The dentist records the diagnosis and
                marks treatment complete.</p>
        </div>
        <div class="status-card">
            <div class="status-card__head">
                <span class="pill ready">Ready to bill</span>
                <span class="status-card__whom">Set by the dentist</span>
            </div>
            <p class="status-card__mean">Treatment is finished and the diagnosis is on record.
                The patient is waiting to pay.</p>
            <p class="status-card__next"><span>Next</span>Reception issues the bill from the day view.</p>
        </div>
        <div class="status-card">
            <div class="status-card__head">
                <span class="pill billed">Paid &amp; done</span>
                <span class="status-card__whom">Set by reception</span>
            </div>
            <p class="status-card__mean">The bill has been issued and the visit is fully complete.</p>
            <p class="status-card__next"><span>Next</span>Nothing &mdash; this is the end of the journey.</p>
        </div>
        <div class="status-card">
            <div class="status-card__head">
                <span class="pill cancelled">Cancelled</span>
                <span class="status-card__whom">Patient or reception</span>
            </div>
            <p class="status-card__mean">The appointment was called off. The time slot is released
                for another patient.</p>
            <p class="status-card__next"><span>Next</span>Nothing &mdash; the slot is free again.</p>
        </div>
    </div>
</div>

<div class="card" id="journey">
    <h2>The patient journey</h2>
    <p class="page-subtitle">From booking to payment, in four steps.</p>
    <div class="help-timeline">
        <div class="help-timeline__step">
            <div class="help-timeline__dot" aria-hidden="true">1</div>
            <div class="help-timeline__card">
                <h3>Book <span class="help-timeline__who">you online, or reception walking you in</span></h3>
                <p>Appointment created &mdash; status <span class="pill open">Waiting</span>.</p>
            </div>
        </div>
        <div class="help-timeline__step">
            <div class="help-timeline__dot" aria-hidden="true">2</div>
            <div class="help-timeline__card">
                <h3>Arrive <span class="help-timeline__who">you</span></h3>
                <p>You reach the clinic. Reception sees you on the day view.</p>
            </div>
        </div>
        <div class="help-timeline__step">
            <div class="help-timeline__dot" aria-hidden="true">3</div>
            <div class="help-timeline__card">
                <h3>Treat <span class="help-timeline__who">the dentist</span></h3>
                <p>The dentist records the diagnosis and marks the treatment complete &mdash;
                    status becomes <span class="pill ready">Ready to bill</span>.</p>
            </div>
        </div>
        <div class="help-timeline__step">
            <div class="help-timeline__dot" aria-hidden="true">4</div>
            <div class="help-timeline__card">
                <h3>Pay <span class="help-timeline__who">reception</span></h3>
                <p>Reception issues the bill from the day view &mdash; status becomes
                    <span class="pill billed">Paid &amp; done</span>.</p>
            </div>
        </div>
    </div>
</div>

<div class="card" id="trouble">
    <h2>Something went wrong?</h2>
    <p class="page-subtitle">Tap the message you saw to read what to do about it.</p>
    <div class="help-faq">
        <details class="help-faq__item">
            <summary>&ldquo;Incorrect email or password.&rdquo;</summary>
            <p>Check both. If you are sure they are right, you may be on the wrong sign-in page for
                your role &mdash; or the account may be locked (below).</p>
        </details>
        <details class="help-faq__item">
            <summary>&ldquo;This account is locked.&rdquo;</summary>
            <p>Five wrong passwords lock the account and it does not unlock itself. Ask the
                clinic administrator to clear it.</p>
        </details>
        <details class="help-faq__item">
            <summary>Signed out unexpectedly</summary>
            <p>Sessions end after 30 minutes of inactivity. Sign in again &mdash; your appointments
                and record are all still there.</p>
        </details>
    </div>
</div>

<div class="card" id="contact">
    <h2>Still stuck? Talk to the clinic</h2>
    <p class="page-subtitle">Prefer a person? Call or write to us &mdash; a question needs no appointment.</p>
    <div class="help-contact">
        <a class="help-contact__item" href="tel:${clinicPhone}">
            <span class="help-contact__label">Call us</span>
            <span class="help-contact__value"><c:out value="${clinicPhone}" /></span>
        </a>
        <a class="help-contact__item" href="mailto:${clinicEmail}">
            <span class="help-contact__label">Email us</span>
            <span class="help-contact__value"><c:out value="${clinicEmail}" /></span>
        </a>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/shared/footer.jspf" %>