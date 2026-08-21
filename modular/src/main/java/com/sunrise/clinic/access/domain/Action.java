package com.sunrise.clinic.access.domain;

/**
 * Everything a signed-in user can ask the system to do.
 *
 * <p>Authorisation is expressed as {@code AccessControl.require(user, Action.X)}
 * rather than as a test on {@link Role}. The difference matters: a role list at
 * a call site says <em>who</em> may do a thing and leaves <em>what</em> implicit,
 * so the same rule ends up restated in every servlet that needs it. In
 * {@code layered/} that produced 28 separate role tests across 13 classes, and
 * no single file answered "what may a receptionist do?".</p>
 *
 * <p>Here each {@link RolePolicy} answers for its own role, in one place
 * (FR-OOP-04, FR-WEB-08).</p>
 */
public enum Action {

    // --- appointments -------------------------------------------------
    /** Book an appointment for oneself. */
    BOOK_OWN,
    /** Book on behalf of a patient. */
    BOOK_FOR_PATIENT,
    /** Cancel one's own appointment. */
    CANCEL_OWN,
    /** Cancel anyone's appointment. */
    CANCEL_ANY,
    /** Read an appointment's clinical detail — diagnosis and medical notes. */
    READ_CLINICAL,
    /** Record a diagnosis and mark a treatment complete. */
    COMPLETE_TREATMENT,

    // --- the register -------------------------------------------------
    /**
     * Search or list the patient register.
     *
     * <p>Deliberately separate from {@link #READ_PATIENT_RECORD}. Listing everyone
     * the clinic has ever treated is a front-desk capability; reading the record of
     * the patient in your chair is a clinical one. A dentist has the second and not
     * the first, so this is two actions rather than one with an exception.</p>
     */
    SEARCH_PATIENTS,
    /** Read one patient's record - name, contact details, date of birth. */
    READ_PATIENT_RECORD,
    /** Register a walk-in patient. */
    REGISTER_PATIENT,
    /** Declare one's own medical notes. */
    DECLARE_OWN_NOTES,

    // --- scheduling ---------------------------------------------------
    /** Publish a dentist's availability. */
    PUBLISH_AVAILABILITY,

    // --- money --------------------------------------------------------
    /** Issue a bill. */
    ISSUE_BILL,
    /** Read the clinic's income and revenue split. */
    READ_REPORTS,

    // --- governance ---------------------------------------------------
    /** Create, unlock or deactivate an account. */
    MANAGE_ACCOUNTS,
    /** Add, edit pricing and deactivate treatments in the catalogue. */
    MANAGE_TREATMENTS,
    /** Read the audit trail. */
    READ_AUDIT,
    /** Raise a concern about a dentist. */
    RAISE_CONCERN,
    /** Read and resolve concerns. */
    REVIEW_CONCERNS,
    /** Rate a completed visit. */
    RATE_VISIT,
    /** Read individual reviews, with their comments and authors. */
    READ_REVIEWS,
    /** Read one's own rating as an aggregate only. */
    READ_OWN_RATING
}
