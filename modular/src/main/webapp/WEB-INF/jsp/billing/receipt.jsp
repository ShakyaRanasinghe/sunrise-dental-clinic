<%--
    The printable receipt - FR-REC-52.

    A page with a print stylesheet, not a generated PDF. The clinic gets paper from any
    browser and the patient gets the same document saved; a PDF library would be a
    dependency added to produce what the browser already produces.

    The revenue split is absent because BillResponse has no fields for it. A patient's
    receipt cannot carry the clinic's commission figures by accident.
--%>
<%@ include file="/WEB-INF/jsp/shared/taglibs.jspf" %>
<c:set var="pageTitle" value="Receipt" />
<%@ include file="/WEB-INF/jsp/shared/header.jspf" %>

<div class="narrow">
    <%--
        The one line of client-side script in the application, and it is worth being
        explicit about it: nothing else here uses JavaScript, and this page works without
        it - Ctrl+P prints the same receipt. The button is a convenience that degrades to
        nothing if script is unavailable, which is the only reason it is acceptable.
    --%>
    <div class="no-print form-actions">
        <button type="button" class="btn" onclick="window.print()">Print</button>
        <a class="btn btn-secondary" href="${ctx}${user.policy().homePath()}">Done</a>
    </div>

    <div class="card receipt">
        <div class="receipt-head">
            <h2><c:out value="${clinicName}" /></h2>
            <p class="page-subtitle">
                <c:out value="${clinicPhone}" />
            </p>
        </div>

        <div class="table-wrap">
            <table>
                <tbody>
                    <tr><th>Receipt</th><td><code>${bill.id()}</code></td></tr>
                    <tr><th>Appointment</th><td><code>${bill.appointmentNo()}</code></td></tr>
                    <tr><th>Patient</th><td><c:out value="${bill.patientName()}" /></td></tr>
                    <tr><th>Dentist</th><td><c:out value="${bill.dentistName()}" /></td></tr>
                    <tr><th>Treatment</th><td><c:out value="${bill.treatmentName()}" /></td></tr>
                    <%--
                        GAP-PAT-30: what the dentist recorded, printed only on the
                        patient's own copy — never where reception or admin reads it.
                    --%>
                    <c:if test="${showClinical and not empty bill.diagnosis()}">
                        <tr><th>Dentist's note</th><td><c:out value="${bill.diagnosis()}" /></td></tr>
                    </c:if>
                    <tr><th>Issued</th><td>${bill.issuedAt()}</td></tr>
                </tbody>
            </table>
        </div>

        <h3>Charges</h3>
        <div class="table-wrap">
            <table>
                <tbody>
                    <tr>
                        <th>Consultation</th>
                        <td class="right">Rs <fmt:formatNumber value="${bill.consultationFee()}"
                                minFractionDigits="2" maxFractionDigits="2" /></td>
                    </tr>
                    <tr>
                        <th>Treatment</th>
                        <td class="right">Rs <fmt:formatNumber value="${bill.treatmentCost()}"
                                minFractionDigits="2" maxFractionDigits="2" /></td>
                    </tr>
                    <tr>
                        <th>Service charge</th>
                        <td class="right">Rs <fmt:formatNumber value="${bill.serviceCharge()}"
                                minFractionDigits="2" maxFractionDigits="2" /></td>
                    </tr>
                    <%-- Shown only when they apply. A line of "Rs 0.00" on a receipt invites
                         the question of what it was for. --%>
                    <c:if test="${bill.discount() gt 0}">
                        <tr>
                            <th>Discount</th>
                            <td class="right">&minus; Rs <fmt:formatNumber value="${bill.discount()}"
                                    minFractionDigits="2" maxFractionDigits="2" /></td>
                        </tr>
                    </c:if>
                    <c:if test="${bill.tax() gt 0}">
                        <tr>
                            <th>Tax</th>
                            <td class="right">Rs <fmt:formatNumber value="${bill.tax()}"
                                    minFractionDigits="2" maxFractionDigits="2" /></td>
                        </tr>
                    </c:if>
                    <tr class="total">
                        <th>Total paid</th>
                        <td class="right"><strong>Rs <fmt:formatNumber value="${bill.total()}"
                                minFractionDigits="2" maxFractionDigits="2" /></strong></td>
                    </tr>
                </tbody>
            </table>
        </div>

        <p class="page-subtitle receipt-foot">
            Thank you. Please quote the appointment number if you contact the clinic about
            this visit.
        </p>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/shared/footer.jspf" %>
