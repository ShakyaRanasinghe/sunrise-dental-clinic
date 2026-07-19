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
import com.sunrise.clinic.service.notification.AppointmentEvent;
import com.sunrise.clinic.service.notification.AppointmentEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Core appointment workflow: book, cancel, complete.
 *
 * <p>{@link #book} is the heart of the system. It (1) checks the slot is OPEN,
 * (2) mints an appointment number via the Singleton generator, (3) flips the
 * slot to BOOKED and saves the appointment, then (4) publishes an event so the
 * Observer(s) send a confirmation and write the audit trail.</p>
 *
 * <p>The method is {@code synchronized} so the check-then-book is atomic in the
 * in-memory demo — two patients cannot grab the same slot. The Firestore
 * adapter achieves the same with {@code runTransaction} on the slot document.</p>
 */
@Service
public class AppointmentService {

    private final SlotRepository slots;
    private final AppointmentRepository appointments;
    private final PatientRepository patients;
    private final AppointmentEventPublisher publisher;

    public AppointmentService(SlotRepository slots,
                              AppointmentRepository appointments,
                              PatientRepository patients,
                              AppointmentEventPublisher publisher) {
        this.slots = slots;
        this.appointments = appointments;
        this.patients = patients;
        this.publisher = publisher;
    }

    public synchronized Appointment book(String patientId, String slotId, String treatmentId,
                                         String createdByUid, Role createdByRole) {
        Slot slot = slots.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found: " + slotId));

        // --- double-booking guard (atomic within this synchronized block) ---
        if (slot.getStatus() != SlotStatus.OPEN) {
            throw new SlotUnavailableException("Slot already booked: " + slotId);
        }

        String appointmentNo = AppointmentNumberGenerator.getInstance().next(slot.getDate());

        slot.setStatus(SlotStatus.BOOKED);
        slot.setAppointmentNo(appointmentNo);
        slots.save(slot);

        Appointment appointment = Appointment.builder()
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
        appointments.save(appointment);

        // --- notify + audit via the Observer pattern (best-effort) ---
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

    public Appointment cancel(String appointmentNo) {
        Appointment appointment = findByNo(appointmentNo);
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointments.save(appointment);
        // release the slot
        slots.findById(appointment.getSlotId()).ifPresent(slot -> {
            slot.setStatus(SlotStatus.OPEN);
            slot.setAppointmentNo(null);
            slots.save(slot);
        });
        return appointment;
    }
}
