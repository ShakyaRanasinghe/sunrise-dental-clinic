package com.sunrise.clinic.appointments.data;

/**
 * The per-day appointment number sequence.
 *
 * <p>New in the modular build, and it exists to close a defect the previous version
 * documented as already solved. {@code AppointmentNumberGenerator} kept its counters in
 * a {@code ConcurrentHashMap} and its javadoc claimed that "the
 * {@code appointment_counter} table in the schema extends the same guarantee across
 * restarts and multiple nodes". Nothing in Java ever read or wrote that table - only
 * {@code sp_register_appointment} did, and nothing called the procedure either.</p>
 *
 * <p>So the counter restarted at zero every time Tomcat did. The first booking after a
 * restart minted the number an earlier booking already held, and
 * {@code appointment.appointment_no} is the primary key - a duplicate-key failure on a
 * patient's booking, for no reason they could see or fix.</p>
 *
 * <p>Not a {@code Repository<T, ID>}: there is no entity here, only a number. An
 * interface of one method is the honest shape.</p>
 */
public interface CounterRepository {

    /**
     * Atomically take the next value for a day.
     *
     * <p>Must be called inside a transaction. Implementations serialise concurrent
     * callers - the JDBC one through the row lock its upsert takes on
     * {@code appointment_counter}, which is what makes the sequence gapless rather
     * than merely unique.</p>
     *
     * @param dayKey {@code yyyyMMdd}
     * @return 1 for the first appointment of that day, then 2, 3, …
     */
    int nextFor(String dayKey);

    /** The current value without taking one, for tests and reporting. */
    int currentFor(String dayKey);
}
