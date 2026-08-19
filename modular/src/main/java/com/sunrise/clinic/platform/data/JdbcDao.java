package com.sunrise.clinic.platform.data;

import com.sunrise.clinic.platform.error.DataAccessException;

import com.sunrise.clinic.platform.db.Database;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Shared JDBC plumbing for the DAOs.
 *
 * <p>Every query in this project is written by hand against {@code java.sql} —
 * there is no ORM — so this base class exists to keep that from becoming
 * repetitive. It owns three things:</p>
 *
 * <ul>
 *   <li>the try-with-resources template for queries and updates, so no
 *       {@link PreparedStatement} or {@link ResultSet} is ever leaked;</li>
 *   <li>the conversions between MySQL's {@code DATE}/{@code TIME}/{@code TIMESTAMP}
 *       and the {@code java.time} types the domain model uses;</li>
 *   <li>translation of {@link SQLException} into an unchecked
 *       {@link DataAccessException}, so the service layer is not forced to handle
 *       checked exceptions from a storage detail.</li>
 * </ul>
 *
 * <p>All statements are parameterised. String concatenation is never used to build
 * SQL, which is what makes the application immune to SQL injection.</p>
 *
 * @param <T>  the entity type
 * @param <ID> the identifier type
 */
public abstract class JdbcDao<T, ID> {

    /** Binds the parameters of a prepared statement. */
    @FunctionalInterface
    protected interface Binder {
        void bind(PreparedStatement statement) throws SQLException;
    }

    /** Maps the current row of a result set to an object. */
    @FunctionalInterface
    protected interface RowMapper<R> {
        R map(ResultSet rs) throws SQLException;
    }

    protected static final Binder NO_PARAMETERS = statement -> {
    };

    protected final Database db;

    protected JdbcDao(Database db) {
        this.db = db;
    }

    // ------------------------------------------------------------------
    // Query templates
    // ------------------------------------------------------------------

    /** Run a query and map every row. */
    protected <R> List<R> queryList(String sql, Binder binder, RowMapper<R> mapper) {
        try (Connection connection = db.borrow();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            try (ResultSet rs = statement.executeQuery()) {
                List<R> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(mapper.map(rs));
                }
                return results;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Query failed: " + sql, e);
        }
    }

    /** Run a query expected to match at most one row. */
    protected <R> Optional<R> queryOne(String sql, Binder binder, RowMapper<R> mapper) {
        List<R> results = queryList(sql, binder, mapper);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /** Run an INSERT/UPDATE/DELETE and return the affected row count. */
    protected int update(String sql, Binder binder) {
        try (Connection connection = db.borrow();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Update failed: " + sql, e);
        }
    }

    /** Run a {@code SELECT COUNT(*)} style query. */
    protected long queryCount(String sql) {
        return queryOne(sql, NO_PARAMETERS, rs -> rs.getLong(1)).orElse(0L);
    }

    // ------------------------------------------------------------------
    // java.time <-> java.sql conversions
    // ------------------------------------------------------------------

    protected static Date toSqlDate(LocalDate value) {
        return value == null ? null : Date.valueOf(value);
    }

    protected static Time toSqlTime(LocalTime value) {
        return value == null ? null : Time.valueOf(value);
    }

    protected static Timestamp toSqlTimestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    protected static LocalDate readDate(ResultSet rs, String column) throws SQLException {
        Date value = rs.getDate(column);
        return value == null ? null : value.toLocalDate();
    }

    protected static LocalTime readTime(ResultSet rs, String column) throws SQLException {
        Time value = rs.getTime(column);
        return value == null ? null : value.toLocalTime();
    }

    protected static Instant readInstant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    /** Read an enum column, tolerating NULL. */
    protected static <E extends Enum<E>> E readEnum(ResultSet rs, String column, Class<E> type)
            throws SQLException {
        String value = rs.getString(column);
        return value == null ? null : Enum.valueOf(type, value);
    }

    /** Write an enum as its name, tolerating null. */
    protected static String enumName(Enum<?> value) {
        return value == null ? null : value.name();
    }
}
