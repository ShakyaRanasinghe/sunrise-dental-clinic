package com.sunrise.clinic.billing.service;

import com.sunrise.clinic.billing.domain.BillBreakdown;

import java.math.BigDecimal;

/**
 * Strategy - how fees become a payable total.
 *
 * <p>Behind an interface so a different pricing policy (promotional, insurance) can be
 * used without touching {@link BillingService}. There is one implementation today;
 * the interface earns its place because pricing is the part of a clinic system most
 * likely to change, and the alternative is an {@code if} chain inside the service that
 * grows a branch per policy.</p>
 */
public interface BillingStrategy {

    /**
     * @param consultationFee the dentist's own fee
     * @param treatmentCost   the treatment's published price
     * @param serviceCharge   the front-desk handling charge
     * @return the line items and the total
     */
    BillBreakdown calculate(BigDecimal consultationFee, BigDecimal treatmentCost,
                            BigDecimal serviceCharge);
}
