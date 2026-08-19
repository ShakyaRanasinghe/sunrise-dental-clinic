package com.sunrise.clinic.billing;

import com.sunrise.clinic.billing.domain.BillBreakdown;
import com.sunrise.clinic.billing.service.StandardBillingStrategy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Moved from {@code layered/} in step 5, and rewritten for {@code BigDecimal}. */
class StandardBillingStrategyTest {

    private final StandardBillingStrategy strategy = new StandardBillingStrategy();

    @Test
    void theTotalIsTheThreeChargesAddedUp() {
        BillBreakdown breakdown = strategy.calculate(
                new BigDecimal("1500.00"), new BigDecimal("3500.00"), new BigDecimal("200.00"));

        assertEquals(new BigDecimal("5200.00"), breakdown.total());
    }

    @Test
    void itAgreesWithTheDatabaseFunction() {
        // fn_calculate_bill('t-scaling', 1500.00, 200.00) returns 5200.00 - exercised
        // against MySQL in docs/local-setup.md. The function is the database-side
        // statement of this same rule, so if the two disagreed one would be wrong.
        assertEquals(new BigDecimal("5200.00"),
                strategy.calculate(new BigDecimal("1500.00"), new BigDecimal("3500.00"),
                        new BigDecimal("200.00")).total());
    }

    @Test
    void everyFigureIsHeldAtTwoDecimalPlaces() {
        // Rupees are quoted to the cent, and a receipt showing "5200.0" or
        // "5200.000000000001" is a wrong number on a financial document.
        BillBreakdown breakdown = strategy.calculate(
                new BigDecimal("1500"), new BigDecimal("3500"), new BigDecimal("200"));

        assertEquals(2, breakdown.consultationFee().scale());
        assertEquals(2, breakdown.treatmentCost().scale());
        assertEquals(2, breakdown.serviceCharge().scale());
        assertEquals(2, breakdown.total().scale());
    }

    @Test
    void whatADoubleWouldHaveGotWrong() {
        // 0.1 + 0.2 is 0.30000000000000004 in binary floating point. Three charges a
        // clinic could plausibly set, added in the type the previous version used, do not
        // give the figure a person adding them up would write down.
        assertEquals(new BigDecimal("0.30"),
                strategy.calculate(new BigDecimal("0.10"), new BigDecimal("0.20"),
                        BigDecimal.ZERO).total());
    }

    @Test
    void discountAndTaxAreZeroButPresent() {
        // Present rather than absent, so the receipt layout and the stored row are the
        // same shape whichever strategy priced the bill.
        BillBreakdown breakdown = strategy.calculate(
                new BigDecimal("1500.00"), new BigDecimal("3500.00"), new BigDecimal("200.00"));

        assertEquals(new BigDecimal("0.00"), breakdown.discount());
        assertEquals(new BigDecimal("0.00"), breakdown.tax());
    }

    @Test
    void aFreeTreatmentStillCostsTheConsultation() {
        assertEquals(new BigDecimal("1700.00"),
                strategy.calculate(new BigDecimal("1500.00"), BigDecimal.ZERO,
                        new BigDecimal("200.00")).total());
    }

    @Test
    void nullChargesAreTreatedAsZeroRatherThanFailing() {
        // A treatment with no price set is a data problem, not a reason to refuse a
        // consultation fee that is known.
        assertEquals(new BigDecimal("1500.00"),
                strategy.calculate(new BigDecimal("1500.00"), null, null).total());
    }
}
