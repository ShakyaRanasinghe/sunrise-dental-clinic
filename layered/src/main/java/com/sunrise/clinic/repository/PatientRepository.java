package com.sunrise.clinic.repository;

import com.sunrise.clinic.domain.Patient;

import java.util.Optional;

/** Persistence for {@link Patient}s. */
public interface PatientRepository extends Repository<Patient, String> {

    Optional<Patient> findByUserUid(String userUid);
}
