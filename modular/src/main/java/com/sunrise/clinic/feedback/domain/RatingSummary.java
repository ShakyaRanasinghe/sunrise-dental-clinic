package com.sunrise.clinic.feedback.domain;

import java.math.BigDecimal;

/**
 * What a dentist is allowed to know about their own reviews - FR-RVW-07, FR-RVW-08.
 *
 * <p>A mean and a count, and nothing else. No individual rating, no comment, no dates - a
 * dentist who could see "3 stars, last Tuesday afternoon" would know exactly who wrote it,
 * and a review the patient believes is confidential is not confidential.</p>
 *
 * <h2>The five-review floor</h2>
 *
 * <p>{@code mean} is null below five reviews (FR-RVW-12), and {@link #isPublishable()} says
 * so. One bad visit should not define a career, and an average of two is not a measurement -
 * it is two opinions with a decimal point. The same floor is in {@code fn_dentist_rating},
 * which returns NULL under five, so the rule holds whether it is asked in Java or in SQL.</p>
 *
 * @param dentistId  whose reviews these are
 * @param reviews    how many have been left
 * @param mean       the average to two places, or <b>null</b> below the floor
 */
public record RatingSummary(String dentistId, int reviews, BigDecimal mean) {

    /** Reviews needed before an average means anything. */
    public static final int FLOOR = 5;

    public static RatingSummary none(String dentistId) {
        return new RatingSummary(dentistId, 0, null);
    }

    /** @return true if there are enough reviews for the mean to be shown at all. */
    public boolean isPublishable() {
        return mean != null && reviews >= FLOOR;
    }

    /** @return how many more reviews are needed before the mean appears. */
    public int reviewsUntilPublishable() {
        return Math.max(0, FLOOR - reviews);
    }
}
