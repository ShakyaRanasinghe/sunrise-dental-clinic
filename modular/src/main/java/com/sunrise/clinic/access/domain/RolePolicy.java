package com.sunrise.clinic.access.domain;

import java.util.List;
import java.util.Set;

/**
 * What one role may do, and where it lands after signing in.
 *
 * <p>One subclass per role, so each role's authority is stated in a single
 * class rather than scattered across the servlets that happen to enforce it
 * (FR-OOP-04). Adding a fifth role is one new subclass and one new
 * {@code AbstractLoginServlet} subclass, with no edit to either superclass
 * (FR-OOP-13).</p>
 *
 * <p>Subclasses declare a {@link Set} of permitted {@link Action}s rather than
 * overriding {@link #permits} with a switch: a set is data, so it can be read,
 * logged and tested without executing anything.</p>
 */
public abstract class RolePolicy {

    /** The role this policy speaks for. */
    public abstract Role role();

    /** Where this role lands after signing in. */
    public abstract String homePath();

    /** The portal this role signs in through. */
    public abstract String loginPath();

    /** Everything this role may do. */
    protected abstract Set<Action> permitted();

    /**
     * The URL prefix this role owns, derived from {@link #homePath()} - so
     * {@code /reception/home} yields {@code /reception/}.
     *
     * <p>Derived rather than declared on purpose. A second abstract method would
     * be a second place to state the same fact, and the two could disagree; a
     * policy whose home is {@code /dentist/schedule} cannot then claim to own
     * {@code /admin/}. The four prefixes are exclusive across every route in
     * docs/servlets.md, which is what makes the check in
     * {@code AuthenticationFilter} sound.</p>
     */
    public final String ownedPrefix() {
        String home = homePath();
        int second = home.indexOf('/', 1);
        return second < 0 ? home + "/" : home.substring(0, second + 1);
    }

    /**
     * One entry in the site navigation.
     *
     * @param label what the link says
     * @param path  where it goes, relative to the context path
     */
    public record NavItem(String label, String path) {
    }

    /**
     * The navigation this role sees, in order.
     *
     * <p>Declared by the role rather than assembled in the header, for the same
     * reason {@link #homePath()} is: the alternative is a chain of
     * {@code <c:if test="${user.role == 'RECEPTIONIST'}">} in a JSP, which is a
     * switch on role written in the least testable place in the codebase, and it has
     * to be edited every time any module adds a screen.</p>
     *
     * <p>The base list is what every role has. Each policy prepends its own, and a
     * module landing in a later step adds one line to one class.</p>
     */
    public List<NavItem> navigation() {
        List<NavItem> items = new java.util.ArrayList<>();
        items.add(new NavItem("Home", homePath()));
        items.addAll(ownNavigation());
        items.add(new NavItem("Help", "/help"));
        return List.copyOf(items);
    }

    /**
     * What this role has beyond Home and Help. Empty until a module gives the role a
     * screen of its own.
     */
    protected List<NavItem> ownNavigation() {
        return List.of();
    }

    /**
     * The prefixes this role may enter.
     *
     * <p>Its own by default. {@link AdminPolicy} widens it, because the administrator
     * holds every action the front-desk screens use - {@code SEARCH_PATIENTS},
     * {@code REGISTER_PATIENT}, {@code CANCEL_ANY}, {@code ISSUE_BILL} - so barring it
     * from those addresses while permitting the operations behind them was an
     * inconsistency, not a boundary.</p>
     *
     * <p>It is still not a superuser hatch. The administrator may not enter
     * {@code /patient/} or {@code /dentist/}: those are one person's own pages and the
     * clinical record, and the administrator deliberately lacks
     * {@code READ_CLINICAL}. Authority follows purpose here, not rank.</p>
     */
    public Set<String> enterablePrefixes() {
        return Set.of(ownedPrefix());
    }

    /** Every role-owned prefix, for the filter's structural check. */
    public static java.util.List<String> allOwnedPrefixes() {
        return java.util.Arrays.stream(Role.values()).map(RolePolicy::of)
                .map(RolePolicy::ownedPrefix).distinct().toList();
    }

    /** Whether this role may perform {@code action}. */
    public final boolean permits(Action action) {
        return permitted().contains(action);
    }

    /** The policy for {@code role}. */
    public static RolePolicy of(Role role) {
        return switch (role) {
            case PATIENT -> new PatientPolicy();
            case RECEPTIONIST -> new ReceptionPolicy();
            case DENTIST -> new DentistPolicy();
            case ADMIN -> new AdminPolicy();
        };
    }
}
