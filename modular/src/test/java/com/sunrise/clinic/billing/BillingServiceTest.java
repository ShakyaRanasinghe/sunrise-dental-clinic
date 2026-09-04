package com.sunrise.clinic.billing;

import com.sunrise.clinic.access.domain.ClinicPrincipal;
import com.sunrise.clinic.access.domain.Role;
import com.sunrise.clinic.access.service.AccessControl;
import com.sunrise.clinic.appointments.AppointmentTestFixture;
import com.sunrise.clinic.appointments.domain.AppointmentStatus;
import com.sunrise.clinic.billing.data.InMemoryBillRepository;
import com.sunrise.clinic.billing.domain.BillResponse;
import com.sunrise.clinic.billing.service.BillingService;
import com.sunrise.clinic.billing.service.DefaultRevenueSplitStrategy;
import com.sunrise.clinic.billing.service.StandardBillingStrategy;
import com.sunrise.clinic.platform.data.SerialTransactionRunner;
import com.sunrise.clinic.platform.error.ResourceNotFoundException;
import com.sunrise.clinic.scheduling.domain.Treatment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Moved from {@code layered/} in step 5 and extended for the three things this step
 * refuses that the previous version did not: billing twice, billing an appointment nobody
 * has treated, and reading someone else's bill.
 */
class BillingServiceTest {

    private AppointmentTestFixture appointments;
    private InMemoryBillRepository bills;
    private BillingService billing;
    private ClinicPrincipal nimal;
    private ClinicPrincipal kamala;
    private String appointmentNo;

    @BeforeEach
    void setUp() {
        appointments = new AppointmentTestFixture();
        appointments.treatments.save(Treatment.builder().id("t-scaling")
                .name("Scaling & polishing").baseCost(new BigDecimal("3500.00")).active(true).build());
        nimal = appointments.addPatient("p-nimal", "u-pat1", "Nimal Perera");
        kamala = appointments.addPatient("p-kamala", "u-pat2", "Kamala Fernando");
        appointments.addSlot("s1", LocalTime.of(16, 0));

        bills = new InMemoryBillRepository();
        billing = new BillingService(bills, appointments.service,
                new com.sunrise.clinic.scheduling.service.ReferenceService(
                        appointments.dentists, appointments.treatments,
                        new com.sunrise.clinic.scheduling.data.InMemoryDentistTreatmentRepository()),
                appointments.clinicAccess,
                new StandardBillingStrategy(),
                new DefaultRevenueSplitStrategy(new BigDecimal("0.60"), BigDecimal.ZERO),
                new BigDecimal("200.00"),
                new SerialTransactionRunner());

        appointmentNo = appointments.service.book(nimal, "s1", "t-scaling", null).appointmentNo();
    }

    private void treat() {
        appointments.service.complete(AppointmentTestFixture.silva(), appointmentNo, "Scaling done");
    }

    // --- issuing ------------------------------------------------------

    @Test
    void aCompletedAppointmentIsBilled() {
        treat();

        BillResponse bill = billing.issue(AppointmentTestFixture.reception(), appointmentNo);

        assertEquals(new BigDecimal("1500.00"), bill.consultationFee());
        assertEquals(new BigDecimal("3500.00"), bill.treatmentCost());
        assertEquals(new BigDecimal("200.00"), bill.serviceCharge());
        assertEquals(new BigDecimal("5200.00"), bill.total());
        assertEquals("Nimal Perera", bill.patientName());
        assertEquals("Scaling & polishing", bill.treatmentName());
        assertNotNull(bill.issuedAt());
    }

    @Test
    void billingMovesTheAppointmentToBilled() {
        // In one transaction with the bill. A stored bill whose appointment still read
        // COMPLETED would be billed again by the next person to look at the day.
        treat();

        billing.issue(AppointmentTestFixture.reception(), appointmentNo);

        assertEquals(AppointmentStatus.BILLED,
                appointments.service.require(appointmentNo).getStatus());
    }

    @Test
    void theStoredSplitSumsToWhatThePatientPaid() {
        treat();
        billing.issue(AppointmentTestFixture.reception(), appointmentNo);

        var split = billing.revenueFor(AppointmentTestFixture.admin(), appointmentNo);

        assertEquals(new BigDecimal("5200.00"), split.sum());
    }

    @Test
    void theOwnersMarginIsWhatThePatientPaidLessTheDentist() {
        // The clinic's default policy: the whole difference is the owner's, which is what
        // a service charge on top of the dentist's own fees is for.
        treat();
        billing.issue(AppointmentTestFixture.reception(), appointmentNo);

        var split = billing.revenueFor(AppointmentTestFixture.admin(), appointmentNo);

        assertEquals(new BigDecimal("3600.00"), split.dentistEarning());
        assertEquals(new BigDecimal("1600.00"), split.clinicEarning());
        assertEquals(new BigDecimal("0.00"), split.receptionistEarning());
    }

    // --- the defect: billing twice ------------------------------------

    @Test
    void anAppointmentCannotBeBilledTwice() {
        // Nothing checked. The second call answered 201 with an id that was never stored,
        // because the DAO upserted on uq_bill_appointment and so rewrote the first bill's
        // row while handing back the new object: two receipts, one payment, one row.
        treat();
        BillResponse first = billing.issue(AppointmentTestFixture.reception(), appointmentNo);

        IllegalStateException refused = assertThrows(IllegalStateException.class,
                () -> billing.issue(AppointmentTestFixture.reception(), appointmentNo));

        assertTrue(refused.getMessage().contains(first.id()),
                "the refusal should name the receipt that already covers it: " + refused.getMessage());
        assertEquals(1, bills.count(), "exactly one bill may exist for an appointment");
    }

    @Test
    void theFirstBillSurvivesASecondAttempt() {
        treat();
        BillResponse first = billing.issue(AppointmentTestFixture.reception(), appointmentNo);

        assertThrows(IllegalStateException.class,
                () -> billing.issue(AppointmentTestFixture.reception(), appointmentNo));

        assertEquals(first.id(),
                billing.forAppointment(AppointmentTestFixture.reception(), appointmentNo).id());
    }

    // --- the defect: billing something nobody treated -----------------

    @Test
    void aConfirmedAppointmentCannotBeBilled() {
        // FR-BIL-05. trg_bill_requires_completion refuses it in the database, so integrity
        // held - but the caller got a 500 carrying a SQL message. The rule is about the
        // clinic, so it is stated where a person can read it.
        IllegalStateException refused = assertThrows(IllegalStateException.class,
                () -> billing.issue(AppointmentTestFixture.reception(), appointmentNo));

        assertTrue(refused.getMessage().contains("has not been treated yet"), refused.getMessage());
        assertEquals(0, bills.count());
    }

    @Test
    void aCancelledAppointmentCannotBeBilled() {
        appointments.service.cancel(nimal, appointmentNo);

        assertThrows(IllegalStateException.class,
                () -> billing.issue(AppointmentTestFixture.reception(), appointmentNo));
    }

    @Test
    void anUnknownAppointmentIsNotFound() {
        assertThrows(ResourceNotFoundException.class,
                () -> billing.issue(AppointmentTestFixture.reception(), "APT-nobody"));
    }

    // --- who may issue ------------------------------------------------

    @Test
    void onlyStaffWhoHoldIssueBillMayBill() {
        treat();

        assertThrows(AccessControl.AccessDeniedException.class,
                () -> billing.issue(nimal, appointmentNo));
        // A dentist treats; the desk takes the money.
        assertThrows(AccessControl.AccessDeniedException.class,
                () -> billing.issue(AppointmentTestFixture.silva(), appointmentNo));
    }

    @Test
    void theAdministratorMayAlsoBill() {
        treat();

        assertNotNull(billing.issue(AppointmentTestFixture.admin(), appointmentNo));
    }

    @Test
    void nothingIsWrittenWhenTheCallerIsRefused() {
        treat();

        assertThrows(AccessControl.AccessDeniedException.class,
                () -> billing.issue(nimal, appointmentNo));

        assertEquals(0, bills.count());
        assertEquals(AppointmentStatus.COMPLETED,
                appointments.service.require(appointmentNo).getStatus());
    }

    // --- the defect: reading someone else's bill -----------------------

    @Test
    void aPatientMayReadTheirOwnBill() {
        treat();
        billing.issue(AppointmentTestFixture.reception(), appointmentNo);

        assertEquals(new BigDecimal("5200.00"), billing.forAppointment(nimal, appointmentNo).total());
    }

    @Test
    void aPatientMayNotReadAnotherPatientsBill() {
        // There was no ownership check at all: any signed-in caller could read any bill,
        // which states what a named person paid for which treatment.
        treat();
        billing.issue(AppointmentTestFixture.reception(), appointmentNo);

        assertThrows(AccessControl.AccessDeniedException.class,
                () -> billing.forAppointment(kamala, appointmentNo));
    }

    @Test
    void anAnonymousCallerIsNotAuthenticated() {
        treat();
        billing.issue(AppointmentTestFixture.reception(), appointmentNo);

        assertThrows(AccessControl.NotAuthenticatedException.class,
                () -> billing.forAppointment(null, appointmentNo));
    }

    @Test
    void aMissingBillIsNotFoundRatherThanEmpty() {
        treat();

        assertThrows(ResourceNotFoundException.class,
                () -> billing.forAppointment(AppointmentTestFixture.reception(), appointmentNo));
    }

    // --- the revenue split is the administrator's ----------------------

    @Test
    void onlyTheAdministratorSeesTheRevenueSplit() {
        treat();
        billing.issue(AppointmentTestFixture.reception(), appointmentNo);

        assertNotNull(billing.revenueFor(AppointmentTestFixture.admin(), appointmentNo));
        // Not even the receptionist who issued it: they see their own earning through the
        // reports, never a colleague's.
        assertThrows(AccessControl.AccessDeniedException.class,
                () -> billing.revenueFor(AppointmentTestFixture.reception(), appointmentNo));
        assertThrows(AccessControl.AccessDeniedException.class,
                () -> billing.revenueFor(nimal, appointmentNo));
    }

    @Test
    void theResponseCarriesNoEarningsAtAll() {
        // Not stripped by a mapper - BillResponse has no components for them, so a
        // patient's receipt cannot carry the clinic's commission figures by accident.
        assertFalse(java.util.List.of(BillResponse.class.getRecordComponents()).stream()
                        .anyMatch(c -> c.getName().toLowerCase().contains("earning")),
                "BillResponse must not expose the revenue split");
    }

    @Test
    void isBilledAnswersWhatTheScreenNeeds() {
        treat();
        assertFalse(billing.isBilled(appointmentNo));

        billing.issue(AppointmentTestFixture.reception(), appointmentNo);

        assertTrue(billing.isBilled(appointmentNo));
    }

    @Test
    void aPatientWithNoTreatmentRecordedCannotBeBilled() {
        // The treatment is what is priced, so there is nothing to bill without one.
        AppointmentTestFixture fresh = new AppointmentTestFixture();
        ClinicPrincipal patient = fresh.addPatient("p-x", "u-x", "Someone");
        fresh.addSlot("s9", LocalTime.of(9, 0));
        String no = fresh.service.book(patient, "s9", "t-checkup", null).appointmentNo();
        fresh.service.complete(AppointmentTestFixture.silva(), no, "done");
        fresh.appointments.findById(no).orElseThrow().setTreatmentId(null);

        BillingService other = new BillingService(new InMemoryBillRepository(), fresh.service,
                new com.sunrise.clinic.scheduling.service.ReferenceService(
                        fresh.dentists, fresh.treatments,
                        new com.sunrise.clinic.scheduling.data.InMemoryDentistTreatmentRepository()),
                fresh.clinicAccess, new StandardBillingStrategy(),
                new DefaultRevenueSplitStrategy(new BigDecimal("0.60"), BigDecimal.ZERO),
                new BigDecimal("200.00"), new SerialTransactionRunner());

        assertThrows(IllegalStateException.class,
                () -> other.issue(AppointmentTestFixture.reception(), no));
    }

    // GAP-DEN-13 / GAP-REC-13: an "Other" visit carries the dentist's own price.

    @Test
    void completingAnOtherVisitWithoutAPriceIsRefused() {
        appointments.addSlot("s2", LocalTime.of(10, 0));
        String no = appointments.service.book(nimal, "s2", null, null, "sore wisdom tooth").appointmentNo();

        assertThrows(IllegalArgumentException.class, () ->
                appointments.service.complete(AppointmentTestFixture.silva(), no, "Wisdom tooth out"));
    }

    @Test
    void anOtherVisitIsBilledAtTheDentistRecordedPrice() {
        appointments.addSlot("s2", LocalTime.of(10, 0));
        String no = appointments.service.book(nimal, "s2", null, null, "sore wisdom tooth").appointmentNo();
        appointments.service.complete(AppointmentTestFixture.silva(), no,
                "Wisdom tooth extracted", new BigDecimal("8000.00"));

        BillResponse bill = billing.issue(AppointmentTestFixture.reception(), no);

        assertEquals(new BigDecimal("8000.00"), bill.treatmentCost());
        assertEquals(new BigDecimal("9700.00"), bill.total());
        // GAP-REC-13: the receipt names the patient's stated reason…
        assertEquals("sore wisdom tooth", bill.treatmentName());
        // …GAP-PAT-30: and carries the dentist's description for patient views.
        assertEquals("Wisdom tooth extracted", bill.diagnosis());
    }

    @Test
    void aNamedTreatmentPricesFromTheCatalogNotThePassedPrice() {
        appointments.service.complete(AppointmentTestFixture.silva(), appointmentNo,
                "Scaling done", new BigDecimal("9999.00"));

        BillResponse bill = billing.issue(AppointmentTestFixture.reception(), appointmentNo);

        assertEquals(new BigDecimal("3500.00"), bill.treatmentCost());
        assertEquals("Scaling & polishing", bill.treatmentName());
    }
}
