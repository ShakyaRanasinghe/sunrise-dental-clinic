package com.sunrise.clinic.billing.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The computed line items of a bill. Produced by a
 * {@link com.sunrise.clinic.billing.service.BillingStrategy}, consumed by a
 * {@link com.sunrise.clinic.billing.service.RevenueSplitStrategy}.
 *
 * <p>{@code BigDecimal} throughout, at scale 2. Every one of these was a {@code double}:
 * a total assembled from several binary fractions drifts from the figure the database
 * computes for the same inputs, and {@code fn_calculate_bill} works in
 * {@code DECIMAL(10,2)}. On a receipt a patient signs, "5200.000000000001" is not a
 * rounding curiosity - it is a wrong number on a financial document.</p>
 *
 * @param consultationFee the dentist's fee
 * @param treatmentCost   the treatment's published price
 * @param serviceCharge   the front-desk handling charge
 * @param discount        any discount applied
 * @param tax             any tax applied
 * @param total           what the patient pays
 */
public record BillBreakdown(BigDecimal consultationFee,
                            BigDecimal treatmentCost,
                            BigDecimal serviceCharge,
                            BigDecimal discount,
                            BigDecimal tax,
                            BigDecimal total) {

    /** Rupees are quoted to the cent; every figure here is stored and shown at scale 2. */
    public static final int SCALE = 2;

    /** Rounds every component to scale 2, so the record is always in its stored form. */
    public BillBreakdown {
        consultationFee = money(consultationFee);
        treatmentCost = money(treatmentCost);
        serviceCharge = money(serviceCharge);
        discount = money(discount);
        tax = money(tax);
        total = money(total);
    }

    /**
     * @return the revenue to be attributed: consultation + treatment + service charge
     *
     * <p>The gross, not {@link #total}. With the standard strategy the two are equal,
     * because discount and tax are zero. They are separated because the invariant a
     * revenue split must satisfy is that its three shares sum to <em>this</em> - a
     * strategy that introduces a discount has to decide whose share it comes out of, and
     * summing to a discounted total would hide that decision rather than force it.</p>
     */
    public BigDecimal attributableRevenue() {
        return consultationFee.add(treatmentCost).add(serviceCharge);
    }

    /** Half-up at scale 2: the rounding a person doing this on paper would use. */
    public static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(SCALE, RoundingMode.HALF_UP);
    }
}
