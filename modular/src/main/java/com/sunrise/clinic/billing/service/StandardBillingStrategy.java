package com.sunrise.clinic.billing.service;

import com.sunrise.clinic.billing.domain.BillBreakdown;

import java.math.BigDecimal;

/**
 * The clinic's standard pricing: consultation + treatment + service charge, less any
 * discount, plus any tax.
 *
 * <p>Discount and tax are zero here rather than absent, because the columns exist and a
 * promotional or insurance strategy will fill them. Producing the fields with zeroes keeps
 * the receipt layout and the database shape the same whichever strategy priced the
 * bill.</p>
 *
 * <p>Matches {@code fn_calculate_bill} in the schema, deliberately - the function is the
 * database-side statement of the same rule, and if the two disagreed one of them would be
 * wrong.</p>
 */
public class StandardBillingStrategy implements BillingStrategy {

    @Override
    public BillBreakdown calculate(BigDecimal consultationFee, BigDecimal treatmentCost,
                                   BigDecimal serviceCharge) {
        BigDecimal fee = BillBreakdown.money(consultationFee);
        BigDecimal treatment = BillBreakdown.money(treatmentCost);
        BigDecimal charge = BillBreakdown.money(serviceCharge);
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;

        BigDecimal total = fee.add(treatment).add(charge).subtract(discount).add(tax);
        return new BillBreakdown(fee, treatment, charge, discount, tax, total);
    }
}
