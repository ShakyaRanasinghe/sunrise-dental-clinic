package com.sunrise.clinic.feedback.domain;

/** What a complaint is about - FR-CMP-02. Matches {@code complaint.category}. */
public enum ComplaintCategory {

    /** How the patient was spoken to or treated as a person. */
    CONDUCT,
    /**
     * A worry about the treatment itself.
     *
     * <p>Deliberately <em>not</em> a place for clinical detail. FR-ADM-58: a complaint must
     * not expose medical notes or a diagnosis, even when the complaint is about the
     * clinical care - the administrator reviewing conduct is not the patient's dentist.</p>
     */
    CLINICAL_CONCERN,
    WAIT_TIME,
    BILLING,
    OTHER;

    public String label() {
        return switch (this) {
            case CONDUCT -> "How I was treated";
            case CLINICAL_CONCERN -> "A concern about my treatment";
            case WAIT_TIME -> "Waiting time";
            case BILLING -> "Billing";
            case OTHER -> "Something else";
        };
    }
}
