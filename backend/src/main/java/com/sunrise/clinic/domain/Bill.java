package com.sunrise.clinic.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * A patient bill/receipt. Carries both the customer-facing total and the
 * internal 3-way revenue split (dentist / clinic / receptionist) computed by
 * {@code RevenueSplitStrategy}. The split fields are visible to Admin only
 * (each staff member sees their own earning).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Bill {
    private String id;
    private String appointmentNo;
    private String patientId;
    private String dentistId;
    private String receptionistUid;   // who handled it (earns the service charge)

    // --- line items ---
    private double consultationFee;
    private double treatmentCost;
    private double serviceCharge;
    @Builder.Default
    private double discount = 0.0;
    @Builder.Default
    private double tax = 0.0;
    private double total;

    // --- revenue split (Admin-visible) ---
    private double dentistEarning;
    private double clinicEarning;
    private double receptionistEarning;

    private Instant issuedAt;
    private String issuedByUid;
}
