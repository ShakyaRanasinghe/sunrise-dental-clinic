package com.sunrise.clinic.feedback.domain;

import java.time.Instant;

/**
 * One review, with its comment.
 *
 * <p>Obtainable by the patient who wrote it and by an administrator (FR-RVW-09), and by
 * nobody else. A dentist receives {@link RatingSummary} - a different type, with no comment
 * field and no patient - so no route can hand a dentist an individual review by mistake
 * (NFR-SEC-13).</p>
 */
public record ReviewResponse(String id,
                             String appointmentNo,
                             String dentistId,
                             String dentistName,
                             int rating,
                             String comment,
                             Instant submittedAt,
                             boolean editable) {

    /**
     * @param editable whether the patient may still change it - within the window. Computed
     *                 by the service and carried here so the screen offers the form only
     *                 when the service would accept it
     */
    public static ReviewResponse of(DentistReview review, String dentistName, boolean editable) {
        return new ReviewResponse(review.getId(), review.getAppointmentNo(),
                review.getDentistId(), dentistName, review.getRating(), review.getComment(),
                review.getSubmittedAt(), editable);
    }
}
