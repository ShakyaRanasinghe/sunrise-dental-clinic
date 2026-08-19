package com.sunrise.clinic.reporting.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One day on a trend: how many, and how much.
 *
 * @param date   the day
 * @param count  how many bills, or appointments, depending on the series
 * @param amount the money for that day, or zero for a series that only counts
 */
public record DailyPoint(LocalDate date, int count, BigDecimal amount) {

    public DailyPoint {
        amount = amount == null ? BigDecimal.ZERO : amount;
    }
}
