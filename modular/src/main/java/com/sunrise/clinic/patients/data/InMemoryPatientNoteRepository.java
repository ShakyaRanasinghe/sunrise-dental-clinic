package com.sunrise.clinic.patients.data;

import com.sunrise.clinic.patients.domain.PatientNote;
import com.sunrise.clinic.platform.data.InMemoryRepository;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/** In-memory {@link PatientNoteRepository}. */
public class InMemoryPatientNoteRepository
        extends InMemoryRepository<PatientNote, String>
        implements PatientNoteRepository {

    @Override
    protected String idOf(PatientNote entity) {
        return entity.getId();
    }

    @Override
    public List<PatientNote> findByPatientId(String patientId) {
        return store.values().stream()
                .filter(note -> note.belongsTo(patientId))
                // Critical first, then newest — the same order the SQL gives, so a test
                // exercising the order is exercising what production does.
                .sorted(Comparator.comparing(PatientNote::isCritical).reversed()
                        .thenComparing(InMemoryPatientNoteRepository::changedAt,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Override
    public boolean hasCritical(String patientId) {
        return store.values().stream()
                .anyMatch(note -> note.belongsTo(patientId) && note.isCritical());
    }

    private static Instant changedAt(PatientNote note) {
        return note.getUpdatedAt() == null ? note.getCreatedAt() : note.getUpdatedAt();
    }
}
