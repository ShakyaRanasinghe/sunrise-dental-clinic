package com.sunrise.clinic.scheduling.data;

import com.sunrise.clinic.platform.data.Repository;

import com.sunrise.clinic.scheduling.domain.Treatment;

import java.util.List;

/** Persistence for the {@link Treatment} catalogue. */
public interface TreatmentRepository extends Repository<Treatment, String> {

    List<Treatment> findActive();
}
