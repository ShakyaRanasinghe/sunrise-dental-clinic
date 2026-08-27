package com.sunrise.clinic.dao;

import com.sunrise.clinic.db.Database;
import com.sunrise.clinic.domain.AuditEvent;
import com.sunrise.clinic.domain.Role;
import com.sunrise.clinic.repository.AuditRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
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
            statement.setString(3, enumName(event.getActorRole()));
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
                .actorRole(readEnum(rs, "actor_role", Role.class))
                .action(rs.getString("action"))
                .targetType(rs.getString("target_type"))
                .targetId(rs.getString("target_id"))
                .timestamp(readInstant(rs, "event_time"))
                .build();
    }
}
