package com.sunrise.clinic.patients.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Something a patient has declared about their own health.
 *
 * <p><b>It belongs to the patient, not to an appointment</b> - FR-NOTE-06. A fact declared
 * once is visible at every future visit, which is the whole point: a patient should not have
 * to remember to repeat their penicillin allergy each time they book.</p>
 *
 * <p>The patient writes these and only the patient may change them (FR-NOTE-05, FR-NOTE-10).
 * A dentist reads them; a receptionist and an administrator never see them at all
 * (FR-NOTE-11). That is three different rules pointing three different ways, and they are
 * enforced in {@link com.sunrise.clinic.patients.service.PatientNoteService} rather than at
 * each screen.</p>
 */
public class PatientNote {

    private String id;
    private String patientId;
    private NoteCategory category;
    private String detail;
    private boolean critical;
    private Instant createdAt;
    private Instant updatedAt;

    public PatientNote() {
    }

    public PatientNote(String id, String patientId, NoteCategory category, String detail,
                       boolean critical, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.patientId = patientId;
        this.category = category;
        this.detail = detail;
        this.critical = critical;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public NoteCategory getCategory() {
        return category;
    }

    public void setCategory(NoteCategory category) {
        this.category = category;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public boolean isCritical() {
        return critical;
    }

    public void setCritical(boolean critical) {
        this.critical = critical;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    /** @return true if this note belongs to the given patient. */
    public boolean belongsTo(String candidatePatientId) {
        return patientId != null && patientId.equals(candidatePatientId);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof PatientNote note && Objects.equals(id, note.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * Deliberately does not include the detail.
     *
     * <p>{@code toString} ends up in logs and in exception messages, and the detail is the
     * one field here that is medical information about a named person.</p>
     */
    @Override
    public String toString() {
        return "PatientNote{id=" + id + ", category=" + category + ", critical=" + critical + "}";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String patientId;
        private NoteCategory category;
        private String detail;
        private boolean critical;
        private Instant createdAt;
        private Instant updatedAt;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder patientId(String patientId) {
            this.patientId = patientId;
            return this;
        }

        public Builder category(NoteCategory category) {
            this.category = category;
            return this;
        }

        public Builder detail(String detail) {
            this.detail = detail;
            return this;
        }

        public Builder critical(boolean critical) {
            this.critical = critical;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public PatientNote build() {
            return new PatientNote(id, patientId, category, detail, critical, createdAt, updatedAt);
        }
    }
}
