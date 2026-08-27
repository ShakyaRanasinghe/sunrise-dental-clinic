package com.sunrise.clinic.scheduling.data;

import com.sunrise.clinic.platform.data.Repository;

import com.sunrise.clinic.scheduling.domain.Dentist;

import java.util.List;
import java.util.Optional;

/** Persistence for {@link Dentist}s. */
public interface DentistRepository extends Repository<Dentist, String> {

    /** The dentist record behind a portal account, if the dentist has one. */
    Optional<Dentist> findByUserUid(String userUid);

    /**
     * Dentists currently practising.
     *
     * <p>Declared on the port, not only on {@link DentistDao}. It existed on the DAO
     * alone, so {@code ReferenceApiServlet} had to hold the concrete class to call it -
     * the same shape of leak that put {@code search} on {@code PatientDao}.</p>
     */
    List<Dentist> findActive();
}
