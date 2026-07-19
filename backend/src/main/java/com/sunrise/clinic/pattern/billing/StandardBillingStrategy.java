package com.sunrise.clinic.pattern.billing;

import org.springframework.stereotype.Component;

/**
 * Default {@link BillingStrategy}: total = consultation + treatment + service charge
 * (minus discount, plus tax). Registered as the primary Spring bean; another
 * strategy (e.g. {@code PromotionalBillingStrategy}) could be swapped in via config.
 */
@Component
public class StandardBillingStrategy implements BillingStrategy {

    @Override
    public BillBreakdown calculate(double consultationFee, double treatmentCost, double serviceCharge) {
        double discount = 0.0;
        double tax = 0.0;
        double total = consultationFee + treatmentCost + serviceCharge - discount + tax;
        return new BillBreakdown(consultationFee, treatmentCost, serviceCharge, discount, tax, total);
    }
}
