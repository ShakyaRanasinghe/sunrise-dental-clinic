package com.sunrise.clinic.scheduling.data;

import com.sunrise.clinic.platform.data.JdbcDao;

import com.sunrise.clinic.platform.db.Database;
import com.sunrise.clinic.scheduling.domain.Treatment;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** MySQL-backed {@link TreatmentRepository} — the Admin-maintained catalogue. */
public class TreatmentDao extends JdbcDao<Treatment, String> implements TreatmentRepository {

    private static final String COLUMNS = "id, name, description, base_cost, active";

    public TreatmentDao(Database db) {
        super(db);
    }

    @Override
    public Treatment save(Treatment treatment) {
        update("""
                INSERT INTO treatment (id, name, description, base_cost, active)
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    name = VALUES(name),
                    description = VALUES(description),
                    base_cost = VALUES(base_cost),
                    active = VALUES(active)
                """, statement -> bindTreatment(statement, treatment));
        return treatment;
    }

    @Override
    public Optional<Treatment> findById(String id) {
        return queryOne("SELECT " + COLUMNS + " FROM treatment WHERE id = ?",
                statement -> statement.setString(1, id),
                TreatmentDao::mapTreatment);
    }

    @Override
    public List<Treatment> findAll() {
        return queryList("SELECT " + COLUMNS + " FROM treatment ORDER BY name",
                NO_PARAMETERS, TreatmentDao::mapTreatment);
    }

    @Override
    public List<Treatment> findActive() {
        return queryList("SELECT " + COLUMNS + " FROM treatment WHERE active = TRUE ORDER BY name",
                NO_PARAMETERS, TreatmentDao::mapTreatment);
    }

    @Override
    public void deleteById(String id) {
        update("DELETE FROM treatment WHERE id = ?", statement -> statement.setString(1, id));
    }

    @Override
    public long count() {
        return queryCount("SELECT COUNT(*) FROM treatment");
    }

    private static void bindTreatment(PreparedStatement statement, Treatment t) throws SQLException {
        statement.setString(1, t.getId());
        statement.setString(2, t.getName());
        statement.setString(3, t.getDescription());
        statement.setBigDecimal(4, t.getBaseCost());
        statement.setBoolean(5, t.isActive());
    }

    private static Treatment mapTreatment(ResultSet rs) throws SQLException {
        return Treatment.builder()
                .id(rs.getString("id"))
                .name(rs.getString("name"))
                .description(rs.getString("description"))
                .baseCost(rs.getBigDecimal("base_cost"))
                .active(rs.getBoolean("active"))
                .build();
    }
}
