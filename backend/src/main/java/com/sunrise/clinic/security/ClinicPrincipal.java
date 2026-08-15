package com.sunrise.clinic.security;

import com.sunrise.clinic.domain.Role;

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
