package com.sunrise.clinic.appointments.domain;

import com.sunrise.clinic.access.domain.Role;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/**
 * The central aggregate. {@code appointmentNo} (APT-yyyymmdd-####) is generated
 * by the Singleton {@code AppointmentNumberGenerator}.
 *
 * <p><b>Confidentiality:</b> {@code diagnosis} is clinical data - visible only to the
 * treating dentist and to the patient themselves. It is not stripped from a response;
 * {@link AppointmentResponse} has no field for it, so it cannot leak by accident.</p>
 *
 * <p><b>Behaviour lives here, not in the service.</b> {@code complete} and
 * {@code cancel} are the appointment's own transitions, and they refuse an illegal one
 * (see {@link AppointmentStatus}). The service used to assign the status directly, so
 * a cancelled appointment could be completed and a billed one cancelled. An entity
 * that cannot be put into an invalid state is worth more than a service that
 * remembers to check.</p>
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
    /**
     * The reason a patient states when they book "Other (describe…)" instead of a
     * listed procedure. GAP-FTB-06. NULL for a named treatment.
     */
    private String patientReason;
    /** GAP-DEN-13: the price the dentist enters when completing a visit with no
     * catalog treatment. NULL for a named treatment (the catalog rules). */
    private BigDecimal customPrice;
    private String createdByUid;
    private Role createdByRole;
    private Instant createdAt;

    public Appointment() {
    }

    public Appointment(String appointmentNo, String patientId, String dentistId, String slotId, String treatmentId, LocalDate date, LocalTime time, AppointmentStatus status, String diagnosis, String createdByUid, Role createdByRole, Instant createdAt) {
        this(appointmentNo, patientId, dentistId, slotId, treatmentId, date, time, status, diagnosis, null, createdByUid, createdByRole, createdAt);
    }

    public Appointment(String appointmentNo, String patientId, String dentistId, String slotId, String treatmentId, LocalDate date, LocalTime time, AppointmentStatus status, String diagnosis, String patientReason, String createdByUid, Role createdByRole, Instant createdAt) {
        this(appointmentNo, patientId, dentistId, slotId, treatmentId, date, time, status, diagnosis, patientReason, null, createdByUid, createdByRole, createdAt);
    }

    public Appointment(String appointmentNo, String patientId, String dentistId, String slotId, String treatmentId, LocalDate date, LocalTime time, AppointmentStatus status, String diagnosis, String patientReason, BigDecimal customPrice, String createdByUid, Role createdByRole, Instant createdAt) {
        this.appointmentNo = appointmentNo;
        this.patientId = patientId;
        this.dentistId = dentistId;
        this.slotId = slotId;
        this.treatmentId = treatmentId;
        this.date = date;
        this.time = time;
        this.status = status;
        this.diagnosis = diagnosis;
        this.patientReason = patientReason;
        this.customPrice = customPrice;
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

    public String getPatientReason() {
        return patientReason;
    }

    public void setPatientReason(String patientReason) {
        this.patientReason = patientReason;
    }

    public BigDecimal getCustomPrice() {
        return customPrice;
    }

    public void setCustomPrice(BigDecimal customPrice) {
        this.customPrice = customPrice;
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

    // --- behaviour -------------------------------------------------------

    /**
     * Record the diagnosis and mark the visit done.
     *
     * @throws IllegalStateException if the appointment is not in a status from which
     *         completing is legal - a cancelled or already-billed appointment
     */
    public void complete(String diagnosis) {
        complete(diagnosis, null);
    }

    /**
     * Record the diagnosis and mark the visit done, carrying the dentist-entered
     * price for work with no catalog treatment (GAP-DEN-13). A price on a visit
     * that names a treatment is dropped — the catalog rules there.
     *
     * @throws IllegalStateException if the appointment is not in a status from which
     *         completing is legal - a cancelled or already-billed appointment
     */
    public void complete(String diagnosis, BigDecimal customPrice) {
        moveTo(AppointmentStatus.COMPLETED);
        this.diagnosis = diagnosis == null || diagnosis.isBlank() ? null : diagnosis.trim();
        this.customPrice = treatmentId == null ? customPrice : null;
    }

    /**
     * Call the appointment off.
     *
     * <p>Refuses a COMPLETED or BILLED appointment. Cancelling one would release the
     * slot while the visit stands - the clinic would have charged for a visit whose
     * time was given away, and a billed appointment's revenue split would refer to
     * one that no longer claims it. Reversing a bill is a financial operation, not a
     * scheduling one.</p>
     */
    public void cancel() {
        moveTo(AppointmentStatus.CANCELLED);
    }

    /** Mark the appointment billed. Called by billing once the bill is written. */
    public void markBilled() {
        moveTo(AppointmentStatus.BILLED);
    }

    /** @return true if this appointment may move to {@code next} from where it is. */
    public boolean canTransitionTo(AppointmentStatus next) {
        return status != null && status.canMoveTo(next);
    }

    /** @return true if the appointment is the given dentist's to treat. */
    public boolean isTreatedBy(String candidateDentistId) {
        return dentistId != null && dentistId.equals(candidateDentistId);
    }

    /** @return true if the appointment belongs to the given patient. */
    public boolean belongsTo(String candidatePatientId) {
        return patientId != null && patientId.equals(candidatePatientId);
    }

    private void moveTo(AppointmentStatus next) {
        if (!canTransitionTo(next)) {
            throw new IllegalStateException(
                    appointmentNo + " is " + status + ", so it cannot become " + next + ".");
        }
        this.status = next;
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
        private String patientReason;
        private BigDecimal customPrice;
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

        public Builder patientReason(String patientReason) {
            this.patientReason = patientReason;
            return this;
        }

        public Builder customPrice(BigDecimal customPrice) {
            this.customPrice = customPrice;
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
            return new Appointment(appointmentNo, patientId, dentistId, slotId, treatmentId, date, time, status, diagnosis, patientReason, customPrice, createdByUid, createdByRole, createdAt);
        }
    }
}
