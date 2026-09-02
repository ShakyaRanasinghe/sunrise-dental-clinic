package com.sunrise.clinic.platform.service;

/**
 * Validation for the phone numbers the clinic stores: a patient's contact number
 * and the clinic's own phone.
 *
 * <p>One rule, one place. Registration (walk-in and self-service), the patient's
 * profile edit, the receptionist's phone screen and the administrator's clinic
 * settings all reach the same check, so none of them can disagree about what a
 * number looks like — the defect the email pattern used to have.</p>
 *
 * <p>A Sri Lankan number, however it is typed, is a ten-digit local number that
 * starts with {@code 0} (mobile {@code 07...}, landline {@code 0XX...}: a
 * subscriber part of nine digits with an area or mobile prefix on the front).
 * The international form{, the {@code +94} prefix, is accepted and counted as the
 * same number.</p>
 */
public final class PhoneNumbers {

    private static final String ALLOWED_CHARS = "[0-9+()\\- ]+";
    private static final int LOCAL_DIGITS = 10;
    private static final String COUNTRY_CODE = "94";

    private PhoneNumbers() {
    }

    /**
     * Validate and return a trimmed phone number, or {@code null} when the value
     * is blank — the caller decides whether the field is required.
     *
     * @throws IllegalArgumentException when the value is not blank but is not a
     *         number the clinic would store (letters, a wrong start, or a length
     *         that cannot be a Sri Lankan number)
     */
    public static String validate(String value, String label) {
        String trimmed = value == null ? null : value.trim();
        if (trimmed == null || trimmed.isBlank()) {
            return null;
        }
        if (!trimmed.matches(ALLOWED_CHARS)) {
            throw new IllegalArgumentException(
                    label + " can contain digits with + ( ) and - only. No letters.");
        }

        String digits = trimmed.replaceAll("[^0-9]", "");
        boolean international = digits.startsWith(COUNTRY_CODE) && digits.length() == LOCAL_DIGITS + 1;
        if (international) {
            // +94 11 234 5678 is the same number as 011 234 5678. Count it as such.
            digits = "0" + digits.substring(COUNTRY_CODE.length());
        }
        if (!digits.startsWith("0")) {
            throw new IllegalArgumentException(
                    label + " must start with 0 (a local number) or +94 (international).");
        }
        if (digits.length() != LOCAL_DIGITS) {
            throw new IllegalArgumentException(
                    label + " must be exactly " + LOCAL_DIGITS + " digits, e.g. 077 123 4567.");
        }
        return trimmed;
    }
}