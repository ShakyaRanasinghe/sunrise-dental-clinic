package com.sunrise.clinic.access.domain;

import java.util.Set;

/** What a {@code DENTIST} may do. */
public final class DentistPolicy extends RolePolicy {

    private static final Set<Action> PERMITTED = Set.of(
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
    protected Set<Action> permitted() {
        return PERMITTED;
    }
}
