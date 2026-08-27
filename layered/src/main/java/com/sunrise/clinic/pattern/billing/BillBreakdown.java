package com.sunrise.clinic.pattern.billing;

/**
 * Immutable value object holding the computed line items of a bill.
 * Produced by a {@link BillingStrategy}; consumed by a {@link RevenueSplitStrategy}.
 *
 * @param consultationFee the dentist's consultation fee (Rs)
 * @param treatmentCost   the treatment's base cost (Rs)
 * @param serviceCharge   the front-desk handling charge (Rs)
 * @param discount        any discount applied (Rs)
 * @param tax             any tax applied (Rs)
 * @param total           the amount payable by the patient (Rs)
 */
public record BillBreakdown(
        double consultationFee,
        double treatmentCost,
        double serviceCharge,
        double discount,
        double tax,
        double total) {
}
