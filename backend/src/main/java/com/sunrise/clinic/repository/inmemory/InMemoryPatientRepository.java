package com.sunrise.clinic.repository.inmemory;

import com.sunrise.clinic.domain.Patient;
import com.sunrise.clinic.repository.InMemoryRepository;
import com.sunrise.clinic.repository.PatientRepository;

import java.util.Optional;

/** In-memory {@link PatientRepository}. */
public class InMemoryPatientRepository
        extends InMemoryRepository<Patient, String>
        implements PatientRepository {

    @Override
    protected String idOf(Patient entity) {
        return entity.getId();
    }

    @Override
    public Optional<Patient> findByUserUid(String userUid) {
        return store.values().stream()
                .filter(p -> userUid.equals(p.getUserUid()))
                .findFirst();
    }
}
