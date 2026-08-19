package com.sunrise.clinic.billing;

import com.sunrise.clinic.billing.domain.BillBreakdown;
import com.sunrise.clinic.billing.domain.RevenueSplit;
import com.sunrise.clinic.billing.service.DefaultRevenueSplitStrategy;
import com.sunrise.clinic.billing.service.StandardBillingStrategy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Moved from {@code layered/} in step 5, and rewritten for {@code BigDecimal}. */
class DefaultRevenueSplitStrategyTest {

    private final DefaultRevenueSplitStrategy sixtyForty =
            new DefaultRevenueSplitStrategy(new BigDecimal("0.60"));

    @Test
    void theDentistTakesTheConsultationAndTheirShareOfTheTreatment() {
        RevenueSplit split = sixtyForty.split(breakdown("1500.00", "3500.00", "200.00"));

        // 1500 + 60% of 3500
        assertEquals(new BigDecimal("3600.00"), split.dentistEarning());
        // the remaining 40%
        assertEquals(new BigDecimal("1400.00"), split.clinicEarning());
        // the handling charge
        assertEquals(new BigDecimal("200.00"), split.receptionistEarning());
    }

    @Test
    void theThreeSharesSumToTheRevenue() {
        BillBreakdown breakdown = breakdown("1500.00", "3500.00", "200.00");

        assertEquals(breakdown.attributableRevenue(), sixtyForty.split(breakdown).sum());
    }

    @Test
    void theySumEvenWhenTheShareDoesNotDivideCleanly() {
        // The rounding leak this class is shaped to avoid. 333.33 x 0.60 is 199.998, and
        // rounding both shares independently would give 200.00 + 133.33 = 333.33 by luck
        // and 200.00 + 133.34 elsewhere. The clinic's share is the exact remainder, so the
        // pair sums by construction rather than by arithmetic coincidence.
        for (String treatment : new String[] {"333.33", "0.01", "999.99", "1.05", "7.77", "12345.67"}) {
            BillBreakdown breakdown = breakdown("1500.00", treatment, "200.00");
            RevenueSplit split = sixtyForty.split(breakdown);

            assertEquals(breakdown.attributableRevenue(), split.sum(),
                    "the shares must sum to the revenue for a treatment of " + treatment);
        }
    }

    @Test
    void anyShareStillSums() {
        for (String share : new String[] {"0", "0.01", "0.333", "0.5", "0.7", "1"}) {
            BillBreakdown breakdown = breakdown("1500.00", "3333.33", "200.00");
            RevenueSplit split = new DefaultRevenueSplitStrategy(new BigDecimal(share))
                    .split(breakdown);

            assertEquals(breakdown.attributableRevenue(), split.sum(),
                    "the shares must sum to the revenue at a share of " + share);
        }
    }

    @Test
    void aShareOfZeroGivesTheClinicTheWholeTreatment() {
        RevenueSplit split = new DefaultRevenueSplitStrategy(BigDecimal.ZERO)
                .split(breakdown("1500.00", "3500.00", "200.00"));

        assertEquals(new BigDecimal("1500.00"), split.dentistEarning());
        assertEquals(new BigDecimal("3500.00"), split.clinicEarning());
    }

    @Test
    void aShareOfOneLeavesTheClinicNothingFromTheTreatment() {
        RevenueSplit split = new DefaultRevenueSplitStrategy(BigDecimal.ONE)
                .split(breakdown("1500.00", "3500.00", "200.00"));

        assertEquals(new BigDecimal("5000.00"), split.dentistEarning());
        assertEquals(new BigDecimal("0.00"), split.clinicEarning());
    }

    @Test
    void aShareOutsideZeroToOneIsRefusedAtConstruction() {
        // A misconfigured share would otherwise attribute more than the bill, silently,
        // on every bill until somebody reconciled the ledger.
        assertThrows(IllegalArgumentException.class,
                () -> new DefaultRevenueSplitStrategy(new BigDecimal("1.5")));
        assertThrows(IllegalArgumentException.class,
                () -> new DefaultRevenueSplitStrategy(new BigDecimal("-0.1")));
        assertThrows(IllegalArgumentException.class, () -> new DefaultRevenueSplitStrategy(null));
    }

    @Test
    void everyFigureIsHeldAtTwoDecimalPlaces() {
        RevenueSplit split = sixtyForty.split(breakdown("1500.00", "333.33", "200.00"));

        assertEquals(2, split.dentistEarning().scale());
        assertEquals(2, split.clinicEarning().scale());
        assertEquals(2, split.receptionistEarning().scale());
    }

    private static BillBreakdown breakdown(String fee, String treatment, String charge) {
        return new StandardBillingStrategy().calculate(
                new BigDecimal(fee), new BigDecimal(treatment), new BigDecimal(charge));
    }
}
