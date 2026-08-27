package com.sunrise.clinic.access.data;

import com.sunrise.clinic.access.domain.Role;
import com.sunrise.clinic.access.domain.UserAccount;
import com.sunrise.clinic.platform.data.JdbcDao;
import com.sunrise.clinic.platform.db.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** MySQL-backed {@link UserRepository}. */
public class UserDao extends JdbcDao<UserAccount, String> implements UserRepository {

    private static final String COLUMNS =
            "uid, email, password_hash, display_name, role, active, failed_attempts, locked, created_at";

    public UserDao(Database db) {
        super(db);
    }

    @Override
    public UserAccount save(UserAccount user) {
        update("""
                INSERT INTO user_account (uid, email, password_hash, display_name, role,
                                          active, failed_attempts, locked, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    email = VALUES(email),
                    password_hash = VALUES(password_hash),
                    display_name = VALUES(display_name),
                    role = VALUES(role),
                    active = VALUES(active),
                    failed_attempts = VALUES(failed_attempts),
                    locked = VALUES(locked)
                """, statement -> bindUser(statement, user));
        return user;
    }

    @Override
    public Optional<UserAccount> findById(String uid) {
        return queryOne("SELECT " + COLUMNS + " FROM user_account WHERE uid = ?",
                statement -> statement.setString(1, uid),
                UserDao::mapUser);
    }

    @Override
    public Optional<UserAccount> findByEmail(String email) {
        return queryOne("SELECT " + COLUMNS + " FROM user_account WHERE email = ?",
                statement -> statement.setString(1, email),
                UserDao::mapUser);
    }

    @Override
    public List<UserAccount> findAll() {
        return queryList("SELECT " + COLUMNS + " FROM user_account ORDER BY display_name",
                NO_PARAMETERS, UserDao::mapUser);
    }

    /** Staff accounts the Administrator manages (everyone who is not a patient). */
    public List<UserAccount> findStaff() {
        return queryList("SELECT " + COLUMNS + " FROM user_account"
                        + " WHERE role <> 'PATIENT' ORDER BY role, display_name",
                NO_PARAMETERS, UserDao::mapUser);
    }

    @Override
    public void deleteById(String uid) {
        update("DELETE FROM user_account WHERE uid = ?", statement -> statement.setString(1, uid));
    }

    @Override
    public long count() {
        return queryCount("SELECT COUNT(*) FROM user_account");
    }

    private static void bindUser(PreparedStatement statement, UserAccount u) throws SQLException {
        statement.setString(1, u.getUid());
        statement.setString(2, u.getEmail());
        statement.setString(3, u.getPasswordHash());
        statement.setString(4, u.getDisplayName());
        statement.setString(5, enumName(u.getRole()));
        statement.setBoolean(6, u.isActive());
        statement.setInt(7, u.getFailedAttempts());
        statement.setBoolean(8, u.isLocked());
        statement.setTimestamp(9, toSqlTimestamp(u.getCreatedAt()));
    }

    private static UserAccount mapUser(ResultSet rs) throws SQLException {
        return UserAccount.builder()
                .uid(rs.getString("uid"))
                .email(rs.getString("email"))
                .passwordHash(rs.getString("password_hash"))
                .displayName(rs.getString("display_name"))
                .role(readEnum(rs, "role", Role.class))
                .active(rs.getBoolean("active"))
                .failedAttempts(rs.getInt("failed_attempts"))
                .locked(rs.getBoolean("locked"))
                .createdAt(readInstant(rs, "created_at"))
                .build();
    }
}
