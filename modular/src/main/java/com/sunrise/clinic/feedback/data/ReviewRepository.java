package com.sunrise.clinic.feedback.data;

import com.sunrise.clinic.feedback.domain.DentistReview;
import com.sunrise.clinic.feedback.domain.RatingSummary;
import com.sunrise.clinic.platform.data.Repository;

import java.util.List;
import java.util.Optional;

/** Persistence for {@link DentistReview}s. */
public interface ReviewRepository extends Repository<DentistReview, String> {

    /** The review for one appointment, if the patient left one - FR-RVW-03. */
    Optional<DentistReview> findByAppointmentNo(String appointmentNo);

    /** What one patient has written, newest first. */
    List<DentistReview> findByPatientId(String patientId);

    /** Every review of one dentist - for the administrator only (FR-RVW-09). */
    List<DentistReview> findByDentistId(String dentistId);

    /**
     * The aggregate for one dentist.
     *
     * <p>Counted and averaged by the database rather than by loading every review, and the
     * five-review floor is applied here so no caller can obtain a mean below it - see
     * {@link RatingSummary}.</p>
     */
    RatingSummary summaryFor(String dentistId);
}
