package com.sunrise.clinic.repository;

import com.sunrise.clinic.domain.Slot;

import java.time.LocalDate;
import java.util.List;

/** Persistence for bookable {@link Slot}s. */
public interface SlotRepository extends Repository<Slot, String> {

    List<Slot> findByDentistIdAndDate(String dentistId, LocalDate date);

    List<Slot> findByDateBetween(LocalDate from, LocalDate to);

    List<Slot> findBySessionId(String sessionId);
}
