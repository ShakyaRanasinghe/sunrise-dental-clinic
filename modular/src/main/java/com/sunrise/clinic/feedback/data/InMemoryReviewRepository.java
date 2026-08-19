package com.sunrise.clinic.feedback.data;

import com.sunrise.clinic.feedback.domain.DentistReview;
import com.sunrise.clinic.feedback.domain.RatingSummary;
import com.sunrise.clinic.platform.data.InMemoryRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** In-memory {@link ReviewRepository}. */
public class InMemoryReviewRepository
        extends InMemoryRepository<DentistReview, String>
        implements ReviewRepository {

    @Override
    protected String idOf(DentistReview entity) {
        return entity.getId();
    }

    @Override
    public Optional<DentistReview> findByAppointmentNo(String appointmentNo) {
        return store.values().stream()
                .filter(review -> appointmentNo != null
                        && appointmentNo.equals(review.getAppointmentNo()))
                .findFirst();
    }

    @Override
    public List<DentistReview> findByPatientId(String patientId) {
        return store.values().stream()
                .filter(review -> review.belongsTo(patientId))
                .sorted(newestFirst())
                .toList();
    }

    @Override
    public List<DentistReview> findByDentistId(String dentistId) {
        return store.values().stream()
                .filter(review -> dentistId != null && dentistId.equals(review.getDentistId()))
                .sorted(newestFirst())
                .toList();
    }

    @Override
    public RatingSummary summaryFor(String dentistId) {
        List<DentistReview> reviews = findByDentistId(dentistId);
        if (reviews.size() < RatingSummary.FLOOR) {
            // The floor applied here too, so no caller can obtain a mean below it whichever
            // implementation is wired in.
            return new RatingSummary(dentistId, reviews.size(), null);
        }
        BigDecimal total = reviews.stream()
                .map(review -> BigDecimal.valueOf(review.getRating()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new RatingSummary(dentistId, reviews.size(),
                total.divide(BigDecimal.valueOf(reviews.size()), 2, RoundingMode.HALF_UP));
    }

    private static Comparator<DentistReview> newestFirst() {
        return Comparator.comparing(DentistReview::getSubmittedAt,
                Comparator.nullsLast(Comparator.reverseOrder()));
    }
}
