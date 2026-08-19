package com.sunrise.clinic.access.domain;

import java.util.Set;

/** What a {@code RECEPTIONIST} may do. */
public final class ReceptionPolicy extends RolePolicy {

    private static final Set<Action> PERMITTED = Set.of(
            Action.BOOK_FOR_PATIENT,
            Action.CANCEL_ANY,
            Action.SEARCH_PATIENTS,
            Action.REGISTER_PATIENT,
            Action.PUBLISH_AVAILABILITY,
            Action.ISSUE_BILL
    );

    @Override
    public Role role() {
        return Role.RECEPTIONIST;
    }

    @Override
    public String homePath() {
        return "/reception/home";
    }

    @Override
    public String loginPath() {
        return "/login/reception";
    }

    @Override
    protected Set<Action> permitted() {
        return PERMITTED;
    }
}
