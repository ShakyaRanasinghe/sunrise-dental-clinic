package com.sunrise.clinic.scheduling.data;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory {@link DentistTreatmentRepository} (default; tests + offline demo). GAP-FTB-07. */
public class InMemoryDentistTreatmentRepository implements DentistTreatmentRepository {

    private final Map<String, Set<String>> offered = new ConcurrentHashMap<>();

    @Override
    public Set<String> offeredTreatmentIds(String dentistId) {
        return offered.getOrDefault(dentistId, Set.of());
    }

    @Override
    public void enable(String dentistId, String treatmentId) {
        offered.computeIfAbsent(dentistId, k -> new HashSet<>()).add(treatmentId);
    }

    @Override
    public void disable(String dentistId, String treatmentId) {
        offered.computeIfPresent(dentistId, (k, set) -> {
            set.remove(treatmentId);
            return set;
        });
    }
}
