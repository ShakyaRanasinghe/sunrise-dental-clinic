package com.sunrise.clinic.reporting.domain;

import java.math.BigDecimal;

/**
 * The period's money, in one row.
 *
 * <p>{@code gross} is what patients paid. The other three are the revenue policy's
 * attribution of it, and they sum to {@code gross} - the same invariant every individual
 * bill satisfies, so a report that failed it would mean a bill had been written by an older
 * policy or the query had double-counted.</p>
 */
public record IncomeTotals(int bills,
                           BigDecimal gross,
                           BigDecimal dentistEarnings,
                           BigDecimal clinicEarnings,
                           BigDecimal receptionistEarnings) {

    public static final IncomeTotals NONE = new IncomeTotals(0, BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

    public IncomeTotals {
        gross = zeroIfNull(gross);
        dentistEarnings = zeroIfNull(dentistEarnings);
        clinicEarnings = zeroIfNull(clinicEarnings);
        receptionistEarnings = zeroIfNull(receptionistEarnings);
    }

    /** @return the three attributed shares added up; should equal {@link #gross()}. */
    public BigDecimal attributed() {
        return dentistEarnings.add(clinicEarnings).add(receptionistEarnings);
    }

    /** @return true if the attribution accounts for every rupee taken. */
    public boolean isBalanced() {
        return attributed().compareTo(gross) == 0;
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
