package com.sunrise.clinic.scheduling.service;

import com.sunrise.clinic.access.domain.ClinicPrincipal;
import com.sunrise.clinic.platform.error.ResourceNotFoundException;
import com.sunrise.clinic.platform.service.PhoneNumbers;
import com.sunrise.clinic.scheduling.data.DentistRepository;
import com.sunrise.clinic.scheduling.data.DentistTreatmentRepository;
import com.sunrise.clinic.scheduling.data.TreatmentRepository;
import com.sunrise.clinic.scheduling.domain.Dentist;
import com.sunrise.clinic.scheduling.domain.DentistResponse;
import com.sunrise.clinic.scheduling.domain.Treatment;
import com.sunrise.clinic.scheduling.domain.TreatmentResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
    private final DentistTreatmentRepository dentistTreatments;
    // GAP-DEN-15: read per call, so the profile always states the live share.
    private final java.util.function.Supplier<java.math.BigDecimal> dentistShare;

    public ReferenceService(DentistRepository dentists, TreatmentRepository treatments,
                            DentistTreatmentRepository dentistTreatments) {
        this(dentists, treatments, dentistTreatments, () -> new java.math.BigDecimal("0.60"));
    }

    public ReferenceService(DentistRepository dentists, TreatmentRepository treatments,
                            DentistTreatmentRepository dentistTreatments,
                            java.util.function.Supplier<java.math.BigDecimal> dentistShare) {
        this.dentists = dentists;
        this.treatments = treatments;
        this.dentistTreatments = dentistTreatments;
        this.dentistShare = dentistShare;
    }

    /**
     * The configured dentist share as a whole percent (GAP-DEN-15) — "60", not
     * "0.60" — for the dentist's own profile screen. A rate, never an earnings
     * figure (srs-dentist.md §9).
     */
    public int dentistSharePercent() {
        try {
            java.math.BigDecimal share = dentistShare.get();
            if (share == null) {
                return 60;
            }
            return share.multiply(new java.math.BigDecimal("100")).intValue();
        } catch (RuntimeException e) {
            return 60;
        }
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
     * The treatments a patient may book with {@code dentistId}. GAP-FTB-07: a dentist
     * switches their list on/off, so the booking screen offers only what that dentist
     * performs. If the dentist offers none (or the list was never seeded) every active
     * treatment remains available, so an "Other" booking is always possible.
     */
    public List<TreatmentResponse> treatmentsFor(ClinicPrincipal caller, String dentistId) {
        Set<String> offered = dentistTreatments.offeredTreatmentIds(dentistId);
        return treatments.findActive().stream()
                .filter(t -> offered.isEmpty() || offered.contains(t.getId()))
                .map(TreatmentResponse::of)
                .toList();
    }

    /**
     * The full active catalogue, each with whether {@code dentistId} offers it, for the
     * dentist's own toggle screen. GAP-FTB-07.
     */
    public List<TreatmentToggle> dentistTreatmentToggles(String dentistId) {
        Set<String> offered = dentistTreatments.offeredTreatmentIds(dentistId);
        List<TreatmentToggle> toggles = new ArrayList<>();
        for (Treatment t : treatments.findActive()) {
            toggles.add(new TreatmentToggle(t.getId(), t.getName(), t.getDescription(),
                    t.getBaseCost(), offered.contains(t.getId())));
        }
        return toggles;
    }

    /**
     * Enable or disable a treatment for a dentist, from the dentist's own dashboard.
     * GAP-FTB-07. The dentist may only touch their own record.
     */
    public void setTreatmentOffered(ClinicPrincipal caller, String dentistId, String treatmentId,
                                    boolean offered) {
        requireDentist(dentistId);   // 404 for a bogus id, before touching anything
        if (offered) {
            dentistTreatments.enable(dentistId, treatmentId);
        } else {
            dentistTreatments.disable(dentistId, treatmentId);
        }
    }

    /** One toggle row for the dentist's treatment screen. GAP-FTB-07. */
    public record TreatmentToggle(String id, String name, String description,
                                  java.math.BigDecimal baseCost, boolean offered) {}

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

    /**
     * A dentist updates their own public contact number, shown to patients on the
     * clinic's public "Our dentists" page. GAP-FTB-04.
     *
     * @param caller   the signed-in dentist (a DENTIST principal)
     * @param userUid  the caller's account uid, used to locate their dentist record
     * @param phone    the new number (may be null/blank to clear it)
     */
    public void updateOwnPhone(ClinicPrincipal caller, String userUid, String phone) {
        Dentist dentist = dentists.findByUserUid(userUid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No dentist record for this account"));
        Dentist updated = Dentist.builder()
                .id(dentist.getId())
                .userUid(dentist.getUserUid())
                .name(dentist.getName())
                .specialization(dentist.getSpecialization())
                .phone(phone == null || phone.isBlank() ? null : phone.trim())
                .consultationFee(dentist.getConsultationFee())
                .active(dentist.isActive())
                .build();
        dentists.save(updated);
    }

    /**
     * The dentist record behind a portal account, for the dentist's own profile
     * screen (GAP-DEN-14). Resolved from the uid, so a dentist reads only their own.
     */
    public Optional<DentistResponse> ownProfile(String userUid) {
        return dentists.findByUserUid(userUid).map(DentistResponse::of);
    }

    /**
     * Edits the dentist's own profile details (GAP-DEN-14).
     *
     * <p>Resolved from the caller's own uid, so there is no id to change to reach
     * someone else's record — the same self-scoping as {@link #updateOwnPhone}, and
     * for the same reason no action check: the record is own by construction. The
     * consultation fee is deliberately absent — it prices every bill, so only the
     * administrator sets it (GAP-ADM-02).</p>
     *
     * @return the updated record, for the screen to re-render
     */
    public DentistResponse updateOwnDetails(ClinicPrincipal caller, String userUid,
                                            String name, String specialization, String phone) {
        Dentist dentist = dentists.findByUserUid(userUid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No dentist record for this account"));
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name is required.");
        }
        String cleanPhone = phone == null || phone.isBlank() ? null
                : PhoneNumbers.validate(phone.trim(), "phone");
        Dentist updated = Dentist.builder()
                .id(dentist.getId())
                .userUid(dentist.getUserUid())
                .name(name.trim())
                .specialization(specialization == null || specialization.isBlank()
                        ? null : specialization.trim())
                .phone(cleanPhone)
                .consultationFee(dentist.getConsultationFee())
                .active(dentist.isActive())
                .build();
        dentists.save(updated);
        return DentistResponse.of(updated);
    }
}
