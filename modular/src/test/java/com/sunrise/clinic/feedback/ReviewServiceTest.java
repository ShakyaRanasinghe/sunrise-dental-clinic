package com.sunrise.clinic.feedback;

import com.sunrise.clinic.access.domain.ClinicPrincipal;
import com.sunrise.clinic.access.service.AccessControl;
import com.sunrise.clinic.appointments.AppointmentTestFixture;
import com.sunrise.clinic.feedback.domain.RatingSummary;
import com.sunrise.clinic.feedback.domain.ReviewResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * New in step 8b. The two rules worth the most attention are the thirty-day window and the
 * five-review floor, and the confidentiality boundary between a dentist and their own
 * reviews.
 */
class ReviewServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 20);

    private FeedbackTestFixture fixture;
    private ClinicPrincipal nimal;
    private ClinicPrincipal kamala;
    private String visit;

    @BeforeEach
    void setUp() {
        fixture = new FeedbackTestFixture(TODAY);
        nimal = fixture.appointments.addPatient("p-nimal", "u-pat1", "Nimal Perera");
        kamala = fixture.appointments.addPatient("p-kamala", "u-pat2", "Kamala Fernando");
        visit = fixture.treatedVisit(nimal, "s1", TODAY.minusDays(2));
    }

    // --- rating a visit -------------------------------------------------

    @Test
    void aPatientRatesACompletedVisit() {
        ReviewResponse left = fixture.reviewService.rate(nimal, visit, 5, "Very gentle, thank you");

        assertEquals(5, left.rating());
        assertEquals("Very gentle, thank you", left.comment());
        assertEquals("Dr. Ranil Silva", left.dentistName());
        assertTrue(left.editable());
    }

    @Test
    void aCommentIsOptional() {
        // FR-RVW-02. Requiring words suppresses ratings, and a star with no comment is
        // still information.
        assertNull(fixture.reviewService.rate(nimal, visit, 4, null).comment());
        assertNull(fixture.reviewService.rate(nimal, visit, 4, "   ").comment());
    }

    @Test
    void aRatingOutsideOneToFiveIsRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> fixture.reviewService.rate(nimal, visit, 0, null));
        assertThrows(IllegalArgumentException.class,
                () -> fixture.reviewService.rate(nimal, visit, 6, null));
    }

    @Test
    void thereIsAtMostOneReviewPerAppointment() {
        // FR-RVW-03. A second submission amends the first rather than adding a vote, so a
        // patient cannot weight the average by repeating themselves.
        fixture.reviewService.rate(nimal, visit, 1, "Awful");
        fixture.reviewService.rate(nimal, visit, 5, "Actually fine on reflection");

        assertEquals(1, fixture.reviews.count());
        assertEquals(5, fixture.reviewService.forAppointment(nimal, visit).orElseThrow().rating());
    }

    @Test
    void anUntreatedOrCancelledVisitCannotBeRated() {
        // FR-RVW-04. There is nothing to rate.
        String upcoming = fixture.untreatedVisit(nimal, "s9", TODAY.plusDays(3));
        assertThrows(IllegalStateException.class,
                () -> fixture.reviewService.rate(nimal, upcoming, 5, null));

        String cancelled = fixture.treatedVisit(nimal, "s8", TODAY.minusDays(1));
        fixture.appointments.appointments.findById(cancelled).orElseThrow()
                .setStatus(com.sunrise.clinic.appointments.domain.AppointmentStatus.CANCELLED);
        assertThrows(IllegalStateException.class,
                () -> fixture.reviewService.rate(nimal, cancelled, 5, null));
    }

    @Test
    void aPatientCannotRateSomeoneElsesVisit() {
        assertThrows(AccessControl.AccessDeniedException.class,
                () -> fixture.reviewService.rate(kamala, visit, 1, "Not my appointment"));
    }

    @Test
    void onlyAPatientCanRate() {
        for (ClinicPrincipal caller : List.of(AppointmentTestFixture.reception(),
                AppointmentTestFixture.silva(), AppointmentTestFixture.admin())) {
            assertThrows(AccessControl.AccessDeniedException.class,
                    () -> fixture.reviewService.rate(caller, visit, 5, null));
        }
    }

    // --- the thirty-day window ------------------------------------------

    @Test
    void aVisitInsideTheWindowCanBeRated() {
        String recent = fixture.treatedVisit(nimal, "s-recent", TODAY.minusDays(30));

        assertEquals(4, fixture.reviewService.rate(nimal, recent, 4, null).rating());
    }

    @Test
    void aVisitBeyondTheWindowCannotBeRated() {
        // FR-RVW-06, measured from the visit rather than from when the review was written:
        // an unbounded window lets a review be rewritten years later, describing a visit
        // nobody remembers.
        String old = fixture.treatedVisit(nimal, "s-old", TODAY.minusDays(31));

        IllegalStateException refused = assertThrows(IllegalStateException.class,
                () -> fixture.reviewService.rate(nimal, old, 5, null));
        assertTrue(refused.getMessage().contains("31 days ago"), refused.getMessage());
    }

    @Test
    void aReviewCannotBeChangedOnceTheWindowHasClosed() {
        // The review is left two days after the visit, and the clock then moves past the
        // window. The same repositories throughout - see reviewServiceAsOf.
        fixture.reviewService.rate(nimal, visit, 5, "Left it soon after");

        assertThrows(IllegalStateException.class, () -> fixture
                .reviewServiceAsOf(TODAY.plusDays(40))
                .rate(nimal, visit, 1, "Changed my mind"));
    }

    @Test
    void editableSaysWhetherTheFormShouldBeOffered() {
        // The screen uses this to decide whether to show the form, so it must agree with
        // what the service would actually accept.
        assertTrue(fixture.reviewService.rate(nimal, visit, 5, null).editable());

        ReviewResponse later = fixture.reviewServiceAsOf(TODAY.plusDays(60))
                .forAppointment(nimal, visit).orElseThrow();

        assertEquals(5, later.rating(), "the review is still there");
        assertFalse(later.editable(), "but the window has closed, so no form");
    }

    // --- the dentist sees an aggregate and nothing else -----------------

    @Test
    void aDentistGetsASummaryAndNeverAReview() {
        // FR-RVW-08 and NFR-SEC-13. A dentist who could see "3 stars, last Tuesday
        // afternoon" would know who wrote it.
        fixture.reviewService.rate(nimal, visit, 5, "Very gentle");

        Object summary = fixture.reviewService.ownSummary(AppointmentTestFixture.silva());

        assertTrue(summary instanceof RatingSummary);
        assertFalse(Set.of(RatingSummary.class.getRecordComponents()).stream()
                        .anyMatch(c -> c.getName().toLowerCase().contains("comment")),
                "RatingSummary must have no comment field");
    }

    @Test
    void aDentistCannotReadIndividualReviewsOfThemselves() {
        fixture.reviewService.rate(nimal, visit, 5, "Very gentle");

        assertThrows(AccessControl.AccessDeniedException.class,
                () -> fixture.reviewService.forDentist(AppointmentTestFixture.silva(), "d-silva"));
    }

    @Test
    void aReceptionistSeesNothingAtAll() {
        // FR-RVW-10.
        fixture.reviewService.rate(nimal, visit, 5, null);
        ClinicPrincipal reception = AppointmentTestFixture.reception();

        assertThrows(AccessControl.AccessDeniedException.class,
                () -> fixture.reviewService.forDentist(reception, "d-silva"));
        assertThrows(AccessControl.AccessDeniedException.class,
                () -> fixture.reviewService.summaryFor(reception, "d-silva"));
        assertThrows(AccessControl.AccessDeniedException.class,
                () -> fixture.reviewService.ownSummary(reception));
    }

    @Test
    void theAdministratorReadsIndividualReviewsWithComments() {
        // FR-RVW-09.
        fixture.reviewService.rate(nimal, visit, 5, "Very gentle");

        List<ReviewResponse> all = fixture.reviewService
                .forDentist(AppointmentTestFixture.admin(), "d-silva");

        assertEquals(1, all.size());
        assertEquals("Very gentle", all.get(0).comment());
        assertFalse(all.get(0).editable(), "the administrator cannot change one");
    }

    // --- the five-review floor ------------------------------------------

    @Test
    void anAggregateIsWithheldBelowFiveReviews() {
        // FR-RVW-12. One bad visit should not define a career, and an average of two is not
        // a measurement - it is two opinions with a decimal point.
        for (int i = 1; i <= 4; i++) {
            String no = fixture.treatedVisit(
                    fixture.appointments.addPatient("p" + i, "u" + i, "Patient " + i),
                    "slot" + i, TODAY.minusDays(1));
            fixture.reviewService.rate(new ClinicPrincipal("u" + i, "Patient " + i,
                    com.sunrise.clinic.access.domain.Role.PATIENT), no, 1, null);
        }

        RatingSummary summary = fixture.reviewService.ownSummary(AppointmentTestFixture.silva());

        assertEquals(4, summary.reviews());
        assertNull(summary.mean(), "four reviews is below the floor");
        assertFalse(summary.isPublishable());
        assertEquals(1, summary.reviewsUntilPublishable());
    }

    @Test
    void theAggregateAppearsAtFiveReviews() {
        for (int i = 1; i <= 5; i++) {
            String no = fixture.treatedVisit(
                    fixture.appointments.addPatient("p" + i, "u" + i, "Patient " + i),
                    "slot" + i, TODAY.minusDays(1));
            fixture.reviewService.rate(new ClinicPrincipal("u" + i, "Patient " + i,
                    com.sunrise.clinic.access.domain.Role.PATIENT), no, i, null);
        }

        RatingSummary summary = fixture.reviewService.ownSummary(AppointmentTestFixture.silva());

        assertEquals(5, summary.reviews());
        // 1+2+3+4+5 = 15, over 5
        assertEquals(new BigDecimal("3.00"), summary.mean());
        assertTrue(summary.isPublishable());
        assertEquals(0, summary.reviewsUntilPublishable());
    }

    @Test
    void aDentistWithNoReviewsGetsAnEmptySummaryRatherThanAFailure() {
        RatingSummary summary = fixture.reviewService
                .ownSummary(AppointmentTestFixture.jayasuriya());

        assertEquals(0, summary.reviews());
        assertNull(summary.mean());
        assertEquals(5, summary.reviewsUntilPublishable());
    }

    // --- what the patient can see ---------------------------------------

    @Test
    void aPatientSeesTheirOwnReviews() {
        fixture.reviewService.rate(nimal, visit, 5, "Very gentle");

        assertEquals(1, fixture.reviewService.own(nimal).size());
        assertEquals(0, fixture.reviewService.own(kamala).size());
    }

    @Test
    void noRatingIsExposedToPatientsInThisRelease() {
        // FR-RVW-11. There is no method here a patient can call that returns a
        // RatingSummary for anybody - the data is collected now and displayed later.
        fixture.reviewService.rate(nimal, visit, 5, null);

        assertThrows(AccessControl.AccessDeniedException.class,
                () -> fixture.reviewService.summaryFor(nimal, "d-silva"));
        assertThrows(AccessControl.AccessDeniedException.class,
                () -> fixture.reviewService.ownSummary(nimal));
    }

    @Test
    void theCommentNeverAppearsInToString() {
        fixture.reviewService.rate(nimal, visit, 2, "The waiting room was filthy");

        assertFalse(fixture.reviews.findAll().get(0).toString().contains("filthy"),
                fixture.reviews.findAll().get(0).toString());
    }
}
