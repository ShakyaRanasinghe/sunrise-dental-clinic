package com.sunrise.clinic.billing.domain;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A bill as a patient or the front desk sees it.
 *
 * <p><b>The revenue split is not here</b>, and not because a mapper strips it - there are
 * no fields for it. Those three figures are the administrator's, surfaced through the
 * reports, so a patient's receipt cannot carry them by accident.</p>
 *
 * <p>Carries the names the receipt has to print. The previous record carried the
 * appointment number and the amounts alone, so a receipt could not say who was treated or
 * by whom without a second lookup per line.</p>
 */
public record BillResponse(String id,
                           String appointmentNo,
                           String patientName,
                           String dentistName,
                           String treatmentName,
                           BigDecimal consultationFee,
                           BigDecimal treatmentCost,
                           BigDecimal serviceCharge,
                           BigDecimal discount,
                           BigDecimal tax,
                           BigDecimal total,
                           Instant issuedAt) {

    public static BillResponse of(Bill bill, String patientName, String dentistName,
                                  String treatmentName) {
        return new BillResponse(bill.getId(), bill.getAppointmentNo(),
                patientName, dentistName, treatmentName,
                bill.getConsultationFee(), bill.getTreatmentCost(), bill.getServiceCharge(),
                bill.getDiscount(), bill.getTax(), bill.getTotal(),
                bill.getIssuedAt());
    }
}
