package com.sunrise.clinic.scheduling.data;

import java.util.Set;

/**
 * Which treatments each dentist offers.
 *
 * <p>GAP-FTB-07: a dentist switches their treatment list on and off, and the booking
 * screen offers a patient only the treatments that dentist actually performs. A
 * junction table (<code>dentist_treatment</code>) holds the links; this port is the
 * persistence for it.</p>
 */
public interface DentistTreatmentRepository {

    /** The ids of the treatments {@code dentistId} currently offers. */
    Set<String> offeredTreatmentIds(String dentistId);

    /** Record that {@code dentistId} offers {@code treatmentId}. */
    void enable(String dentistId, String treatmentId);

    /** Record that {@code dentistId} no longer offers {@code treatmentId}. */
    void disable(String dentistId, String treatmentId);
}
