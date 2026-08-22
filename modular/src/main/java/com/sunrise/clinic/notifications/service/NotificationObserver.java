package com.sunrise.clinic.notifications.service;

import com.sunrise.clinic.appointments.service.AppointmentEvent;
import com.sunrise.clinic.appointments.service.AppointmentObserver;
import com.sunrise.clinic.notifications.data.NotificationRepository;
import com.sunrise.clinic.notifications.domain.ChannelType;
import com.sunrise.clinic.notifications.domain.DispatchResult;
import com.sunrise.clinic.notifications.domain.Notification;
import com.sunrise.clinic.notifications.domain.NotificationKind;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tells the patient when something happens to their appointment, and records that it did —
 * FR-NOT-01, FR-NOT-03, FR-PAT-16.
 *
 * <p>An {@link AppointmentObserver}, so {@code AppointmentService} does not know it exists. It is
 * registered in {@code AppContext} beside {@code AuditObserver} and called after the booking
 * transaction has committed — which is the only safe moment: a patient told about a booking that
 * was then rolled back is worse than one told about nothing.</p>
 *
 * <h2>Which events, and why not all of them</h2>
 *
 * <ul>
 *   <li><b>CREATED</b> — the confirmation FR-NOT-01 asks for, carrying the appointment number
 *       the patient will be asked to quote.</li>
 *   <li><b>CANCELLED</b> — not in any requirement, and included anyway. A patient whose
 *       appointment is called off by the clinic and not told will turn up; that is a worse
 *       outcome than a missing confirmation, and the machinery is already here.</li>
 *   <li><b>COMPLETED</b> and <b>BILLED</b> — nothing sent. The patient was standing there for
 *       one and receives a receipt for the other, and a message telling somebody what they have
 *       just watched happen trains them to ignore the ones that matter.</li>
 * </ul>
 */
public class NotificationObserver implements AppointmentObserver {

    private static final Logger log = Logger.getLogger(NotificationObserver.class.getName());

    private final NotificationChannelFactory channels;
    private final NotificationRepository notifications;

    public NotificationObserver(NotificationChannelFactory channels,
                                NotificationRepository notifications) {
        this.channels = channels;
        this.notifications = notifications;
    }

    @Override
    public void onEvent(AppointmentEvent event) {
        Message message = messageFor(event);
        if (message == null) {
            return;
        }

        // find, not create: a deployment with no email channel configured should send nothing,
        // not fail the booking that triggered this. create() throws, and throwing here would
        // land inside the publisher's catch and produce a warning about a wiring mistake on
        // every single booking.
        Optional<NotificationChannel> channel = channels.find(ChannelType.EMAIL);
        if (channel.isEmpty()) {
            log.log(Level.FINE, "no_email_channel appointment={0}", message.appointmentNo());
            return;
        }

        // dispatch, not send: the never-throw guarantee lives on the interface.
        DispatchResult result = channel.get()
                .dispatch(event.recipientEmail(), message.subject(), message.body());

        // The status comes straight from the result. This used to be a private toStatus() that
        // rebuilt it from a boolean and a reason string - which is the reinterpretation that
        // DispatchResult carrying a NotificationStatus removed.
        notifications.save(Notification.builder()
                .id(UUID.randomUUID().toString())
                .appointmentNo(message.appointmentNo())
                .channel(ChannelType.EMAIL)
                .kind(message.kind())
                .recipient(event.recipientEmail())
                .subject(message.subject())
                .body(message.body())
                .status(result.status())
                .sentAt(Instant.now())
                .build());

        log.log(Level.INFO, "notification_recorded appointment={0} channel=EMAIL status={1}",
                new Object[] { message.appointmentNo(), result.status() });
    }

    /** What to say, or null for an event the patient does not need telling about. */
    private static Message messageFor(AppointmentEvent event) {
        String no = event.appointment().getAppointmentNo();
        String name = event.recipientName() == null ? "Patient" : event.recipientName();
        return switch (event.type()) {
            case CREATED -> new Message(no, NotificationKind.CONFIRMATION,
                    "Your appointment is confirmed — " + no,
                    "Dear " + name + ", your appointment on " + event.appointment().getDate()
                            + " at " + event.appointment().getTime() + " is confirmed."
                            + " Please quote " + no + " if you contact us."
                            + " — Sunrise Dental Clinic");
            case CANCELLED -> new Message(no, NotificationKind.CANCELLATION,
                    "Your appointment has been cancelled — " + no,
                    "Dear " + name + ", your appointment on " + event.appointment().getDate()
                            + " at " + event.appointment().getTime() + " has been cancelled."
                            + " Please contact us to book another time."
                            + " — Sunrise Dental Clinic");
            case COMPLETED, BILLED -> null;
        };
    }

    private record Message(String appointmentNo, NotificationKind kind, String subject,
                           String body) {
    }
}
