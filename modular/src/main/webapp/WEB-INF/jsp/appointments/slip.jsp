<%--
    Printable appointment confirmation slip — for walk-in patients to take away.
    No navigation chrome in print view.
--%>
<%@ include file="/WEB-INF/jsp/shared/taglibs.jspf" %>
<c:set var="pageTitle" value="Appointment confirmation" />
<%@ include file="/WEB-INF/jsp/shared/header.jspf" %>

<style>
    @media print {
        .site-header, .form-actions, .no-print { display: none !important; }
        .slip-card { border: 2px solid #333; }
    }
</style>

<div class="card slip-card">
    <h1 class="page-title" style="font-size:1.4rem;">Appointment Confirmation</h1>
    <p class="page-subtitle">Please keep this slip and bring it with you on the day.</p>

    <div class="table-wrap" style="margin-top:1rem;">
        <table>
            <tbody>
                <tr>
                    <th>Appointment number</th>
                    <td><code style="font-size:1.1rem;">${appointment.appointmentNo()}</code></td>
                </tr>
                <tr>
                    <th>Patient</th>
                    <td><c:out value="${appointment.patientName()}" /></td>
                </tr>
                <tr>
                    <th>Dentist</th>
                    <td><c:out value="${appointment.dentistName()}" /></td>
                </tr>
                <tr>
                    <th>Treatment</th>
                    <td><c:out value="${appointment.treatmentName()}" /></td>
                </tr>
                <tr>
                    <th>Date</th>
                    <td><strong>${appointment.date()}</strong></td>
                </tr>
                <tr>
                    <th>Time</th>
                    <td><strong>${appointment.time()}</strong></td>
                </tr>
                <c:if test="${not empty clinicAddress}">
                    <tr>
                        <th>Clinic address</th>
                        <td><c:out value="${clinicAddress}" /></td>
                    </tr>
                </c:if>
                <c:if test="${not empty clinicPhone}">
                    <tr>
                        <th>Contact number</th>
                        <td><c:out value="${clinicPhone}" /></td>
                    </tr>
                </c:if>
            </tbody>
        </table>
    </div>

    <div class="notice" style="margin-top:1rem;">
        Please arrive a few minutes before your appointment time. If you need to cancel
        or reschedule, please call the clinic as early as possible.
    </div>

    <div class="form-actions no-print" style="margin-top:1rem;">
        <button onclick="window.print()" class="btn">Print this slip</button>
        <a href="${ctx}/reception/home" class="btn secondary">Back to day view</a>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/shared/footer.jspf" %>
