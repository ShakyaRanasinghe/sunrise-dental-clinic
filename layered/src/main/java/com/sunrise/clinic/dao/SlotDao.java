package com.sunrise.clinic.dao;

import com.sunrise.clinic.db.Database;
import com.sunrise.clinic.domain.Slot;
import com.sunrise.clinic.domain.SlotStatus;
import com.sunrise.clinic.repository.SlotRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** MySQL-backed {@link SlotRepository}. */
public class SlotDao extends JdbcDao<Slot, String> implements SlotRepository {

    private static final String COLUMNS =
            "id, session_id, dentist_id, slot_date, start_time, duration_minutes, status, appointment_no";

    public SlotDao(Database db) {
        super(db);
    }

    @Override
    public Slot save(Slot slot) {
        // MySQL upsert: one round trip whether the slot is new or being updated.
        update("""
                INSERT INTO slot (id, session_id, dentist_id, slot_date, start_time,
                                  duration_minutes, status, appointment_no)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    session_id = VALUES(session_id),
                    dentist_id = VALUES(dentist_id),
                    slot_date = VALUES(slot_date),
                    start_time = VALUES(start_time),
                    duration_minutes = VALUES(duration_minutes),
                    status = VALUES(status),
                    appointment_no = VALUES(appointment_no)
                """, statement -> bindSlot(statement, slot));
        return slot;
    }

    /**
     * Read a slot <b>and hold a row lock on it</b> until the surrounding
     * transaction commits.
     *
     * <p>This is the double-booking guard. Two concurrent bookings for the same
     * slot both reach this statement; MySQL lets only one of them through, and the
     * second blocks until the first commits — by which time the slot is BOOKED and
     * the caller correctly fails with a conflict. Reading without the lock would
     * let both transactions see OPEN and both proceed.</p>
     *
     * @param slotId the slot to lock
     * @return the locked slot, if it exists
     */
    public Optional<Slot> findByIdForUpdate(String slotId) {
        return queryOne("SELECT " + COLUMNS + " FROM slot WHERE id = ? FOR UPDATE",
                statement -> statement.setString(1, slotId),
                SlotDao::mapSlot);
    }

    @Override
    public Optional<Slot> findById(String id) {
        return queryOne("SELECT " + COLUMNS + " FROM slot WHERE id = ?",
                statement -> statement.setString(1, id),
                SlotDao::mapSlot);
    }

    @Override
    public List<Slot> findAll() {
        return queryList("SELECT " + COLUMNS + " FROM slot ORDER BY slot_date, start_time",
                NO_PARAMETERS, SlotDao::mapSlot);
    }

    @Override
    public List<Slot> findByDentistIdAndDate(String dentistId, LocalDate date) {
        return queryList("SELECT " + COLUMNS + " FROM slot WHERE dentist_id = ? AND slot_date = ?"
                        + " ORDER BY start_time",
                statement -> {
                    statement.setString(1, dentistId);
                    statement.setDate(2, toSqlDate(date));
                },
                SlotDao::mapSlot);
    }

    @Override
    public List<Slot> findByDateBetween(LocalDate from, LocalDate to) {
        return queryList("SELECT " + COLUMNS + " FROM slot WHERE slot_date BETWEEN ? AND ?"
                        + " ORDER BY slot_date, start_time",
                statement -> {
                    statement.setDate(1, toSqlDate(from));
                    statement.setDate(2, toSqlDate(to));
                },
                SlotDao::mapSlot);
    }

    @Override
    public List<Slot> findBySessionId(String sessionId) {
        return queryList("SELECT " + COLUMNS + " FROM slot WHERE session_id = ? ORDER BY start_time",
                statement -> statement.setString(1, sessionId),
                SlotDao::mapSlot);
    }

    @Override
    public void deleteById(String id) {
        update("DELETE FROM slot WHERE id = ?", statement -> statement.setString(1, id));
    }

    @Override
    public long count() {
        return queryCount("SELECT COUNT(*) FROM slot");
    }

    private static void bindSlot(PreparedStatement statement, Slot slot) throws SQLException {
        statement.setString(1, slot.getId());
        statement.setString(2, slot.getSessionId());
        statement.setString(3, slot.getDentistId());
        statement.setDate(4, toSqlDate(slot.getDate()));
        statement.setTime(5, toSqlTime(slot.getStartTime()));
        statement.setInt(6, slot.getDurationMinutes());
        statement.setString(7, enumName(slot.getStatus()));
        statement.setString(8, slot.getAppointmentNo());
    }

    private static Slot mapSlot(ResultSet rs) throws SQLException {
        return Slot.builder()
                .id(rs.getString("id"))
                .sessionId(rs.getString("session_id"))
                .dentistId(rs.getString("dentist_id"))
                .date(readDate(rs, "slot_date"))
                .startTime(readTime(rs, "start_time"))
                .durationMinutes(rs.getInt("duration_minutes"))
                .status(readEnum(rs, "status", SlotStatus.class))
                .appointmentNo(rs.getString("appointment_no"))
                .build();
    }

    /** Used by the schema bootstrap to check whether demo data is needed. */
    static boolean tableIsEmpty(Connection connection, String table) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM " + table);
             ResultSet rs = statement.executeQuery()) {
            return rs.next() && rs.getLong(1) == 0;
        }
    }
}
