package com.sunrise.clinic.service;

import com.sunrise.clinic.domain.DentistSession;
import com.sunrise.clinic.domain.Slot;
import com.sunrise.clinic.domain.SlotStatus;
import com.sunrise.clinic.repository.SessionRepository;
import com.sunrise.clinic.repository.SlotRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Availability management. A receptionist publishes a {@link DentistSession}
 * (a window like "Dr. Silva, 16:00–18:00"), which this service explodes into
 * fixed-length bookable {@link Slot}s. Patients then browse open slots.
 */
public class SlotService {

    private final SessionRepository sessions;
    private final SlotRepository slots;

    public SlotService(SessionRepository sessions, SlotRepository slots) {
        this.sessions = sessions;
        this.slots = slots;
    }

    /**
     * Publish an availability window and generate its bookable slots.
     * Each slot's id is deterministic ({@code dentistId_date_startTime}), so
     * republishing the same window cannot create duplicate slots.
     */
    public DentistSession publishSession(String dentistId, LocalDate date,
                                         LocalTime start, LocalTime end,
                                         int slotMinutes, String publishedByUid) {
        DentistSession session = DentistSession.builder()
                .id(UUID.randomUUID().toString())
                .dentistId(dentistId)
                .date(date)
                .startTime(start)
                .endTime(end)
                .slotDurationMinutes(slotMinutes)
                .publishedByUid(publishedByUid)
                .build();
        sessions.save(session);

        LocalTime cursor = start;
        while (!cursor.plusMinutes(slotMinutes).isAfter(end)) {
            String slotId = dentistId + "_" + date + "_" + cursor;
            slots.save(Slot.builder()
                    .id(slotId)
                    .sessionId(session.getId())
                    .dentistId(dentistId)
                    .date(date)
                    .startTime(cursor)
                    .durationMinutes(slotMinutes)
                    .status(SlotStatus.OPEN)
                    .build());
            cursor = cursor.plusMinutes(slotMinutes);
        }
        return session;
    }

    /** Open slots for a dentist on a date (what a patient sees "today"). */
    public List<Slot> openSlots(String dentistId, LocalDate date) {
        return slots.findByDentistIdAndDate(dentistId, date).stream()
                .filter(s -> s.getStatus() == SlotStatus.OPEN)
                .sorted(Comparator.comparing(Slot::getStartTime))
                .toList();
    }

    /** Open slots across a date range (what a patient sees "this week"). */
    public List<Slot> openSlotsBetween(LocalDate from, LocalDate to) {
        return slots.findByDateBetween(from, to).stream()
                .filter(s -> s.getStatus() == SlotStatus.OPEN)
                .sorted(Comparator.comparing(Slot::getDate).thenComparing(Slot::getStartTime))
                .toList();
    }
}
