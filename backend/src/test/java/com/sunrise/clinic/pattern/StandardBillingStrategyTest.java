package com.sunrise.clinic.pattern;

import com.sunrise.clinic.pattern.billing.BillBreakdown;
import com.sunrise.clinic.pattern.billing.StandardBillingStrategy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * TC-CLI-A05 — Strategy: standard bill calculation.
 */
class StandardBillingStrategyTest {

    private final StandardBillingStrategy strategy = new StandardBillingStrategy();

    @Test
    void totalIsConsultationPlusTreatmentPlusServiceCharge() {
        BillBreakdown b = strategy.calculate(1500, 5000, 200);
        assertEquals(1500, b.consultationFee(), 0.001);
        assertEquals(5000, b.treatmentCost(), 0.001);
        assertEquals(200, b.serviceCharge(), 0.001);
        assertEquals(6700, b.total(), 0.001);
    }
}
