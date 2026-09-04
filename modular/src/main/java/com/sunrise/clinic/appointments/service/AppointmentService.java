package com.sunrise.clinic.appointments.service;

import com.sunrise.clinic.access.domain.Action;
import com.sunrise.clinic.access.domain.ClinicPrincipal;
import com.sunrise.clinic.access.domain.Role;
import com.sunrise.clinic.access.service.AccessControl;
import com.sunrise.clinic.appointments.data.AppointmentRepository;
import com.sunrise.clinic.appointments.domain.Appointment;
import com.sunrise.clinic.appointments.domain.AppointmentDetailResponse;
import com.sunrise.clinic.appointments.domain.AppointmentResponse;
import com.sunrise.clinic.appointments.domain.AppointmentStatus;
import com.sunrise.clinic.patients.domain.Patient;
import com.sunrise.clinic.patients.domain.PatientNoteResponse;
import com.sunrise.clinic.patients.service.PatientNoteService;
import com.sunrise.clinic.platform.data.TransactionRunner;
import com.sunrise.clinic.platform.error.ResourceNotFoundException;
import com.sunrise.clinic.platform.error.SlotUnavailableException;
import com.sunrise.clinic.scheduling.data.SlotRepository;
import com.sunrise.clinic.scheduling.domain.Dentist;
import com.sunrise.clinic.scheduling.domain.Slot;
import com.sunrise.clinic.scheduling.domain.Treatment;
import com.sunrise.clinic.scheduling.service.ReferenceService;

import java.math.BigDecimal;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Booking, cancelling and completing - the heart of the system.
 *
 * <h2>Preventing a double booking</h2>
 *
 * <p>{@link #book} runs inside a {@link TransactionRunner} and reads the slot with
 * {@link SlotRepository#findByIdForUpdate} rather than an ordinary lookup. Without the
 * lock, two patients clicking the same time in the same second both read it as OPEN and
 * both proceed. With it, the second request waits, then sees BOOKED and is refused with
 * {@link SlotUnavailableException}. The unique key on {@code slot.appointment_no} is the
 * last-resort guarantee in the database itself, and the appointment number now comes from
 * a locked table row rather than an in-memory counter.</p>
 *
 * <p>The event is published <em>after</em> the transaction commits, so no patient is ever
 * sent a confirmation for a booking that was then rolled back.</p>
 *
 * <h2>What this version enforces that the previous one did not</h2>
 *
 * <ol>
 *   <li><b>{@code complete} checked the role and not the appointment.</b>
 *       {@code AccessControl.require(user, Role.DENTIST)} let <em>any</em> dentist write
 *       a diagnosis into <em>any</em> appointment, including another dentist's patient.
 *       Now the appointment must be the caller's own to treat.</li>
 *   <li><b>Nothing enforced the status machine.</b> {@code complete} and {@code cancel}
 *       assigned the new status directly, so a cancelled appointment could be completed
 *       and a <b>billed</b> one cancelled - releasing its slot while the bill stood. The
 *       rules now live on {@link AppointmentStatus} and are applied by the entity.</li>
 *   <li><b>There was no way to ask what is booked.</b> Only {@code /{no}} existed, so no
 *       screen or client could list a day, a patient's appointments or a dentist's
 *       schedule without reading the repository directly - which is exactly what all
 *       four servlets did.</li>
 * </ol>
 */
public class AppointmentService {

    private static final Logger log = Logger.getLogger(AppointmentService.class.getName());

    private final SlotRepository slots;
    private final AppointmentRepository appointments;
    private final ReferenceService reference;
    private final ClinicAccess clinicAccess;
    private final AppointmentNumberGenerator numbers;
    private final PatientNoteService patientNotes;
    private final AppointmentEventPublisher publisher;
    private final TransactionRunner transaction;

    public AppointmentService(SlotRepository slots,
                             AppointmentRepository appointments,
                             ReferenceService reference,
                             ClinicAccess clinicAccess,
                             AppointmentNumberGenerator numbers,
                             PatientNoteService patientNotes,
                             AppointmentEventPublisher publisher,
                             TransactionRunner transaction) {
        this.slots = slots;
        this.appointments = appointments;
        this.reference = reference;
        this.clinicAccess = clinicAccess;
        this.numbers = numbers;
        this.patientNotes = patientNotes;
        this.publisher = publisher;
        this.transaction = transaction;
    }

    // --- booking ------------------------------------------------------

    public AppointmentResponse book(ClinicPrincipal caller, String slotId, String treatmentId,
                                    String requestedPatientId) {
        return book(caller, slotId, treatmentId, requestedPatientId, null);
    }

    /**
     * Book a slot.
     *
     * <p>A patient books for themselves and sends no {@code patientId}; the id is
     * resolved from their own account so they cannot book in someone else's name. Staff
     * book on a patient's behalf and must name them. That resolution used to live in the
     * servlet, duplicated in {@code cancel}, and reached the patient repository from the
     * web tier to do it.</p>
     *
     * <p>{@code patientReason} carries GAP-FTB-06's "Other (describe…)" booking: the
     * patient wanted a visit but no listed procedure fit, so they book with an
     * unspecified {@code treatmentId} and record in their own words what it is for.</p>
     */
    public AppointmentResponse book(ClinicPrincipal caller, String slotId, String treatmentId,
                                    String requestedPatientId, String patientReason) {
        Action needed = caller != null && caller.role() == Role.PATIENT
                ? Action.BOOK_OWN
                : Action.BOOK_FOR_PATIENT;
        AccessControl.require(caller, needed);

        String patientId = resolvePatient(caller, requestedPatientId);
        // Fails before the transaction opens: an unknown treatment is a bad request, not
        // a foreign-key violation discovered halfway through booking. An "Other" booking
        // has no treatment at all, so the reason is what validates instead.
        if (treatmentId != null && !treatmentId.isBlank()) {
            reference.requireTreatment(treatmentId);
        }

        Appointment booked = transaction.execute(() -> {
            Slot slot = slots.findByIdForUpdate(slotId)
                    .orElseThrow(() -> new ResourceNotFoundException("Slot not found: " + slotId));
            if (!slot.isOpen()) {
                throw new SlotUnavailableException(
                        "That time has just been taken. Please choose another.");
            }

            String appointmentNo = numbers.next(slot.getDate());
            Appointment appointment = Appointment.builder()
                    .appointmentNo(appointmentNo)
                    .patientId(patientId)
                    .dentistId(slot.getDentistId())
                    .slotId(slotId)
                    .treatmentId(treatmentId == null || treatmentId.isBlank() ? null : treatmentId)
                    .patientReason(treatmentId == null || treatmentId.isBlank()
                                    && patientReason != null && !patientReason.isBlank()
                            ? patientReason.trim()
                            : null)
                    .date(slot.getDate())
                    .time(slot.getStartTime())
                    .status(AppointmentStatus.CONFIRMED)
                    .createdByUid(caller.uid())
                    .createdByRole(caller.role())
                    .createdAt(Instant.now())
                    .build();

            // The appointment row is written before the slot points at it, so the
            // foreign key from slot.appointment_no is satisfied at every instant.
            appointments.save(appointment);
            slot.bookFor(appointmentNo);
            slots.save(slot);
            return appointment;
        });

        log.log(Level.INFO, "appointment_booked no={0} patient={1} by={2}",
                new Object[] { booked.getAppointmentNo(), patientId, caller.uid() });
        announce(AppointmentEvent.Type.CREATED, booked, caller);
        return describe(booked);
    }

    // --- reading ------------------------------------------------------

    /** One appointment, without clinical detail. */
    public AppointmentResponse findByNo(ClinicPrincipal caller, String appointmentNo) {
        return describe(readable(caller, appointmentNo));
    }

    /**
     * One appointment, with the diagnosis if the caller may see it.
     *
     * @return {@link AppointmentDetailResponse} for the treating dentist or the patient
     *         themselves, {@link AppointmentResponse} for anyone else who may read it at
     *         all. The two are different types so the choice cannot be made by accident
     */
    public Object findDetail(ClinicPrincipal caller, String appointmentNo) {
        Appointment appointment = readable(caller, appointmentNo);
        if (clinicAccess.canViewClinical(appointment, caller)) {
            return detailWithNotes(caller, appointment);
        }
        return describe(appointment);
    }

    /**
     * Everything booked on a date - the front desk's day view.
     *
     * <p>This and the two below are what {@code GET /api/appointments} was missing. There
     * was no way to ask the system what was booked, so every dashboard read the
     * repository itself.</p>
     */
    public List<AppointmentResponse> onDate(ClinicPrincipal caller, LocalDate date) {
        AccessControl.require(caller, Action.SEARCH_PATIENTS);
        return describeAll(appointments.findByDate(date));
    }

    /** A dentist's own schedule for a date. */
    public List<AppointmentResponse> forDentistOn(ClinicPrincipal caller, LocalDate date) {
        Dentist dentist = clinicAccess.dentistFor(caller).orElseThrow(() ->
                new ResourceNotFoundException("No dentist record for " + describeCaller(caller)));
        return describeAll(appointments.findByDentistId(dentist.getId()).stream()
                .filter(a -> date.equals(a.getDate()))
                .toList());
    }

    /** The signed-in patient's own appointments, soonest first. */
    public List<AppointmentResponse> forSelf(ClinicPrincipal caller) {
        Patient patient = clinicAccess.patientFor(caller).orElseThrow(() ->
                new ResourceNotFoundException("No patient record for " + describeCaller(caller)));
        return describeAll(appointments.findByPatientId(patient.getId()));
    }

    /**
     * The signed-in patient's own appointments, with the dentist's comment if recorded.
     *
     * <p>Patients may see their own diagnosis ({@link ClinicAccess#canViewClinical}),
     * so this returns the full {@link AppointmentDetailResponse} for every appointment
     * rather than the diagnosis-free {@link AppointmentResponse}. The patient dashboard
     * ({@code patient-home.jsp}) uses this so the dentist's comment is visible once
     * recorded (GAP-DEN-09).</p>
     */
    public List<AppointmentDetailResponse> forSelfDetail(ClinicPrincipal caller) {
        Patient patient = clinicAccess.patientFor(caller).orElseThrow(() ->
                new ResourceNotFoundException("No patient record for " + describeCaller(caller)));
        return appointments.findByPatientId(patient.getId()).stream()
                .sorted(Comparator.comparing(Appointment::getDate)
                        .thenComparing(Appointment::getTime))
                .map(a -> detailWithNotes(caller, a))
                .toList();
    }

    // --- changing state -----------------------------------------------

    /**
     * Record the diagnosis and mark the visit done.
     *
     * <p>The caller must be the dentist this appointment is assigned to. Holding the
     * DENTIST role is not enough, and that was the defect: any dentist could write into
     * any appointment.</p>
     */
    public AppointmentDetailResponse complete(ClinicPrincipal caller, String appointmentNo,
                                              String diagnosis) {
        return complete(caller, appointmentNo, diagnosis, null);
    }

    /**
     * Record the diagnosis and mark the visit done, carrying the dentist-entered
     * price for work with no catalog treatment (GAP-DEN-13).
     *
     * <p>A treatment-less ("Other") visit has nothing to price it from, so the
     * price is required there and must be positive; on a visit naming a treatment
     * the catalog rules and any passed price is ignored.</p>
     */
    public AppointmentDetailResponse complete(ClinicPrincipal caller, String appointmentNo,
                                              String diagnosis, BigDecimal customPrice) {
        AccessControl.require(caller, Action.COMPLETE_TREATMENT);
        Appointment appointment = require(appointmentNo);

        Dentist dentist = clinicAccess.dentistFor(caller).orElseThrow(() ->
                new AccessControl.AccessDeniedException(
                        "Your account is not linked to a dentist record."));
        if (!appointment.isTreatedBy(dentist.getId())) {
            throw new AccessControl.AccessDeniedException(
                    "That appointment is not yours to treat.");
        }
        // GAP-DEN-06: a future visit cannot be completed today. No diagnosis may be
        // written for a consultation that has not happened, and a completed-but-unborn
        // appointment would be billable by reception before the patient has sat down.
        if (appointment.getDate().isAfter(LocalDate.now())) {
            throw new IllegalStateException(
                    appointmentNo + " is scheduled for " + appointment.getDate()
                            + ". Treatment can only be recorded on the day of the appointment.");
        }

        // GAP-DEN-13: treatment-less work carries the dentist's own price, so the
        // visit is billable; a named treatment prices from the catalog instead.
        BigDecimal price = null;
        if (appointment.getTreatmentId() == null) {
            if (customPrice == null) {
                throw new IllegalArgumentException(
                        "Enter the price for this work — there is no listed treatment to price it from.");
            }
            if (customPrice.signum() <= 0) {
                throw new IllegalArgumentException("Price must be more than zero.");
            }
            price = customPrice;
        }

        // Refuses an illegal move - a cancelled or already-billed appointment.
        appointment.complete(diagnosis, price);
        appointments.save(appointment);

        log.log(Level.INFO, "appointment_completed no={0} by={1}",
                new Object[] { appointmentNo, caller.uid() });
        announce(AppointmentEvent.Type.COMPLETED, appointment, caller);
        return detailWithNotes(caller, appointment);
    }

    /**
     * Cancel an appointment and release its slot.
     *
     * <p>Both writes happen in one transaction: an appointment marked CANCELLED whose
     * slot stayed BOOKED would take that time out of the diary permanently.</p>
     *
     * <p>A patient may cancel only their own. A completed or billed appointment cannot
     * be cancelled at all - see {@link Appointment#cancel()}.</p>
     */
    public AppointmentResponse cancel(ClinicPrincipal caller, String appointmentNo) {
        Appointment existing = require(appointmentNo);
        if (caller != null && caller.role() == Role.PATIENT) {
            AccessControl.require(caller, Action.CANCEL_OWN);
            String ownPatientId = clinicAccess.patientFor(caller)
                    .map(Patient::getId).orElse(null);
            if (!existing.belongsTo(ownPatientId)) {
                throw new AccessControl.AccessDeniedException(
                        "You can only cancel your own appointments.");
            }
        } else {
            AccessControl.require(caller, Action.CANCEL_ANY);
        }

        Appointment cancelled = transaction.execute(() -> {
            Appointment appointment = require(appointmentNo);
            appointment.cancel();

            // Release the slot before saving the appointment: appointment_no on slot
            // carries a unique key, so it must be cleared before the time can be
            // claimed again.
            slots.findById(appointment.getSlotId()).ifPresent(slot -> {
                slot.release();
                slots.save(slot);
            });
            appointments.save(appointment);
            return appointment;
        });

        log.log(Level.INFO, "appointment_cancelled no={0} by={1}",
                new Object[] { appointmentNo, caller.uid() });
        announce(AppointmentEvent.Type.CANCELLED, cancelled, caller);
        return describe(cancelled);
    }

    /**
     * Mark an appointment billed.
     *
     * <p>Called by billing once the bill is written, and inside billing's transaction so
     * the two commit together. It lives here rather than in billing because the status is
     * the appointment's own: letting another module load the entity, set a field and save
     * it would put appointment state transitions in two places, which is how the status
     * machine came to be unenforced in the first place.</p>
     *
     * <p>Publishing the BILLED event here also means it is published at all - nothing
     * did.</p>
     */
    public void markBilled(ClinicPrincipal caller, String appointmentNo) {
        AccessControl.require(caller, Action.ISSUE_BILL);
        Appointment appointment = require(appointmentNo);
        appointment.markBilled();
        appointments.save(appointment);
        announce(AppointmentEvent.Type.BILLED, appointment, caller);
    }

    /** The appointment behind a number, for billing. Package-visible to the module. */
    public Appointment require(String appointmentNo) {
        return appointments.findById(appointmentNo).orElseThrow(() ->
                new ResourceNotFoundException("Appointment not found: " + appointmentNo));
    }

    // --- helpers ------------------------------------------------------

    /**
     * @return the appointment, if this caller may read it at all
     *
     * <p>Three positions, three rules, and none of them is "holds the right role":</p>
     *
     * <ul>
     *   <li>a <b>patient</b> may read their own, and nobody else's;</li>
     *   <li>a <b>dentist</b> may read the ones they are treating. Written first as
     *       {@code require(caller, SEARCH_PATIENTS)} for everyone who is not a patient,
     *       which no dentist holds - so no dentist could read any appointment at all.
     *       Granting them {@code SEARCH_PATIENTS} would have been the wrong repair: it
     *       would also let them list the whole patient register;</li>
     *   <li><b>reception and the administrator</b> need {@code SEARCH_PATIENTS}, which is
     *       what running the diary means.</li>
     * </ul>
     */
    private Appointment readable(ClinicPrincipal caller, String appointmentNo) {
        Appointment appointment = require(appointmentNo);
        if (caller == null) {
            throw new AccessControl.NotAuthenticatedException("Authentication is required.");
        }
        switch (caller.role()) {
            case PATIENT -> {
                String ownPatientId = clinicAccess.patientFor(caller).map(Patient::getId).orElse(null);
                if (!appointment.belongsTo(ownPatientId)) {
                    throw new AccessControl.AccessDeniedException("That appointment is not yours.");
                }
            }
            case DENTIST -> {
                String ownDentistId = clinicAccess.dentistFor(caller).map(Dentist::getId).orElse(null);
                if (!appointment.isTreatedBy(ownDentistId)) {
                    throw new AccessControl.AccessDeniedException(
                            "That appointment is not one of yours.");
                }
            }
            case RECEPTIONIST, ADMIN -> AccessControl.require(caller, Action.SEARCH_PATIENTS);
        }
        return appointment;
    }

    private String resolvePatient(ClinicPrincipal caller, String requestedPatientId) {
        if (caller.role() == Role.PATIENT) {
            // Resolved from the account, never from the request, so a patient cannot
            // book in someone else's name by sending their id.
            return clinicAccess.patientFor(caller).map(Patient::getId).orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Your account has no patient record. Ask the clinic to add one."));
        }
        if (requestedPatientId == null || requestedPatientId.isBlank()) {
            throw new IllegalArgumentException(
                    "patientId is required when booking on behalf of a patient");
        }
        return requestedPatientId.trim();
    }

    private void announce(AppointmentEvent.Type type, Appointment appointment, ClinicPrincipal actor) {
        Optional<Patient> patient = patientRecord(appointment);
        publisher.publish(AppointmentEvent.of(type, appointment, actor,
                patient.map(Patient::getEmail).orElse(null),
                patient.map(Patient::getName).orElse("Patient")));
    }

    /**
     * The clinical view, with the patient's declared notes attached - FR-NOTE-07.
     *
     * <p>The same gate guards both: {@code canViewPatientNotes} is
     * {@code canViewClinical}, so a caller who may read the diagnosis may read the notes and
     * one who may not gets neither. Asked through {@link PatientNoteService} rather than
     * decided here, so the rule lives in one place.</p>
     */
    private AppointmentDetailResponse detailWithNotes(ClinicPrincipal caller, Appointment appointment) {
        List<PatientNoteResponse> notes = List.of();
        if (clinicAccess.canViewPatientNotes(appointment, caller)) {
            notes = patientNotes.forPatient(caller, appointment.getPatientId());
        }
        return AppointmentDetailResponse.of(appointment, patientName(appointment),
                dentistName(appointment), treatmentName(appointment), notes);
    }

    /**
     * A dentist's day, each appointment flagged if the patient has declared anything
     * critical - FR-NOTE-08.
     *
     * <p>A flag, not the notes: the schedule warns, and opening the appointment shows what
     * the warning is about. Loading every patient's notes to render a day would read a great
     * deal of medical information to print one exclamation mark.</p>
     */
    public List<DentistDay> forDentistWithWarnings(ClinicPrincipal caller, LocalDate date) {
        return forDentistOn(caller, date).stream()
                .map(appointment -> new DentistDay(appointment,
                        patientNotes.hasCriticalNotes(caller, appointment.patientId()),
                        treatmentCost(appointment.treatmentId())))
                .toList();
    }

    /**
     * The dentist's week ahead, grouped by day - FR-DEN-15.
     *
     * <p>The day view answers "what is happening today"; it cannot answer "what is
     * coming", because a booking made for any other day simply does not appear until
     * that day is picked. This returns today and the days after it as separate groups,
     * each flagged exactly as {@link #forDentistWithWarnings} flags a single day, so
     * the schedule can show the whole week at once. Days with nothing booked are
     * included, so the screen can say so instead of skipping them silently.</p>
     */
    public List<WeekDay> forDentistWeekWithWarnings(ClinicPrincipal caller, LocalDate from,
                                                    LocalDate to) {
        Dentist dentist = clinicAccess.dentistFor(caller).orElseThrow(() ->
                new ResourceNotFoundException("No dentist record for " + describeCaller(caller)));
        Map<LocalDate, List<DentistDay>> byDay = new TreeMap<>();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            byDay.put(day, new ArrayList<>());
        }
        appointments.findByDentistIdAndDateBetween(dentist.getId(), from, to).forEach(appointment ->
                byDay.get(appointment.getDate()).add(new DentistDay(describe(appointment),
                        patientNotes.hasCriticalNotes(caller, appointment.getPatientId()),
                        treatmentCost(appointment.getTreatmentId()))));
        return byDay.entrySet().stream()
                .map(entry -> new WeekDay(entry.getKey(), List.copyOf(entry.getValue())))
                .toList();
    }

    /** One day of a dentist's week: the date and its flagged appointments, possibly none. */
    public record WeekDay(LocalDate date, List<DentistDay> appointments) {
    }

    /** One row of a dentist's day: the appointment, whether to warn about it, and the treatment cost. */
    public record DentistDay(AppointmentResponse appointment, boolean hasCriticalNotes,
                             BigDecimal treatmentCost) {
    }

    private List<AppointmentResponse> describeAll(List<Appointment> found) {
        return found.stream()
                .sorted(Comparator.comparing(Appointment::getDate)
                        .thenComparing(Appointment::getTime))
                .map(this::describe)
                .toList();
    }

    private AppointmentResponse describe(Appointment appointment) {
        return AppointmentResponse.of(appointment,
                patientName(appointment), dentistName(appointment), treatmentName(appointment));
    }

    private Optional<Patient> patientRecord(Appointment appointment) {
        return clinicAccess.patientById(appointment.getPatientId());
    }

    private String patientName(Appointment appointment) {
        return patientRecord(appointment).map(Patient::getName).orElse(null);
    }

    private String dentistName(Appointment appointment) {
        return reference.requireDentist(appointment.getDentistId()).getName();
    }

    private String treatmentName(Appointment appointment) {
        return appointment.getTreatmentId() == null ? null
                : reference.requireTreatment(appointment.getTreatmentId()).getName();
    }

    private BigDecimal treatmentCost(String treatmentId) {
        return treatmentId == null ? null
                : reference.requireTreatment(treatmentId).getBaseCost();
    }

    private static String describeCaller(ClinicPrincipal caller) {
        return caller == null ? "an anonymous caller" : caller.uid();
    }
}
