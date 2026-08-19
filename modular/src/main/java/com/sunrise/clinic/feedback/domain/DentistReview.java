package com.sunrise.clinic.feedback.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * A patient's rating of one visit - FR-RVW-01 to FR-RVW-12.
 *
 * <p>One review per appointment ({@code uq_review_appointment}), so a patient cannot weight
 * a dentist's average by submitting repeatedly (FR-RVW-03). The rating is 1 to 5, enforced by
 * {@code ck_review_rating} as well as here - a check constraint is the guarantee that holds
 * when something writes the table without going through this class.</p>
 *
 * <p><b>Nobody sees this individually except an administrator</b> (FR-RVW-09). The dentist
 * gets a {@link RatingSummary} and never a review, a comment or a count small enough to
 * identify who wrote it. A receptionist gets nothing. Patients see no ratings at all in this
 * release (FR-RVW-11) - the data is collected now and displayed later, which is the only
 * honest way to start: an average of two reviews shown to patients would be worse than no
 * average.</p>
 */
public class DentistReview {

    public static final int MIN_RATING = 1;
    public static final int MAX_RATING = 5;

    private String id;
    private String appointmentNo;
    private String dentistId;
    private String patientId;
    private int rating;
    private String comment;
    private Instant submittedAt;
    private Instant updatedAt;

    public DentistReview() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAppointmentNo() {
        return appointmentNo;
    }

    public void setAppointmentNo(String appointmentNo) {
        this.appointmentNo = appointmentNo;
    }

    public String getDentistId() {
        return dentistId;
    }

    public void setDentistId(String dentistId) {
        this.dentistId = dentistId;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public int getRating() {
        return rating;
    }

    /** @throws IllegalArgumentException outside 1..5, the same range the check constraint holds */
    public void setRating(int rating) {
        if (rating < MIN_RATING || rating > MAX_RATING) {
            throw new IllegalArgumentException(
                    "A rating is between " + MIN_RATING + " and " + MAX_RATING + ", not " + rating);
        }
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        // Blank becomes null: a comment is optional (FR-RVW-02), and "" and "no comment"
        // should not be two different states in the table.
        this.comment = comment == null || comment.isBlank() ? null : comment.trim();
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean belongsTo(String candidatePatientId) {
        return patientId != null && patientId.equals(candidatePatientId);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof DentistReview review && Objects.equals(id, review.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /** Never the comment, and never which patient wrote it. */
    @Override
    public String toString() {
        return "DentistReview{id=" + id + ", rating=" + rating + "}";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final DentistReview review = new DentistReview();

        public Builder id(String id) {
            review.id = id;
            return this;
        }

        public Builder appointmentNo(String appointmentNo) {
            review.appointmentNo = appointmentNo;
            return this;
        }

        public Builder dentistId(String dentistId) {
            review.dentistId = dentistId;
            return this;
        }

        public Builder patientId(String patientId) {
            review.patientId = patientId;
            return this;
        }

        public Builder rating(int rating) {
            review.setRating(rating);
            return this;
        }

        public Builder comment(String comment) {
            review.setComment(comment);
            return this;
        }

        public Builder submittedAt(Instant submittedAt) {
            review.submittedAt = submittedAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            review.updatedAt = updatedAt;
            return this;
        }

        public DentistReview build() {
            return review;
        }
    }
}
