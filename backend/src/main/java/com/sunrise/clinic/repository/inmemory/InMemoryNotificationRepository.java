package com.sunrise.clinic.repository.inmemory;

import com.sunrise.clinic.domain.Notification;
import com.sunrise.clinic.repository.InMemoryRepository;
import com.sunrise.clinic.repository.NotificationRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;

/** In-memory {@link NotificationRepository}. */
@Repository
@Profile("!firestore")
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
