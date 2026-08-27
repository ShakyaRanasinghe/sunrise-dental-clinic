package com.sunrise.clinic.repository;

import com.sunrise.clinic.domain.Notification;

import java.util.List;

/** Persistence for {@link Notification} delivery records. */
public interface NotificationRepository extends Repository<Notification, String> {

    List<Notification> findByAppointmentNo(String appointmentNo);
}
