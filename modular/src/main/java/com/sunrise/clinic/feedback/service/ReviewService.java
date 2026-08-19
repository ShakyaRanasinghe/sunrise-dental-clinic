package com.sunrise.clinic.feedback.service;

import com.sunrise.clinic.access.domain.Action;
import com.sunrise.clinic.access.domain.ClinicPrincipal;
import com.sunrise.clinic.access.domain.Role;
import com.sunrise.clinic.access.service.AccessControl;
import com.sunrise.clinic.appointments.domain.Appointment;
import com.sunrise.clinic.appointments.domain.AppointmentStatus;
import com.sunrise.clinic.appointments.service.AppointmentService;
import com.sunrise.clinic.appointments.service.ClinicAccess;
import com.sunrise.clinic.feedback.data.ReviewRepository;
import com.sunrise.clinic.feedback.domain.DentistReview;
import com.sunrise.clinic.feedback.domain.RatingSummary;
import com.sunrise.clinic.feedback.domain.ReviewResponse;
import com.sunrise.clinic.patients.domain.Patient;
import com.sunrise.clinic.platform.error.ResourceNotFoundException;
import com.sunrise.clinic.scheduling.domain.Dentist;
import com.sunrise.clinic.scheduling.service.ReferenceService;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dentist reviews - FR-RVW-01 to FR-RVW-12.
 *
 * <h2>Who sees what</h2>
 *
 * <ul>
 *   <li>the <b>patient</b> sees and can change their own review, within the window;</li>
 *   <li>the <b>dentist</b> sees a {@link RatingSummary} - a mean and a count - and never an
 *       individual review, comment or date. A dentist who could see "3 stars, last Tuesday
 *       afternoon" would know exactly who wrote it, and a review the patient believes is
 *       confidential is not confidential (FR-RVW-08, NFR-SEC-13);</li>
 *   <li>the <b>administrator</b> reads individual reviews with comments (FR-RVW-09), and
 *       cannot edit or delete one (FR-ADM-61) - curating the feedback would make the average
 *       worthless;</li>
 *   <li>a <b>receptionist</b> sees nothing (FR-RVW-10);</li>
 *   <li><b>patients</b> see no ratings at all in this release (FR-RVW-11). The data is
 *       collected now and displayed later, which is the only honest way to start.</li>
 * </ul>
 */
public class ReviewService {

    private static final Logger log = Logger.getLogger(ReviewService.class.getName());

    /**
     * How long after the visit a patient may leave or change a review - FR-RVW-06.
     *
     * <p>Measured from the appointment date, not from when the review was written: a patient
     * who reviews on day 29 gets one day to change it, which is the point. An unbounded
     * window would let a review be rewritten years later, by which time it describes a visit
     * nobody remembers.</p>
     */
    private static final int WINDOW_DAYS = 30;

    private final ReviewRepository reviews;
    private final AppointmentService appointments;
    private final ClinicAccess clinicAccess;
    private final ReferenceService reference;
    private final Clock clock;

    public ReviewService(ReviewRepository reviews, AppointmentService appointments,
                         ClinicAccess clinicAccess, ReferenceService reference, Clock clock) {
        this.reviews = reviews;
        this.appointments = appointments;
        this.clinicAccess = clinicAccess;
        this.reference = reference;
        this.clock = clock;
    }

    // --- the patient's side -------------------------------------------

    /**
     * Rate a visit - FR-RVW-01.
     *
     * <p>Only for an appointment that happened: COMPLETED or BILLED. A cancelled appointment
     * or one still to come cannot be rated (FR-RVW-04), because there is nothing to rate. The
     * comment is optional (FR-RVW-02) - requiring words suppresses ratings, and a star with
     * no comment is still information.</p>
     */
    public ReviewResponse rate(ClinicPrincipal caller, String appointmentNo, int rating,
                               String comment) {
        AccessControl.require(caller, Action.RATE_VISIT);
        String patientId = requireOwnPatientId(caller);
        Appointment appointment = requireRateable(caller, appointmentNo, patientId);

        Optional<DentistReview> existing = reviews.findByAppointmentNo(appointmentNo);
        if (existing.isPresent()) {
            // FR-RVW-03 and FR-RVW-06 together: one review per appointment, and changing it
            // is an amendment rather than a second submission.
            return amend(caller, appointmentNo, rating, comment);
        }
        requireWithinWindow(appointment);

        DentistReview review = DentistReview.builder()
                .id(UUID.randomUUID().toString())
                .appointmentNo(appointmentNo)
                .dentistId(appointment.getDentistId())
                .patientId(patientId)
                .rating(rating)
                .comment(comment)
                .submittedAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        reviews.save(review);

        // Never the comment: it belongs to the patient, not in a log file.
        log.log(Level.INFO, "review_left appointment={0} rating={1}",
                new Object[] { appointmentNo, rating });
        return describe(review, appointment);
    }

    /** Change a review, up to thirty days after the visit - FR-RVW-06. */
    public ReviewResponse amend(ClinicPrincipal caller, String appointmentNo, int rating,
                                String comment) {
        AccessControl.require(caller, Action.RATE_VISIT);
        String patientId = requireOwnPatientId(caller);
        Appointment appointment = requireRateable(caller, appointmentNo, patientId);
        requireWithinWindow(appointment);

        DentistReview review = reviews.findByAppointmentNo(appointmentNo).orElseThrow(() ->
                new ResourceNotFoundException("You have not reviewed " + appointmentNo));
        if (!review.belongsTo(patientId)) {
            throw new AccessControl.AccessDeniedException("That review is not yours.");
        }
        review.setRating(rating);
        review.setComment(comment);
        review.setUpdatedAt(Instant.now());
        reviews.save(review);
        return describe(review, appointment);
    }

    /** The review this patient left for a visit, if any. */
    public Optional<ReviewResponse> forAppointment(ClinicPrincipal caller, String appointmentNo) {
        String patientId = requireOwnPatientId(caller);
        Appointment appointment = appointments.require(appointmentNo);
        if (!appointment.belongsTo(patientId)) {
            throw new AccessControl.AccessDeniedException("That appointment is not yours.");
        }
        return reviews.findByAppointmentNo(appointmentNo)
                .filter(review -> review.belongsTo(patientId))
                .map(review -> describe(review, appointment));
    }

    /** Everything this patient has written. */
    public List<ReviewResponse> own(ClinicPrincipal caller) {
        String patientId = requireOwnPatientId(caller);
        return reviews.findByPatientId(patientId).stream()
                .map(review -> describe(review, appointments.require(review.getAppointmentNo())))
                .toList();
    }

    // --- the dentist's side: the aggregate, and only the aggregate ------

    /**
     * A dentist's own aggregate - FR-RVW-08.
     *
     * <p>Returns {@link RatingSummary}, never a {@link ReviewResponse}. The mean is absent
     * below five reviews (FR-RVW-12), so one bad visit cannot define a career and an average
     * of two is never presented as a measurement.</p>
     */
    public RatingSummary ownSummary(ClinicPrincipal caller) {
        AccessControl.require(caller, Action.READ_OWN_RATING);
        Dentist dentist = clinicAccess.dentistFor(caller).orElseThrow(() ->
                new ResourceNotFoundException("Your account is not linked to a dentist record."));
        return reviews.summaryFor(dentist.getId());
    }

    // --- the administrator's side --------------------------------------

    /** Individual reviews for any dentist, with comments - FR-RVW-09. */
    public List<ReviewResponse> forDentist(ClinicPrincipal caller, String dentistId) {
        AccessControl.require(caller, Action.READ_REVIEWS);
        Dentist dentist = reference.requireDentist(dentistId);
        return reviews.findByDentistId(dentistId).stream()
                .map(review -> ReviewResponse.of(review, dentist.getName(), false))
                .toList();
    }

    /** The aggregate for any dentist - for the reports screen (FR-ADM-60). */
    public RatingSummary summaryFor(ClinicPrincipal caller, String dentistId) {
        AccessControl.require(caller, Action.READ_REVIEWS);
        return reviews.summaryFor(dentistId);
    }

    // --- the rules ----------------------------------------------------

    /**
     * @throws IllegalStateException unless the appointment happened - FR-RVW-04
     */
    private Appointment requireRateable(ClinicPrincipal caller, String appointmentNo,
                                        String patientId) {
        Appointment appointment = appointments.require(appointmentNo);
        if (!appointment.belongsTo(patientId)) {
            throw new AccessControl.AccessDeniedException("That appointment is not yours.");
        }
        AppointmentStatus status = appointment.getStatus();
        if (status != AppointmentStatus.COMPLETED && status != AppointmentStatus.BILLED) {
            throw new IllegalStateException(
                    status == AppointmentStatus.CANCELLED
                            ? "That appointment was cancelled, so there is nothing to rate."
                            : "You can rate a visit once it has happened.");
        }
        return appointment;
    }

    private void requireWithinWindow(Appointment appointment) {
        long days = ChronoUnit.DAYS.between(appointment.getDate(), today());
        if (days > WINDOW_DAYS) {
            throw new IllegalStateException(
                    "Reviews close " + WINDOW_DAYS + " days after the visit, and that one was "
                            + days + " days ago.");
        }
    }

    private boolean isWithinWindow(Appointment appointment) {
        return ChronoUnit.DAYS.between(appointment.getDate(), today()) <= WINDOW_DAYS;
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private String requireOwnPatientId(ClinicPrincipal caller) {
        if (caller == null || caller.role() != Role.PATIENT) {
            // Neutral wording: this guard covers reading a patient's own reviews as well
            // as leaving one, and "only the patient can rate a visit" read oddly as the
            // refusal for a receptionist who had asked to see a list.
            throw new AccessControl.AccessDeniedException(
                    "Reviews are between the patient and the clinic's administrator.");
        }
        return clinicAccess.patientFor(caller).map(Patient::getId).orElseThrow(() ->
                new ResourceNotFoundException("Your account has no patient record."));
    }

    private ReviewResponse describe(DentistReview review, Appointment appointment) {
        return ReviewResponse.of(review,
                reference.requireDentist(review.getDentistId()).getName(),
                isWithinWindow(appointment));
    }
}
