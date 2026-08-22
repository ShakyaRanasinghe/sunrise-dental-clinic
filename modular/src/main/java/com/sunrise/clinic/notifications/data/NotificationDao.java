package com.sunrise.clinic.notifications.data;

import com.sunrise.clinic.platform.data.JdbcDao;

import com.sunrise.clinic.platform.db.Database;
import com.sunrise.clinic.notifications.domain.ChannelType;
import com.sunrise.clinic.notifications.domain.Notification;
import com.sunrise.clinic.notifications.domain.NotificationKind;
import com.sunrise.clinic.notifications.domain.NotificationStatus;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** MySQL-backed {@link NotificationRepository} — delivery receipts. */
public class NotificationDao extends JdbcDao<Notification, String> implements NotificationRepository {

    private static final String COLUMNS =
            "id, appointment_no, channel, kind, recipient, subject, body, status, sent_at";

    public NotificationDao(Database db) {
        super(db);
    }

    @Override
    public Notification save(Notification notification) {
        update("""
                INSERT INTO notification (id, appointment_no, channel, kind, recipient, subject,
                                          body, status, sent_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    status = VALUES(status),
                    sent_at = VALUES(sent_at)
                """, statement -> bindNotification(statement, notification));
        return notification;
    }

    @Override
    public Optional<Notification> findById(String id) {
        return queryOne("SELECT " + COLUMNS + " FROM notification WHERE id = ?",
                statement -> statement.setString(1, id),
                NotificationDao::mapNotification);
    }

    @Override
    public List<Notification> findByAppointmentNo(String appointmentNo) {
        return queryList("SELECT " + COLUMNS + " FROM notification WHERE appointment_no = ?"
                        + " ORDER BY sent_at DESC",
                statement -> statement.setString(1, appointmentNo),
                NotificationDao::mapNotification);
    }

    @Override
    public List<Notification> findAll() {
        return queryList("SELECT " + COLUMNS + " FROM notification ORDER BY sent_at DESC LIMIT 500",
                NO_PARAMETERS, NotificationDao::mapNotification);
    }

    @Override
    public boolean hasBeenAttempted(String appointmentNo, NotificationKind kind) {
        return queryOne("SELECT COUNT(*) FROM notification WHERE appointment_no = ? AND kind = ?",
                statement -> {
                    statement.setString(1, appointmentNo);
                    statement.setString(2, enumName(kind));
                },
                rs -> rs.getInt(1)).orElse(0) > 0;
    }

    @Override
    public void deleteById(String id) {
        update("DELETE FROM notification WHERE id = ?", statement -> statement.setString(1, id));
    }

    @Override
    public long count() {
        return queryCount("SELECT COUNT(*) FROM notification");
    }

    private static void bindNotification(PreparedStatement statement, Notification n) throws SQLException {
        statement.setString(1, n.getId());
        statement.setString(2, n.getAppointmentNo());
        statement.setString(3, enumName(n.getChannel()));
        statement.setString(4, enumName(n.getKind()));
        statement.setString(5, n.getRecipient());
        statement.setString(6, n.getSubject());
        statement.setString(7, n.getBody());
        statement.setString(8, enumName(n.getStatus()));
        statement.setTimestamp(9, toSqlTimestamp(n.getSentAt()));
    }

    private static Notification mapNotification(ResultSet rs) throws SQLException {
        return Notification.builder()
                .id(rs.getString("id"))
                .appointmentNo(rs.getString("appointment_no"))
                .channel(readEnum(rs, "channel", ChannelType.class))
                .kind(readEnum(rs, "kind", NotificationKind.class))
                .recipient(rs.getString("recipient"))
                .subject(rs.getString("subject"))
                .body(rs.getString("body"))
                .status(readEnum(rs, "status", NotificationStatus.class))
                .sentAt(readInstant(rs, "sent_at"))
                .build();
    }
}
