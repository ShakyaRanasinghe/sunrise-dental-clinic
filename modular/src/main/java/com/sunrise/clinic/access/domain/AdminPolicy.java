package com.sunrise.clinic.access.domain;

import java.util.Set;

/** What a {@code ADMIN} may do. */
public final class AdminPolicy extends RolePolicy {

    private static final Set<Action> PERMITTED = Set.of(
            Action.SEARCH_PATIENTS,
            Action.CANCEL_ANY,
            Action.ISSUE_BILL,
            Action.READ_REPORTS,
            Action.MANAGE_ACCOUNTS,
            Action.READ_AUDIT,
            Action.REVIEW_CONCERNS,
            Action.READ_REVIEWS
    );

    @Override
    public Role role() {
        return Role.ADMIN;
    }

    @Override
    public String homePath() {
        return "/admin/reports";
    }

    @Override
    public String loginPath() {
        return "/login/admin";
    }

    @Override
    protected Set<Action> permitted() {
        return PERMITTED;
    }
}
