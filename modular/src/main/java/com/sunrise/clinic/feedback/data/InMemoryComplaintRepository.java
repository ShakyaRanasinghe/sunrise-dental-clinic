package com.sunrise.clinic.feedback.data;

import com.sunrise.clinic.feedback.domain.Complaint;
import com.sunrise.clinic.feedback.domain.ComplaintStatus;
import com.sunrise.clinic.platform.data.InMemoryRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** In-memory {@link ComplaintRepository}. */
public class InMemoryComplaintRepository
        extends InMemoryRepository<Complaint, String>
        implements ComplaintRepository {

    @Override
    protected String idOf(Complaint entity) {
        return entity.getId();
    }

    @Override
    public List<Complaint> findByPatientId(String patientId) {
        return store.values().stream()
                .filter(complaint -> complaint.belongsTo(patientId))
                .sorted(newestFirst())
                .toList();
    }

    @Override
    public List<Complaint> search(ComplaintStatus status, String dentistId,
                                  LocalDate from, LocalDate to) {
        return store.values().stream()
                .filter(c -> status == null || status == c.getStatus())
                .filter(c -> dentistId == null || dentistId.isBlank()
                        || dentistId.equals(c.getDentistId()))
                .filter(c -> from == null || !day(c).isBefore(from))
                .filter(c -> to == null || !day(c).isAfter(to))
                // Open ones first, then newest — see the port.
                .sorted(Comparator.comparing((Complaint c) -> c.getStatus().isClosed())
                        .thenComparing(newestFirst()))
                .toList();
    }

    @Override
    public Map<String, Integer> countByDentist() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        store.values().forEach(c -> counts.merge(c.getDentistId(), 1, Integer::sum));
        return counts;
    }

    private static Comparator<Complaint> newestFirst() {
        return Comparator.comparing(Complaint::getSubmittedAt,
                Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private static LocalDate day(Complaint complaint) {
        Instant at = complaint.getSubmittedAt();
        return at == null ? LocalDate.MIN : at.atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
