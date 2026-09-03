package com.sunrise.clinic.reporting;

import com.sunrise.clinic.access.domain.ClinicPrincipal;
import com.sunrise.clinic.access.service.AccessControl;
import com.sunrise.clinic.appointments.AppointmentTestFixture;
import com.sunrise.clinic.billing.data.InMemoryBillRepository;
import com.sunrise.clinic.billing.service.BillingService;
import com.sunrise.clinic.billing.service.DefaultRevenueSplitStrategy;
import com.sunrise.clinic.billing.service.StandardBillingStrategy;
import com.sunrise.clinic.platform.data.SerialTransactionRunner;
import com.sunrise.clinic.reporting.data.InMemoryReportRepository;
import com.sunrise.clinic.reporting.domain.ClinicReport;
import com.sunrise.clinic.reporting.domain.EarningsRow;
import com.sunrise.clinic.reporting.service.ReportService;
import com.sunrise.clinic.scheduling.domain.Treatment;
import com.sunrise.clinic.scheduling.service.ReferenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * New in step 7, replacing a test that could not exist before: {@code ReportService} took a
 * concrete {@code BillDao}, so testing it needed a database.
 *
 * <p>The clock is fixed. The no-show derivation depends on what "today" is, and a test that
 * reads the system clock would pass or fail depending on when it ran.</p>
 */
class ReportServiceTest {

    /**
     * "Today" for the fixed clock.
     *
     * <p>The real date, not a hard-coded one - because a bill is stamped {@code Instant.now()}
     * when it is issued, and a report window anchored to a fixed past day excluded every bill the
     * test had just created. Determinism comes from placing every appointment relative to
     * this value rather than from the value itself being constant.</p>
     */
    private static final LocalDate TODAY = LocalDate.now();

    private AppointmentTestFixture appointments;
    private InMemoryBillRepository bills;
    private BillingService billing;
    private ReportService reports;
    private ClinicPrincipal admin;

    @BeforeEach
    void setUp() {
        appointments = new AppointmentTestFixture();
        appointments.treatments.save(Treatment.builder().id("t-scaling")
                .name("Scaling & polishing").baseCost(new BigDecimal("3500.00")).active(true).build());
        bills = new InMemoryBillRepository();
        billing = new BillingService(bills, appointments.service,
                new ReferenceService(appointments.dentists, appointments.treatments,
                        new com.sunrise.clinic.scheduling.data.InMemoryDentistTreatmentRepository()),
                appointments.clinicAccess, new StandardBillingStrategy(),
                new DefaultRevenueSplitStrategy(new BigDecimal("0.60"), BigDecimal.ZERO),
                new BigDecimal("200.00"), new SerialTransactionRunner());

        reports = new ReportService(
                new InMemoryReportRepository(bills, appointments.appointments,
                        appointments.patients, appointments.dentists,
                        uid -> "u-recep".equals(uid) ? "Kumari Silva" : null),
                Clock.fixed(TODAY.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                        ZoneId.systemDefault()));
        admin = AppointmentTestFixture.admin();
    }

    /** Books a slot on today, treats it, and bills it. */
    private String billed(String slotId, LocalTime at, String patientId, String uid) {
        ClinicPrincipal patient = appointments.addPatient(patientId, uid, "Patient " + uid);
        appointments.addSlot(slotId, "d-silva", TODAY, at);
        String no = appointments.service.book(patient, slotId, "t-scaling", null).appointmentNo();
        appointments.service.complete(AppointmentTestFixture.silva(), no, "done");
        billing.issue(AppointmentTestFixture.reception(), no);
        return no;
    }

    // --- permission ----------------------------------------------------

    @Test
    void onlyTheAdministratorMayReadReports() {
        for (ClinicPrincipal caller : List.of(AppointmentTestFixture.reception(),
                AppointmentTestFixture.silva())) {
            assertThrows(AccessControl.AccessDeniedException.class,
                    () -> reports.forPeriod(caller, TODAY.minusDays(30), TODAY));
        }
    }

    @Test
    void theCsvExportIsGuardedTheSameWay() {
        // The export goes through forPeriod, so it cannot be a way round the check.
        assertThrows(AccessControl.AccessDeniedException.class,
                () -> reports.asCsv(AppointmentTestFixture.reception(), TODAY.minusDays(30), TODAY));
    }

    // --- the money -----------------------------------------------------

    @Test
    void takingsAndTheirAttributionAreReported() {
        billed("s1", LocalTime.of(9, 0), "p-a", "u-a");
        billed("s2", LocalTime.of(9, 30), "p-b", "u-b");

        ClinicReport report = reports.forPeriod(admin, TODAY.minusDays(30), TODAY);

        assertEquals(2, report.income().bills());
        assertEquals(new BigDecimal("10400.00"), report.income().gross());
        assertEquals(new BigDecimal("7200.00"), report.income().dentistEarnings());
        assertEquals(new BigDecimal("3200.00"), report.income().clinicEarnings());
        assertEquals(new BigDecimal("0.00"), report.income().receptionistEarnings());
    }

    @Test
    void theAttributionAccountsForEveryRupeeTaken() {
        // The report's version of the per-bill invariant. If this ever fails, a bill was
        // written under a policy whose shares did not sum, or the query double-counted.
        billed("s1", LocalTime.of(9, 0), "p-a", "u-a");
        billed("s2", LocalTime.of(9, 30), "p-b", "u-b");

        assertTrue(reports.forPeriod(admin, TODAY.minusDays(30), TODAY).income().isBalanced());
    }

    @Test
    void earningsAreBrokenDownPerDentistBiggestFirst() {
        billed("s1", LocalTime.of(9, 0), "p-a", "u-a");
        appointments.addSlot("s2", "d-jaya", TODAY, LocalTime.of(9, 30));
        ClinicPrincipal other = appointments.addPatient("p-b", "u-b", "Patient B");
        String no = appointments.service.book(other, "s2", "t-scaling", null).appointmentNo();
        appointments.service.complete(AppointmentTestFixture.jayasuriya(), no, "done");
        billing.issue(AppointmentTestFixture.reception(), no);

        List<EarningsRow> rows = reports.forPeriod(admin, TODAY.minusDays(30), TODAY).byDentist();

        assertEquals(2, rows.size());
        assertTrue(rows.get(0).amount().compareTo(rows.get(1).amount()) >= 0, "biggest first");
        assertTrue(rows.stream().anyMatch(r -> "Dr. Ranil Silva".equals(r.name())),
                "the name comes from the query, not from a map the caller passes in");
    }

    @Test
    void aBillIssuedByFormerStaffStillCounts() {
        // fk_bill_receptionist is ON DELETE SET NULL, so a bill can outlive the account
        // that issued it. Those rupees happened and must still appear.
        billed("s1", LocalTime.of(9, 0), "p-a", "u-a");
        bills.findAll().forEach(b -> {
            b.setReceptionistUid("u-departed");
            bills.save(b);
        });

        List<EarningsRow> rows = reports.forPeriod(admin, TODAY.minusDays(30), TODAY).byReceptionist();

        assertEquals(1, rows.size());
        assertEquals("Former staff", rows.get(0).name());
    }

    // --- the no-show derivation ----------------------------------------

    @Test
    void aPastConfirmedAppointmentIsANoShow() {
        // No NO_SHOW status exists, and adding one would mean somebody at the desk
        // remembering to set it. Derived instead: the date has passed, nobody cancelled,
        // and no dentist recorded a treatment.
        ClinicPrincipal patient = appointments.addPatient("p-a", "u-a", "Patient A");
        appointments.addSlot("s1", "d-silva", TODAY.minusDays(2), LocalTime.of(9, 0));
        appointments.service.book(patient, "s1", "t-scaling", null);

        var attendance = reports.forPeriod(admin, TODAY.minusDays(30), TODAY).attendance();

        assertEquals(0, attendance.attended());
        assertEquals(1, attendance.noShows());
        assertEquals(0, attendance.upcoming());
    }

    @Test
    void aFutureConfirmedAppointmentIsUpcomingAndNotANoShow() {
        ClinicPrincipal patient = appointments.addPatient("p-a", "u-a", "Patient A");
        appointments.addSlot("s1", "d-silva", TODAY.plusDays(3), LocalTime.of(9, 0));
        appointments.service.book(patient, "s1", "t-scaling", null);

        var attendance = reports.forPeriod(admin, TODAY.minusDays(30), TODAY.plusDays(30)).attendance();

        assertEquals(1, attendance.upcoming());
        assertEquals(0, attendance.noShows());
    }

    @Test
    void aCancelledAppointmentIsNeitherAttendedNorANoShow() {
        // It was called off, which is the opposite of not turning up.
        ClinicPrincipal patient = appointments.addPatient("p-a", "u-a", "Patient A");
        appointments.addSlot("s1", "d-silva", TODAY.minusDays(2), LocalTime.of(9, 0));
        String no = appointments.service.book(patient, "s1", "t-scaling", null).appointmentNo();
        appointments.service.cancel(patient, no);

        var attendance = reports.forPeriod(admin, TODAY.minusDays(30), TODAY).attendance();

        assertEquals(0, attendance.attended());
        assertEquals(0, attendance.noShows());
        assertEquals(0, attendance.concluded());
    }

    @Test
    void theRateIsOfConcludedAppointmentsNotOfAllOfThem() {
        // Counting upcoming appointments as attended would make the rate drift down every
        // time somebody books, which is the opposite of what the figure is for.
        billed("s1", LocalTime.of(9, 0), "p-a", "u-a");           // attended
        billed("s2", LocalTime.of(9, 30), "p-b", "u-b");          // attended
        ClinicPrincipal c = appointments.addPatient("p-c", "u-c", "Patient C");
        appointments.addSlot("s3", "d-silva", TODAY.minusDays(2), LocalTime.of(10, 0));
        appointments.service.book(c, "s3", "t-scaling", null);     // past, untreated
        ClinicPrincipal d = appointments.addPatient("p-d", "u-d", "Patient D");
        appointments.addSlot("s4", "d-silva", TODAY.plusDays(2), LocalTime.of(10, 0));
        appointments.service.book(d, "s4", "t-scaling", null);     // upcoming

        var attendance = reports.forPeriod(admin, TODAY.minusDays(30), TODAY.plusDays(30)).attendance();

        assertEquals(2, attendance.attended());
        assertEquals(1, attendance.noShows());
        assertEquals(1, attendance.upcoming());
        assertEquals(3, attendance.concluded());
        assertEquals(new BigDecimal("33.3"), attendance.noShowRate());
    }

    @Test
    void anEmptyPeriodHasARateOfZeroRatherThanDividingByNothing() {
        assertEquals(new BigDecimal("0.0"),
                reports.forPeriod(admin, TODAY.minusDays(30), TODAY).attendance().noShowRate());
    }

    // --- an empty period ------------------------------------------------

    @Test
    void aQuietPeriodSaysSoRatherThanRenderingNothing() {
        // FR-ADM-17. An empty chart looks like a broken chart and the administrator cannot
        // tell which.
        assertTrue(reports.forPeriod(admin, TODAY.minusDays(30), TODAY).isEmpty());
    }

    @Test
    void aPeriodWithActivityIsNotEmpty() {
        billed("s1", LocalTime.of(9, 0), "p-a", "u-a");

        assertFalse(reports.forPeriod(admin, TODAY.minusDays(30), TODAY).isEmpty());
    }

    // --- the range ------------------------------------------------------

    @Test
    void aBackwardsRangeIsRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> reports.forPeriod(admin, TODAY, TODAY.minusDays(5)));
    }

    @Test
    void theDefaultRangeIsTheLastThirtyDaysEndingToday() {
        assertEquals(TODAY, reports.today());
        assertEquals(TODAY.minusDays(29), reports.defaultFrom());
    }

    @Test
    void aBillOutsideTheRangeIsExcluded() {
        billed("s1", LocalTime.of(9, 0), "p-a", "u-a");

        // The bill was issued today; a window that ended yesterday must not include it.
        assertEquals(0, reports.forPeriod(admin, TODAY.minusDays(30), TODAY.minusDays(1))
                .income().bills());
    }

    // --- the CSV --------------------------------------------------------

    @Test
    void theCsvCarriesEverySection() {
        billed("s1", LocalTime.of(9, 0), "p-a", "u-a");

        String csv = reports.asCsv(admin, TODAY.minusDays(30), TODAY);

        assertTrue(csv.contains("Summary"), csv);
        assertTrue(csv.contains("Earnings by dentist"), csv);
        assertTrue(csv.contains("Earnings by receptionist"), csv);
        assertTrue(csv.contains("Daily takings"), csv);
        assertTrue(csv.contains("Daily footfall"), csv);
        assertTrue(csv.contains("5200.00"), "the gross should appear: " + csv);
    }

    @Test
    void aNameContainingACommaIsQuoted() {
        // "Silva, Ranil" would otherwise become two columns and shift every figure on the
        // row one place left - a report that is wrong rather than broken, which is worse.
        appointments.dentists.findById("d-silva").ifPresent(d -> {
            d.setName("Silva, Ranil");
            appointments.dentists.save(d);
        });
        billed("s1", LocalTime.of(9, 0), "p-a", "u-a");

        assertTrue(reports.asCsv(admin, TODAY.minusDays(30), TODAY).contains("\"Silva, Ranil\""));
    }
}
