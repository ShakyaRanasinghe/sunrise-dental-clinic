package com.sunrise.clinic.platform.data;

import com.sunrise.clinic.platform.db.Database;

/**
 * MySQL-backed {@link PersonSequenceRepository}.
 *
 * <p>{@code INSERT … ON DUPLICATE KEY UPDATE} takes an exclusive lock on the key's
 * row, so a concurrent caller waits here rather than reading the same value. The
 * {@code SELECT} that follows is inside the same transaction and therefore sees
 * this transaction's own increment.</p>
 */
public class JdbcPersonSequenceRepository extends JdbcDao<Void, String>
        implements PersonSequenceRepository {

    public JdbcPersonSequenceRepository(Database db) {
        super(db);
    }

    @Override
    public int nextFor(String key) {
        update("""
                INSERT INTO person_counter (seq_key, counter_value)
                     VALUES (?, 1)
                ON DUPLICATE KEY UPDATE counter_value = counter_value + 1
                """, statement -> statement.setString(1, key));
        return queryOne("SELECT counter_value FROM person_counter WHERE seq_key = ?",
                statement -> statement.setString(1, key),
                rs -> rs.getInt(1)).orElse(0);
    }
}
