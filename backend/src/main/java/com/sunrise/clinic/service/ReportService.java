package com.sunrise.clinic.service;

import com.sunrise.clinic.dao.BillDao;
import com.sunrise.clinic.domain.Appointment;
import com.sunrise.clinic.domain.AppointmentStatus;
import com.sunrise.clinic.domain.Bill;
import com.sunrise.clinic.repository.AppointmentRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The clinic's management reports: how much was earned, and how busy the
 * practice was.
 *
 * <p>Aggregation is done in Java over the rows the DAOs return, rather than in
 * SQL. At the scale of a single dental practice — a few hundred bills a month —
 * the difference is imperceptible, and keeping the arithmetic here means the
 * revenue rules stay in one readable place and can be unit-tested without a
 * database. A clinic with years of history would push the {@code GROUP BY} down
 * into MySQL instead.</p>
 */
public class ReportService {

    /** Money earned over a period, split the way the clinic accounts for it. */
    public record IncomeReport(LocalDate from,
                               LocalDate to,
                               int billCount,
                               double grossTotal,
                               double dentistEarnings,
                               double clinicEarnings,
                               double receptionistEarnings,
                               List<EarningsRow> byDentist,
                               List<EarningsRow> byReceptionist,
                               List<DailyPoint> daily) {

        // JavaBean accessors for JSP: Expression Language 5.0 (Tomcat 10.1)
        // cannot read a record's x() accessor and looks for getX() instead.

        public LocalDate getFrom() {
            return from;
        }

        public LocalDate getTo() {
            return to;
        }

        public int getBillCount() {
            return billCount;
        }

        public double getGrossTotal() {
            return grossTotal;
        }

        public double getDentistEarnings() {
            return dentistEarnings;
        }

        public double getClinicEarnings() {
            return clinicEarnings;
        }

        public double getReceptionistEarnings() {
            return receptionistEarnings;
        }

        public List<EarningsRow> getByDentist() {
            return byDentist;
        }

        public List<EarningsRow> getByReceptionist() {
            return byReceptionist;
        }

        public List<DailyPoint> getDaily() {
            return daily;
        }
    }

    /** One person's share of the takings. */
    public record EarningsRow(String id, String name, int count, double amount) {

        // JavaBean accessors for JSP: Expression Language 5.0 (Tomcat 10.1)
        // cannot read a record's x() accessor and looks for getX() instead.

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public int getCount() {
            return count;
        }

        public double getAmount() {
            return amount;
        }
    }

    /** One day on a trend line. */
    public record DailyPoint(LocalDate date, int count, double amount) {

        // JavaBean accessors for JSP: Expression Language 5.0 (Tomcat 10.1)
        // cannot read a record's x() accessor and looks for getX() instead.

        public LocalDate getDate() {
            return date;
        }

        public int getCount() {
            return count;
        }

        public double getAmount() {
            return amount;
        }
    }

    /** How many patients were seen, by day. */
    public record FootfallReport(LocalDate from, LocalDate to, int total, List<DailyPoint> daily) {

        // JavaBean accessors for JSP: Expression Language 5.0 (Tomcat 10.1)
        // cannot read a record's x() accessor and looks for getX() instead.

        public LocalDate getFrom() {
            return from;
        }

        public LocalDate getTo() {
            return to;
        }

        public int getTotal() {
            return total;
        }

        public List<DailyPoint> getDaily() {
            return daily;
        }
    }

    private final BillDao bills;
    private final AppointmentRepository appointments;

    public ReportService(BillDao bills, AppointmentRepository appointments) {
        this.bills = bills;
        this.appointments = appointments;
    }

    /**
     * Income for a date range.
     *
     * @param nameOfDentist     resolves a dentist id to a display name
     * @param nameOfReceptionist resolves a staff uid to a display name
     */
    public IncomeReport income(LocalDate from, LocalDate to,
                               Map<String, String> nameOfDentist,
                               Map<String, String> nameOfReceptionist) {
        requireOrderedRange(from, to);
        List<Bill> issued = bills.findIssuedBetween(from, to);

        double gross = 0;
        double dentistTotal = 0;
        double clinicTotal = 0;
        double receptionistTotal = 0;

        Map<String, double[]> perDentist = new LinkedHashMap<>();
        Map<String, double[]> perReceptionist = new LinkedHashMap<>();
        Map<LocalDate, double[]> perDay = new TreeMap<>();

        for (Bill bill : issued) {
            gross += bill.getTotal();
            dentistTotal += bill.getDentistEarning();
            clinicTotal += bill.getClinicEarning();
            receptionistTotal += bill.getReceptionistEarning();

            accumulate(perDentist, bill.getDentistId(), bill.getDentistEarning());
            accumulate(perReceptionist, bill.getReceptionistUid(), bill.getReceptionistEarning());
            if (bill.getIssuedAt() != null) {
                LocalDate day = bill.getIssuedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                accumulate(perDay, day, bill.getTotal());
            }
        }

        return new IncomeReport(from, to, issued.size(),
                round(gross), round(dentistTotal), round(clinicTotal), round(receptionistTotal),
                toRows(perDentist, nameOfDentist),
                toRows(perReceptionist, nameOfReceptionist),
                toPoints(perDay));
    }

    /** Appointments actually attended, by day — the practice's footfall. */
    public FootfallReport footfall(LocalDate from, LocalDate to) {
        requireOrderedRange(from, to);
        Map<LocalDate, double[]> perDay = new TreeMap<>();
        int total = 0;

        for (Appointment appointment : appointments.findByDateBetween(from, to)) {
            // A cancelled appointment is not a visit and must not inflate the figure.
            if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
                continue;
            }
            total++;
            accumulate(perDay, appointment.getDate(), 0);
        }
        return new FootfallReport(from, to, total, toPoints(perDay));
    }

    // ------------------------------------------------------------------

    private static <K> void accumulate(Map<K, double[]> into, K key, double amount) {
        if (key == null) {
            return;
        }
        // [0] = how many, [1] = how much
        double[] cell = into.computeIfAbsent(key, k -> new double[2]);
        cell[0]++;
        cell[1] += amount;
    }

    private static List<EarningsRow> toRows(Map<String, double[]> source, Map<String, String> names) {
        List<EarningsRow> rows = new ArrayList<>();
        source.forEach((id, cell) -> rows.add(new EarningsRow(
                id,
                names.getOrDefault(id, id),
                (int) cell[0],
                round(cell[1]))));
        rows.sort((a, b) -> Double.compare(b.amount(), a.amount()));
        return rows;
    }

    private static List<DailyPoint> toPoints(Map<LocalDate, double[]> source) {
        List<DailyPoint> points = new ArrayList<>();
        source.forEach((day, cell) ->
                points.add(new DailyPoint(day, (int) cell[0], round(cell[1]))));
        return points;
    }

    private static void requireOrderedRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Both a start and an end date are required.");
        }
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("The end date must not be before the start date.");
        }
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
