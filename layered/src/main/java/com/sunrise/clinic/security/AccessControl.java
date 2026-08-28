package com.sunrise.clinic.security;

import com.sunrise.clinic.domain.Role;

/**
 * Role checks, expressed as a guard a servlet calls at the top of an operation.
 *
 * <p>This is the replacement for the {@code @PreAuthorize} annotations the previous
 * build used. The rule is written in Java at the start of the method it protects,
 * so it is visible in the same file as the operation, steps through in a debugger,
 * and is exercised by ordinary unit tests rather than depending on a proxy being
 * woven in at runtime.</p>
 */
public final class AccessControl {

    /** Thrown when a signed-in user attempts something their role does not permit. */
    public static class AccessDeniedException extends RuntimeException {
        public AccessDeniedException(String message) {
            super(message);
        }
    }

    private AccessControl() {
    }

    /**
     * Require that the current user holds one of {@code allowed}.
     *
     * @throws AccessDeniedException if they are anonymous or hold another role
     */
    public static void require(ClinicPrincipal user, Role... allowed) {
        if (user == null) {
            throw new AccessDeniedException("Authentication is required.");
        }
        if (!user.hasRole(allowed)) {
            throw new AccessDeniedException(
                    "Your role (" + user.role() + ") does not permit this action.");
        }
    }

    /** Require that the user is the owner of a record, or holds one of {@code allowed}. */
    public static void requireSelfOr(ClinicPrincipal user, String ownerUid, Role... allowed) {
        if (user != null && user.uid().equals(ownerUid)) {
            return;
        }
        require(user, allowed);
    }
}
