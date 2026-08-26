package com.sunrise.clinic.platform.data;

import com.sunrise.clinic.platform.db.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * JDBC persistence for {@link ClinicSettingRepository}.
 */
public class ClinicSettingDao
        extends JdbcDao<String, String>
        implements ClinicSettingRepository {

    private static final String COLUMNS = "setting_key, setting_value";

    public ClinicSettingDao(Database database) {
        super(database);
    }

    @Override
    public Optional<String> get(String key) {
        return queryOne(
                "SELECT setting_value FROM clinic_setting WHERE setting_key = ?",
                stmt -> stmt.setString(1, key),
                rs -> rs.getString("setting_value"));
    }

    @Override
    public Map<String, String> getAll() {
        Map<String, String> map = new HashMap<>();
        queryList("SELECT " + COLUMNS + " FROM clinic_setting",
                stmt -> {},
                rs -> {
                    map.put(rs.getString("setting_key"),
                            rs.getString("setting_value"));
                    return null;
                });
        return map;
    }

    @Override
    public void save(String key, String value) {
        update(
                "INSERT INTO clinic_setting (setting_key, setting_value) VALUES (?, ?)"
                        + " ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value)",
                stmt -> {
                    stmt.setString(1, key);
                    stmt.setString(2, value);
                });
    }
}
