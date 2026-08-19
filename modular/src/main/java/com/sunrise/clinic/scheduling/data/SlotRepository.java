package com.sunrise.clinic.scheduling.data;

import com.sunrise.clinic.platform.data.Repository;
import com.sunrise.clinic.platform.data.TransactionRunner;

import com.sunrise.clinic.scheduling.domain.Slot;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Persistence for bookable {@link Slot}s. */
public interface SlotRepository extends Repository<Slot, String> {

    /**
     * Read a slot for the purpose of booking it, preventing any other transaction
     * from doing the same until this one finishes.
     *
     * <p>This exists because {@code findById} is not enough to stop double-booking:
     * two concurrent requests can both read the same slot as OPEN and both go on to
     * book it. Implementations must serialise callers — the JDBC one takes a
     * {@code SELECT ... FOR UPDATE} row lock; the in-memory one relies on the
     * surrounding {@link TransactionRunner} holding its lock.</p>
     *
     * <p>Must be called inside a {@link TransactionRunner#execute}, otherwise there
     * is no transaction for the lock to be held by.</p>
     *
     * @param slotId the slot about to be booked
     * @return the slot, if it exists
     */
    Optional<Slot> findByIdForUpdate(String slotId);

    List<Slot> findByDentistIdAndDate(String dentistId, LocalDate date);

    List<Slot> findByDateBetween(LocalDate from, LocalDate to);

    List<Slot> findBySessionId(String sessionId);
}
