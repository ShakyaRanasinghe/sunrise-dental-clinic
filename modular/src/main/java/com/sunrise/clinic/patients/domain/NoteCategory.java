package com.sunrise.clinic.patients.domain;

/**
 * What kind of thing a patient has declared - FR-NOTE-03.
 *
 * <p>A category rather than free text so a dentist can scan a list before treating rather
 * than read paragraphs. The four values match the {@code patient_note.category} enum.</p>
 */
public enum NoteCategory {

    /** Something the patient reacts to. The category most likely to be critical. */
    ALLERGY,
    /** Something they are taking, which may interact with treatment or anaesthetic. */
    MEDICATION,
    /** An ongoing condition - diabetes, a heart condition, pregnancy. */
    CONDITION,
    /** Anything else the patient thinks the dentist should know. */
    OTHER;

    /** @return the label for a screen. */
    public String label() {
        return switch (this) {
            case ALLERGY -> "Allergy";
            case MEDICATION -> "Medication";
            case CONDITION -> "Condition";
            case OTHER -> "Other";
        };
    }
}
