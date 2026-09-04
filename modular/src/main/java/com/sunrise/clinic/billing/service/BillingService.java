package com.sunrise.clinic.billing.service;

import com.sunrise.clinic.access.domain.Action;
import com.sunrise.clinic.access.domain.ClinicPrincipal;
import com.sunrise.clinic.access.domain.Role;
import com.sunrise.clinic.access.service.AccessControl;
import com.sunrise.clinic.appointments.domain.Appointment;
import com.sunrise.clinic.appointments.domain.AppointmentStatus;
import com.sunrise.clinic.appointments.service.AppointmentService;
import com.sunrise.clinic.appointments.service.ClinicAccess;
import com.sunrise.clinic.billing.data.BillRepository;
import com.sunrise.clinic.billing.domain.Bill;
import com.sunrise.clinic.billing.domain.BillBreakdown;
import com.sunrise.clinic.billing.domain.BillResponse;
import com.sunrise.clinic.billing.domain.RevenueSplit;
import com.sunrise.clinic.patients.domain.Patient;
import com.sunrise.clinic.platform.data.TransactionRunner;
import com.sunrise.clinic.platform.error.ResourceNotFoundException;
import com.sunrise.clinic.scheduling.domain.Dentist;
import com.sunrise.clinic.scheduling.domain.Treatment;
import com.sunrise.clinic.scheduling.service.ReferenceService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Produces a patient's bill and its three-way revenue split.
 *
 * <p>The "how" of both belongs to two injected strategies, so pricing and commission
 * policy change without touching this class. What lives here is the workflow: check it may
 * be billed, check it has not been billed already, compute, store, and move the
 * appointment to BILLED - the last two in one transaction, because a bill whose
 * appointment still reads COMPLETED would be billed again by the next person to look.</p>
 *
 * <h2>What this version refuses that the previous one did not</h2>
 *
 * <ol>
 *   <li><b>Billing twice.</b> Nothing checked for an existing bill. The second call
 *       answered {@code 201} with an id that was never stored, because the DAO upserted on
 *       {@code uq_bill_appointment} and so rewrote the first bill's row while returning the
 *       new object. Two receipts, different numbers, one payment, one row.</li>
 *   <li><b>Billing an appointment nobody has treated.</b>
 *       {@code trg_bill_requires_completion} refuses it in the database, so integrity held
 *       - but the caller got a 500 with a SQL message. FR-BIL-05 is a rule about the
 *       clinic, and it should be stated where a person can read it.</li>
 *   <li><b>Reading someone else's bill.</b> {@code GET /api/appointments/{no}/bill} had no
 *       ownership check at all: any signed-in caller could read any bill, which is a
 *       statement of what a named person paid for what treatment.</li>
 * </ol>
 */
public class BillingService {

    private static final Logger log = Logger.getLogger(BillingService.class.getName());

    private final BillRepository bills;
    private final AppointmentService appointments;
    private final ReferenceService reference;
    private final ClinicAccess clinicAccess;
    private final BillingStrategy pricing;
    private final RevenueSplitStrategy revenueSplit;
    // GAP-ADM-10: read per bill, so a Pricing-tab save applies without restart.
    private final java.util.function.Supplier<BigDecimal> serviceCharge;
    private final TransactionRunner transaction;

    public BillingService(BillRepository bills,
                          AppointmentService appointments,
                          ReferenceService reference,
                          ClinicAccess clinicAccess,
                          BillingStrategy pricing,
                          RevenueSplitStrategy revenueSplit,
                          BigDecimal serviceCharge,
                          TransactionRunner transaction) {
        this(bills, appointments, reference, clinicAccess, pricing, revenueSplit,
                () -> serviceCharge, transaction);
    }

    public BillingService(BillRepository bills,
                          AppointmentService appointments,
                          ReferenceService reference,
                          ClinicAccess clinicAccess,
                          BillingStrategy pricing,
                          RevenueSplitStrategy revenueSplit,
                          java.util.function.Supplier<BigDecimal> serviceCharge,
                          TransactionRunner transaction) {
        this.bills = bills;
        this.appointments = appointments;
        this.reference = reference;
        this.clinicAccess = clinicAccess;
        this.pricing = pricing;
        this.revenueSplit = revenueSplit;
        this.serviceCharge = serviceCharge;
        this.transaction = transaction;
    }

    /**
     * Issue the bill for a completed appointment.
     *
     * @throws IllegalStateException if the appointment has not been treated yet, or has
     *         already been billed
     */
    public BillResponse issue(ClinicPrincipal caller, String appointmentNo) {
        AccessControl.require(caller, Action.ISSUE_BILL);

        Appointment appointment = appointments.require(appointmentNo);

        // FR-BIL-05, said here rather than left to the trigger. The database still
        // refuses it; this is so the person at the desk is told why.
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new IllegalStateException(
                    appointmentNo + " was cancelled, so there is nothing to bill.");
        }
        if (appointment.getStatus() == AppointmentStatus.CONFIRMED) {
            throw new IllegalStateException(
                    appointmentNo + " has not been treated yet. The dentist records the"
                            + " treatment first, and then it can be billed.");
        }

        // The check the previous version was missing entirely.
        Optional<Bill> existing = bills.findByAppointmentNo(appointmentNo);
        if (existing.isPresent()) {
            throw new IllegalStateException(
                    appointmentNo + " was already billed. Receipt "
                            + existing.get().getId() + " covers it.");
        }

        Dentist dentist = reference.requireDentist(appointment.getDentistId());

        BillBreakdown breakdown = pricing.calculate(
                dentist.getConsultationFee(), treatmentCost(appointment),
                BillBreakdown.money(serviceCharge.get()));
        RevenueSplit split = revenueSplit.split(breakdown);

        Bill bill = Bill.builder()
                .id(UUID.randomUUID().toString())
                .appointmentNo(appointmentNo)
                .patientId(appointment.getPatientId())
                .dentistId(dentist.getId())
                .receptionistUid(caller.uid())
                .consultationFee(breakdown.consultationFee())
                .treatmentCost(breakdown.treatmentCost())
                .serviceCharge(breakdown.serviceCharge())
                .discount(breakdown.discount())
                .tax(breakdown.tax())
                .total(breakdown.total())
                .dentistEarning(split.dentistEarning())
                .clinicEarning(split.clinicEarning())
                .receptionistEarning(split.receptionistEarning())
                .issuedAt(Instant.now())
                .issuedByUid(caller.uid())
                .build();

        // One transaction. A stored bill whose appointment still read COMPLETED would be
        // billed again by the next person who looked at it.
        transaction.execute(() -> {
            bills.save(bill);
            // The appointments module owns its own transition - see markBilled. Inside
            // this transaction, so the bill and the status commit together.
            appointments.markBilled(caller, appointmentNo);
            return null;
        });

        log.log(Level.INFO, "bill_issued id={0} appointment={1} total={2} by={3}",
                new Object[] { bill.getId(), appointmentNo, bill.getTotal(), caller.uid() });
        return describe(bill, appointment);
    }

    /**
     * The bill for an appointment.
     *
     * <p>Readable by the patient it belongs to, and by staff who issue bills. A bill says
     * what a named person paid for which treatment, so "any signed-in caller" was the
     * wrong audience.</p>
     */
    public BillResponse forAppointment(ClinicPrincipal caller, String appointmentNo) {
        Appointment appointment = appointments.require(appointmentNo);
        requireMayRead(caller, appointment);
        Bill bill = bills.findByAppointmentNo(appointmentNo).orElseThrow(() ->
                new ResourceNotFoundException("No bill has been issued for " + appointmentNo));
        return describe(bill, appointment);
    }

    /** Whether this appointment already has a bill - for a screen deciding what to offer. */
    public boolean isBilled(String appointmentNo) {
        return bills.findByAppointmentNo(appointmentNo).isPresent();
    }

    /**
     * The revenue split behind a bill.
     *
     * <p>Administrator only. Staff see their own earning through the reports; nobody sees
     * a colleague's.</p>
     */
    public RevenueSplit revenueFor(ClinicPrincipal caller, String appointmentNo) {
        AccessControl.require(caller, Action.READ_REPORTS);
        Bill bill = bills.findByAppointmentNo(appointmentNo).orElseThrow(() ->
                new ResourceNotFoundException("No bill has been issued for " + appointmentNo));
        return new RevenueSplit(bill.getDentistEarning(), bill.getClinicEarning(),
                bill.getReceptionistEarning());
    }

    private void requireMayRead(ClinicPrincipal caller, Appointment appointment) {
        if (caller == null) {
            throw new AccessControl.NotAuthenticatedException("Authentication is required.");
        }
        if (caller.role() == Role.PATIENT) {
            String ownPatientId = clinicAccess.patientFor(caller).map(Patient::getId).orElse(null);
            if (!appointment.belongsTo(ownPatientId)) {
                throw new AccessControl.AccessDeniedException("That bill is not yours.");
            }
            return;
        }
        AccessControl.require(caller, Action.ISSUE_BILL);
    }

    /**
     * What the treatment line prices from. A named treatment prices from the
     * catalog; a treatment-less ("Other") visit prices from the amount the
     * dentist recorded when completing it (GAP-DEN-13, GAP-REC-13).
     */
    private BigDecimal treatmentCost(Appointment appointment) {
        if (appointment.getTreatmentId() != null) {
            Treatment treatment = reference.requireTreatment(appointment.getTreatmentId());
            return treatment.getBaseCost();
        }
        if (appointment.getCustomPrice() != null) {
            return appointment.getCustomPrice();
        }
        throw new IllegalStateException(
                appointment.getAppointmentNo() + " has no treatment and no recorded price,"
                        + " so there is nothing to price. The dentist records the price"
                        + " when completing the visit.");
    }

    private BillResponse describe(Bill bill, Appointment appointment) {
        return BillResponse.of(bill,
                clinicAccess.patientById(appointment.getPatientId())
                        .map(Patient::getName).orElse(null),
                reference.requireDentist(appointment.getDentistId()).getName(),
                treatmentName(appointment),
                appointment.getDiagnosis());
    }

    /**
     * What the receipt names as the Treatment line. A named treatment uses the
     * catalog name; a treatment-less visit names the patient's own stated reason
     * (GAP-REC-13) — non-clinical, so it is safe on every receipt.
     */
    private String treatmentName(Appointment appointment) {
        if (appointment.getTreatmentId() != null) {
            return reference.requireTreatment(appointment.getTreatmentId()).getName();
        }
        return appointment.getPatientReason() != null ? appointment.getPatientReason() : "Other";
    }
}
