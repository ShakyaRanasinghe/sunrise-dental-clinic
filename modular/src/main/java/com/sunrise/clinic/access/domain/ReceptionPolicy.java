package com.sunrise.clinic.access.domain;

import java.util.List;
import java.util.Set;

/** What a {@code RECEPTIONIST} may do. */
public final class ReceptionPolicy extends RolePolicy {

    private static final Set<Action> PERMITTED = Set.of(
            Action.BOOK_FOR_PATIENT,
            Action.CANCEL_ANY,
            Action.SEARCH_PATIENTS,
            Action.READ_PATIENT_RECORD,
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

    /**
     * Reception also enters /patient/book to book on behalf of a walk-in patient.
     * Fine-grained checks in the servlet enforce BOOK_FOR_PATIENT; this just opens
     * the structural gate the filter checks.
     */
    @Override
    public Set<String> enterablePrefixes() {
        return Set.of(ownedPrefix(), RolePolicy.of(Role.PATIENT).ownedPrefix());
    }

    @Override
    protected List<NavItem> ownNavigation() {
        return List.of(
                new NavItem("Walk-in", "/reception/walkin"),
                new NavItem("Dentists", "/reception/dentists"),
                new NavItem("Patients", "/reception/patients"),
                new NavItem("Availability", "/reception/availability"),
                new NavItem("Billing", "/reception/billing"));
    }
}
