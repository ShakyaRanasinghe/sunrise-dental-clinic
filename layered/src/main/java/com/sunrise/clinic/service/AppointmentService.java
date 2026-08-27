package com.sunrise.clinic.service;

import com.sunrise.clinic.domain.Appointment;
import com.sunrise.clinic.domain.AppointmentStatus;
import com.sunrise.clinic.domain.Patient;
import com.sunrise.clinic.domain.Role;
import com.sunrise.clinic.domain.Slot;
import com.sunrise.clinic.domain.SlotStatus;
import com.sunrise.clinic.exception.ResourceNotFoundException;
import com.sunrise.clinic.exception.SlotUnavailableException;
import com.sunrise.clinic.pattern.AppointmentNumberGenerator;
import com.sunrise.clinic.repository.AppointmentRepository;
import com.sunrise.clinic.repository.PatientRepository;
import com.sunrise.clinic.repository.SlotRepository;
import com.sunrise.clinic.repository.TransactionRunner;
import com.sunrise.clinic.service.notification.AppointmentEvent;
import com.sunrise.clinic.service.notification.AppointmentEventPublisher;

import java.time.Instant;

/**
 * Core appointment workflow: book, cancel, complete.
 *
 * <p>{@link #book} is the heart of the system. It (1) locks and checks the slot is
 * OPEN, (2) mints an appointment number via the Singleton generator, (3) flips the
 * slot to BOOKED and saves the appointment, then (4) publishes an event so the
 * Observer(s) send a confirmation and write the audit trail.</p>
 *
 * <p><b>Preventing double-booking.</b> Steps 1–3 run inside a
 * {@link TransactionRunner}, and the slot is read with
 * {@link SlotRepository#findByIdForUpdate} rather than an ordinary lookup. Without
 * the lock two patients clicking the same slot at the same moment could both read
 * it as OPEN and both proceed; with it, the second request waits, then sees BOOKED
 * and is correctly rejected with {@link SlotUnavailableException}. The unique key
 * on {@code slot.appointment_no} is the final backstop in the database itself.</p>
 *
 * <p>The event is published <em>after</em> the transaction commits, so a patient is
 * never sent a confirmation for a booking that was subsequently rolled back.</p>
 */
public class AppointmentService {

    private final SlotRepository slots;
    private final AppointmentRepository appointments;
    private final PatientRepository patients;
    private final AppointmentEventPublisher publisher;
    private final TransactionRunner transaction;

    public AppointmentService(SlotRepository slots,
                              AppointmentRepository appointments,
                              PatientRepository patients,
                              AppointmentEventPublisher publisher,
                              TransactionRunner transaction) {
        this.slots = slots;
        this.appointments = appointments;
        this.patients = patients;
        this.publisher = publisher;
        this.transaction = transaction;
    }

    public Appointment book(String patientId, String slotId, String treatmentId,
                            String createdByUid, Role createdByRole) {

        Appointment appointment = transaction.execute(() -> {
            // Locking read — see the class javadoc. Any concurrent booking of this
            // same slot is held here until this transaction finishes.
            Slot slot = slots.findByIdForUpdate(slotId)
                    .orElseThrow(() -> new ResourceNotFoundException("Slot not found: " + slotId));

            if (slot.getStatus() != SlotStatus.OPEN) {
                throw new SlotUnavailableException("Slot already booked: " + slotId);
            }

            String appointmentNo = AppointmentNumberGenerator.getInstance().next(slot.getDate());

            Appointment booked = Appointment.builder()
                    .appointmentNo(appointmentNo)
                    .patientId(patientId)
                    .dentistId(slot.getDentistId())
                    .slotId(slotId)
                    .treatmentId(treatmentId)
                    .date(slot.getDate())
                    .time(slot.getStartTime())
                    .status(AppointmentStatus.CONFIRMED)
                    .createdByUid(createdByUid)
                    .createdByRole(createdByRole)
                    .createdAt(Instant.now())
                    .build();
            // The appointment row is written before the slot points at it, so the
            // foreign key from slot.appointment_no is always satisfied.
            appointments.save(booked);

            slot.setStatus(SlotStatus.BOOKED);
            slot.setAppointmentNo(appointmentNo);
            slots.save(slot);

            return booked;
        });

        // --- notify + audit via the Observer pattern (best-effort, post-commit) ---
        Patient patient = patients.findById(patientId).orElse(null);
        publisher.publish(new AppointmentEvent(
                AppointmentEvent.Type.CREATED,
                appointment,
                patient != null ? patient.getEmail() : null,
                patient != null ? patient.getName() : "Patient"));

        return appointment;
    }

    public Appointment findByNo(String appointmentNo) {
        return appointments.findById(appointmentNo)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + appointmentNo));
    }

    public Appointment complete(String appointmentNo, String diagnosis) {
        Appointment appointment = findByNo(appointmentNo);
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointment.setDiagnosis(diagnosis);
        appointments.save(appointment);
        return appointment;
    }

    /**
     * Cancel an appointment and release its slot back to the pool.
     *
     * <p>Both writes happen in one transaction: an appointment marked CANCELLED
     * while its slot stayed BOOKED would take that time out of the diary for good.</p>
     */
    public Appointment cancel(String appointmentNo) {
        return transaction.execute(() -> {
            Appointment appointment = findByNo(appointmentNo);
            appointment.setStatus(AppointmentStatus.CANCELLED);

            // Clear the slot's back-reference first: it is a unique key, so it must
            // be released before another booking can claim the slot.
            slots.findById(appointment.getSlotId()).ifPresent(slot -> {
                slot.setStatus(SlotStatus.OPEN);
                slot.setAppointmentNo(null);
                slots.save(slot);
            });

            appointments.save(appointment);
            return appointment;
        });
    }
}
