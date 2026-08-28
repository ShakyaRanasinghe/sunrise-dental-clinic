package com.sunrise.clinic.appointments;

import com.sunrise.clinic.access.domain.ClinicPrincipal;
import com.sunrise.clinic.platform.error.SlotUnavailableException;
import com.sunrise.clinic.scheduling.domain.SlotStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-CLI-B05 - two patients cannot book the same slot.
 *
 * <p>{@link AppointmentServiceTest#bookingATakenSlotIsRefused()} proves the check works
 * when one booking follows another. The interesting failure is the one that only appears
 * under load: several requests reading the slot as OPEN in the same instant and all
 * proceeding. This starts a dozen threads, releases them together on a latch so they
 * collide as closely as the scheduler allows, and asserts exactly one wins.</p>
 *
 * <p>Here the exclusion comes from {@code SerialTransactionRunner}; in the running
 * application it comes from the row lock {@code findByIdForUpdate} takes. The service
 * logic under test is identical either way, which is the point of depending on the
 * repository interface rather than on JDBC.</p>
 */
class BookingConcurrencyTest {

    private static final int CONTENDING_PATIENTS = 12;

    private AppointmentTestFixture fixture;
    private List<ClinicPrincipal> patients;

    @BeforeEach
    void setUp() {
        fixture = new AppointmentTestFixture();
        patients = new ArrayList<>();
        for (int i = 0; i < CONTENDING_PATIENTS; i++) {
            patients.add(fixture.addPatient("p" + i, "u" + i, "Patient " + i));
        }
        fixture.addSlot("s1", LocalTime.of(16, 0));
    }

    @Test
    void onlyOneOfManySimultaneousBookingsSucceeds() throws InterruptedException {
        AtomicInteger booked = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        List<Throwable> unexpected = java.util.Collections.synchronizedList(new ArrayList<>());

        CountDownLatch startTogether = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(CONTENDING_PATIENTS);
        ExecutorService pool = Executors.newFixedThreadPool(CONTENDING_PATIENTS);

        for (ClinicPrincipal patient : patients) {
            pool.execute(() -> {
                try {
                    startTogether.await();
                    fixture.service.book(patient, "s1", "t-checkup", null);
                    booked.incrementAndGet();
                } catch (SlotUnavailableException e) {
                    rejected.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (RuntimeException e) {
                    // Collected rather than counted, so a failure names its cause.
                    unexpected.add(e);
                } finally {
                    finished.countDown();
                }
            });
        }

        startTogether.countDown();
        assertTrue(finished.await(10, TimeUnit.SECONDS), "the bookings did not finish in time");
        pool.shutdownNow();

        assertEquals(List.of(), unexpected.stream().map(Throwable::toString).toList(),
                "no booking should fail for an unexpected reason");
        assertEquals(1, booked.get(), "exactly one patient should get the slot");
        assertEquals(CONTENDING_PATIENTS - 1, rejected.get(),
                "everyone else should be told the time is taken");

        assertEquals(1, fixture.appointments.count(), "only one appointment may exist for the slot");
        assertEquals(SlotStatus.BOOKED, fixture.slots.findById("s1").orElseThrow().getStatus());
    }

    @Test
    void concurrentBookingsOfDifferentSlotsAllSucceedWithDistinctNumbers() {
        // The other half of the guarantee: the lock must not serialise bookings that do
        // not contend, and the number generator must not hand out a duplicate when it is
        // called from several threads at once - which is what the in-memory counter it
        // replaced could not promise across a restart.
        for (int i = 0; i < CONTENDING_PATIENTS; i++) {
            fixture.addSlot("slot-" + i, LocalTime.of(9, 0).plusMinutes(30L * i));
        }

        List<String> numbers = patients.parallelStream()
                .map(patient -> fixture.service
                        .book(patient, "slot-" + patients.indexOf(patient), "t-checkup", null)
                        .appointmentNo())
                .toList();

        assertEquals(CONTENDING_PATIENTS, numbers.size());
        assertEquals(CONTENDING_PATIENTS, Set.copyOf(numbers).size(),
                "every appointment number must be distinct");
        numbers.forEach(no -> assertNotNull(no));
    }

    private interface Set {
        static <T> java.util.Set<T> copyOf(List<T> items) {
            return java.util.Set.copyOf(items);
        }
    }
}
