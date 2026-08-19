package com.sunrise.clinic.service;

import com.sunrise.clinic.domain.Patient;
import com.sunrise.clinic.domain.Role;
import com.sunrise.clinic.domain.Slot;
import com.sunrise.clinic.domain.SlotStatus;
import com.sunrise.clinic.exception.SlotUnavailableException;
import com.sunrise.clinic.pattern.AppointmentNumberGenerator;
import com.sunrise.clinic.repository.inmemory.InMemoryAppointmentRepository;
import com.sunrise.clinic.repository.inmemory.InMemoryPatientRepository;
import com.sunrise.clinic.repository.inmemory.InMemorySlotRepository;
import com.sunrise.clinic.repository.inmemory.SerialTransactionRunner;
import com.sunrise.clinic.service.notification.AppointmentEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-CLI-B05 — two patients cannot book the same slot.
 *
 * <p>The sequential test proves the check works when one booking follows another.
 * The interesting failure is the one that only appears under load: several requests
 * reading the slot as OPEN at the same moment and all proceeding. This test starts
 * a number of threads, releases them together on a latch so they collide as closely
 * as the scheduler allows, and asserts that exactly one wins.</p>
 *
 * <p>Here the exclusion comes from {@code SerialTransactionRunner}; in the running
 * application it comes from the row lock the JDBC repository takes. The service
 * logic under test is identical either way, which is the point of depending on the
 * interface rather than on JDBC.</p>
 */
class BookingConcurrencyTest {

    private static final int CONTENDING_PATIENTS = 12;

    private InMemorySlotRepository slots;
    private InMemoryAppointmentRepository appointments;
    private AppointmentService service;

    @BeforeEach
    void setUp() {
        AppointmentNumberGenerator.getInstance().reset();
        slots = new InMemorySlotRepository();
        appointments = new InMemoryAppointmentRepository();
        InMemoryPatientRepository patients = new InMemoryPatientRepository();

        slots.save(Slot.builder()
                .id("s1").dentistId("d1")
                .date(LocalDate.of(2026, 7, 20)).startTime(LocalTime.of(16, 0))
                .status(SlotStatus.OPEN).build());

        for (int i = 0; i < CONTENDING_PATIENTS; i++) {
            patients.save(Patient.builder().id("p" + i).name("Patient " + i).build());
        }

        service = new AppointmentService(slots, appointments, patients,
                new AppointmentEventPublisher(List.of()), new SerialTransactionRunner());
    }

    @Test
    void onlyOneOfManySimultaneousBookingsSucceeds() throws InterruptedException {
        AtomicInteger booked = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        AtomicInteger unexpected = new AtomicInteger();

        CountDownLatch startTogether = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(CONTENDING_PATIENTS);
        ExecutorService pool = Executors.newFixedThreadPool(CONTENDING_PATIENTS);

        for (int i = 0; i < CONTENDING_PATIENTS; i++) {
            String patientId = "p" + i;
            pool.execute(() -> {
                try {
                    startTogether.await();
                    service.book(patientId, "s1", "t1", patientId, Role.PATIENT);
                    booked.incrementAndGet();
                } catch (SlotUnavailableException e) {
                    rejected.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (RuntimeException e) {
                    unexpected.incrementAndGet();
                } finally {
                    finished.countDown();
                }
            });
        }

        startTogether.countDown();
        assertTrue(finished.await(10, TimeUnit.SECONDS), "the bookings did not finish in time");
        pool.shutdownNow();

        assertEquals(0, unexpected.get(), "no booking should fail for an unexpected reason");
        assertEquals(1, booked.get(), "exactly one patient should get the slot");
        assertEquals(CONTENDING_PATIENTS - 1, rejected.get(),
                "everyone else should be told the slot is taken");

        assertEquals(1, appointments.count(), "only one appointment may exist for the slot");
        assertEquals(SlotStatus.BOOKED, slots.findById("s1").orElseThrow().getStatus());
    }
}
