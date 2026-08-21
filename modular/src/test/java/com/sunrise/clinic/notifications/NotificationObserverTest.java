package com.sunrise.clinic.notifications;

import com.sunrise.clinic.access.domain.ClinicPrincipal;
import com.sunrise.clinic.appointments.AppointmentTestFixture;
import com.sunrise.clinic.notifications.data.InMemoryNotificationRepository;
import com.sunrise.clinic.notifications.domain.ChannelType;
import com.sunrise.clinic.notifications.domain.DispatchResult;
import com.sunrise.clinic.notifications.domain.Notification;
import com.sunrise.clinic.notifications.domain.NotificationStatus;
import com.sunrise.clinic.notifications.service.EmailChannel;
import com.sunrise.clinic.notifications.service.NotificationChannel;
import com.sunrise.clinic.notifications.service.NotificationChannelFactory;
import com.sunrise.clinic.notifications.service.NotificationObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Day three: booking actually tells the patient, and the attempt is recorded.
 *
 * <p>Driven through the real {@code AppointmentService} rather than by calling the observer
 * directly. The point of an observer is that the service does not know it exists, and a test
 * that invokes it by hand proves the observer works while proving nothing about whether it is
 * wired to anything.</p>
 */
class NotificationObserverTest {

    private AppointmentTestFixture fixture;
    private InMemoryNotificationRepository notifications;
    private ClinicPrincipal nimal;

    /** Builds the fixture with the observer attached, as {@code AppContext} does. */
    private void withChannels(List<NotificationChannel> channels) {
        notifications = new InMemoryNotificationRepository();
        fixture = new AppointmentTestFixture(List.of(new NotificationObserver(
                new NotificationChannelFactory(channels), notifications)));
        nimal = fixture.addPatient("p-nimal", "u-pat1", "Nimal Perera");
        fixture.addSlot("s1", LocalTime.of(9, 30));
    }

    @BeforeEach
    void setUp() {
        withChannels(List.of(new EmailChannel("no-reply@sunrisedental.lk")));
    }

    private String book() {
        return fixture.service.book(nimal, "s1", "t-checkup", null).appointmentNo();
    }

    // --- the confirmation ----------------------------------------------

    @Test
    void bookingRecordsAConfirmation() {
        String no = book();

        List<Notification> sent = notifications.findByAppointmentNo(no);

        assertEquals(1, sent.size());
        assertEquals(ChannelType.EMAIL, sent.get(0).getChannel());
        assertEquals("p-nimal@example.lk", sent.get(0).getRecipient());
        assertNotNull(sent.get(0).getSentAt());
    }

    @Test
    void theConfirmationCarriesTheAppointmentNumberThePatientWillBeAskedFor() {
        String no = book();

        Notification confirmation = notifications.findByAppointmentNo(no).get(0);

        assertTrue(confirmation.getSubject().contains(no), confirmation.getSubject());
        assertTrue(confirmation.getBody().contains(no), confirmation.getBody());
        assertTrue(confirmation.getBody().contains("Nimal Perera"));
    }

    @Test
    void withNoTransportItIsRecordedAsLoggedAndNotAsSent() {
        // The distinction the whole module turns on: "we have no mail server" and "the patient
        // was told" are different facts.
        String no = book();

        assertEquals(NotificationStatus.LOGGED,
                notifications.findByAppointmentNo(no).get(0).getStatus());
        assertFalse(notifications.findByAppointmentNo(no).get(0).getStatus().reachedTheRecipient());
    }

    @Test
    void withARealTransportItIsRecordedAsSent() {
        withChannels(List.of(new NotificationChannel() {
            @Override
            public ChannelType type() {
                return ChannelType.EMAIL;
            }

            @Override
            public DispatchResult send(String recipient, String subject, String body) {
                return DispatchResult.sent(ChannelType.EMAIL, "accepted by the relay");
            }
        }));

        String no = book();

        assertEquals(NotificationStatus.SENT,
                notifications.findByAppointmentNo(no).get(0).getStatus());
    }

    // --- FR-NOT-04: it must never break the booking ---------------------

    @Test
    void aMailServerThatIsDownDoesNotFailTheBooking() {
        // The appointment is already committed by the time this runs. Reporting a failure would
        // send the patient back to book a second one.
        withChannels(List.of(new NotificationChannel() {
            @Override
            public ChannelType type() {
                return ChannelType.EMAIL;
            }

            @Override
            public DispatchResult send(String recipient, String subject, String body) {
                throw new IllegalStateException("connection refused");
            }
        }));

        String no = book();

        assertNotNull(no, "the booking must succeed");
        assertEquals(1, fixture.appointments.count());
        // And the failure is on the record, so somebody can find out afterwards.
        assertEquals(NotificationStatus.FAILED,
                notifications.findByAppointmentNo(no).get(0).getStatus());
    }

    @Test
    void aDeploymentWithNoEmailChannelStillBooks() {
        // find(), not create(). create() throws, and that would produce a warning about a
        // wiring mistake on every single booking.
        withChannels(List.of());

        String no = book();

        assertNotNull(no);
        assertEquals(0, notifications.count(), "nothing to record if there was nothing to try");
    }

    @Test
    void aPatientWithNoEmailIsRecordedAsAFailureRatherThanSkipped() {
        // Registering without an email is allowed. The row is how anybody later answers "why
        // was this patient not told" — because there was nowhere to send it.
        notifications = new InMemoryNotificationRepository();
        fixture = new AppointmentTestFixture(List.of(new NotificationObserver(
                new NotificationChannelFactory(List.of(new EmailChannel("no-reply@sunrisedental.lk"))),
                notifications)));
        fixture.patients.save(com.sunrise.clinic.patients.domain.Patient.builder()
                .id("p-noemail").userUid("u-noemail").name("No Email").build());
        ClinicPrincipal patient = new ClinicPrincipal("u-noemail", "No Email",
                com.sunrise.clinic.access.domain.Role.PATIENT);
        fixture.addSlot("s2", LocalTime.of(10, 0));

        String no = fixture.service.book(patient, "s2", "t-checkup", null).appointmentNo();

        assertEquals(NotificationStatus.FAILED,
                notifications.findByAppointmentNo(no).get(0).getStatus());
    }

    // --- cancelling ----------------------------------------------------

    @Test
    void cancellingTellsThePatientToo() {
        // Not in any requirement. A patient not told their appointment was called off will turn
        // up, which is worse than a missing confirmation.
        String no = book();
        fixture.service.cancel(AppointmentTestFixture.reception(), no);

        List<Notification> about = notifications.findByAppointmentNo(no);

        assertEquals(2, about.size());
        assertTrue(about.stream().anyMatch(n -> n.getSubject().contains("cancelled")),
                about.stream().map(Notification::getSubject).toList().toString());
    }

    @Test
    void treatingAndBillingSayNothing() {
        // A message telling somebody what they have just watched happen trains them to ignore
        // the ones that matter.
        String no = book();
        fixture.service.complete(AppointmentTestFixture.silva(), no, "Scaling done");

        assertEquals(1, notifications.findByAppointmentNo(no).size(),
                "only the confirmation");
    }

    @Test
    void everyAttemptAboutOneAppointmentIsKept() {
        String no = book();
        fixture.service.cancel(nimal, no);

        assertEquals(2, notifications.findByAppointmentNo(no).size(),
                "the sequence is what somebody needs when a patient says they were never told");
    }
}
