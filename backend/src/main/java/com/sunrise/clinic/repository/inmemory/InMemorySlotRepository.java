package com.sunrise.clinic.repository.inmemory;

import com.sunrise.clinic.domain.Slot;
import com.sunrise.clinic.repository.InMemoryRepository;
import com.sunrise.clinic.repository.SlotRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** In-memory {@link SlotRepository}. */
public class InMemorySlotRepository
        extends InMemoryRepository<Slot, String>
        implements SlotRepository {

    @Override
    protected String idOf(Slot entity) {
        return entity.getId();
    }

    /**
     * {@inheritDoc}
     *
     * <p>There are no row locks over a map, so this is an ordinary read. The
     * exclusion the contract requires is supplied by {@code SerialTransactionRunner},
     * which lets only one unit of work run at a time — so a caller inside a
     * transaction still cannot be interleaved with another booking.</p>
     */
    @Override
    public Optional<Slot> findByIdForUpdate(String slotId) {
        return findById(slotId);
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
