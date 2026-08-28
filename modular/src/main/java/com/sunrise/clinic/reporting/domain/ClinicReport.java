package com.sunrise.clinic.reporting.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Everything the administrator's reports screen shows for one period.
 *
 * @param from                 first day covered, inclusive
 * @param to                   last day covered, inclusive
 * @param income               the period's money and its attribution
 * @param byDentist            earnings per dentist - FR-ADM-13
 * @param byReceptionist       earnings per receptionist - FR-ADM-13
 * @param takings              daily takings, so a trend is visible - FR-ADM-14
 * @param footfall             appointments attended per day - FR-ADM-15
 * @param attendance           attended, no-shows and the rate - FR-ADM-19
 * @param patientsSeen         distinct patients treated in the period
 * @param registeredPatients   the register's size, for context
 */
public record ClinicReport(LocalDate from,
                           LocalDate to,
                           IncomeTotals income,
                           List<EarningsRow> byDentist,
                           List<EarningsRow> byReceptionist,
                           List<DailyPoint> takings,
                           List<DailyPoint> footfall,
                           Attendance attendance,
                           int patientsSeen,
                           long registeredPatients) {

    /**
     * @return true if nothing happened in this period
     *
     * <p>FR-ADM-17: a quiet period must say so rather than render an empty chart. An empty
     * chart looks like a broken chart, and the administrator cannot tell which.</p>
     */
    public boolean isEmpty() {
        return income.bills() == 0 && attendance.concluded() == 0;
    }

    /** @return the largest daily figure, so a bar chart can be scaled without script. */
    public BigDecimal peakTakings() {
        return takings.stream().map(DailyPoint::amount)
                .max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
    }

    /** @return the busiest day's count, for the same reason. */
    public int peakFootfall() {
        return footfall.stream().mapToInt(DailyPoint::count).max().orElse(0);
    }
}
