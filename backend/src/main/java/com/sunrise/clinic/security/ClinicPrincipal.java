package com.sunrise.clinic.security;

import com.sunrise.clinic.domain.Role;

/**
 * The authenticated user for a request: their Firebase UID and role.
 * Injected into controllers via {@code @AuthenticationPrincipal}.
 */
public record ClinicPrincipal(String uid, Role role) {
}
