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
     * The front desk's own screens. Grows as modules land: the day view and billing
     * arrive with {@code appointments} and {@code billing}.
     */
    @Override
    protected List<NavItem> ownNavigation() {
        return List.of(
                new NavItem("Patients", "/reception/patients"),
                new NavItem("Availability", "/reception/availability"),
                new NavItem("Billing", "/reception/billing"));
    }
}
