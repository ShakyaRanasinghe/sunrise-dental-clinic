package com.sunrise.clinic.pattern.billing;

/**
 * Immutable value object holding the three-way split of a bill's revenue.
 * These figures are visible to the Administrator only (each staff member
 * additionally sees their own earning).
 *
 * @param dentistEarning      amount attributed to the treating dentist (Rs)
 * @param clinicEarning       amount retained by the clinic (Rs)
 * @param receptionistEarning amount attributed to the handling receptionist (Rs)
 */
public record RevenueSplit(
        double dentistEarning,
        double clinicEarning,
        double receptionistEarning) {
}
