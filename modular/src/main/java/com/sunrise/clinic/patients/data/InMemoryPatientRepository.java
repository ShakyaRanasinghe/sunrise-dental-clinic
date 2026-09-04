package com.sunrise.clinic.patients.data;

import com.sunrise.clinic.patients.domain.Patient;
import com.sunrise.clinic.platform.data.InMemoryRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * In-memory {@link PatientRepository}, so the service can be tested without a
 * database.
 */
public class InMemoryPatientRepository
        extends InMemoryRepository<Patient, String>
        implements PatientRepository {

    @Override
    protected String idOf(Patient entity) {
        return entity.getId();
    }

    @Override
    public Optional<Patient> findByUserUid(String userUid) {
        if (userUid == null) {
            return Optional.empty();
        }
        return store.values().stream()
                .filter(p -> userUid.equals(p.getUserUid()))
                .findFirst();
    }

    @Override
    public List<Patient> search(String term) {
        return matching(term);
    }

    @Override
    public List<Patient> page(String term, long offset, int limit) {
        List<Patient> all = matching(term);
        long start = Math.max(0, Math.min(offset, all.size()));
        int end = (int) Math.min(all.size(), start + Math.max(0, limit));
        return all.subList((int) start, end).stream().toList();
    }

    @Override
    public long countMatching(String term) {
        return matching(term).size();
    }

    @Override
    public List<Patient> findByContactNumber(String contactNumber) {
        return store.values().stream()
                .filter(p -> contactNumber != null && contactNumber.equals(p.getContactNumber()))
                .sorted(java.util.Comparator.comparing(Patient::getName,
                        java.util.Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    /**
     * The register, filtered to matches and sorted by name - the one order both
     * {@code search} and the paged methods must agree on, so a page number keeps
     * meaning whichever query produced the list.
     */
    private List<Patient> matching(String term) {
        String needle = term == null ? "" : term.trim().toLowerCase();
        return store.values().stream()
                .filter(p -> contains(p.getName(), needle)
                        || contains(p.getContactNumber(), needle)
                        || contains(p.getEmail(), needle)
                        || contains(p.getPatientNo(), needle))
                .sorted(Comparator.comparing(Patient::getName,
                        Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    private static boolean contains(String field, String needle) {
        return field != null && field.toLowerCase().contains(needle);
    }
}
