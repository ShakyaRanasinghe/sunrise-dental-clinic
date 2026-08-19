package com.sunrise.clinic.repository;

import com.sunrise.clinic.domain.Treatment;

import java.util.List;

/** Persistence for the {@link Treatment} catalogue. */
public interface TreatmentRepository extends Repository<Treatment, String> {

    List<Treatment> findActive();
}
