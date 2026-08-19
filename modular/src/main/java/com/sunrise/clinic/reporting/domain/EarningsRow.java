package com.sunrise.clinic.reporting.domain;

import java.math.BigDecimal;

/**
 * What one person earned over the period.
 *
 * <p>The name comes from the query rather than being resolved afterwards. The previous
 * version took two {@code Map<String, String>} of names as parameters to
 * {@code income(...)}, which pushed the job of knowing who people are onto the caller -
 * and the caller was a servlet.</p>
 *
 * @param id     the dentist id, or the staff uid
 * @param name   who they are
 * @param count  how many bills they appear on
 * @param amount what they earned
 */
public record EarningsRow(String id, String name, int count, BigDecimal amount) {

    public EarningsRow {
        amount = amount == null ? BigDecimal.ZERO : amount;
    }
}
