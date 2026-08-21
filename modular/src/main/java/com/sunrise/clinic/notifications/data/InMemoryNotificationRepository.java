package com.sunrise.clinic.notifications.data;

import com.sunrise.clinic.platform.data.InMemoryRepository;

import com.sunrise.clinic.notifications.domain.Notification;

import java.util.List;

/** In-memory {@link NotificationRepository}. */
public class InMemoryNotificationRepository
        extends InMemoryRepository<Notification, String>
        implements NotificationRepository {

    @Override
    protected String idOf(Notification entity) {
        return entity.getId();
    }

    @Override
    public List<Notification> findByAppointmentNo(String appointmentNo) {
        return store.values().stream()
                .filter(n -> appointmentNo.equals(n.getAppointmentNo()))
                .toList();
    }
}
