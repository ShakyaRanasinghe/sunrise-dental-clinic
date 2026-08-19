package com.sunrise.clinic.access.domain;

import java.util.List;
import java.util.Set;

/** What a {@code ADMIN} may do. */
public final class AdminPolicy extends RolePolicy {

    private static final Set<Action> PERMITTED = Set.of(
            Action.SEARCH_PATIENTS,
            Action.READ_PATIENT_RECORD,
            // The administrator already carries the front-desk actions below -
            // CANCEL_ANY, ISSUE_BILL - so lacking this one was an inconsistency, not
            // a boundary. docs/api/patients.md specifies reception OR admin.
            Action.REGISTER_PATIENT,
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

    /**
     * The administrator covers the front desk as well as its own screens, because it
     * holds every action those screens perform. It does not reach {@code /patient/} or
     * {@code /dentist/} - see {@link RolePolicy#enterablePrefixes()}.
     */
    @Override
    public Set<String> enterablePrefixes() {
        return Set.of(ownedPrefix(), RolePolicy.of(Role.RECEPTIONIST).ownedPrefix());
    }

    /** The administrator's own screens. */
    @Override
    protected List<NavItem> ownNavigation() {
        return List.of(
                new NavItem("Reports", "/admin/reports"),
                new NavItem("Accounts", "/admin/accounts"),
                new NavItem("Audit", "/admin/audit"));
    }
}
