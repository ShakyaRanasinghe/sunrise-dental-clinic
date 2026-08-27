package com.sunrise.clinic.access.domain;

import java.util.List;
import java.util.Set;

/** What a {@code DENTIST} may do. */
public final class DentistPolicy extends RolePolicy {

    private static final Set<Action> PERMITTED = Set.of(
            // One patient record, for the person being treated - not the register.
            Action.READ_PATIENT_RECORD,
            Action.READ_CLINICAL,
            Action.COMPLETE_TREATMENT,
            Action.READ_OWN_RATING
    );

    @Override
    public Role role() {
        return Role.DENTIST;
    }

    @Override
    public String homePath() {
        return "/dentist/schedule";
    }

    @Override
    public String loginPath() {
        return "/login/dentist";
    }

    @Override
    protected List<NavItem> ownNavigation() {
        return List.of(
                new NavItem("Schedule", "/dentist/schedule"),
                new NavItem("Availability", "/dentist/availability"));
    }

    @Override
    protected Set<Action> permitted() {
        return PERMITTED;
    }
}
