<%@ include file="/WEB-INF/jsp/shared/taglibs.jspf" %>
<c:set var="pageTitle" value="Administrator help" />
<%@ include file="/WEB-INF/jsp/shared/header.jspf" %>

<%--
    GAP-FTB-12: the administrator's own help page — reports, accounts,
    treatments, clinic identity, complaints and audit, grounded in
    srs-admin.md. Reached from the admin sign-in screen and the admin nav;
    never advertised on a public page (FR-ADM-03).
--%>

<h1 class="page-title">Administrator help</h1>
<p class="page-subtitle">How to run the clinic in the system, step by step.</p>

<div class="card help-toc">
    <h2>Help topics</h2>
    <div class="help-toc__links">
        <a class="help-toc__item" href="#reports">Reports and accounts</a>
        <a class="help-toc__item" href="#treatments">Treatments</a>
        <a class="help-toc__item" href="#clinic">Clinic identity</a>
        <a class="help-toc__item" href="#complaints">Complaints</a>
        <a class="help-toc__item" href="#audit">Audit trail</a>
        <a class="help-toc__item" href="#trouble">Something went wrong</a>
    </div>
</div>

<div class="card" id="reports">
    <h2>Reports and accounts</h2>
    <p>Open <a href="${ctx}/admin/reports">Reports</a> for the clinic's figures and <a href="${ctx}/admin/accounts">Accounts</a> to manage sign-in accounts — including unlocking an account locked by five wrong passwords, which only an administrator can clear. After every save a confirmation window tells you the change is stored.</p>
</div>

<div class="card" id="treatments">
    <h2>Treatments</h2>
    <p>Open <a href="${ctx}/admin/treatments">Treatments</a> to maintain the catalogue patients book from. The <strong>description</strong> you write here is what a patient reads behind the info mark on the booking screen, so keep it plain. Prices set here feed every estimate and bill.</p>
</div>

<div class="card" id="clinic">
    <h2>Clinic identity</h2>
    <p>Open <a href="${ctx}/admin/clinic">Clinic</a> to edit the name, phone, email and address shown on the landing page, the help pages, receipts and slips. The phone number must be a valid Sri Lankan number. This screen is the only place clinic identity is edited.</p>
</div>

<div class="card" id="complaints">
    <h2>Complaints</h2>
    <p>Open <a href="${ctx}/admin/complaints">Complaints</a> to review what patients raise. The queue splits into <strong>to act on</strong> first and reviewed complaints folded away. You are the only reader — the named dentist never sees a concern, and raising one never affects the patient's appointments.</p>
</div>

<div class="card" id="audit">
    <h2>Audit trail</h2>
    <p>Open <a href="${ctx}/admin/audit">Audit</a> to see who changed what across the system. You cannot read any patient's medical notes or diagnosis here — those belong to the patient and the treating dentist, not to administration.</p>
</div>

<div class="card" id="trouble">
    <h2>Something went wrong?</h2>
    <div class="help-faq">
        <details class="help-faq__item">
            <summary>A staff member is locked out</summary>
            <p>Five wrong passwords lock any account and it does not unlock itself. Clear it from the accounts screen — only an administrator can.</p>
        </details>
        <details class="help-faq__item">
            <summary>Signed out unexpectedly</summary>
            <p>Sessions end after 30 minutes of inactivity. Sign in again — reports, settings and the queues are all still there.</p>
        </details>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/shared/footer.jspf" %>
