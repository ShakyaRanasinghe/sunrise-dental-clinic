package com.sunrise.clinic.platform.data;

/**
 * The per-day-per-role sequence behind readable person numbers (GAP-PAT-33).
 *
 * <p>Same row-lock contract as the appointment counter, but a separate table
 * ({@code person_counter}) so reporting's day counts stay clean. Keys look like
 * {@code PAT-20260904}: role code, then the day.</p>
 *
 * <p>Not a {@code Repository<T, ID>}: there is no entity here, only a number.</p>
 */
public interface PersonSequenceRepository {

    /**
     * The next value for {@code key}, starting at 1.
     *
     * <p>Must be called inside the creation transaction where the number is used:
     * if the transaction rolls back the increment rolls back with it, so a failed
     * registration does not burn a number.</p>
     */
    int nextFor(String key);
}
