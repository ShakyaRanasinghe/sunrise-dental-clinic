package com.sunrise.clinic.notifications.data;

import com.sunrise.clinic.platform.data.Repository;

import com.sunrise.clinic.notifications.domain.Notification;
import com.sunrise.clinic.notifications.domain.NotificationKind;

import java.util.List;

/** Persistence for {@link Notification} delivery records. */
public interface NotificationRepository extends Repository<Notification, String> {

    /**
     * Every attempt made about one appointment, newest first.
     *
     * <p>A list rather than the latest, because "we sent a confirmation and then a reminder, and
     * the reminder failed" is the sequence somebody needs when a patient says they were never
     * told.</p>
     */
    List<Notification> findByAppointmentNo(String appointmentNo);

    /**
     * Whether a message of this kind has already been attempted for this appointment.
     *
     * <p>The reminder sweep's whole correctness rests on this. It runs on a timer, so it will be
     * asked the same question about the same appointment more than once — a cron that fires
     * twice, a run that overlaps the previous one, somebody triggering it by hand to check it
     * works. Answering it wrong sends a patient two reminders, which is the fastest way to teach
     * them to ignore the next one.</p>
     *
     * <p>An <em>attempt</em>, not a delivery: a reminder that failed is not retried. Retrying on
     * a timer with no backoff would hammer a broken transport once a day forever, and the row
     * saying it failed is the honest record for somebody to act on.</p>
     */
    boolean hasBeenAttempted(String appointmentNo, NotificationKind kind);
}
