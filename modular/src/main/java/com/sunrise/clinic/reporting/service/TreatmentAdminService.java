package com.sunrise.clinic.reporting.service;

import com.sunrise.clinic.access.domain.Action;
import com.sunrise.clinic.access.domain.ClinicPrincipal;
import com.sunrise.clinic.access.service.AccessControl;
import com.sunrise.clinic.platform.error.ResourceNotFoundException;
import com.sunrise.clinic.scheduling.data.TreatmentRepository;
import com.sunrise.clinic.scheduling.domain.Treatment;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Admin management of the treatment catalogue — add, edit and deactivate treatments.
 *
 * <p>Every method requires {@link Action#MANAGE_TREATMENTS}, so only the administrator
 * can reach it. The treatment table is the single source of truth for what appears in
 * the patient booking dropdown: deactivating a treatment removes it from booking
 * immediately without deleting any appointment history that references it.</p>
 */
public class TreatmentAdminService {

    private final TreatmentRepository treatments;

    public TreatmentAdminService(TreatmentRepository treatments) {
        this.treatments = treatments;
    }

    /** All treatments — active and inactive — for the admin catalogue view. */
    public List<Treatment> listAll(ClinicPrincipal caller) {
        AccessControl.require(caller, Action.MANAGE_TREATMENTS);
        return treatments.findAll();
    }

    /** Save a new treatment. */
    public Treatment add(ClinicPrincipal caller, String name, String description, BigDecimal baseCost) {
        AccessControl.require(caller, Action.MANAGE_TREATMENTS);
        Treatment t = Treatment.builder()
                .id("t-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10))
                .name(name)
                .description(description == null ? "" : description)
                .baseCost(baseCost)
                .active(true)
                .build();
        treatments.save(t);
        return t;
    }

    /** Update name, description and price of an existing treatment. */
    public void update(ClinicPrincipal caller, String id, String name,
                       String description, BigDecimal baseCost) {
        AccessControl.require(caller, Action.MANAGE_TREATMENTS);
        Treatment existing = treatments.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Treatment not found: " + id));
        Treatment updated = Treatment.builder()
                .id(existing.getId())
                .name(name)
                .description(description == null ? "" : description)
                .baseCost(baseCost)
                .active(existing.isActive())
                .build();
        treatments.save(updated);
    }

    /** Toggle a treatment active or inactive. */
    public void setActive(ClinicPrincipal caller, String id, boolean active) {
        AccessControl.require(caller, Action.MANAGE_TREATMENTS);
        Treatment existing = treatments.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Treatment not found: " + id));
        Treatment updated = Treatment.builder()
                .id(existing.getId())
                .name(existing.getName())
                .description(existing.getDescription())
                .baseCost(existing.getBaseCost())
                .active(active)
                .build();
        treatments.save(updated);
    }
}
