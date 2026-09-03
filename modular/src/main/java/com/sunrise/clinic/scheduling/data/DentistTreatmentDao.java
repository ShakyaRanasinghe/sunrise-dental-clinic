package com.sunrise.clinic.scheduling.data;

import com.sunrise.clinic.platform.data.JdbcDao;
import com.sunrise.clinic.platform.db.Database;

import java.util.LinkedHashSet;
import java.util.Set;

/** MySQL-backed {@link DentistTreatmentRepository}. GAP-FTB-07. */
public class DentistTreatmentDao extends JdbcDao<Void, String> implements DentistTreatmentRepository {

    public DentistTreatmentDao(Database db) {
        super(db);
    }

    @Override
    public Set<String> offeredTreatmentIds(String dentistId) {
        return new LinkedHashSet<>(queryList(
                "SELECT treatment_id FROM dentist_treatment WHERE dentist_id = ? ORDER BY treatment_id",
                statement -> statement.setString(1, dentistId),
                rs -> rs.getString(1)));
    }

    @Override
    public void enable(String dentistId, String treatmentId) {
        update("""
                INSERT INTO dentist_treatment (dentist_id, treatment_id)
                VALUES (?, ?)
                ON DUPLICATE KEY UPDATE dentist_id = dentist_id
                """, statement -> {
            statement.setString(1, dentistId);
            statement.setString(2, treatmentId);
        });
    }

    @Override
    public void disable(String dentistId, String treatmentId) {
        update("DELETE FROM dentist_treatment WHERE dentist_id = ? AND treatment_id = ?",
                statement -> {
                    statement.setString(1, dentistId);
                    statement.setString(2, treatmentId);
                });
    }
}
