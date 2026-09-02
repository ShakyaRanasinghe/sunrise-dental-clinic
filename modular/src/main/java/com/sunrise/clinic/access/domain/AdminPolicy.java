package com.sunrise.clinic.access.domain;

import java.util.List;
import java.util.Set;

/** What a {@code ADMIN} may do. */
public final class AdminPolicy extends RolePolicy {

    private static final Set<Action> PERMITTED = Set.of(
            Action.SEARCH_PATIENTS,
            Action.READ_PATIENT_RECORD,
            Action.REGISTER_PATIENT,
            Action.CANCEL_ANY,
            Action.ISSUE_BILL,
            Action.READ_REPORTS,
            Action.MANAGE_ACCOUNTS,
            Action.MANAGE_TREATMENTS,
            Action.MANAGE_CLINIC_SETTINGS,
            Action.MANAGE_PHONE,
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
                new NavItem("Treatments", "/admin/treatments"),
                new NavItem("Clinic", "/admin/clinic"),
                new NavItem("Complaints", "/admin/complaints"),
                new NavItem("Audit", "/admin/audit"));
    }

    /**
     * No "Home" tab: the Sunrise logo already lands here, on the reports screen,
     * so a second doorway to the same page would be noise - the same reason the
     * patient and reception navigation carry no Home (GAP-REC-07).
     */
    @Override
    public List<NavItem> navigation() {
        List<NavItem> items = new java.util.ArrayList<>();
        items.addAll(ownNavigation());
        return List.copyOf(items);
    }
}
