package com.sunrise.clinic.access.domain;

/**
 * System roles (RBAC). Administrator is a functional superset of Receptionist.
 */
public enum Role {
    PATIENT,
    RECEPTIONIST,
    DENTIST,
    ADMIN
}
