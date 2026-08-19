package com.sunrise.clinic.repository.inmemory;

import com.sunrise.clinic.domain.UserAccount;
import com.sunrise.clinic.repository.InMemoryRepository;
import com.sunrise.clinic.repository.UserRepository;

import java.util.Optional;

/** In-memory {@link UserRepository}. */
public class InMemoryUserRepository
        extends InMemoryRepository<UserAccount, String>
        implements UserRepository {

    @Override
    protected String idOf(UserAccount entity) {
        return entity.getUid();
    }

    @Override
    public Optional<UserAccount> findByEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        String normalised = email.trim().toLowerCase();
        return store.values().stream()
                .filter(u -> u.getEmail() != null && normalised.equals(u.getEmail().toLowerCase()))
                .findFirst();
    }
}
