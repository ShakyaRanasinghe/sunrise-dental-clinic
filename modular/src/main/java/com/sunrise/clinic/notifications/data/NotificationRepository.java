package com.sunrise.clinic.notifications.data;

import com.sunrise.clinic.platform.data.Repository;

import com.sunrise.clinic.notifications.domain.Notification;

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
}
