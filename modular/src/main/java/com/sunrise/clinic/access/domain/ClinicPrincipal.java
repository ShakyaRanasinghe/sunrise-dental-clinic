package com.sunrise.clinic.access.domain;

import com.sunrise.clinic.access.web.AuthenticationFilter;

import java.io.Serializable;

/**
 * The authenticated user for a request: their account id, display name and role.
 *
 * <p>Placed in the HTTP session by {@code LoginServlet} and read back out by
 * {@link AuthenticationFilter}, which exposes it to servlets and JSP pages as a
 * request attribute. It implements {@link Serializable} because a servlet
 * container may persist or replicate sessions.</p>
 *
 * @param uid         the {@code user_account.uid}
 * @param displayName the name shown in the page header
 * @param role        the role that drives every access decision
 */
public record ClinicPrincipal(String uid, String displayName, Role role) implements Serializable {

    /** The session attribute the principal is stored under. */
    public static final String SESSION_KEY = "clinicPrincipal";

    /** The request attribute servlets and JSPs read. */
    public static final String REQUEST_KEY = "currentUser";

    // -----------------------------------------------------------------
    // JavaBean-style accessors, for JSP only.
    //
    // Expression Language 5.0 — the version in Tomcat 10.1 — resolves a
    // property by looking for getX(); it does not understand a record's x()
    // accessor. (EL 6.0 does, but requires Tomcat 11.) Without these, every
    // page that reads ${user.role} fails at render time. They delegate to the
    // record components, so there is one source of truth either way.
    // -----------------------------------------------------------------

    public String getUid() {
        return uid;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Role getRole() {
        return role;
    }

    /**
     * What this user may do, and where they belong.
     *
     * <p>The entry point to polymorphic authorisation: a caller asks the policy
     * rather than testing the role (FR-OOP-04).</p>
     */
    public RolePolicy policy() {
        return RolePolicy.of(role);
    }

    public boolean hasRole(Role... allowed) {
        for (Role candidate : allowed) {
            if (role == candidate) {
                return true;
            }
        }
        return false;
    }

    /** Administrators are a functional superset of receptionists. */
    public boolean isStaff() {
        return hasRole(Role.RECEPTIONIST, Role.ADMIN, Role.DENTIST);
    }
}
