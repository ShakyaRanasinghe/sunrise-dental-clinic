package com.sunrise.clinic.scheduling.data;

import com.sunrise.clinic.platform.data.JdbcDao;

import com.sunrise.clinic.platform.db.Database;
import com.sunrise.clinic.scheduling.domain.DentistSession;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** MySQL-backed {@link SessionRepository} — published dentist availability windows. */
public class SessionDao extends JdbcDao<DentistSession, String> implements SessionRepository {

    private static final String COLUMNS =
            "id, dentist_id, session_date, start_time, end_time, slot_duration_minutes, published_by_uid";

    public SessionDao(Database db) {
        super(db);
    }

    @Override
    public DentistSession save(DentistSession session) {
        update("""
                INSERT INTO dentist_session (id, dentist_id, session_date, start_time, end_time,
                                             slot_duration_minutes, published_by_uid)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    dentist_id = VALUES(dentist_id),
                    session_date = VALUES(session_date),
                    start_time = VALUES(start_time),
                    end_time = VALUES(end_time),
                    slot_duration_minutes = VALUES(slot_duration_minutes),
                    published_by_uid = VALUES(published_by_uid)
                """, statement -> bindSession(statement, session));
        return session;
    }

    @Override
    public Optional<DentistSession> findById(String id) {
        return queryOne("SELECT " + COLUMNS + " FROM dentist_session WHERE id = ?",
                statement -> statement.setString(1, id),
                SessionDao::mapSession);
    }

    @Override
    public List<DentistSession> findAll() {
        return queryList("SELECT " + COLUMNS + " FROM dentist_session ORDER BY session_date DESC, start_time",
                NO_PARAMETERS, SessionDao::mapSession);
    }

    @Override
    public List<DentistSession> findByDentistId(String dentistId) {
        return queryList("SELECT " + COLUMNS + " FROM dentist_session WHERE dentist_id = ?"
                        + " ORDER BY session_date DESC, start_time",
                statement -> statement.setString(1, dentistId),
                SessionDao::mapSession);
    }

    @Override
    public List<DentistSession> findByDate(LocalDate date) {
        return queryList("SELECT " + COLUMNS + " FROM dentist_session WHERE session_date = ?"
                        + " ORDER BY start_time",
                statement -> statement.setDate(1, toSqlDate(date)),
                SessionDao::mapSession);
    }

    @Override
    public List<DentistSession> findFromDate(LocalDate from) {
        return queryList("SELECT " + COLUMNS + " FROM dentist_session WHERE session_date >= ?"
                        + " ORDER BY session_date, start_time",
                statement -> statement.setDate(1, toSqlDate(from)),
                SessionDao::mapSession);
    }

    @Override
    public void deleteById(String id) {
        update("DELETE FROM dentist_session WHERE id = ?", statement -> statement.setString(1, id));
    }

    @Override
    public long count() {
        return queryCount("SELECT COUNT(*) FROM dentist_session");
    }

    private static void bindSession(PreparedStatement statement, DentistSession s) throws SQLException {
        statement.setString(1, s.getId());
        statement.setString(2, s.getDentistId());
        statement.setDate(3, toSqlDate(s.getDate()));
        statement.setTime(4, toSqlTime(s.getStartTime()));
        statement.setTime(5, toSqlTime(s.getEndTime()));
        statement.setInt(6, s.getSlotDurationMinutes());
        statement.setString(7, s.getPublishedByUid());
    }

    private static DentistSession mapSession(ResultSet rs) throws SQLException {
        return DentistSession.builder()
                .id(rs.getString("id"))
                .dentistId(rs.getString("dentist_id"))
                .date(readDate(rs, "session_date"))
                .startTime(readTime(rs, "start_time"))
                .endTime(readTime(rs, "end_time"))
                .slotDurationMinutes(rs.getInt("slot_duration_minutes"))
                .publishedByUid(rs.getString("published_by_uid"))
                .build();
    }
}
