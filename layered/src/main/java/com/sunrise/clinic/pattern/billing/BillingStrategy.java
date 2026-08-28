package com.sunrise.clinic.pattern.billing;

/**
 * STRATEGY pattern — the algorithm for turning fees into a payable total.
 *
 * <p>Different pricing policies (standard, promotional, insurance) can be
 * swapped without touching the {@code BillingService} that uses them.</p>
 */
public interface BillingStrategy {

    /**
     * Compute the bill line items and total.
     *
     * @param consultationFee dentist consultation fee (Rs)
     * @param treatmentCost   treatment base cost (Rs)
     * @param serviceCharge   front-desk handling charge (Rs)
     * @return the computed {@link BillBreakdown}
     */
    BillBreakdown calculate(double consultationFee, double treatmentCost, double serviceCharge);
}
