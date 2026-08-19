package com.sunrise.clinic.patients.data;

import com.sunrise.clinic.patients.domain.Patient;
import com.sunrise.clinic.platform.data.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Persistence for {@link Patient}s.
 *
 * <p>{@code search} is declared here, not only on the JDBC implementation. In
 * {@code layered/} it existed on {@code PatientDao} alone, so any caller that
 * wanted to search had to hold the concrete class - which is how the web tier came
 * to depend on the data tier through {@code app().patients().search(term)}, a
 * violation no import grep can see because the expression names no type.</p>
 */
public interface PatientRepository extends Repository<Patient, String> {

    /** The portal account behind this patient, if they have one. */
    Optional<Patient> findByUserUid(String userUid);

    /** Front-desk search across name, contact number and email. */
    List<Patient> search(String term);

    /** Every patient reachable on this contact number - for the duplicate check. */
    List<Patient> findByContactNumber(String contactNumber);
}
