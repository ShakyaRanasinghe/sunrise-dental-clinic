package com.sunrise.clinic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Sunrise Dental Clinic — Appointment &amp; Patient Management REST API.
 *
 * <p>Application (business) tier of a distributed 3-tier system:
 * React SPA (presentation) &rarr; this Spring Boot REST API (business)
 * &rarr; Firestore / in-memory (data).</p>
 */
@SpringBootApplication
public class ClinicApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClinicApplication.class, args);
    }
}
