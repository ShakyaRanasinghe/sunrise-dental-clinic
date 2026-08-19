package com.sunrise.clinic.feedback.data;

import com.sunrise.clinic.feedback.domain.DentistReview;
import com.sunrise.clinic.feedback.domain.RatingSummary;
import com.sunrise.clinic.platform.data.JdbcDao;
import com.sunrise.clinic.platform.db.Database;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** MySQL-backed {@link ReviewRepository}. */
public class ReviewDao extends JdbcDao<DentistReview, String> implements ReviewRepository {

    private static final String COLUMNS =
            "id, appointment_no, dentist_id, patient_id, rating, comment, submitted_at, updated_at";

    public ReviewDao(Database db) {
        super(db);
    }

    /**
     * Insert, or update the rating and comment.
     *
     * <p>{@code submitted_at} survives an edit: a review changed within the window is still
     * one left after that visit, and the window is measured from the visit anyway.</p>
     */
    @Override
    public DentistReview save(DentistReview review) {
        if (findById(review.getId()).isPresent()) {
            update("UPDATE dentist_review SET rating = ?, comment = ?, updated_at = ? WHERE id = ?",
                    statement -> {
                        statement.setInt(1, review.getRating());
                        statement.setString(2, review.getComment());
                        statement.setTimestamp(3, toSqlTimestamp(review.getUpdatedAt()));
                        statement.setString(4, review.getId());
                    });
            return review;
        }
        update("INSERT INTO dentist_review (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                statement -> {
                    statement.setString(1, review.getId());
                    statement.setString(2, review.getAppointmentNo());
                    statement.setString(3, review.getDentistId());
                    statement.setString(4, review.getPatientId());
                    statement.setInt(5, review.getRating());
                    statement.setString(6, review.getComment());
                    statement.setTimestamp(7, toSqlTimestamp(review.getSubmittedAt()));
                    statement.setTimestamp(8, toSqlTimestamp(review.getUpdatedAt()));
                });
        return review;
    }

    @Override
    public Optional<DentistReview> findById(String id) {
        return queryOne("SELECT " + COLUMNS + " FROM dentist_review WHERE id = ?",
                statement -> statement.setString(1, id), ReviewDao::mapReview);
    }

    @Override
    public Optional<DentistReview> findByAppointmentNo(String appointmentNo) {
        return queryOne("SELECT " + COLUMNS + " FROM dentist_review WHERE appointment_no = ?",
                statement -> statement.setString(1, appointmentNo), ReviewDao::mapReview);
    }

    @Override
    public List<DentistReview> findByPatientId(String patientId) {
        return queryList("SELECT " + COLUMNS + " FROM dentist_review WHERE patient_id = ?"
                        + " ORDER BY submitted_at DESC",
                statement -> statement.setString(1, patientId), ReviewDao::mapReview);
    }

    @Override
    public List<DentistReview> findByDentistId(String dentistId) {
        return queryList("SELECT " + COLUMNS + " FROM dentist_review WHERE dentist_id = ?"
                        + " ORDER BY submitted_at DESC",
                statement -> statement.setString(1, dentistId), ReviewDao::mapReview);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Counted and averaged in SQL. The floor is applied in Java rather than by calling
     * {@code fn_dentist_rating}, which returns NULL below five: the function stays in the
     * schema as the database-side statement of the same rule, and having the count as well as
     * the mean lets the caller say "two more reviews needed" instead of only "no rating".</p>
     */
    @Override
    public RatingSummary summaryFor(String dentistId) {
        return queryOne("SELECT COUNT(*) AS reviews, AVG(rating) AS mean"
                        + " FROM dentist_review WHERE dentist_id = ?",
                statement -> statement.setString(1, dentistId),
                rs -> {
                    int reviews = rs.getInt("reviews");
                    BigDecimal mean = rs.getBigDecimal("mean");
                    return reviews < RatingSummary.FLOOR
                            ? new RatingSummary(dentistId, reviews, null)
                            : new RatingSummary(dentistId, reviews,
                                    mean.setScale(2, java.math.RoundingMode.HALF_UP));
                })
                .orElseGet(() -> RatingSummary.none(dentistId));
    }

    @Override
    public List<DentistReview> findAll() {
        return queryList("SELECT " + COLUMNS + " FROM dentist_review ORDER BY submitted_at DESC",
                NO_PARAMETERS, ReviewDao::mapReview);
    }

    @Override
    public void deleteById(String id) {
        // Declared by Repository. Nothing calls it: an administrator must not be able to
        // delete a review (FR-ADM-61), because curating the feedback would make the average
        // worthless.
        update("DELETE FROM dentist_review WHERE id = ?", statement -> statement.setString(1, id));
    }

    @Override
    public long count() {
        return queryCount("SELECT COUNT(*) FROM dentist_review");
    }

    private static DentistReview mapReview(ResultSet rs) throws SQLException {
        return DentistReview.builder()
                .id(rs.getString("id"))
                .appointmentNo(rs.getString("appointment_no"))
                .dentistId(rs.getString("dentist_id"))
                .patientId(rs.getString("patient_id"))
                .rating(rs.getInt("rating"))
                .comment(rs.getString("comment"))
                .submittedAt(readInstant(rs, "submitted_at"))
                .updatedAt(readInstant(rs, "updated_at"))
                .build();
    }
}
