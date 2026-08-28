package com.sunrise.clinic.scheduling.service;

import com.sunrise.clinic.access.domain.ClinicPrincipal;
import com.sunrise.clinic.platform.error.ResourceNotFoundException;
import com.sunrise.clinic.scheduling.data.DentistRepository;
import com.sunrise.clinic.scheduling.data.TreatmentRepository;
import com.sunrise.clinic.scheduling.domain.Dentist;
import com.sunrise.clinic.scheduling.domain.DentistResponse;
import com.sunrise.clinic.scheduling.domain.Treatment;
import com.sunrise.clinic.scheduling.domain.TreatmentResponse;

import java.util.List;

/**
 * The reference data every booking screen needs: who practises here, and what the
 * clinic treats.
 *
 * <p>New in the modular build, and it exists to close a boundary violation rather
 * than to add indirection. {@code ReferenceApiServlet} used to call
 * {@code app().dentists().findActive()} and {@code app().treatments().findActive()} -
 * the web tier reaching straight into the data tier. That expression names no type,
 * so an import grep never saw it; it is exactly the violation that made
 * {@code AppContext} stop exposing repositories at all.</p>
 *
 * <p>No permission check. Both lists are things any signed-in user may read - a
 * patient choosing a dentist needs the list of dentists - and the filter has already
 * turned anonymous callers away. Adding a check here would be theatre.</p>
 */
public class ReferenceService {

    private final DentistRepository dentists;
    private final TreatmentRepository treatments;

    public ReferenceService(DentistRepository dentists, TreatmentRepository treatments) {
        this.dentists = dentists;
        this.treatments = treatments;
    }

    /** Dentists currently practising, for a booking screen. */
    public List<DentistResponse> activeDentists(ClinicPrincipal caller) {
        return dentists.findActive().stream().map(DentistResponse::of).toList();
    }

    /** The treatment catalogue, active entries only. */
    public List<TreatmentResponse> activeTreatments(ClinicPrincipal caller) {
        return treatments.findActive().stream().map(TreatmentResponse::of).toList();
    }

    /**
     * Dentists currently practising, for the public landing page — anyone may read it.
     *
     * <p>The same list {@link #activeDentists} returns, minus the signed-in caller a
     * booking screen happens to have. A visitor reading the clinic's front page has no
     * account yet, so there is nobody to pass; the information itself (name,
     * specialisation, fee) is exactly what the practice advertises.</p>
     */
    public List<DentistResponse> directoryDentists() {
        return dentists.findActive().stream().map(DentistResponse::of).toList();
    }

    /** The treatment catalogue, for the public landing page — see {@link #directoryDentists}. */
    public List<TreatmentResponse> directoryTreatments() {
        return treatments.findActive().stream().map(TreatmentResponse::of).toList();
    }

    /**
     * One dentist.
     *
     * @throws ResourceNotFoundException if there is no such dentist - which is what
     *         turns an unknown {@code dentistId} into a 404 instead of the 500 a
     *         foreign-key violation used to produce
     */
    public Dentist requireDentist(String dentistId) {
        return dentists.findById(dentistId)
                .orElseThrow(() -> new ResourceNotFoundException("Dentist not found: " + dentistId));
    }

    /** One treatment, or {@link ResourceNotFoundException}. */
    public Treatment requireTreatment(String treatmentId) {
        return treatments.findById(treatmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Treatment not found: " + treatmentId));
    }

    /** The dentist record behind a signed-in dentist's account, if there is one. */
    public java.util.Optional<Dentist> forUser(String userUid) {
        return dentists.findByUserUid(userUid);
    }
}
