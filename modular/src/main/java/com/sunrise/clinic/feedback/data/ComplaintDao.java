package com.sunrise.clinic.feedback.data;

import com.sunrise.clinic.feedback.domain.Complaint;
import com.sunrise.clinic.feedback.domain.ComplaintCategory;
import com.sunrise.clinic.feedback.domain.ComplaintStatus;
import com.sunrise.clinic.platform.data.JdbcDao;
import com.sunrise.clinic.platform.db.Database;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** MySQL-backed {@link ComplaintRepository}. */
public class ComplaintDao extends JdbcDao<Complaint, String> implements ComplaintRepository {

    private static final String COLUMNS =
            "id, patient_id, dentist_id, appointment_no, category, detail, status, "
                    + "submitted_at, reviewed_by_uid, resolution, resolved_at";

    public ComplaintDao(Database db) {
        super(db);
    }

    /**
     * Insert, or update only the reviewing fields.
     *
     * <p>The update touches {@code status}, {@code reviewed_by_uid}, {@code resolution} and
     * {@code resolved_at} and nothing else. FR-ADM-55: the administrator must not be able to
     * edit the patient's account of what happened, and the cleanest way to guarantee that is
     * for no statement in the application to be capable of writing {@code detail} after the
     * insert.</p>
     */
    @Override
    public Complaint save(Complaint complaint) {
        if (findById(complaint.getId()).isPresent()) {
            update("""
                    UPDATE complaint
                       SET status = ?, reviewed_by_uid = ?, resolution = ?, resolved_at = ?
                     WHERE id = ?
                    """, statement -> {
                statement.setString(1, enumName(complaint.getStatus()));
                statement.setString(2, complaint.getReviewedByUid());
                statement.setString(3, complaint.getResolution());
                statement.setTimestamp(4, toSqlTimestamp(complaint.getResolvedAt()));
                statement.setString(5, complaint.getId());
            });
            return complaint;
        }
        update("INSERT INTO complaint (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                statement -> {
                    statement.setString(1, complaint.getId());
                    statement.setString(2, complaint.getPatientId());
                    statement.setString(3, complaint.getDentistId());
                    statement.setString(4, complaint.getAppointmentNo());
                    statement.setString(5, enumName(complaint.getCategory()));
                    statement.setString(6, complaint.getDetail());
                    statement.setString(7, enumName(complaint.getStatus()));
                    statement.setTimestamp(8, toSqlTimestamp(complaint.getSubmittedAt()));
                    statement.setString(9, complaint.getReviewedByUid());
                    statement.setString(10, complaint.getResolution());
                    statement.setTimestamp(11, toSqlTimestamp(complaint.getResolvedAt()));
                });
        return complaint;
    }

    @Override
    public Optional<Complaint> findById(String id) {
        return queryOne("SELECT " + COLUMNS + " FROM complaint WHERE id = ?",
                statement -> statement.setString(1, id), ComplaintDao::mapComplaint);
    }

    @Override
    public List<Complaint> findByPatientId(String patientId) {
        return queryList("SELECT " + COLUMNS + " FROM complaint WHERE patient_id = ?"
                        + " ORDER BY submitted_at DESC",
                statement -> statement.setString(1, patientId), ComplaintDao::mapComplaint);
    }

    @Override
    public List<Complaint> search(ComplaintStatus status, String dentistId,
                                  LocalDate from, LocalDate to) {
        StringBuilder sql = new StringBuilder("SELECT " + COLUMNS + " FROM complaint WHERE 1 = 1");
        List<Object> values = new ArrayList<>();
        if (status != null) {
            sql.append(" AND status = ?");
            values.add(status.name());
        }
        if (dentistId != null && !dentistId.isBlank()) {
            sql.append(" AND dentist_id = ?");
            values.add(dentistId.trim());
        }
        if (from != null) {
            sql.append(" AND DATE(submitted_at) >= ?");
            values.add(Date.valueOf(from));
        }
        if (to != null) {
            sql.append(" AND DATE(submitted_at) <= ?");
            values.add(Date.valueOf(to));
        }
        // Open ones first: sorting by date alone puts a complaint resolved this morning
        // above one submitted last week and still untouched.
        sql.append(" ORDER BY status IN ('RESOLVED','DISMISSED'), submitted_at DESC");

        return queryList(sql.toString(), statement -> {
            for (int i = 0; i < values.size(); i++) {
                statement.setObject(i + 1, values.get(i));
            }
        }, ComplaintDao::mapComplaint);
    }

    @Override
    public Map<String, Integer> countByDentist() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        queryList("SELECT dentist_id, COUNT(*) AS raised FROM complaint GROUP BY dentist_id"
                        + " ORDER BY raised DESC", NO_PARAMETERS,
                rs -> Map.entry(rs.getString("dentist_id"), rs.getInt("raised")))
                .forEach(entry -> counts.put(entry.getKey(), entry.getValue()));
        return counts;
    }

    @Override
    public List<Complaint> findAll() {
        return search(null, null, null, null);
    }

    @Override
    public void deleteById(String id) {
        // Exists because Repository declares it. Nothing in the application calls it: a
        // complaint is a record of a concern and is not deleted (FR-CMP-06, FR-ADM-55).
        update("DELETE FROM complaint WHERE id = ?", statement -> statement.setString(1, id));
    }

    @Override
    public long count() {
        return queryCount("SELECT COUNT(*) FROM complaint");
    }

    private static Complaint mapComplaint(ResultSet rs) throws SQLException {
        return Complaint.builder()
                .id(rs.getString("id"))
                .patientId(rs.getString("patient_id"))
                .dentistId(rs.getString("dentist_id"))
                .appointmentNo(rs.getString("appointment_no"))
                .category(readEnum(rs, "category", ComplaintCategory.class))
                .detail(rs.getString("detail"))
                .status(readEnum(rs, "status", ComplaintStatus.class))
                .submittedAt(readInstant(rs, "submitted_at"))
                .reviewedByUid(rs.getString("reviewed_by_uid"))
                .resolution(rs.getString("resolution"))
                .resolvedAt(readInstant(rs, "resolved_at"))
                .build();
    }
}
