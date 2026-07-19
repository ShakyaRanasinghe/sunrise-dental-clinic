package com.sunrise.clinic.service.notification;

import com.sunrise.clinic.domain.ChannelType;
import com.sunrise.clinic.domain.Notification;
import com.sunrise.clinic.domain.NotificationStatus;
import com.sunrise.clinic.pattern.factory.DispatchResult;
import com.sunrise.clinic.pattern.factory.NotificationChannel;
import com.sunrise.clinic.pattern.factory.NotificationChannelFactory;
import com.sunrise.clinic.repository.NotificationRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Observer that sends the patient a confirmation (via the Factory-created email
 * channel) and records the delivery when an appointment is created.
 */
@Component
public class NotificationObserver implements AppointmentObserver {

    private final NotificationChannelFactory channelFactory;
    private final NotificationRepository notifications;

    public NotificationObserver(NotificationChannelFactory channelFactory,
                                NotificationRepository notifications) {
        this.channelFactory = channelFactory;
        this.notifications = notifications;
    }

    @Override
    public void onEvent(AppointmentEvent event) {
        if (event.type() != AppointmentEvent.Type.CREATED || event.recipientEmail() == null) {
            return;
        }
        String appointmentNo = event.appointment().getAppointmentNo();
        String subject = "Appointment confirmed — " + appointmentNo;
        String body = String.format(
                "Dear %s, your appointment %s on %s at %s is confirmed. — Sunrise Dental Clinic",
                event.recipientName(), appointmentNo,
                event.appointment().getDate(), event.appointment().getTime());

        NotificationChannel channel = channelFactory.create(ChannelType.EMAIL);
        DispatchResult result = channel.send(event.recipientEmail(), subject, body);

        notifications.save(Notification.builder()
                .id(UUID.randomUUID().toString())
                .appointmentNo(appointmentNo)
                .channel(ChannelType.EMAIL)
                .recipient(event.recipientEmail())
                .subject(subject)
                .body(body)
                .status(toStatus(result))
                .sentAt(Instant.now())
                .build());
    }

    private static NotificationStatus toStatus(DispatchResult r) {
        if (!r.sent()) {
            return NotificationStatus.FAILED;
        }
        return "sent".equals(r.reason()) ? NotificationStatus.SENT : NotificationStatus.LOGGED;
    }
}
