package com.sunrise.clinic.access.domain;

import java.util.Set;

/** What a {@code PATIENT} may do. */
public final class PatientPolicy extends RolePolicy {

    private static final Set<Action> PERMITTED = Set.of(
            Action.BOOK_OWN,
            Action.CANCEL_OWN,
            Action.READ_CLINICAL,
            Action.DECLARE_OWN_NOTES,
            Action.RAISE_CONCERN,
            Action.RATE_VISIT
    );

    @Override
    public Role role() {
        return Role.PATIENT;
    }

    @Override
    public String homePath() {
        return "/patient/home";
    }

    @Override
    public String loginPath() {
        return "/login/patient";
    }

    @Override
    protected Set<Action> permitted() {
        return PERMITTED;
    }
}
