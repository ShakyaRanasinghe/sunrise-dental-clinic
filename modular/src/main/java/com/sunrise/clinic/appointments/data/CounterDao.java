package com.sunrise.clinic.appointments.data;

import com.sunrise.clinic.platform.data.JdbcDao;
import com.sunrise.clinic.platform.db.Database;

/**
 * MySQL-backed {@link CounterRepository}.
 *
 * <p>The upsert and the read are the same two statements
 * {@code sp_register_appointment} uses, deliberately: the procedure remains in the
 * schema as the database-side equivalent of this transaction, and if the two computed
 * numbers differently the schema documentation would be wrong.</p>
 */
public class CounterDao extends JdbcDao<Void, String> implements CounterRepository {

    // JdbcDao is a helper base, not a Repository, so there are no entity methods to
    // implement here - which suits a table that holds a number rather than a record.

    public CounterDao(Database db) {
        super(db);
    }

    /**
     * {@inheritDoc}
     *
     * <p>{@code INSERT … ON DUPLICATE KEY UPDATE} takes an exclusive lock on the day's
     * row, so a concurrent caller waits here rather than reading the same value. The
     * {@code SELECT} that follows is inside the same transaction and therefore sees
     * this transaction's own increment.</p>
     */
    @Override
    public int nextFor(String dayKey) {
        update("""
                INSERT INTO appointment_counter (day_key, counter_value)
                     VALUES (?, 1)
                ON DUPLICATE KEY UPDATE counter_value = counter_value + 1
                """, statement -> statement.setString(1, dayKey));
        return read(dayKey);
    }

    @Override
    public int currentFor(String dayKey) {
        return read(dayKey);
    }

    private int read(String dayKey) {
        return queryOne("SELECT counter_value FROM appointment_counter WHERE day_key = ?",
                statement -> statement.setString(1, dayKey),
                rs -> rs.getInt(1)).orElse(0);
    }

    /** How many days have ever had an appointment. Used by the reporting screens. */
    public long daysWithAppointments() {
        return queryCount("SELECT COUNT(*) FROM appointment_counter");
    }
}
