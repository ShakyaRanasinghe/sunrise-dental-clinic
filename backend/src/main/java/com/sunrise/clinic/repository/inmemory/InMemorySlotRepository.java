package com.sunrise.clinic.repository.inmemory;

import com.sunrise.clinic.domain.Slot;
import com.sunrise.clinic.repository.InMemoryRepository;
import com.sunrise.clinic.repository.SlotRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/** In-memory {@link SlotRepository}. */
@Repository
@Profile("!firestore")
public class InMemorySlotRepository
        extends InMemoryRepository<Slot, String>
        implements SlotRepository {

    @Override
    protected String idOf(Slot entity) {
        return entity.getId();
    }

    @Override
    public List<Slot> findByDentistIdAndDate(String dentistId, LocalDate date) {
        return store.values().stream()
                .filter(s -> dentistId.equals(s.getDentistId()) && date.equals(s.getDate()))
                .toList();
    }

    @Override
    public List<Slot> findByDateBetween(LocalDate from, LocalDate to) {
        return store.values().stream()
                .filter(s -> s.getDate() != null
                        && !s.getDate().isBefore(from) && !s.getDate().isAfter(to))
                .toList();
    }

    @Override
    public List<Slot> findBySessionId(String sessionId) {
        return store.values().stream().filter(s -> sessionId.equals(s.getSessionId())).toList();
    }
}
