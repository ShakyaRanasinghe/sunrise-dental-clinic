package com.sunrise.clinic.platform.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** GAP-ADM-10: the Pricing tab's validation — percent in, fraction stored. */
class PricingValidationTest {

    @Test
    void percentBecomesFraction() {
        // Compared numerically: "60" stores as 0.6, which prices identically to 0.60.
        assertEquals(0, new java.math.BigDecimal(ClinicIdentityService.validatedShare("60"))
                .compareTo(new java.math.BigDecimal("0.60")));
        assertEquals("1", ClinicIdentityService.validatedShare("100"));
        assertEquals("0", ClinicIdentityService.validatedShare("0"));
    }

    @Test
    void fractionPassesThrough() {
        assertEquals(0, new java.math.BigDecimal(ClinicIdentityService.validatedShare("0.60"))
                .compareTo(new java.math.BigDecimal("0.6")));
    }

    @Test
    void outOfRangeShareIsRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> ClinicIdentityService.validatedShare("101"));
        assertThrows(IllegalArgumentException.class,
                () -> ClinicIdentityService.validatedShare("-5"));
        assertThrows(IllegalArgumentException.class,
                () -> ClinicIdentityService.validatedShare("sixty"));
    }

    @Test
    void negativeChargeIsRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> ClinicIdentityService.validatedCharge("-1"));
        assertThrows(IllegalArgumentException.class,
                () -> ClinicIdentityService.validatedCharge("two hundred"));
    }

    @Test
    void chargePassesThrough() {
        assertEquals("200", ClinicIdentityService.validatedCharge("200"));
        assertEquals("250.50", ClinicIdentityService.validatedCharge("250.50"));
    }
}
