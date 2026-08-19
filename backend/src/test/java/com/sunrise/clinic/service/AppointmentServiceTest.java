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
import com.sunrise.clinic.repository.inmemory.InMemoryAppointmentRepository;
import com.sunrise.clinic.repository.inmemory.InMemoryPatientRepository;
import com.sunrise.clinic.repository.inmemory.InMemorySlotRepository;
import com.sunrise.clinic.repository.inmemory.SerialTransactionRunner;
import com.sunrise.clinic.service.notification.AppointmentEvent;
import com.sunrise.clinic.service.notification.AppointmentEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-CLI-B01..B04 — booking flow: success, double-booking guard, cancel, not-found.
 */
class AppointmentServiceTest {

    private InMemorySlotRepository slots;
    private InMemoryAppointmentRepository appointments;
    private InMemoryPatientRepository patients;
    private List<AppointmentEvent> capturedEvents;
    private AppointmentService service;

    @BeforeEach
    void setUp() {
        AppointmentNumberGenerator.getInstance().reset();
        slots = new InMemorySlotRepository();
        appointments = new InMemoryAppointmentRepository();
        patients = new InMemoryPatientRepository();
        capturedEvents = new ArrayList<>();
        // Observer that just captures events, so we can assert one was published.
        AppointmentEventPublisher publisher = new AppointmentEventPublisher(List.of(capturedEvents::add));

        slots.save(Slot.builder()
                .id("s1").dentistId("d1")
                .date(LocalDate.of(2026, 7, 20)).startTime(LocalTime.of(16, 0))
                .status(SlotStatus.OPEN).build());
        patients.save(Patient.builder().id("p1").name("Nimal").email("nimal@example.lk").build());

        service = new AppointmentService(
                slots, appointments, patients, publisher, new SerialTransactionRunner());
    }

    @Test
    void bookConfirmsAppointmentAndMarksSlotBooked() {
        Appointment a = service.book("p1", "s1", "t1", "p1", Role.PATIENT);

        assertEquals(AppointmentStatus.CONFIRMED, a.getStatus());
        assertTrue(a.getAppointmentNo().startsWith("APT-"));
        assertEquals(SlotStatus.BOOKED, slots.findById("s1").orElseThrow().getStatus());
        assertEquals(1, capturedEvents.size(), "an AppointmentCreated event must be published");
        assertEquals(AppointmentEvent.Type.CREATED, capturedEvents.get(0).type());
    }

    @Test
    void secondBookingOfSameSlotIsRejected() {
        service.book("p1", "s1", "t1", "p1", Role.PATIENT);
        assertThrows(SlotUnavailableException.class,
                () -> service.book("p1", "s1", "t1", "p1", Role.PATIENT));
    }

    @Test
    void cancelReleasesTheSlot() {
        Appointment a = service.book("p1", "s1", "t1", "p1", Role.PATIENT);
        service.cancel(a.getAppointmentNo());
        assertEquals(SlotStatus.OPEN, slots.findById("s1").orElseThrow().getStatus());
    }

    @Test
    void bookingAMissingSlotThrowsNotFound() {
        assertThrows(ResourceNotFoundException.class,
                () -> service.book("p1", "missing", "t1", "p1", Role.PATIENT));
    }
}
