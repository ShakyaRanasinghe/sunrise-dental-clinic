package com.sunrise.clinic.reporting.data;

import com.sunrise.clinic.platform.data.JdbcDao;
import com.sunrise.clinic.platform.db.Database;
import com.sunrise.clinic.reporting.domain.Attendance;
import com.sunrise.clinic.reporting.domain.DailyPoint;
import com.sunrise.clinic.reporting.domain.EarningsRow;
import com.sunrise.clinic.reporting.domain.IncomeTotals;

import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * MySQL-backed {@link ReportRepository}. Every figure is a {@code SUM} or a {@code COUNT}
 * computed by the database, and every money column is read as {@code BigDecimal}.
 *
 * <p>A bill is dated by {@code issued_at}, and an appointment by {@code appointment_date}.
 * They are deliberately different questions: takings belong to the day the money was taken,
 * footfall to the day of the visit. A bill issued the morning after a late appointment
 * belongs to the day it was issued.</p>
 */
public class JdbcReportDao extends JdbcDao<Void, String> implements ReportRepository {

    private final ZoneId clinicZone;

    /**
     * @param clinicZone the clinic's own timezone. {@code bill.issued_at} is a
     *                   {@code TIMESTAMP} read back as UTC, so which day a bill belongs to
     *                   depends on where the clinic is - not on where the server is
     */
    public JdbcReportDao(Database db, ZoneId clinicZone) {
        super(db);
        this.clinicZone = clinicZone;
    }

    /**
     * {@code issued_at} shifted into the clinic's day.
     *
     * <p>Written as an interval in minutes rather than {@code CONVERT_TZ}, which needs
     * MySQL's timezone tables loaded - they are absent from the standard container image and
     * {@code CONVERT_TZ} then returns NULL, which would silently empty every report.</p>
     *
     * <p>The offset is taken now rather than per row, so a zone with daylight saving would
     * be a few hours out for rows either side of a transition. Sri Lanka has no daylight
     * saving, and the honest note matters more than the false precision.</p>
     */
    private String billDay() {
        return billDay("");
    }

    private String billDay(String alias) {
        long minutes = clinicZone.getRules().getOffset(java.time.Instant.now()).getTotalSeconds() / 60;
        return "DATE(" + alias + "issued_at + INTERVAL " + minutes + " MINUTE)";
    }

    @Override
    public IncomeTotals income(LocalDate from, LocalDate to) {
        return queryOne("SELECT COUNT(*) AS bills,"
                        + " COALESCE(SUM(total), 0) AS gross,"
                        + " COALESCE(SUM(dentist_earning), 0) AS dentist,"
                        + " COALESCE(SUM(clinic_earning), 0) AS clinic,"
                        + " COALESCE(SUM(receptionist_earning), 0) AS reception"
                        + " FROM bill WHERE " + billDay("") + " BETWEEN ? AND ?",
                between(from, to),
                rs -> new IncomeTotals(rs.getInt("bills"),
                        rs.getBigDecimal("gross"),
                        rs.getBigDecimal("dentist"),
                        rs.getBigDecimal("clinic"),
                        rs.getBigDecimal("reception")))
                .orElse(IncomeTotals.NONE);
    }

    @Override
    public List<EarningsRow> earningsByDentist(LocalDate from, LocalDate to) {
        return queryList("SELECT d.id, d.name, COUNT(*) AS bills,"
                        + " COALESCE(SUM(b.dentist_earning), 0) AS amount"
                        + " FROM bill b JOIN dentist d ON d.id = b.dentist_id"
                        + " WHERE " + billDay("b.") + " BETWEEN ? AND ?"
                        + " GROUP BY d.id, d.name ORDER BY amount DESC",
                between(from, to),
                rs -> new EarningsRow(rs.getString("id"), rs.getString("name"),
                        rs.getInt("bills"), rs.getBigDecimal("amount")));
    }

    /**
     * {@inheritDoc}
     *
     * <p>A LEFT JOIN on the account: {@code fk_bill_receptionist} is ON DELETE SET NULL, so
     * a bill issued by somebody who has since left keeps its figures with no account to name
     * them. Those rupees still happened and must still appear in the total.</p>
     */
    @Override
    public List<EarningsRow> earningsByReceptionist(LocalDate from, LocalDate to) {
        return queryList("SELECT b.receptionist_uid AS uid,"
                        + " COALESCE(u.display_name, 'Former staff') AS name,"
                        + " COUNT(*) AS bills,"
                        + " COALESCE(SUM(b.receptionist_earning), 0) AS amount"
                        + " FROM bill b LEFT JOIN user_account u ON u.uid = b.receptionist_uid"
                        + " WHERE " + billDay("b.") + " BETWEEN ? AND ?"
                        + " GROUP BY b.receptionist_uid, u.display_name ORDER BY amount DESC",
                between(from, to),
                rs -> new EarningsRow(rs.getString("uid"), rs.getString("name"),
                        rs.getInt("bills"), rs.getBigDecimal("amount")));
    }

    @Override
    public List<DailyPoint> takingsByDay(LocalDate from, LocalDate to) {
        String day = billDay("");
        return queryList("SELECT " + day + " AS day, COUNT(*) AS bills,"
                        + " COALESCE(SUM(total), 0) AS amount"
                        + " FROM bill WHERE " + day + " BETWEEN ? AND ?"
                        + " GROUP BY " + day + " ORDER BY day",
                between(from, to),
                rs -> new DailyPoint(rs.getDate("day").toLocalDate(),
                        rs.getInt("bills"), rs.getBigDecimal("amount")));
    }

    @Override
    public List<DailyPoint> footfallByDay(LocalDate from, LocalDate to) {
        return queryList("""
                SELECT appointment_date AS day, COUNT(*) AS visits
                  FROM appointment
                 WHERE appointment_date BETWEEN ? AND ?
                   AND status IN ('COMPLETED', 'BILLED')
                 GROUP BY appointment_date
                 ORDER BY day
                """, between(from, to),
                rs -> new DailyPoint(rs.getDate("day").toLocalDate(),
                        rs.getInt("visits"), null));
    }

    @Override
    public Attendance attendance(LocalDate from, LocalDate to, LocalDate asOf) {
        return queryOne("""
                SELECT
                  SUM(status IN ('COMPLETED','BILLED'))                       AS attended,
                  SUM(status = 'CONFIRMED' AND appointment_date < ?)          AS no_shows,
                  SUM(status = 'CONFIRMED' AND appointment_date >= ?)         AS upcoming
                  FROM appointment
                 WHERE appointment_date BETWEEN ? AND ?
                """, statement -> {
                    statement.setDate(1, Date.valueOf(asOf));
                    statement.setDate(2, Date.valueOf(asOf));
                    statement.setDate(3, Date.valueOf(from));
                    statement.setDate(4, Date.valueOf(to));
                },
                rs -> new Attendance(rs.getInt("attended"), rs.getInt("no_shows"),
                        rs.getInt("upcoming")))
                .orElse(Attendance.NONE);
    }

    @Override
    public int patientsSeen(LocalDate from, LocalDate to) {
        return queryOne("""
                SELECT COUNT(DISTINCT patient_id) AS seen
                  FROM appointment
                 WHERE appointment_date BETWEEN ? AND ?
                   AND status IN ('COMPLETED', 'BILLED')
                """, between(from, to), rs -> rs.getInt("seen")).orElse(0);
    }

    @Override
    public long registeredPatients() {
        return queryCount("SELECT COUNT(*) FROM patient");
    }

    private static Binder between(LocalDate from, LocalDate to) {
        return statement -> {
            statement.setDate(1, Date.valueOf(from));
            statement.setDate(2, Date.valueOf(to));
        };
    }
}
