package com.sunrise.clinic.repository.inmemory;

import com.sunrise.clinic.domain.Dentist;
import com.sunrise.clinic.repository.DentistRepository;
import com.sunrise.clinic.repository.InMemoryRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** In-memory {@link DentistRepository}. */
@Repository
@Profile("!firestore")
public class InMemoryDentistRepository
        extends InMemoryRepository<Dentist, String>
        implements DentistRepository {

    @Override
    protected String idOf(Dentist entity) {
        return entity.getId();
    }

    @Override
    public Optional<Dentist> findByUserUid(String userUid) {
        return store.values().stream()
                .filter(d -> userUid.equals(d.getUserUid()))
                .findFirst();
    }
}
