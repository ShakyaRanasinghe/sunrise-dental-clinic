package com.sunrise.clinic.patients.data;

import com.sunrise.clinic.patients.domain.NoteCategory;
import com.sunrise.clinic.patients.domain.PatientNote;
import com.sunrise.clinic.platform.data.JdbcDao;
import com.sunrise.clinic.platform.db.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** MySQL-backed {@link PatientNoteRepository}. */
public class PatientNoteDao extends JdbcDao<PatientNote, String> implements PatientNoteRepository {

    private static final String COLUMNS =
            "id, patient_id, category, detail, critical, created_at, updated_at";

    public PatientNoteDao(Database db) {
        super(db);
    }

    /**
     * Insert or update, distinguished rather than upserted.
     *
     * <p>{@code created_at} must survive an edit: a note the patient corrected is still a
     * note they declared on the day they first declared it, and an upsert that rewrites every
     * column would move that date forward each time.</p>
     */
    @Override
    public PatientNote save(PatientNote note) {
        boolean exists = findById(note.getId()).isPresent();
        if (exists) {
            update("""
                    UPDATE patient_note
                       SET category = ?, detail = ?, critical = ?, updated_at = ?
                     WHERE id = ?
                    """, statement -> {
                statement.setString(1, enumName(note.getCategory()));
                statement.setString(2, note.getDetail());
                statement.setBoolean(3, note.isCritical());
                statement.setTimestamp(4, toSqlTimestamp(note.getUpdatedAt()));
                statement.setString(5, note.getId());
            });
            return note;
        }
        update("INSERT INTO patient_note (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?)",
                statement -> bindNote(statement, note));
        return note;
    }

    @Override
    public Optional<PatientNote> findById(String id) {
        return queryOne("SELECT " + COLUMNS + " FROM patient_note WHERE id = ?",
                statement -> statement.setString(1, id), PatientNoteDao::mapNote);
    }

    @Override
    public List<PatientNote> findByPatientId(String patientId) {
        // critical DESC first — see the port. Then newest, so a recent correction is near
        // the top rather than buried by declaration order.
        return queryList("SELECT " + COLUMNS + " FROM patient_note WHERE patient_id = ?"
                        + " ORDER BY critical DESC, COALESCE(updated_at, created_at) DESC",
                statement -> statement.setString(1, patientId), PatientNoteDao::mapNote);
    }

    @Override
    public boolean hasCritical(String patientId) {
        return queryOne("SELECT COUNT(*) FROM patient_note WHERE patient_id = ? AND critical = TRUE",
                statement -> statement.setString(1, patientId),
                rs -> rs.getInt(1)).orElse(0) > 0;
    }

    @Override
    public List<PatientNote> findAll() {
        return queryList("SELECT " + COLUMNS + " FROM patient_note"
                + " ORDER BY patient_id, critical DESC", NO_PARAMETERS, PatientNoteDao::mapNote);
    }

    @Override
    public void deleteById(String id) {
        update("DELETE FROM patient_note WHERE id = ?", statement -> statement.setString(1, id));
    }

    @Override
    public long count() {
        return queryCount("SELECT COUNT(*) FROM patient_note");
    }

    private static void bindNote(PreparedStatement statement, PatientNote note) throws SQLException {
        statement.setString(1, note.getId());
        statement.setString(2, note.getPatientId());
        statement.setString(3, enumName(note.getCategory()));
        statement.setString(4, note.getDetail());
        statement.setBoolean(5, note.isCritical());
        statement.setTimestamp(6, toSqlTimestamp(note.getCreatedAt()));
        statement.setTimestamp(7, toSqlTimestamp(note.getUpdatedAt()));
    }

    private static PatientNote mapNote(ResultSet rs) throws SQLException {
        return PatientNote.builder()
                .id(rs.getString("id"))
                .patientId(rs.getString("patient_id"))
                .category(readEnum(rs, "category", NoteCategory.class))
                .detail(rs.getString("detail"))
                .critical(rs.getBoolean("critical"))
                .createdAt(readInstant(rs, "created_at"))
                .updatedAt(readInstant(rs, "updated_at"))
                .build();
    }
}
