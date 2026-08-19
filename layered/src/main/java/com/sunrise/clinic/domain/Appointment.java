package com.sunrise.clinic.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/**
 * The central aggregate. {@code appointmentNo} (APT-yyyymmdd-####) is generated
 * by the Singleton {@code AppointmentNumberGenerator}.
 *
 * <p><b>Confidentiality:</b> {@code diagnosis} is clinical data — visible only to the
 * treating Dentist and the Patient. It is stripped from responses served to
 * Receptionist/Admin by the DTO mapper.</p>
 */
public class Appointment {
    private String appointmentNo;   // primary key, e.g. APT-20260720-0001
    private String patientId;
    private String dentistId;
    private String slotId;
    private String treatmentId;
    private LocalDate date;
    private LocalTime time;
    private AppointmentStatus status = AppointmentStatus.CONFIRMED;
    /** Clinical diagnosis — CONFIDENTIAL (dentist + patient only). */
    private String diagnosis;
    private String createdByUid;
    private Role createdByRole;
    private Instant createdAt;

    public Appointment() {
    }

    public Appointment(String appointmentNo, String patientId, String dentistId, String slotId, String treatmentId, LocalDate date, LocalTime time, AppointmentStatus status, String diagnosis, String createdByUid, Role createdByRole, Instant createdAt) {
        this.appointmentNo = appointmentNo;
        this.patientId = patientId;
        this.dentistId = dentistId;
        this.slotId = slotId;
        this.treatmentId = treatmentId;
        this.date = date;
        this.time = time;
        this.status = status;
        this.diagnosis = diagnosis;
        this.createdByUid = createdByUid;
        this.createdByRole = createdByRole;
        this.createdAt = createdAt;
    }

    public String getAppointmentNo() {
        return appointmentNo;
    }

    public void setAppointmentNo(String appointmentNo) {
        this.appointmentNo = appointmentNo;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getDentistId() {
        return dentistId;
    }

    public void setDentistId(String dentistId) {
        this.dentistId = dentistId;
    }

    public String getSlotId() {
        return slotId;
    }

    public void setSlotId(String slotId) {
        this.slotId = slotId;
    }

    public String getTreatmentId() {
        return treatmentId;
    }

    public void setTreatmentId(String treatmentId) {
        this.treatmentId = treatmentId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getTime() {
        return time;
    }

    public void setTime(LocalTime time) {
        this.time = time;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public String getCreatedByUid() {
        return createdByUid;
    }

    public void setCreatedByUid(String createdByUid) {
        this.createdByUid = createdByUid;
    }

    public Role getCreatedByRole() {
        return createdByRole;
    }

    public void setCreatedByRole(Role createdByRole) {
        this.createdByRole = createdByRole;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Appointment)) return false;
        return Objects.equals(appointmentNo, ((Appointment) o).appointmentNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(appointmentNo);
    }

    @Override
    public String toString() {
        return "Appointment{appointmentNo=" + appointmentNo + "}";
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Hand-written Builder — replaces Lombok's {@code @Builder}. */
    public static class Builder {
        private String appointmentNo;
        private String patientId;
        private String dentistId;
        private String slotId;
        private String treatmentId;
        private LocalDate date;
        private LocalTime time;
        private AppointmentStatus status = AppointmentStatus.CONFIRMED;
        private String diagnosis;
        private String createdByUid;
        private Role createdByRole;
        private Instant createdAt;

        public Builder appointmentNo(String appointmentNo) {
            this.appointmentNo = appointmentNo;
            return this;
        }

        public Builder patientId(String patientId) {
            this.patientId = patientId;
            return this;
        }

        public Builder dentistId(String dentistId) {
            this.dentistId = dentistId;
            return this;
        }

        public Builder slotId(String slotId) {
            this.slotId = slotId;
            return this;
        }

        public Builder treatmentId(String treatmentId) {
            this.treatmentId = treatmentId;
            return this;
        }

        public Builder date(LocalDate date) {
            this.date = date;
            return this;
        }

        public Builder time(LocalTime time) {
            this.time = time;
            return this;
        }

        public Builder status(AppointmentStatus status) {
            this.status = status;
            return this;
        }

        public Builder diagnosis(String diagnosis) {
            this.diagnosis = diagnosis;
            return this;
        }

        public Builder createdByUid(String createdByUid) {
            this.createdByUid = createdByUid;
            return this;
        }

        public Builder createdByRole(Role createdByRole) {
            this.createdByRole = createdByRole;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Appointment build() {
            return new Appointment(appointmentNo, patientId, dentistId, slotId, treatmentId, date, time, status, diagnosis, createdByUid, createdByRole, createdAt);
        }
    }
}
