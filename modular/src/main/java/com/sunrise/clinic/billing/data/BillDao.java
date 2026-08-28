package com.sunrise.clinic.billing.data;

import com.sunrise.clinic.platform.data.JdbcDao;

import com.sunrise.clinic.platform.db.Database;
import com.sunrise.clinic.billing.domain.Bill;

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

    /**
     * Insert a bill. There is no update path, on purpose.
     *
     * <p>This was {@code INSERT … ON DUPLICATE KEY UPDATE}, and that quietly produced the
     * defect the API documentation records: billing an appointment twice answered
     * {@code 201} with an id that was never stored. {@code uq_bill_appointment} is on
     * {@code appointment_no}, so the second bill - carrying a fresh UUID - matched that
     * key and <b>updated the first bill's row</b>. The database kept the original id; the
     * caller was handed the new one and a success. Two receipts could be printed with
     * different numbers for one payment, and only one of them existed.</p>
     *
     * <p>A bill is a financial record of something that happened. It is issued once and
     * never rewritten, so an insert that conflicts is the correct behaviour rather than an
     * inconvenience - the duplicate now reaches
     * {@link com.sunrise.clinic.billing.service.BillingService}, which refuses it in
     * plain language before it gets this far, and this remains the guarantee underneath.</p>
     */
    @Override
    public Bill save(Bill bill) {
        update("""
                INSERT INTO bill (id, appointment_no, patient_id, dentist_id, receptionist_uid,
                                  consultation_fee, treatment_cost, service_charge, discount, tax,
                                  total, dentist_earning, clinic_earning, receptionist_earning,
                                  issued_at, issued_by_uid)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
        statement.setBigDecimal(6, b.getConsultationFee());
        statement.setBigDecimal(7, b.getTreatmentCost());
        statement.setBigDecimal(8, b.getServiceCharge());
        statement.setBigDecimal(9, b.getDiscount());
        statement.setBigDecimal(10, b.getTax());
        statement.setBigDecimal(11, b.getTotal());
        statement.setBigDecimal(12, b.getDentistEarning());
        statement.setBigDecimal(13, b.getClinicEarning());
        statement.setBigDecimal(14, b.getReceptionistEarning());
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
                .consultationFee(rs.getBigDecimal("consultation_fee"))
                .treatmentCost(rs.getBigDecimal("treatment_cost"))
                .serviceCharge(rs.getBigDecimal("service_charge"))
                .discount(rs.getBigDecimal("discount"))
                .tax(rs.getBigDecimal("tax"))
                .total(rs.getBigDecimal("total"))
                .dentistEarning(rs.getBigDecimal("dentist_earning"))
                .clinicEarning(rs.getBigDecimal("clinic_earning"))
                .receptionistEarning(rs.getBigDecimal("receptionist_earning"))
                .issuedAt(readInstant(rs, "issued_at"))
                .issuedByUid(rs.getString("issued_by_uid"))
                .build();
    }
}
