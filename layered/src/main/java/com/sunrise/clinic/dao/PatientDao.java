package com.sunrise.clinic.dao;

import com.sunrise.clinic.db.Database;
import com.sunrise.clinic.domain.Patient;
import com.sunrise.clinic.repository.PatientRepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** MySQL-backed {@link PatientRepository}. */
public class PatientDao extends JdbcDao<Patient, String> implements PatientRepository {

    private static final String COLUMNS =
            "id, user_uid, name, address, contact_number, email, dob";

    public PatientDao(Database db) {
        super(db);
    }

    @Override
    public Patient save(Patient patient) {
        update("""
                INSERT INTO patient (id, user_uid, name, address, contact_number, email, dob)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    user_uid = VALUES(user_uid),
                    name = VALUES(name),
                    address = VALUES(address),
                    contact_number = VALUES(contact_number),
                    email = VALUES(email),
                    dob = VALUES(dob)
                """, statement -> bindPatient(statement, patient));
        return patient;
    }

    @Override
    public Optional<Patient> findById(String id) {
        return queryOne("SELECT " + COLUMNS + " FROM patient WHERE id = ?",
                statement -> statement.setString(1, id),
                PatientDao::mapPatient);
    }

    @Override
    public Optional<Patient> findByUserUid(String userUid) {
        return queryOne("SELECT " + COLUMNS + " FROM patient WHERE user_uid = ?",
                statement -> statement.setString(1, userUid),
                PatientDao::mapPatient);
    }

    @Override
    public List<Patient> findAll() {
        return queryList("SELECT " + COLUMNS + " FROM patient ORDER BY name",
                NO_PARAMETERS, PatientDao::mapPatient);
    }

    /** Front-desk search by name, contact number or email. */
    public List<Patient> search(String term) {
        String pattern = "%" + term + "%";
        return queryList("SELECT " + COLUMNS + " FROM patient"
                        + " WHERE name LIKE ? OR contact_number LIKE ? OR email LIKE ?"
                        + " ORDER BY name LIMIT 50",
                statement -> {
                    statement.setString(1, pattern);
                    statement.setString(2, pattern);
                    statement.setString(3, pattern);
                },
                PatientDao::mapPatient);
    }

    @Override
    public void deleteById(String id) {
        update("DELETE FROM patient WHERE id = ?", statement -> statement.setString(1, id));
    }

    @Override
    public long count() {
        return queryCount("SELECT COUNT(*) FROM patient");
    }

    private static void bindPatient(PreparedStatement statement, Patient p) throws SQLException {
        statement.setString(1, p.getId());
        statement.setString(2, p.getUserUid());
        statement.setString(3, p.getName());
        statement.setString(4, p.getAddress());
        statement.setString(5, p.getContactNumber());
        statement.setString(6, p.getEmail());
        statement.setDate(7, toSqlDate(p.getDob()));
    }

    private static Patient mapPatient(ResultSet rs) throws SQLException {
        return Patient.builder()
                .id(rs.getString("id"))
                .userUid(rs.getString("user_uid"))
                .name(rs.getString("name"))
                .address(rs.getString("address"))
                .contactNumber(rs.getString("contact_number"))
                .email(rs.getString("email"))
                .dob(readDate(rs, "dob"))
                .build();
    }
}
