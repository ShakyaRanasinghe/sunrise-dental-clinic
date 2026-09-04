package com.sunrise.clinic.platform.service;

import com.sunrise.clinic.platform.data.InMemoryPersonSequenceRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** GAP-PAT-33: readable person numbers — YYMMDD + role + daily sequence. */
class PersonNumberGeneratorTest {

    private final PersonNumberGenerator numbers =
            new PersonNumberGenerator(new InMemoryPersonSequenceRepository());
    private static final LocalDate DAY = LocalDate.of(2026, 9, 4);

    @Test
    void firstPatientOfTheDay() {
        assertEquals("260904PAT0001", numbers.next(DAY, "PAT"));
    }

    @Test
    void sequenceIncrementsPerRoleAndDay() {
        assertEquals("260904PAT0001", numbers.next(DAY, "PAT"));
        assertEquals("260904PAT0002", numbers.next(DAY, "PAT"));
        // Other roles and days run their own sequences.
        assertEquals("260904DEN0001", numbers.next(DAY, "DEN"));
        assertEquals("260905PAT0001", numbers.next(DAY.plusDays(1), "PAT"));
    }

    @Test
    void roleCodeMustBeThreeUppercaseLetters() {
        assertThrows(IllegalArgumentException.class, () -> numbers.next(DAY, "pat"));
        assertThrows(IllegalArgumentException.class, () -> numbers.next(DAY, "PATIENT"));
        assertThrows(IllegalArgumentException.class, () -> numbers.next(DAY, null));
    }

    @Test
    void numbersMatchTheQuotableShape() {
        assertTrue(numbers.next(DAY, "REC").matches("\\d{6}REC\\d{4}"));
        assertTrue(numbers.next(DAY, "ADM").matches("\\d{6}ADM\\d{4}"));
    }
}
