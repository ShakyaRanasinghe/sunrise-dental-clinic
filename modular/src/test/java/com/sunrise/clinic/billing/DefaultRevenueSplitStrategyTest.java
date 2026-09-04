package com.sunrise.clinic.billing;

import com.sunrise.clinic.billing.domain.BillBreakdown;
import com.sunrise.clinic.billing.domain.RevenueSplit;
import com.sunrise.clinic.billing.service.DefaultRevenueSplitStrategy;
import com.sunrise.clinic.billing.service.StandardBillingStrategy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Moved from {@code layered/} in step 5, rewritten for {@code BigDecimal}, and revised when
 * the split gained its second dial.
 *
 * <p>The invariant these tests exist for: <b>the three shares must sum to the bill
 * exactly</b>, whatever the dials are set to. A split that misses by a cent leaves a ledger
 * permanently short with nobody able to say where, and it is the failure that arithmetic
 * this shape produces most easily.</p>
 */
class DefaultRevenueSplitStrategyTest {

    /** The clinic's defaults: 60% of treatment to the dentist, no handling commission. */
    private final DefaultRevenueSplitStrategy defaults =
            new DefaultRevenueSplitStrategy(new BigDecimal("0.60"), BigDecimal.ZERO);

    // --- the default policy -------------------------------------------

    @Test
    void theDentistTakesTheConsultationAndTheirShareOfTheTreatment() {
        RevenueSplit split = defaults.split(breakdown("1500.00", "3500.00", "200.00"));

        assertEquals(new BigDecimal("3600.00"), split.dentistEarning(), "1500 + 60% of 3500");
    }

    @Test
    void theOwnerTakesTheWholeDifference() {
        // The point of the default policy: what the patient pays less what the dentist is
        // paid is the owner's margin - the clinic's 40% of the treatment plus the whole
        // service charge. 5200 - 3600 = 1600.
        RevenueSplit split = defaults.split(breakdown("1500.00", "3500.00", "200.00"));

        assertEquals(new BigDecimal("1600.00"), split.clinicEarning());
    }

    @Test
    void theReceptionistTakesNothingByDefault() {
        // Reception is salaried. A commission on every bill is unusual for a practice and
        // puts an incentive on the person deciding what to charge.
        assertEquals(new BigDecimal("0.00"),
                defaults.split(breakdown("1500.00", "3500.00", "200.00")).receptionistEarning());
    }

    @Test
    void theConsultationFeeIsNeverShared() {
        // dentist.consultation_fee is the dentist's own fee, so no dial touches it. With a
        // treatment of zero the dentist takes exactly the consultation and no more.
        RevenueSplit split = defaults.split(breakdown("1500.00", "0.00", "200.00"));

        assertEquals(new BigDecimal("1500.00"), split.dentistEarning());
        assertEquals(new BigDecimal("200.00"), split.clinicEarning());
    }

    // --- the invariant -------------------------------------------------

    @Test
    void theThreeSharesSumToTheRevenue() {
        BillBreakdown breakdown = breakdown("1500.00", "3500.00", "200.00");

        assertEquals(breakdown.attributableRevenue(), defaults.split(breakdown).sum());
    }

    @Test
    void theySumWhenNeitherShareDividesCleanly() {
        // 333.33 x 0.60 is 199.998 and 175.55 x 0.35 is 61.4425 - both need rounding, and
        // each rounded share is subtracted from its own charge so the clinic absorbs the
        // remainder rather than the bill losing it.
        DefaultRevenueSplitStrategy awkward =
                new DefaultRevenueSplitStrategy(new BigDecimal("0.60"), new BigDecimal("0.35"));

        for (String treatment : new String[] {"333.33", "0.01", "999.99", "1.05", "7.77", "12345.67"}) {
            for (String charge : new String[] {"175.55", "0.01", "200.00", "3.33"}) {
                BillBreakdown breakdown = breakdown("1500.00", treatment, charge);

                assertEquals(breakdown.attributableRevenue(), awkward.split(breakdown).sum(),
                        "treatment " + treatment + ", service charge " + charge);
            }
        }
    }

    @Test
    void theySumAtEveryCombinationOfDials() {
        String[] shares = {"0", "0.01", "0.333", "0.5", "0.6", "0.7", "1"};
        for (String dentistShare : shares) {
            for (String receptionistShare : shares) {
                BillBreakdown breakdown = breakdown("1500.00", "3333.33", "199.99");
                RevenueSplit split = new DefaultRevenueSplitStrategy(
                        new BigDecimal(dentistShare), new BigDecimal(receptionistShare))
                        .split(breakdown);

                assertEquals(breakdown.attributableRevenue(), split.sum(),
                        "dentist " + dentistShare + ", receptionist " + receptionistShare);
            }
        }
    }

    // --- the second dial, when a practice does pay commission ----------

    @Test
    void aFullReceptionistShareGivesThemTheWholeServiceCharge() {
        RevenueSplit split = new DefaultRevenueSplitStrategy(new BigDecimal("0.60"), BigDecimal.ONE)
                .split(breakdown("1500.00", "3500.00", "200.00"));

        assertEquals(new BigDecimal("200.00"), split.receptionistEarning());
        // The clinic keeps only its share of the treatment now.
        assertEquals(new BigDecimal("1400.00"), split.clinicEarning());
        assertEquals(new BigDecimal("3600.00"), split.dentistEarning());
    }

    @Test
    void aPartialReceptionistShareSplitsTheServiceCharge() {
        RevenueSplit split = new DefaultRevenueSplitStrategy(
                new BigDecimal("0.60"), new BigDecimal("0.25"))
                .split(breakdown("1500.00", "3500.00", "200.00"));

        assertEquals(new BigDecimal("50.00"), split.receptionistEarning());
        assertEquals(new BigDecimal("1550.00"), split.clinicEarning(), "1400 + the other 150");
    }

    // --- misconfiguration ----------------------------------------------

    @Test
    void aCorruptLiveShareFallsBackInsteadOfMispricing() {
        // GAP-ADM-10: the dials are read live from the settings table, so a corrupt
        // row must fall back to the shipped defaults rather than misprice a bill —
        // and validation moved to the Pricing tab, which refuses bad values outright.
        DefaultRevenueSplitStrategy corrupt = new DefaultRevenueSplitStrategy(
                () -> new BigDecimal("1.5"), () -> null);
        RevenueSplit split = corrupt.split(breakdown("1500.00", "3500.00", "200.00"));

        assertEquals(new BigDecimal("3600.00"), split.dentistEarning());
        assertEquals(new BigDecimal("0.00"), split.receptionistEarning());
    }

    @Test
    void everyFigureIsHeldAtTwoDecimalPlaces() {
        RevenueSplit split = defaults.split(breakdown("1500.00", "333.33", "200.00"));

        assertEquals(2, split.dentistEarning().scale());
        assertEquals(2, split.clinicEarning().scale());
        assertEquals(2, split.receptionistEarning().scale());
    }

    private static BillBreakdown breakdown(String fee, String treatment, String charge) {
        return new StandardBillingStrategy().calculate(
                new BigDecimal(fee), new BigDecimal(treatment), new BigDecimal(charge));
    }
}
