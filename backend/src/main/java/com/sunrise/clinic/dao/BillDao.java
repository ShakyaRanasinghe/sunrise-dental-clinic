package com.sunrise.clinic.dao;

import com.sunrise.clinic.db.Database;
import com.sunrise.clinic.domain.Bill;
import com.sunrise.clinic.repository.BillRepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** MySQL-backed {@link BillRepository}, including the aggregates the reports use. */
public class BillDao extends JdbcDao<Bill, String> implements BillRepository {

    private static final String COLUMNS =
            "id, appointment_no, patient_id, dentist_id, receptionist_uid, consultation_fee, "
                    + "treatment_cost, service_charge, discount, tax, total, dentist_earning, "
                    + "clinic_earning, receptionist_earning, issued_at, issued_by_uid";

    public BillDao(Database db) {
        super(db);
    }

    @Override
    public Bill save(Bill bill) {
        update("""
                INSERT INTO bill (id, appointment_no, patient_id, dentist_id, receptionist_uid,
                                  consultation_fee, treatment_cost, service_charge, discount, tax,
                                  total, dentist_earning, clinic_earning, receptionist_earning,
                                  issued_at, issued_by_uid)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    consultation_fee = VALUES(consultation_fee),
                    treatment_cost = VALUES(treatment_cost),
                    service_charge = VALUES(service_charge),
                    discount = VALUES(discount),
                    tax = VALUES(tax),
                    total = VALUES(total),
                    dentist_earning = VALUES(dentist_earning),
                    clinic_earning = VALUES(clinic_earning),
                    receptionist_earning = VALUES(receptionist_earning)
                """, statement -> bindBill(statement, bill));
        return bill;
    }

    @Override
    public Optional<Bill> findById(String id) {
        return queryOne("SELECT " + COLUMNS + " FROM bill WHERE id = ?",
                statement -> statement.setString(1, id),
                BillDao::mapBill);
    }

    @Override
    public Optional<Bill> findByAppointmentNo(String appointmentNo) {
        return queryOne("SELECT " + COLUMNS + " FROM bill WHERE appointment_no = ?",
                statement -> statement.setString(1, appointmentNo),
                BillDao::mapBill);
    }

    @Override
    public List<Bill> findAll() {
        return queryList("SELECT " + COLUMNS + " FROM bill ORDER BY issued_at DESC",
                NO_PARAMETERS, BillDao::mapBill);
    }

    /**
     * Bills issued in a date range — the basis of the income report.
     * Aggregation is left to the caller so the same query serves the
     * per-dentist, per-receptionist and clinic-total breakdowns.
     */
    public List<Bill> findIssuedBetween(LocalDate from, LocalDate to) {
        return queryList("SELECT " + COLUMNS + " FROM bill"
                        + " WHERE DATE(issued_at) BETWEEN ? AND ?"
                        + " ORDER BY issued_at",
                statement -> {
                    statement.setDate(1, toSqlDate(from));
                    statement.setDate(2, toSqlDate(to));
                },
                BillDao::mapBill);
    }

    @Override
    public void deleteById(String id) {
        update("DELETE FROM bill WHERE id = ?", statement -> statement.setString(1, id));
    }

    @Override
    public long count() {
        return queryCount("SELECT COUNT(*) FROM bill");
    }

    private static void bindBill(PreparedStatement statement, Bill b) throws SQLException {
        statement.setString(1, b.getId());
        statement.setString(2, b.getAppointmentNo());
        statement.setString(3, b.getPatientId());
        statement.setString(4, b.getDentistId());
        statement.setString(5, b.getReceptionistUid());
        statement.setDouble(6, b.getConsultationFee());
        statement.setDouble(7, b.getTreatmentCost());
        statement.setDouble(8, b.getServiceCharge());
        statement.setDouble(9, b.getDiscount());
        statement.setDouble(10, b.getTax());
        statement.setDouble(11, b.getTotal());
        statement.setDouble(12, b.getDentistEarning());
        statement.setDouble(13, b.getClinicEarning());
        statement.setDouble(14, b.getReceptionistEarning());
        statement.setTimestamp(15, toSqlTimestamp(b.getIssuedAt()));
        statement.setString(16, b.getIssuedByUid());
    }

    private static Bill mapBill(ResultSet rs) throws SQLException {
        return Bill.builder()
                .id(rs.getString("id"))
                .appointmentNo(rs.getString("appointment_no"))
                .patientId(rs.getString("patient_id"))
                .dentistId(rs.getString("dentist_id"))
                .receptionistUid(rs.getString("receptionist_uid"))
                .consultationFee(rs.getDouble("consultation_fee"))
                .treatmentCost(rs.getDouble("treatment_cost"))
                .serviceCharge(rs.getDouble("service_charge"))
                .discount(rs.getDouble("discount"))
                .tax(rs.getDouble("tax"))
                .total(rs.getDouble("total"))
                .dentistEarning(rs.getDouble("dentist_earning"))
                .clinicEarning(rs.getDouble("clinic_earning"))
                .receptionistEarning(rs.getDouble("receptionist_earning"))
                .issuedAt(readInstant(rs, "issued_at"))
                .issuedByUid(rs.getString("issued_by_uid"))
                .build();
    }
}
