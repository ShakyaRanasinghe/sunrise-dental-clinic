package com.sunrise.clinic.dto;

/**
 * A bill/receipt as shown to a patient or front-desk staff. The internal 3-way revenue
 * split (dentist/clinic/receptionist earnings) is intentionally omitted here — those
 * figures are Admin-only and surfaced through the reports API.
 */
public record BillResponse(
        String id,
        String appointmentNo,
        double consultationFee,
        double treatmentCost,
        double serviceCharge,
        double discount,
        double tax,
        double total) {
}
