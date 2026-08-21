package com.sunrise.clinic.notifications;

import com.sunrise.clinic.notifications.data.InMemoryNotificationRepository;
import com.sunrise.clinic.notifications.domain.ChannelType;
import com.sunrise.clinic.notifications.domain.Notification;
import com.sunrise.clinic.notifications.domain.NotificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The dispatch record — day one of the notifications module.
 *
 * <p>Nothing sends anything yet: the channels and the observer follow. What is testable now is
 * the record itself and the one distinction that matters in it, between a message that reached
 * somebody and one that was only written down.</p>
 */
class NotificationRecordTest {

    private InMemoryNotificationRepository notifications;

    @BeforeEach
    void setUp() {
        notifications = new InMemoryNotificationRepository();
    }

    private Notification attempt(String id, ChannelType channel, NotificationStatus status) {
        return Notification.builder()
                .id(id)
                .appointmentNo("APT-20260821-0001")
                .channel(channel)
                .recipient("nimal@example.lk")
                .subject("Your appointment is confirmed")
                .body("Dear Nimal, your appointment with Dr. Ranil Silva is confirmed.")
                .status(status)
                .sentAt(Instant.now())
                .build();
    }

    @Test
    void anAttemptIsRecordedWhateverTheOutcome() {
        // FR-NOT-03. A message that failed leaves a row saying so, because the question asked
        // later is "was the patient told", and silence is not an answer to it.
        notifications.save(attempt("n-1", ChannelType.EMAIL, NotificationStatus.SENT));
        notifications.save(attempt("n-2", ChannelType.SMS, NotificationStatus.FAILED));
        notifications.save(attempt("n-3", ChannelType.EMAIL, NotificationStatus.LOGGED));

        assertEquals(3, notifications.count());
    }

    @Test
    void onlySentMeansThePatientWasTold() {
        // LOGGED is a distinct value rather than folded into SENT, because "we have no mail
        // server" and "the patient was told" are different facts.
        assertTrue(NotificationStatus.SENT.reachedTheRecipient());
        assertFalse(NotificationStatus.FAILED.reachedTheRecipient());
        assertFalse(NotificationStatus.LOGGED.reachedTheRecipient());
    }

    @Test
    void everyAttemptAboutOneAppointmentIsKept() {
        // Not just the latest. "We sent a confirmation and then a reminder, and the reminder
        // failed" is the sequence somebody needs when a patient says they were never told.
        notifications.save(attempt("n-1", ChannelType.EMAIL, NotificationStatus.SENT));
        notifications.save(attempt("n-2", ChannelType.SMS, NotificationStatus.FAILED));

        List<Notification> about = notifications.findByAppointmentNo("APT-20260821-0001");

        assertEquals(2, about.size());
        assertEquals(1, about.stream().filter(n -> n.getStatus() == NotificationStatus.FAILED).count());
    }

    @Test
    void anAppointmentWithNoAttemptsReturnsEmptyRatherThanFailing() {
        assertEquals(List.of(), notifications.findByAppointmentNo("APT-20260821-9999"));
    }

    @Test
    void theRecordKeepsTheWordsThatWereSent() {
        // What makes the record worth keeping: "a reminder went out" is far less useful than
        // the words the patient received, when they turn up on the wrong day.
        notifications.save(attempt("n-1", ChannelType.EMAIL, NotificationStatus.SENT));

        assertTrue(notifications.findById("n-1").orElseThrow().getBody().contains("Dr. Ranil Silva"));
    }

    @Test
    void theBodyNeverAppearsInToString() {
        // It ends up in logs and exception messages, and it names a patient and their
        // appointment. Same rule as PatientNote and Complaint.
        String described = attempt("n-1", ChannelType.EMAIL, NotificationStatus.SENT).toString();

        assertFalse(described.contains("Nimal"), described);
        assertFalse(described.contains("Dr. Ranil Silva"), described);
    }

    @Test
    void bothChannelsAreAvailableToTheFactoryThatFollows() {
        // FR-NOT-05. A third channel is one new implementation and one line in the factory.
        assertEquals(2, ChannelType.values().length);
    }
}
