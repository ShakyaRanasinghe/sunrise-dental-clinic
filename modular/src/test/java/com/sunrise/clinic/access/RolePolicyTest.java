package com.sunrise.clinic.access;

import com.sunrise.clinic.access.domain.Action;
import com.sunrise.clinic.access.domain.Role;
import com.sunrise.clinic.access.domain.RolePolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * New in step 2.
 *
 * <p>Written after testing the deployed application found that a signed-in
 * patient could open all four role landing pages. The fix put a coarse prefix
 * check in {@code AuthenticationFilter}, and that check is only sound if the
 * four prefixes are exclusive — so the property it depends on is asserted here
 * rather than left to hold by luck as routes are added in later steps.</p>
 */
class RolePolicyTest {

    @ParameterizedTest
    @EnumSource(Role.class)
    void everyRoleHasAPolicy(Role role) {
        RolePolicy policy = RolePolicy.of(role);

        assertNotNull(policy);
        assertEquals(role, policy.role(), "the policy must speak for the role it was asked for");
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void everyPathIsAbsolute(Role role) {
        RolePolicy policy = RolePolicy.of(role);

        assertTrue(policy.homePath().startsWith("/"), policy.homePath());
        assertTrue(policy.loginPath().startsWith("/"), policy.loginPath());
        assertTrue(policy.ownedPrefix().startsWith("/") && policy.ownedPrefix().endsWith("/"),
                policy.ownedPrefix());
    }

    @Test
    void theOwnedPrefixIsTheFirstSegmentOfTheHomePath() {
        assertEquals("/patient/", RolePolicy.of(Role.PATIENT).ownedPrefix());
        assertEquals("/reception/", RolePolicy.of(Role.RECEPTIONIST).ownedPrefix());
        assertEquals("/dentist/", RolePolicy.of(Role.DENTIST).ownedPrefix());
        assertEquals("/admin/", RolePolicy.of(Role.ADMIN).ownedPrefix());
    }

    @Test
    void aRoleOwnsItsOwnHomePathAndNoOtherRoles() {
        // The property AuthenticationFilter's check rests on. If a fifth role
        // were given a home under an existing prefix, this fails rather than
        // silently opening that role's pages to the other.
        for (Role role : Role.values()) {
            String home = RolePolicy.of(role).homePath();
            List<Role> owners = new ArrayList<>();
            for (Role candidate : Role.values()) {
                if (home.startsWith(RolePolicy.of(candidate).ownedPrefix())) {
                    owners.add(candidate);
                }
            }
            assertEquals(List.of(role), owners, home + " must be owned by exactly one role");
        }
    }

    @Test
    void noPrefixIsAPrefixOfAnother() {
        // "/admin/" and "/administration/" would both match a path beginning
        // "/admin", and the first role in enum order would win.
        for (Role a : Role.values()) {
            for (Role b : Role.values()) {
                if (a == b) {
                    continue;
                }
                String pa = RolePolicy.of(a).ownedPrefix();
                String pb = RolePolicy.of(b).ownedPrefix();
                assertFalse(pa.startsWith(pb), pa + " starts with " + pb);
            }
        }
    }

    @Test
    void everyRoleSignsInThroughItsOwnPortal() {
        Set<String> portals = new HashSet<>();
        for (Role role : Role.values()) {
            assertTrue(portals.add(RolePolicy.of(role).loginPath()),
                    "two roles share a portal, so one could sign in through the other's page");
        }
        assertEquals(Role.values().length, portals.size());
        assertEquals(List.of("/login/patient"), List.of(RolePolicy.of(Role.PATIENT).loginPath()));
    }

    @Test
    void allOwnedPrefixesListsOnePerRole() {
        assertEquals(Role.values().length, RolePolicy.allOwnedPrefixes().size());
    }

    @Test
    void onlyAnAdministratorManagesAccounts() {
        assertTrue(RolePolicy.of(Role.ADMIN).permits(Action.MANAGE_ACCOUNTS));
        assertFalse(RolePolicy.of(Role.RECEPTIONIST).permits(Action.MANAGE_ACCOUNTS));
        assertFalse(RolePolicy.of(Role.DENTIST).permits(Action.MANAGE_ACCOUNTS));
        assertFalse(RolePolicy.of(Role.PATIENT).permits(Action.MANAGE_ACCOUNTS));
    }

    @Test
    void theRolesAreNotRankedButDistinct() {
        // Written first as "staff permit more actions than a patient", which
        // failed: the sets are 6, 6, 3 and 8, and a dentist permitting the fewest
        // is correct - a dentist does clinical work, not booking or billing.
        // Authority here follows purpose, not seniority, so what is worth
        // asserting is that each role has something of its own.
        for (Role role : Role.values()) {
            Set<Action> mine = permittedBy(role);
            Set<Action> others = new HashSet<>();
            for (Role other : Role.values()) {
                if (other != role) {
                    others.addAll(permittedBy(other));
                }
            }
            mine.removeAll(others);
            assertFalse(mine.isEmpty(), role + " permits nothing another role does not");
        }
    }

    @Test
    void onlyThePatientAndTheDentistReadTheClinicalRecord() {
        // The confidentiality rule the medical-note requirement turns on. A
        // receptionist books and bills without ever seeing why the patient came,
        // and an administrator reads totals, not conditions.
        assertTrue(RolePolicy.of(Role.DENTIST).permits(Action.READ_CLINICAL));
        assertTrue(RolePolicy.of(Role.PATIENT).permits(Action.READ_CLINICAL),
                "their own record is theirs; AccessControl.requireSelfOr scopes it to them");
        assertFalse(RolePolicy.of(Role.RECEPTIONIST).permits(Action.READ_CLINICAL));
        assertFalse(RolePolicy.of(Role.ADMIN).permits(Action.READ_CLINICAL));
    }

    @Test
    void everyActionIsPermittedBySomeRole() {
        // An action no role permits is dead code that reads like a rule.
        Set<Action> covered = new HashSet<>();
        for (Role role : Role.values()) {
            covered.addAll(permittedBy(role));
        }
        Set<Action> orphaned = new HashSet<>(Set.of(Action.values()));
        orphaned.removeAll(covered);
        assertEquals(Set.of(), orphaned, "no role permits these");
    }

    private static Set<Action> permittedBy(Role role) {
        RolePolicy policy = RolePolicy.of(role);
        Set<Action> permitted = new HashSet<>();
        for (Action action : Action.values()) {
            if (policy.permits(action)) {
                permitted.add(action);
            }
        }
        return permitted;
    }
}
