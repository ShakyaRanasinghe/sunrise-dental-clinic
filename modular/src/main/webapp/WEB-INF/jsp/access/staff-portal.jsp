<%--
    The central staff portal. Lists the three staff roles — administrator, reception,
    dentist — and sends each to its own sign-in screen. Deliberately does not list the
    patient door: that lives on the public home page. Blue theme, matching the
    navigation and role tags (GAP-FTB-09). No navigation link points at this page; it
    is reached by its own address (/staff).
--%>
<%@ include file="/WEB-INF/jsp/shared/taglibs.jspf" %>
<c:set var="pageTitle" value="Staff sign in" />
<%@ include file="/WEB-INF/jsp/shared/header.jspf" %>

<div class="narrow">
    <div class="brand-block">
        <div class="name">Sunrise Dental Clinic</div>
        <div class="tagline">Staff sign-in by role</div>
    </div>

    <p class="page-subtitle" style="margin-top:4px; text-align:center">
        Choose your role to open your sign-in screen. Patients sign in from the
        <a href="${ctx}/login/patient">public home page</a> instead.
    </p>

    <div class="portal-grid">
        <a class="staff-card" href="${ctx}/login/admin">
            <span class="staff-card__badge" aria-hidden="true">A</span>
            <span class="staff-card__body">
                <strong class="staff-card__name">Administrator</strong>
                <span class="staff-card__desc">Reports, accounts, audit trail and clinic-wide settings.</span>
            </span>
            <span class="staff-card__cta">Sign in &rarr;</span>
        </a>

        <a class="staff-card" href="${ctx}/login/reception">
            <span class="staff-card__badge" aria-hidden="true">R</span>
            <span class="staff-card__body">
                <strong class="staff-card__name">Reception</strong>
                <span class="staff-card__desc">Front desk: day view, patients, booking and billing.</span>
            </span>
            <span class="staff-card__cta">Sign in &rarr;</span>
        </a>

        <a class="staff-card" href="${ctx}/login/dentist">
            <span class="staff-card__badge" aria-hidden="true">D</span>
            <span class="staff-card__body">
                <strong class="staff-card__name">Dentist</strong>
                <span class="staff-card__desc">Your schedule, availability and treatment notes.</span>
            </span>
            <span class="staff-card__cta">Sign in &rarr;</span>
        </a>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/shared/footer.jspf" %>
