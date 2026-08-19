package com.sunrise.clinic.access.service;

import com.sunrise.clinic.access.domain.Action;
import com.sunrise.clinic.access.domain.ClinicPrincipal;

/**
 * The single place a request is refused.
 *
 * <p>Authorisation is asked as a question about an {@link Action}, never about a
 * {@link com.sunrise.clinic.access.domain.Role}. A call site that lists roles
 * restates a rule that belongs to the role itself, which is how {@code layered/}
 * ended with 28 role tests across 13 servlets and no file that answered "what
 * may a receptionist do?" (FR-OOP-04, FR-WEB-08).</p>
 */
public final class AccessControl {

    /** Thrown when a caller may not do what they asked. Mapped to 403. */
    public static class AccessDeniedException extends RuntimeException {
        public AccessDeniedException(String message) {
            super(message);
        }
    }

    /**
     * Thrown when there is no caller at all. Mapped to 401, not 403.
     *
     * <p>A separate type because the two answers mean different things to a
     * client: 401 says "sign in and try again", 403 says "signing in will not
     * help". Both used to raise {@code AccessDeniedException}, so an anonymous
     * call to {@code /api/auth/lock-status} was refused with 403
     * {@code forbidden} while the same anonymous call to any other API path was
     * refused by {@code AuthenticationFilter} with 401 {@code unauthenticated} -
     * the same condition reported two ways, depending on whether the path
     * happened to be in the filter's public list.</p>
     */
    public static class NotAuthenticatedException extends AccessDeniedException {
        public NotAuthenticatedException(String message) {
            super(message);
        }
    }

    private AccessControl() {
    }

    /** Refuses unless {@code user} may perform {@code action}. */
    public static void require(ClinicPrincipal user, Action action) {
        if (user == null) {
            throw new NotAuthenticatedException("Authentication is required.");
        }
        if (!user.policy().permits(action)) {
            throw new AccessDeniedException(
                    "Your role (" + user.role() + ") does not permit this action.");
        }
    }

    /**
     * Refuses unless {@code user} owns the record, or may perform {@code action}
     * on anyone's.
     *
     * <p>The ownership case is checked first and deliberately does not consult
     * the policy: a patient reading their own record needs no permission beyond
     * being that patient.</p>
     */
    public static void requireSelfOr(ClinicPrincipal user, String ownerUid, Action action) {
        if (user != null && user.uid().equals(ownerUid)) {
            return;
        }
        require(user, action);
    }
}
