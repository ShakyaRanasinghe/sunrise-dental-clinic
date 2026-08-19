package com.sunrise.clinic.pattern;

import com.sunrise.clinic.pattern.billing.BillBreakdown;
import com.sunrise.clinic.pattern.billing.DefaultRevenueSplitStrategy;
import com.sunrise.clinic.pattern.billing.RevenueSplit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * TC-CLI-A06 — Strategy: three-way revenue split.
 * consultation 1500 -> dentist; treatment 5000 split 60/40; service 200 -> receptionist.
 */
class DefaultRevenueSplitStrategyTest {

    private final DefaultRevenueSplitStrategy strategy = new DefaultRevenueSplitStrategy(0.60);

    @Test
    void splitsBillThreeWays() {
        BillBreakdown b = new BillBreakdown(1500, 5000, 200, 0, 0, 6700);
        RevenueSplit split = strategy.split(b);

        // dentist = consultation 1500 + 60% of 5000 (3000) = 4500
        assertEquals(4500, split.dentistEarning(), 0.001);
        // clinic = 40% of 5000 = 2000
        assertEquals(2000, split.clinicEarning(), 0.001);
        // receptionist = service charge 200
        assertEquals(200, split.receptionistEarning(), 0.001);
    }

    @Test
    void earningsSumToBillableComponents() {
        BillBreakdown b = new BillBreakdown(1500, 5000, 200, 0, 0, 6700);
        RevenueSplit s = strategy.split(b);
        assertEquals(6700, s.dentistEarning() + s.clinicEarning() + s.receptionistEarning(), 0.001);
    }
}
