package com.sunrise.clinic.domain;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/** A single bookable time slot. Booking flips OPEN &rarr; BOOKED atomically. */
public class Slot {
    private String id;
    private String sessionId;
    private String dentistId;
    private LocalDate date;
    private LocalTime startTime;
    private int durationMinutes = 30;
    private SlotStatus status = SlotStatus.OPEN;
    private String appointmentNo;   // set when booked

    public Slot() {
    }

    public Slot(String id, String sessionId, String dentistId, LocalDate date, LocalTime startTime, int durationMinutes, SlotStatus status, String appointmentNo) {
        this.id = id;
        this.sessionId = sessionId;
        this.dentistId = dentistId;
        this.date = date;
        this.startTime = startTime;
        this.durationMinutes = durationMinutes;
        this.status = status;
        this.appointmentNo = appointmentNo;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
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

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public SlotStatus getStatus() {
        return status;
    }

    public void setStatus(SlotStatus status) {
        this.status = status;
    }

    public String getAppointmentNo() {
        return appointmentNo;
    }

    public void setAppointmentNo(String appointmentNo) {
        this.appointmentNo = appointmentNo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Slot)) return false;
        return Objects.equals(id, ((Slot) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Slot{id=" + id + "}";
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Hand-written Builder — replaces Lombok's {@code @Builder}. */
    public static class Builder {
        private String id;
        private String sessionId;
        private String dentistId;
        private LocalDate date;
        private LocalTime startTime;
        private int durationMinutes = 30;
        private SlotStatus status = SlotStatus.OPEN;
        private String appointmentNo;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
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

        public Builder durationMinutes(int durationMinutes) {
            this.durationMinutes = durationMinutes;
            return this;
        }

        public Builder status(SlotStatus status) {
            this.status = status;
            return this;
        }

        public Builder appointmentNo(String appointmentNo) {
            this.appointmentNo = appointmentNo;
            return this;
        }

        public Slot build() {
            return new Slot(id, sessionId, dentistId, date, startTime, durationMinutes, status, appointmentNo);
        }
    }
}
