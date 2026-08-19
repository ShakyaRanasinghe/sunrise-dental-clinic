package com.sunrise.clinic.scheduling.domain;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/**
 * An availability window a Receptionist publishes for a dentist —
 * "Dr. Silva is in the clinic on 2026-07-20 from 16:00 to 18:00".
 * The {@code SlotService} explodes it into bookable {@link Slot}s.
 */
public class DentistSession {
    private String id;
    private String dentistId;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private int slotDurationMinutes = 30;
    private String publishedByUid;   // receptionist/admin

    public DentistSession() {
    }

    public DentistSession(String id, String dentistId, LocalDate date, LocalTime startTime, LocalTime endTime, int slotDurationMinutes, String publishedByUid) {
        this.id = id;
        this.dentistId = dentistId;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.slotDurationMinutes = slotDurationMinutes;
        this.publishedByUid = publishedByUid;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDentistId() {
        return dentistId;
    }

    public void setDentistId(String dentistId) {
        this.dentistId = dentistId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public int getSlotDurationMinutes() {
        return slotDurationMinutes;
    }

    public void setSlotDurationMinutes(int slotDurationMinutes) {
        this.slotDurationMinutes = slotDurationMinutes;
    }

    public String getPublishedByUid() {
        return publishedByUid;
    }

    public void setPublishedByUid(String publishedByUid) {
        this.publishedByUid = publishedByUid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DentistSession)) return false;
        return Objects.equals(id, ((DentistSession) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "DentistSession{id=" + id + "}";
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Hand-written Builder — replaces Lombok's {@code @Builder}. */
    public static class Builder {
        private String id;
        private String dentistId;
        private LocalDate date;
        private LocalTime startTime;
        private LocalTime endTime;
        private int slotDurationMinutes = 30;
        private String publishedByUid;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder dentistId(String dentistId) {
            this.dentistId = dentistId;
            return this;
        }

        public Builder date(LocalDate date) {
            this.date = date;
            return this;
        }

        public Builder startTime(LocalTime startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder endTime(LocalTime endTime) {
            this.endTime = endTime;
            return this;
        }

        public Builder slotDurationMinutes(int slotDurationMinutes) {
            this.slotDurationMinutes = slotDurationMinutes;
            return this;
        }

        public Builder publishedByUid(String publishedByUid) {
            this.publishedByUid = publishedByUid;
            return this;
        }

        public DentistSession build() {
            return new DentistSession(id, dentistId, date, startTime, endTime, slotDurationMinutes, publishedByUid);
        }
    }
}
