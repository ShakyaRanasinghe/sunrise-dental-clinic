package com.sunrise.clinic.repository;

import com.sunrise.clinic.domain.Dentist;

import java.util.Optional;

/** Persistence for {@link Dentist}s. */
public interface DentistRepository extends Repository<Dentist, String> {

    Optional<Dentist> findByUserUid(String userUid);
}
