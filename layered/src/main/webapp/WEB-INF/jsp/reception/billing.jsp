<%--
    Billing.

    The receipt shows the patient-facing figures only. The three-way revenue
    split lives on the same Bill object but is deliberately not rendered here —
    those numbers belong to the Administrator's reports.
--%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Billing" />
<c:set var="nav" value="billing" />
<%@ include file="/WEB-INF/jsp/layout/header.jspf" %>

<h1 class="page-title">Billing</h1>
<p class="page-subtitle">Look up an appointment to issue or reprint its receipt.</p>

<c:if test="${not empty param.issued}">
    <div class="notice success">Bill issued.</div>
</c:if>

<div class="card no-print">
    <form method="get" action="${ctx}/reception/billing">
        <div class="form-row" style="max-width:520px">
            <div class="field" style="margin:0">
                <label for="appointmentNo">Appointment reference</label>
                <input type="text" id="appointmentNo" name="appointmentNo"
                       value="<c:out value='${appointmentNo}' />"
                       placeholder="APT-20260720-0001" required>
            </div>
            <div class="field" style="margin:0; align-self:end">
                <button type="submit" class="btn">Look up</button>
            </div>
        </div>
    </form>
</div>

<c:if test="${not empty appointment}">
    <div class="grid two">
        <div class="card">
            <h2>Appointment</h2>
            <table>
                <tbody>
                <tr><th>Reference</th><td class="mono">${appointment.appointmentNo}</td></tr>
                <tr><th>Patient</th><td><c:out value="${patient.name}" /></td></tr>
                <tr><th>Dentist</th><td><c:out value="${dentist.name}" /></td></tr>
                <tr><th>Treatment</th><td><c:out value="${treatment.name}" /></td></tr>
                <tr><th>Date</th><td>${appointment.date} at ${appointment.time}</td></tr>
                <tr>
                    <th>Status</th>
                    <td><span class="pill ${fn:toLowerCase(appointment.status)}">${appointment.status}</span></td>
                </tr>
                </tbody>
            </table>

            <c:if test="${empty bill}">
                <div class="form-actions">
                    <form method="post" action="${ctx}/reception/billing">
                        <input type="hidden" name="appointmentNo" value="${appointment.appointmentNo}">
                        <button type="submit" class="btn">Generate bill</button>
                    </form>
                </div>
                <p class="hint" style="margin-top:10px">
                    The total is calculated from the dentist's consultation fee, the
                    treatment's price and the clinic's service charge.
                </p>
            </c:if>
        </div>

        <c:if test="${not empty bill}">
            <div class="card">
                <h2>Receipt</h2>
                <div class="receipt">
                    <div class="line">
                        <span>Consultation</span>
                        <span class="amount">Rs <fmt:formatNumber value="${bill.consultationFee}" pattern="#,##0.00" /></span>
                    </div>
                    <div class="line">
                        <span><c:out value="${treatment.name}" /></span>
                        <span class="amount">Rs <fmt:formatNumber value="${bill.treatmentCost}" pattern="#,##0.00" /></span>
                    </div>
                    <div class="line">
                        <span>Service charge</span>
                        <span class="amount">Rs <fmt:formatNumber value="${bill.serviceCharge}" pattern="#,##0.00" /></span>
                    </div>
                    <c:if test="${bill.discount > 0}">
                        <div class="line">
                            <span>Discount</span>
                            <span class="amount">− Rs <fmt:formatNumber value="${bill.discount}" pattern="#,##0.00" /></span>
                        </div>
                    </c:if>
                    <c:if test="${bill.tax > 0}">
                        <div class="line">
                            <span>Tax</span>
                            <span class="amount">Rs <fmt:formatNumber value="${bill.tax}" pattern="#,##0.00" /></span>
                        </div>
                    </c:if>
                    <div class="line total">
                        <span>Total</span>
                        <span class="amount">Rs <fmt:formatNumber value="${bill.total}" pattern="#,##0.00" /></span>
                    </div>
                </div>

                <div class="form-actions no-print">
                    <button type="button" class="btn secondary" onclick="window.print()">
                        Print receipt
                    </button>
                </div>
            </div>
        </c:if>
    </div>
</c:if>

<%@ include file="/WEB-INF/jsp/layout/footer.jspf" %>
