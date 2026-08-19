package com.sunrise.clinic.appointments.data;

import com.sunrise.clinic.platform.data.JdbcDao;

import com.sunrise.clinic.platform.db.Database;
import com.sunrise.clinic.appointments.domain.Appointment;
import com.sunrise.clinic.appointments.domain.AppointmentStatus;
import com.sunrise.clinic.access.domain.Role;

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

    /**
     * Insert a new appointment, or update the one that exists.
     *
     * <p><b>Not an upsert, and that is the point.</b> This was
     * {@code INSERT … ON DUPLICATE KEY UPDATE}, which is the idiom every other DAO here
     * uses - and it made the whole appointment lifecycle impossible against the real
     * database, while every in-memory test passed.</p>
     *
     * <p>{@code trg_prevent_double_booking} is a {@code BEFORE INSERT} trigger that
     * refuses an appointment whose slot is already BOOKED. MySQL fires a BEFORE INSERT
     * trigger on an upsert <em>before</em> it discovers the duplicate key, so the trigger
     * ran on every update too. Booking itself worked, because the appointment row is
     * written while the slot is still OPEN - but the moment anything tried to save that
     * appointment again, its own slot was BOOKED and the trigger refused it. Completing
     * and cancelling both failed with "That slot is already booked", reported as a 500.</p>
     *
     * <p>The fix is here rather than in the trigger. The trigger's rule is right, and
     * weakening it to "unless the slot already points at this appointment" would trade a
     * real integrity guarantee for a convenience of the persistence idiom. Distinguishing
     * the two operations is also more honest: an upsert quietly rewrites every column,
     * including {@code created_by_uid} and {@code created_at}, which for an appointment
     * that already exists is not a thing anyone wants.</p>
     */
    @Override
    public Appointment save(Appointment appointment) {
        boolean exists = findById(appointment.getAppointmentNo()).isPresent();
        if (exists) {
            update("""
                    UPDATE appointment
                       SET patient_id = ?, dentist_id = ?, slot_id = ?, treatment_id = ?,
                           appointment_date = ?, appointment_time = ?, status = ?, diagnosis = ?
                     WHERE appointment_no = ?
                    """, statement -> {
                statement.setString(1, appointment.getPatientId());
                statement.setString(2, appointment.getDentistId());
                statement.setString(3, appointment.getSlotId());
                statement.setString(4, appointment.getTreatmentId());
                statement.setDate(5, toSqlDate(appointment.getDate()));
                statement.setTime(6, toSqlTime(appointment.getTime()));
                statement.setString(7, enumName(appointment.getStatus()));
                statement.setString(8, appointment.getDiagnosis());
                statement.setString(9, appointment.getAppointmentNo());
            });
            return appointment;
        }
        update("""
                INSERT INTO appointment (appointment_no, patient_id, dentist_id, slot_id, treatment_id,
                                         appointment_date, appointment_time, status, diagnosis,
                                         created_by_uid, created_by_role, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
