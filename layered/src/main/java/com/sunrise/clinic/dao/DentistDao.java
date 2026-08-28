package com.sunrise.clinic.dao;

import com.sunrise.clinic.db.Database;
import com.sunrise.clinic.domain.Dentist;
import com.sunrise.clinic.repository.DentistRepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** MySQL-backed {@link DentistRepository}. */
public class DentistDao extends JdbcDao<Dentist, String> implements DentistRepository {

    private static final String COLUMNS =
            "id, user_uid, name, specialization, consultation_fee, active";

    public DentistDao(Database db) {
        super(db);
    }

    @Override
    public Dentist save(Dentist dentist) {
        update("""
                INSERT INTO dentist (id, user_uid, name, specialization, consultation_fee, active)
                VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    user_uid = VALUES(user_uid),
                    name = VALUES(name),
                    specialization = VALUES(specialization),
                    consultation_fee = VALUES(consultation_fee),
                    active = VALUES(active)
                """, statement -> bindDentist(statement, dentist));
        return dentist;
    }

    @Override
    public Optional<Dentist> findById(String id) {
        return queryOne("SELECT " + COLUMNS + " FROM dentist WHERE id = ?",
                statement -> statement.setString(1, id),
                DentistDao::mapDentist);
    }

    @Override
    public Optional<Dentist> findByUserUid(String userUid) {
        return queryOne("SELECT " + COLUMNS + " FROM dentist WHERE user_uid = ?",
                statement -> statement.setString(1, userUid),
                DentistDao::mapDentist);
    }

    @Override
    public List<Dentist> findAll() {
        return queryList("SELECT " + COLUMNS + " FROM dentist ORDER BY name",
                NO_PARAMETERS, DentistDao::mapDentist);
    }

    /** Only dentists currently practising — what the booking screen offers. */
    public List<Dentist> findActive() {
        return queryList("SELECT " + COLUMNS + " FROM dentist WHERE active = TRUE ORDER BY name",
                NO_PARAMETERS, DentistDao::mapDentist);
    }

    @Override
    public void deleteById(String id) {
        update("DELETE FROM dentist WHERE id = ?", statement -> statement.setString(1, id));
    }

    @Override
    public long count() {
        return queryCount("SELECT COUNT(*) FROM dentist");
    }

    private static void bindDentist(PreparedStatement statement, Dentist d) throws SQLException {
        statement.setString(1, d.getId());
        statement.setString(2, d.getUserUid());
        statement.setString(3, d.getName());
        statement.setString(4, d.getSpecialization());
        statement.setDouble(5, d.getConsultationFee());
        statement.setBoolean(6, d.isActive());
    }

    private static Dentist mapDentist(ResultSet rs) throws SQLException {
        return Dentist.builder()
                .id(rs.getString("id"))
                .userUid(rs.getString("user_uid"))
                .name(rs.getString("name"))
                .specialization(rs.getString("specialization"))
                .consultationFee(rs.getDouble("consultation_fee"))
                .active(rs.getBoolean("active"))
                .build();
    }
}
