package com.sunrise.clinic.feedback.data;

import com.sunrise.clinic.feedback.domain.Complaint;
import com.sunrise.clinic.feedback.domain.ComplaintStatus;
import com.sunrise.clinic.platform.data.Repository;

import java.time.LocalDate;
import java.util.List;

/** Persistence for {@link Complaint}s. */
public interface ComplaintRepository extends Repository<Complaint, String> {

    /** What one patient has raised, newest first - FR-CMP-04. */
    List<Complaint> findByPatientId(String patientId);

    /**
     * The administrator's list, narrowed - FR-ADM-50.
     *
     * <p>Open ones first regardless of the other filters, because a list sorted by date puts
     * a resolved complaint from this morning above one submitted last week and still
     * untouched (FR-ADM-51).</p>
     *
     * @param status    only this state, or null for all
     * @param dentistId only complaints naming this dentist, or null
     * @param from      earliest submission date, or null
     * @param to        latest submission date, or null
     */
    List<Complaint> search(ComplaintStatus status, String dentistId, LocalDate from, LocalDate to);

    /**
     * How many complaints name each dentist - FR-ADM-57.
     *
     * <p>A count for the reports screen, never a rate: complaints per appointment would turn
     * a handful of concerns into a performance metric, and that is not what they are.</p>
     *
     * @return dentist id to count, only for dentists with at least one
     */
    java.util.Map<String, Integer> countByDentist();
}
