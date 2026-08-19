package com.sunrise.clinic.dao;

import com.sunrise.clinic.db.Database;
import com.sunrise.clinic.domain.Appointment;
import com.sunrise.clinic.domain.AppointmentStatus;
import com.sunrise.clinic.domain.Role;
import com.sunrise.clinic.repository.AppointmentRepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** MySQL-backed {@link AppointmentRepository}. */
public class AppointmentDao extends JdbcDao<Appointment, String> implements AppointmentRepository {

    private static final String COLUMNS =
            "appointment_no, patient_id, dentist_id, slot_id, treatment_id, appointment_date, "
                    + "appointment_time, status, diagnosis, created_by_uid, created_by_role, created_at";

    public AppointmentDao(Database db) {
        super(db);
    }

    @Override
    public Appointment save(Appointment appointment) {
        update("""
                INSERT INTO appointment (appointment_no, patient_id, dentist_id, slot_id, treatment_id,
                                         appointment_date, appointment_time, status, diagnosis,
                                         created_by_uid, created_by_role, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    patient_id = VALUES(patient_id),
                    dentist_id = VALUES(dentist_id),
                    slot_id = VALUES(slot_id),
                    treatment_id = VALUES(treatment_id),
                    appointment_date = VALUES(appointment_date),
                    appointment_time = VALUES(appointment_time),
                    status = VALUES(status),
                    diagnosis = VALUES(diagnosis)
                """, statement -> bindAppointment(statement, appointment));
        return appointment;
    }

    @Override
    public Optional<Appointment> findById(String appointmentNo) {
        return queryOne("SELECT " + COLUMNS + " FROM appointment WHERE appointment_no = ?",
                statement -> statement.setString(1, appointmentNo),
                AppointmentDao::mapAppointment);
    }

    @Override
    public List<Appointment> findAll() {
        return queryList("SELECT " + COLUMNS + " FROM appointment"
                        + " ORDER BY appointment_date DESC, appointment_time DESC",
                NO_PARAMETERS, AppointmentDao::mapAppointment);
    }

    @Override
    public List<Appointment> findByPatientId(String patientId) {
        return queryList("SELECT " + COLUMNS + " FROM appointment WHERE patient_id = ?"
                        + " ORDER BY appointment_date DESC, appointment_time DESC",
                statement -> statement.setString(1, patientId),
                AppointmentDao::mapAppointment);
    }

    @Override
    public List<Appointment> findByDentistId(String dentistId) {
        return queryList("SELECT " + COLUMNS + " FROM appointment WHERE dentist_id = ?"
                        + " ORDER BY appointment_date DESC, appointment_time DESC",
                statement -> statement.setString(1, dentistId),
                AppointmentDao::mapAppointment);
    }

    @Override
    public List<Appointment> findByDate(LocalDate date) {
        return queryList("SELECT " + COLUMNS + " FROM appointment WHERE appointment_date = ?"
                        + " ORDER BY appointment_time",
                statement -> statement.setDate(1, toSqlDate(date)),
                AppointmentDao::mapAppointment);
    }

    @Override
    public List<Appointment> findByDateBetween(LocalDate from, LocalDate to) {
        return queryList("SELECT " + COLUMNS + " FROM appointment"
                        + " WHERE appointment_date BETWEEN ? AND ?"
                        + " ORDER BY appointment_date, appointment_time",
                statement -> {
                    statement.setDate(1, toSqlDate(from));
                    statement.setDate(2, toSqlDate(to));
                },
                AppointmentDao::mapAppointment);
    }

    @Override
    public void deleteById(String appointmentNo) {
        update("DELETE FROM appointment WHERE appointment_no = ?",
                statement -> statement.setString(1, appointmentNo));
    }

    @Override
    public long count() {
        return queryCount("SELECT COUNT(*) FROM appointment");
    }

    private static void bindAppointment(PreparedStatement statement, Appointment a) throws SQLException {
        statement.setString(1, a.getAppointmentNo());
        statement.setString(2, a.getPatientId());
        statement.setString(3, a.getDentistId());
        statement.setString(4, a.getSlotId());
        statement.setString(5, a.getTreatmentId());
        statement.setDate(6, toSqlDate(a.getDate()));
        statement.setTime(7, toSqlTime(a.getTime()));
        statement.setString(8, enumName(a.getStatus()));
        statement.setString(9, a.getDiagnosis());
        statement.setString(10, a.getCreatedByUid());
        statement.setString(11, enumName(a.getCreatedByRole()));
        statement.setTimestamp(12, toSqlTimestamp(a.getCreatedAt()));
    }

    private static Appointment mapAppointment(ResultSet rs) throws SQLException {
        return Appointment.builder()
                .appointmentNo(rs.getString("appointment_no"))
                .patientId(rs.getString("patient_id"))
                .dentistId(rs.getString("dentist_id"))
                .slotId(rs.getString("slot_id"))
                .treatmentId(rs.getString("treatment_id"))
                .date(readDate(rs, "appointment_date"))
                .time(readTime(rs, "appointment_time"))
                .status(readEnum(rs, "status", AppointmentStatus.class))
                .diagnosis(rs.getString("diagnosis"))
                .createdByUid(rs.getString("created_by_uid"))
                .createdByRole(readEnum(rs, "created_by_role", Role.class))
                .createdAt(readInstant(rs, "created_at"))
                .build();
    }
}
