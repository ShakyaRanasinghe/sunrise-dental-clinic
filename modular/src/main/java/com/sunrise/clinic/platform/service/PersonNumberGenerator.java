package com.sunrise.clinic.platform.service;

import com.sunrise.clinic.platform.data.PersonSequenceRepository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * The one source of readable person numbers: {@code YYMMDD + ROLE + NNNN}
 * (GAP-PAT-33) — e.g. {@code 260904PAT0001} for the first patient enrolled on
 * 4 September 2026. Same family as the appointment numbers, so the front desk
 * reads one down a telephone the same way.
 *
 * <p>Role codes are the role's first three letters, uppercase: PAT, REC, DEN,
 * ADM. The daily sequence restarts per role and day ({@code PAT-20260904}), so
 * numbers stay short while staying unique. Like its appointment sibling, this
 * class holds no state: the database row lock does the concurrency work and it
 * survives a restart.</p>
 */
public class PersonNumberGenerator {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter SHORT_DAY = DateTimeFormatter.ofPattern("yyMMdd");

    private final PersonSequenceRepository sequences;

    public PersonNumberGenerator(PersonSequenceRepository sequences) {
        this.sequences = sequences;
    }

    /**
     * The next number for a role on a date.
     *
     * <p>Must be called inside the creation transaction. If the transaction rolls
     * back the increment rolls back with it, so a failed registration does not burn
     * a number and leave a gap.</p>
     */
    public String next(LocalDate date, String roleCode) {
        String code = requireRoleCode(roleCode);
        return format(date, code, sequences.nextFor(code + "-" + date.format(DAY)));
    }

    private static String requireRoleCode(String roleCode) {
        if (roleCode == null || !roleCode.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException(
                    "Role code must be three uppercase letters, not " + roleCode);
        }
        return roleCode;
    }

    private static String format(LocalDate date, String roleCode, int sequence) {
        return String.format("%s%s%04d", date.format(SHORT_DAY), roleCode, sequence);
    }
}
