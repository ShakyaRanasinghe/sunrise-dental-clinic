package com.sunrise.clinic.reporting.data;

import com.sunrise.clinic.appointments.data.AppointmentRepository;
import com.sunrise.clinic.appointments.domain.Appointment;
import com.sunrise.clinic.appointments.domain.AppointmentStatus;
import com.sunrise.clinic.billing.data.BillRepository;
import com.sunrise.clinic.billing.domain.Bill;
import com.sunrise.clinic.patients.data.PatientRepository;
import com.sunrise.clinic.reporting.domain.Attendance;
import com.sunrise.clinic.reporting.domain.DailyPoint;
import com.sunrise.clinic.reporting.domain.EarningsRow;
import com.sunrise.clinic.reporting.domain.IncomeTotals;
import com.sunrise.clinic.scheduling.data.DentistRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * In-memory {@link ReportRepository}, for tests.
 *
 * <p>This does in Java what {@link JdbcReportDao} does in SQL - which is also, near enough,
 * what {@code ReportService} used to do in production. Doing the grouping by hand is fine
 * for a test fixture holding a handful of bills; it is the wrong place for it when the
 * bills are a year's worth in a database.</p>
 */
public class InMemoryReportRepository implements ReportRepository {

    private final BillRepository bills;
    private final AppointmentRepository appointments;
    private final PatientRepository patients;
    private final DentistRepository dentists;
    private final Function<String, String> staffName;

    public InMemoryReportRepository(BillRepository bills,
                                    AppointmentRepository appointments,
                                    PatientRepository patients,
                                    DentistRepository dentists,
                                    Function<String, String> staffName) {
        this.bills = bills;
        this.appointments = appointments;
        this.patients = patients;
        this.dentists = dentists;
        this.staffName = staffName;
    }

    @Override
    public IncomeTotals income(LocalDate from, LocalDate to) {
        List<Bill> issued = issuedBetween(from, to);
        BigDecimal gross = BigDecimal.ZERO;
        BigDecimal dentist = BigDecimal.ZERO;
        BigDecimal clinic = BigDecimal.ZERO;
        BigDecimal reception = BigDecimal.ZERO;
        for (Bill bill : issued) {
            gross = gross.add(bill.getTotal());
            dentist = dentist.add(bill.getDentistEarning());
            clinic = clinic.add(bill.getClinicEarning());
            reception = reception.add(bill.getReceptionistEarning());
        }
        return new IncomeTotals(issued.size(), gross, dentist, clinic, reception);
    }

    @Override
    public List<EarningsRow> earningsByDentist(LocalDate from, LocalDate to) {
        return group(issuedBetween(from, to), Bill::getDentistId, Bill::getDentistEarning,
                id -> dentists.findById(id).map(d -> d.getName()).orElse(id));
    }

    @Override
    public List<EarningsRow> earningsByReceptionist(LocalDate from, LocalDate to) {
        return group(issuedBetween(from, to), Bill::getReceptionistUid,
                Bill::getReceptionistEarning,
                uid -> {
                    String name = staffName.apply(uid);
                    return name == null ? "Former staff" : name;
                });
    }

    @Override
    public List<DailyPoint> takingsByDay(LocalDate from, LocalDate to) {
        Map<LocalDate, int[]> counts = new TreeMap<>();
        Map<LocalDate, BigDecimal> amounts = new TreeMap<>();
        for (Bill bill : issuedBetween(from, to)) {
            LocalDate day = dayOf(bill);
            counts.computeIfAbsent(day, k -> new int[1])[0]++;
            amounts.merge(day, bill.getTotal(), BigDecimal::add);
        }
        List<DailyPoint> points = new ArrayList<>();
        amounts.forEach((day, amount) -> points.add(
                new DailyPoint(day, counts.get(day)[0], amount)));
        return points;
    }

    @Override
    public List<DailyPoint> footfallByDay(LocalDate from, LocalDate to) {
        Map<LocalDate, int[]> perDay = new TreeMap<>();
        for (Appointment appointment : appointments.findByDateBetween(from, to)) {
            if (attended(appointment)) {
                perDay.computeIfAbsent(appointment.getDate(), k -> new int[1])[0]++;
            }
        }
        List<DailyPoint> points = new ArrayList<>();
        perDay.forEach((day, count) -> points.add(new DailyPoint(day, count[0], null)));
        return points;
    }

    @Override
    public Attendance attendance(LocalDate from, LocalDate to, LocalDate asOf) {
        int attended = 0;
        int noShows = 0;
        int upcoming = 0;
        for (Appointment appointment : appointments.findByDateBetween(from, to)) {
            if (attended(appointment)) {
                attended++;
            } else if (appointment.getStatus() == AppointmentStatus.CONFIRMED) {
                if (appointment.getDate().isBefore(asOf)) {
                    noShows++;
                } else {
                    upcoming++;
                }
            }
            // CANCELLED is neither: it was called off, so it is not a no-show.
        }
        return new Attendance(attended, noShows, upcoming);
    }

    @Override
    public int patientsSeen(LocalDate from, LocalDate to) {
        return (int) appointments.findByDateBetween(from, to).stream()
                .filter(InMemoryReportRepository::attended)
                .map(Appointment::getPatientId)
                .distinct()
                .count();
    }

    @Override
    public long registeredPatients() {
        return patients.count();
    }

    private static boolean attended(Appointment appointment) {
        return appointment.getStatus() == AppointmentStatus.COMPLETED
                || appointment.getStatus() == AppointmentStatus.BILLED;
    }

    private List<Bill> issuedBetween(LocalDate from, LocalDate to) {
        return bills.findAll().stream()
                .filter(bill -> bill.getIssuedAt() != null)
                .filter(bill -> {
                    LocalDate day = dayOf(bill);
                    return !day.isBefore(from) && !day.isAfter(to);
                })
                .toList();
    }

    private static LocalDate dayOf(Bill bill) {
        return bill.getIssuedAt().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private static List<EarningsRow> group(List<Bill> source,
                                           Function<Bill, String> key,
                                           Function<Bill, BigDecimal> amount,
                                           Function<String, String> nameOf) {
        Map<String, int[]> counts = new LinkedHashMap<>();
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        for (Bill bill : source) {
            String id = key.apply(bill);
            if (id == null) {
                continue;
            }
            counts.computeIfAbsent(id, k -> new int[1])[0]++;
            totals.merge(id, amount.apply(bill), BigDecimal::add);
        }
        List<EarningsRow> rows = new ArrayList<>();
        totals.forEach((id, total) -> rows.add(
                new EarningsRow(id, nameOf.apply(id), counts.get(id)[0], total)));
        rows.sort(Comparator.comparing(EarningsRow::amount).reversed());
        return rows;
    }
}
