package com.sunrise.clinic.access.web;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Where an anonymous browser is sent when it reaches a protected page — the
 * session-timeout redirect. Each role owns one prefix, so a timed-out
 * receptionist lands on the reception login, a dentist on the dentist login,
 * an administrator on the admin login, and a patient back on the public home
 * page.
 */
class AuthenticationFilterLandingTest {

    @Test
    void patientAreaReturnsToPublicHome() {
        assertEquals("/", AuthenticationFilter.anonymousLanding("/patient/home"));
        assertEquals("/", AuthenticationFilter.anonymousLanding("/patient/book"));
    }

    @Test
    void receptionAreaLandsOnReceptionLogin() {
        assertEquals("/login/reception", AuthenticationFilter.anonymousLanding("/reception/home"));
        assertEquals("/login/reception", AuthenticationFilter.anonymousLanding("/reception/day/2026-08-02"));
    }

    @Test
    void dentistAreaLandsOnDentistLogin() {
        assertEquals("/login/dentist", AuthenticationFilter.anonymousLanding("/dentist/schedule"));
    }

    @Test
    void adminAreaLandsOnAdminLogin() {
        assertEquals("/login/admin", AuthenticationFilter.anonymousLanding("/admin/reports"));
    }

    @Test
    void unknownProtectedPathFallsBackToPublicHome() {
        assertEquals("/", AuthenticationFilter.anonymousLanding("/shared/mystery"));
    }
}