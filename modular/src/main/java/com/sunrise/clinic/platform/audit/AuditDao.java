package com.sunrise.clinic.platform.audit;

import com.sunrise.clinic.platform.data.JdbcDao;

import com.sunrise.clinic.platform.db.Database;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * MySQL-backed {@link AuditRepository}.
 *
 * <p>The audit trail is append-only by design, so {@code save} is a plain INSERT
 * with no upsert clause and {@link #deleteById(String)} deliberately refuses to
 * delete: an audit record that can be edited or removed is not an audit record.</p>
 */
public class AuditDao extends JdbcDao<AuditEvent, String> implements AuditRepository {

    private static final String COLUMNS =
            "id, actor_uid, actor_role, action, target_type, target_id, event_time";

    public AuditDao(Database db) {
        super(db);
    }

    @Override
    public AuditEvent save(AuditEvent event) {
        update("""
                INSERT INTO audit_event (id, actor_uid, actor_role, action, target_type,
                                         target_id, event_time)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, statement -> {
            statement.setString(1, event.getId());
            statement.setString(2, event.getActorUid());
            statement.setString(3, event.getActorRole());
            statement.setString(4, event.getAction());
            statement.setString(5, event.getTargetType());
            statement.setString(6, event.getTargetId());
            statement.setTimestamp(7, toSqlTimestamp(event.getTimestamp()));
        });
        return event;
    }

    @Override
    public Optional<AuditEvent> findById(String id) {
        return queryOne("SELECT " + COLUMNS + " FROM audit_event WHERE id = ?",
                statement -> statement.setString(1, id),
                AuditDao::mapEvent);
    }

    @Override
    public List<AuditEvent> findAll() {
        return queryList("SELECT " + COLUMNS + " FROM audit_event ORDER BY event_time DESC LIMIT 500",
                NO_PARAMETERS, AuditDao::mapEvent);
    }

    /** Everything that happened to one entity — what an Admin reviews. */
    public List<AuditEvent> findByTarget(String targetType, String targetId) {
        return queryList("SELECT " + COLUMNS + " FROM audit_event"
                        + " WHERE target_type = ? AND target_id = ? ORDER BY event_time DESC",
                statement -> {
                    statement.setString(1, targetType);
                    statement.setString(2, targetId);
                },
                AuditDao::mapEvent);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Built as a dynamic {@code WHERE} rather than four separate queries, because the
     * filters combine. Every value still goes in as a bound parameter - the clauses are
     * chosen by the code, never assembled from what the administrator typed.</p>
     */
    @Override
    public List<AuditEvent> search(String actorUid, String targetId,
                                   LocalDate from, LocalDate to, int limit) {
        StringBuilder sql = new StringBuilder("SELECT " + COLUMNS + " FROM audit_event WHERE 1 = 1");
        List<Object> values = new ArrayList<>();
        if (actorUid != null && !actorUid.isBlank()) {
            sql.append(" AND actor_uid = ?");
            values.add(actorUid.trim());
        }
        if (targetId != null && !targetId.isBlank()) {
            sql.append(" AND target_id = ?");
            values.add(targetId.trim());
        }
        if (from != null) {
            sql.append(" AND DATE(event_time) >= ?");
            values.add(Date.valueOf(from));
        }
        if (to != null) {
            sql.append(" AND DATE(event_time) <= ?");
            values.add(Date.valueOf(to));
        }
        sql.append(" ORDER BY event_time DESC LIMIT ").append(Math.max(1, Math.min(limit, 1000)));

        return queryList(sql.toString(), statement -> {
            for (int i = 0; i < values.size(); i++) {
                statement.setObject(i + 1, values.get(i));
            }
        }, AuditDao::mapEvent);
    }

    @Override
    public void deleteById(String id) {
        throw new UnsupportedOperationException("The audit trail is append-only and cannot be deleted");
    }

    @Override
    public long count() {
        return queryCount("SELECT COUNT(*) FROM audit_event");
    }

    private static AuditEvent mapEvent(ResultSet rs) throws SQLException {
        return AuditEvent.builder()
                .id(rs.getString("id"))
                .actorUid(rs.getString("actor_uid"))
                .actorRole(rs.getString("actor_role"))
                .action(rs.getString("action"))
                .targetType(rs.getString("target_type"))
                .targetId(rs.getString("target_id"))
                .timestamp(readInstant(rs, "event_time"))
                .build();
    }
}
