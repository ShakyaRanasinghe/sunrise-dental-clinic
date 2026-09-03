package com.sunrise.clinic.patients.data;

import com.sunrise.clinic.patients.domain.Patient;
import com.sunrise.clinic.platform.db.Database;
import com.sunrise.clinic.platform.data.JdbcDao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * MySQL-backed {@link PatientRepository}.
 *
 * <p>{@code search} now carries {@code @Override}, because the port declares it -
 * see {@link PatientRepository}.</p>
 */
public class PatientDao extends JdbcDao<Patient, String> implements PatientRepository {

    // GAP-PAT-27: diagnosis_details carries the patient's own history details.
    private static final String COLUMNS =
            "id, user_uid, name, address, contact_number, email, dob, diagnosis_details";

    public PatientDao(Database db) {
        super(db);
    }

    @Override
    public Patient save(Patient patient) {
        update("""
                INSERT INTO patient (id, user_uid, name, address, contact_number, email, dob, diagnosis_details)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    user_uid = VALUES(user_uid),
                    name = VALUES(name),
                    address = VALUES(address),
                    contact_number = VALUES(contact_number),
                    email = VALUES(email),
                    dob = VALUES(dob),
                    diagnosis_details = VALUES(diagnosis_details)
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
    @Override
    public List<Patient> search(String term) {
        String pattern = "%" + term + "%";
        return queryList("SELECT " + COLUMNS + " FROM patient"
                        + " WHERE " + WHERE_TERM
                        + " ORDER BY name LIMIT 50",
                statement -> {
                    statement.setString(1, pattern);
                    statement.setString(2, pattern);
                    statement.setString(3, pattern);
                },
                PatientDao::mapPatient);
    }

    /**
     * One page of the register. The term and the page limits are all bound
     * parameters, and the page only stretches to {@code LIMIT} rows, so a long
     * register never ships the whole list to the front desk in one request.
     */
    @Override
    public List<Patient> page(String term, long offset, int limit) {
        if (isBlank(term)) {
            return queryList("SELECT " + COLUMNS + " FROM patient ORDER BY name LIMIT ? OFFSET ?",
                    statement -> {
                        statement.setInt(1, limit);
                        statement.setLong(2, offset);
                    },
                    PatientDao::mapPatient);
        }
        String pattern = "%" + term + "%";
        return queryList("SELECT " + COLUMNS + " FROM patient WHERE " + WHERE_TERM
                        + " ORDER BY name LIMIT ? OFFSET ?",
                statement -> {
                    statement.setString(1, pattern);
                    statement.setString(2, pattern);
                    statement.setString(3, pattern);
                    statement.setInt(4, limit);
                    statement.setLong(5, offset);
                },
                PatientDao::mapPatient);
    }

    /** The size of the page {@link #page(String, long, int)} addresses. */
    @Override
    public long countMatching(String term) {
        if (isBlank(term)) {
            return count();
        }
        String pattern = "%" + term + "%";
        return queryCount("SELECT COUNT(*) FROM patient WHERE " + WHERE_TERM,
                statement -> {
                    statement.setString(1, pattern);
                    statement.setString(2, pattern);
                    statement.setString(3, pattern);
                });
    }

    private static boolean isBlank(String term) {
        return term == null || term.isBlank();
    }

    private static final String WHERE_TERM =
            "name LIKE ? OR contact_number LIKE ? OR email LIKE ?";

    /**
     * Exact match on the contact number, for the duplicate warning (FR-REC-25).
     *
     * <p>A list, not an Optional: duplicates are exactly what this looks for, and
     * returning the first of several would hide the problem it exists to report.</p>
     */
    @Override
    public List<Patient> findByContactNumber(String contactNumber) {
        return queryList("SELECT " + COLUMNS + " FROM patient WHERE contact_number = ? ORDER BY name",
                statement -> statement.setString(1, contactNumber),
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
        statement.setString(8, p.getDiagnosisDetails());
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
                .diagnosisDetails(rs.getString("diagnosis_details"))
                .build();
    }
}
