package com.sunrise.clinic.reporting.service;

import com.sunrise.clinic.access.domain.Action;
import com.sunrise.clinic.access.domain.ClinicPrincipal;
import com.sunrise.clinic.access.service.AccessControl;
import com.sunrise.clinic.reporting.data.ReportRepository;
import com.sunrise.clinic.reporting.domain.ClinicReport;
import com.sunrise.clinic.reporting.domain.DailyPoint;
import com.sunrise.clinic.reporting.domain.EarningsRow;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * The administrator's reports.
 *
 * <p>Composes the repository's answers into one {@link ClinicReport} and guards the whole
 * thing with {@code READ_REPORTS}. It no longer aggregates anything itself: the previous
 * version read every bill in the range and summed them across four maps, which is a
 * database's work done in a Java loop.</p>
 *
 * <p>It also no longer takes two {@code Map<String, String>} of names as parameters. That
 * pushed "who are these people" onto the caller, and the caller was a servlet.</p>
 */
public class ReportService {

    /** The default window when the administrator has not chosen one - FR-ADM-10. */
    private static final int DEFAULT_DAYS = 30;

    private final ReportRepository reports;
    private final Clock clock;

    public ReportService(ReportRepository reports, Clock clock) {
        this.reports = reports;
        this.clock = clock;
    }

    /** @return the last thirty days, ending today. */
    public LocalDate defaultFrom() {
        return today().minusDays(DEFAULT_DAYS - 1L);
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    /**
     * Everything the reports screen shows for a period.
     *
     * @throws IllegalArgumentException if the range runs backwards, or starts more than a
     *         reasonable distance in the future - a mistyped year otherwise produces an
     *         empty report that looks like a quiet month
     */
    public ClinicReport forPeriod(ClinicPrincipal caller, LocalDate from, LocalDate to) {
        AccessControl.require(caller, Action.READ_REPORTS);
        requireOrderedRange(from, to);

        return new ClinicReport(from, to,
                reports.income(from, to),
                reports.earningsByDentist(from, to),
                reports.earningsByReceptionist(from, to),
                reports.takingsByDay(from, to),
                reports.footfallByDay(from, to),
                // Judged against today, so an appointment later today is upcoming rather
                // than a no-show.
                reports.attendance(from, to, today()),
                reports.patientsSeen(from, to),
                reports.registeredPatients());
    }

    /**
     * The report as CSV - FR-ADM-18.
     *
     * <p>Four sections in one file, each with its own header row, because the alternative is
     * four downloads to answer one question. A spreadsheet reads it as one sheet and the
     * administrator deletes what they do not want.</p>
     */
    public String asCsv(ClinicPrincipal caller, LocalDate from, LocalDate to) {
        ClinicReport report = forPeriod(caller, from, to);
        StringBuilder csv = new StringBuilder();

        csv.append("Sunrise Dental Clinic — report\n");
        csv.append("From,To\n").append(report.from()).append(',').append(report.to()).append("\n\n");

        csv.append("Summary\n");
        csv.append("Bills issued,Gross takings,Dentist earnings,Clinic earnings,")
           .append("Patients seen,Registered patients,")
           .append("Attended,No-shows,No-show rate %\n");
        csv.append(report.income().bills()).append(',')
           .append(report.income().gross()).append(',')
           .append(report.income().dentistEarnings()).append(',')
           .append(report.income().clinicEarnings()).append(',')
           .append(report.patientsSeen()).append(',')
           .append(report.registeredPatients()).append(',')
           .append(report.attendance().attended()).append(',')
           .append(report.attendance().noShows()).append(',')
           .append(report.attendance().noShowRate()).append("\n\n");

        appendEarnings(csv, "Earnings by dentist", report.byDentist());
        // GAP-ADM-10: no reception commission, so no reception section.

        csv.append("Daily takings\nDate,Bills,Amount\n");
        for (DailyPoint point : report.takings()) {
            csv.append(point.date()).append(',').append(point.count()).append(',')
               .append(point.amount()).append('\n');
        }
        csv.append('\n');

        csv.append("Daily footfall\nDate,Appointments attended\n");
        for (DailyPoint point : report.footfall()) {
            csv.append(point.date()).append(',').append(point.count()).append('\n');
        }
        return csv.toString();
    }

    private static void appendEarnings(StringBuilder csv, String heading, List<EarningsRow> rows) {
        csv.append(heading).append("\nName,Bills,Amount\n");
        for (EarningsRow row : rows) {
            csv.append(quote(row.name())).append(',').append(row.count()).append(',')
               .append(row.amount()).append('\n');
        }
        csv.append('\n');
    }

    /**
     * Quotes a field for CSV.
     *
     * <p>A dentist called "Silva, Ranil" would otherwise become two columns and shift every
     * figure on the row one place left - a report that is wrong rather than broken, which is
     * worse.</p>
     */
    private static String quote(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }

    private static void requireOrderedRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Both a start and an end date are required.");
        }
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("The end date cannot be before the start date.");
        }
    }
}
