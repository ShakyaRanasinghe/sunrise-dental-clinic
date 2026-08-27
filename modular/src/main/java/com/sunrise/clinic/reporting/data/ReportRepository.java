package com.sunrise.clinic.reporting.data;

import com.sunrise.clinic.reporting.domain.Attendance;
import com.sunrise.clinic.reporting.domain.DailyPoint;
import com.sunrise.clinic.reporting.domain.EarningsRow;
import com.sunrise.clinic.reporting.domain.IncomeTotals;

import java.time.LocalDate;
import java.util.List;

/**
 * The queries the administrator's reports need.
 *
 * <p>New in the modular build, and it exists for two reasons.</p>
 *
 * <p><b>It closes the last boundary violation.</b> {@code ReportService} imported
 * {@code BillDao} - the concrete JDBC class, not a port. It was the one service that did,
 * and the only reason was that the aggregate query it wanted was declared on the
 * implementation.</p>
 *
 * <p><b>Aggregation belongs in the database.</b> The previous version loaded every bill in
 * the range with {@code findIssuedBetween} and summed them in a Java loop over four
 * {@code Map}s. A year's reporting means reading a year of bills into memory to produce
 * twenty numbers. {@code SUM} and {@code GROUP BY} are what the database is for, and the
 * figures come back already grouped and named.</p>
 *
 * <p>Not a {@code Repository<T, ID>}: there is no entity here. A report is a question, and
 * these are the questions.</p>
 */
public interface ReportRepository {

    /** Takings and their attribution for the period - FR-ADM-11, FR-ADM-12. */
    IncomeTotals income(LocalDate from, LocalDate to);

    /** Earnings per dentist, biggest first - FR-ADM-13. */
    List<EarningsRow> earningsByDentist(LocalDate from, LocalDate to);

    /** Earnings per receptionist, biggest first - FR-ADM-13. */
    List<EarningsRow> earningsByReceptionist(LocalDate from, LocalDate to);

    /** Takings per day, in date order - FR-ADM-14. */
    List<DailyPoint> takingsByDay(LocalDate from, LocalDate to);

    /** Appointments attended per day, in date order - FR-ADM-15. */
    List<DailyPoint> footfallByDay(LocalDate from, LocalDate to);

    /**
     * Attended, no-shows and upcoming for the period - FR-ADM-19.
     *
     * @param asOf the date a past appointment is judged against, so the derivation is
     *             testable rather than depending on the clock
     */
    Attendance attendance(LocalDate from, LocalDate to, LocalDate asOf);

    /** Distinct patients treated in the period. */
    int patientsSeen(LocalDate from, LocalDate to);

    /** How many patients are on the register at all. */
    long registeredPatients();
}
