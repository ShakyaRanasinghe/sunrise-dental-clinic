package com.sunrise.clinic.feedback.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * A patient's account of something that went wrong - FR-CMP-01 to FR-CMP-12.
 *
 * <p><b>The patient's account is not editable, by anybody.</b> Not by the patient once
 * submitted (FR-CMP-06), and not by the administrator reviewing it (FR-ADM-55). A record of
 * a concern that the subject of the concern's employer can rewrite is not a record. What an
 * administrator adds is a {@code resolution} - a separate field, alongside the account
 * rather than over it.</p>
 *
 * <p><b>The dentist named in it never sees it</b> (FR-CMP-08) - not the complaint, not its
 * existence, not an aggregate count that would let them infer it. Neither does a
 * receptionist (FR-CMP-09). That is enforced in {@code ComplaintService}: there is no route,
 * screen or response shape that puts one in front of either.</p>
 */
public class Complaint {

    private String id;
    private String patientId;
    private String dentistId;
    private String appointmentNo;
    private ComplaintCategory category;
    private String detail;
    private ComplaintStatus status = ComplaintStatus.SUBMITTED;
    private Instant submittedAt;
    private String reviewedByUid;
    private String resolution;
    private Instant resolvedAt;

    public Complaint() {
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

    public String getDentistId() {
        return dentistId;
    }

    public void setDentistId(String dentistId) {
        this.dentistId = dentistId;
    }

    public String getAppointmentNo() {
        return appointmentNo;
    }

    public void setAppointmentNo(String appointmentNo) {
        this.appointmentNo = appointmentNo;
    }

    public ComplaintCategory getCategory() {
        return category;
    }

    public void setCategory(ComplaintCategory category) {
        this.category = category;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public ComplaintStatus getStatus() {
        return status;
    }

    public void setStatus(ComplaintStatus status) {
        this.status = status;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public String getReviewedByUid() {
        return reviewedByUid;
    }

    public void setReviewedByUid(String reviewedByUid) {
        this.reviewedByUid = reviewedByUid;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public boolean belongsTo(String candidatePatientId) {
        return patientId != null && patientId.equals(candidatePatientId);
    }

    // --- behaviour ----------------------------------------------------

    /** Pick it up for review - FR-ADM-52. */
    public void beginReview(String administratorUid) {
        moveTo(ComplaintStatus.UNDER_REVIEW);
        this.reviewedByUid = administratorUid;
    }

    /**
     * Close it, with a written explanation.
     *
     * @throws IllegalArgumentException if the resolution is blank. FR-ADM-53: a concern
     *         closed with no explanation is a concern ignored with extra steps
     */
    public void close(ComplaintStatus outcome, String administratorUid, String writtenResolution) {
        if (!outcome.requiresResolution()) {
            throw new IllegalArgumentException(outcome + " is not a way of closing a complaint.");
        }
        String trimmed = writtenResolution == null ? "" : writtenResolution.trim();
        if (trimmed.length() < 10) {
            throw new IllegalArgumentException(
                    "Say what was done about it. A complaint closed with no explanation is a"
                            + " complaint ignored.");
        }
        moveTo(outcome);
        this.reviewedByUid = administratorUid;
        this.resolution = trimmed;
        this.resolvedAt = Instant.now();
    }

    private void moveTo(ComplaintStatus next) {
        if (status == null || !status.canMoveTo(next)) {
            throw new IllegalStateException(
                    "A complaint that is " + status + " cannot become " + next + ".");
        }
        this.status = next;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof Complaint complaint && Objects.equals(id, complaint.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /** Never the detail: it is a patient's account of something and belongs in no log. */
    @Override
    public String toString() {
        return "Complaint{id=" + id + ", category=" + category + ", status=" + status + "}";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Complaint complaint = new Complaint();

        public Builder id(String id) {
            complaint.id = id;
            return this;
        }

        public Builder patientId(String patientId) {
            complaint.patientId = patientId;
            return this;
        }

        public Builder dentistId(String dentistId) {
            complaint.dentistId = dentistId;
            return this;
        }

        public Builder appointmentNo(String appointmentNo) {
            complaint.appointmentNo = appointmentNo;
            return this;
        }

        public Builder category(ComplaintCategory category) {
            complaint.category = category;
            return this;
        }

        public Builder detail(String detail) {
            complaint.detail = detail;
            return this;
        }

        public Builder status(ComplaintStatus status) {
            complaint.status = status;
            return this;
        }

        public Builder submittedAt(Instant submittedAt) {
            complaint.submittedAt = submittedAt;
            return this;
        }

        public Builder reviewedByUid(String reviewedByUid) {
            complaint.reviewedByUid = reviewedByUid;
            return this;
        }

        public Builder resolution(String resolution) {
            complaint.resolution = resolution;
            return this;
        }

        public Builder resolvedAt(Instant resolvedAt) {
            complaint.resolvedAt = resolvedAt;
            return this;
        }

        public Complaint build() {
            return complaint;
        }
    }
}
