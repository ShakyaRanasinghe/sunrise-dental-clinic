package com.sunrise.clinic.access.domain;

import java.util.List;
import java.util.Set;

/** What a {@code PATIENT} may do. */
public final class PatientPolicy extends RolePolicy {

    private static final Set<Action> PERMITTED = Set.of(
            Action.BOOK_OWN,
            Action.CANCEL_OWN,
            Action.READ_CLINICAL,
            Action.EDIT_OWN_PROFILE,
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

    /** A patient's own pages: their appointments, booking another, and their help. */
    @Override
    protected List<NavItem> ownNavigation() {
        return List.of(
                new NavItem("Book", "/patient/book"),
                new NavItem("Raise a concern", "/patient/complaints"),
                new NavItem("Help", "/help/patient"));
    }

    /**
     * The "Home" tab is omitted: the Sunrise logo already takes a signed-in
     * visitor to the same landing page. "Help" is kept (GAP-PAT-24): it leads to
     * the patient's own help page (GAP-FTB-12), not the visitor page.
     */
    @Override
    public List<NavItem> navigation() {
        List<NavItem> items = new java.util.ArrayList<>();
        items.addAll(ownNavigation());
        return List.copyOf(items);
    }
}
