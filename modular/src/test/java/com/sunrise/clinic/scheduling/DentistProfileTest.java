package com.sunrise.clinic.scheduling;

import com.sunrise.clinic.access.domain.ClinicPrincipal;
import com.sunrise.clinic.access.domain.Role;
import com.sunrise.clinic.platform.error.ResourceNotFoundException;
import com.sunrise.clinic.scheduling.data.InMemoryDentistRepository;
import com.sunrise.clinic.scheduling.data.InMemoryDentistTreatmentRepository;
import com.sunrise.clinic.scheduling.data.InMemoryTreatmentRepository;
import com.sunrise.clinic.scheduling.domain.Dentist;
import com.sunrise.clinic.scheduling.domain.DentistResponse;
import com.sunrise.clinic.scheduling.service.ReferenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** GAP-DEN-14: the dentist's own profile details are editable from the dashboard. */
class DentistProfileTest {

    private final InMemoryDentistRepository dentists = new InMemoryDentistRepository();
    private ReferenceService reference;

    private static final ClinicPrincipal SILVA =
            new ClinicPrincipal("u-dent1", "Dr. Ranil Silva", Role.DENTIST);

    @BeforeEach
    void setUp() {
        dentists.save(Dentist.builder().id("d-silva").userUid("u-dent1").name("Dr. Ranil Silva")
                .specialization("General Dentistry").phone("0771234567")
                .consultationFee(new BigDecimal("1500.00")).active(true).build());
        reference = new ReferenceService(dentists, new InMemoryTreatmentRepository(),
                new InMemoryDentistTreatmentRepository());
    }

    @Test
    void ownProfileReadsBack() {
        DentistResponse profile = reference.ownProfile("u-dent1").orElseThrow();

        assertEquals("Dr. Ranil Silva", profile.name());
        assertEquals("0771234567", profile.phone());
    }

    @Test
    void updateOwnDetailsSavesNameSpecialisationAndPhone() {
        DentistResponse updated = reference.updateOwnDetails(SILVA, "u-dent1",
                "Dr. R. Silva", "Orthodontics", "+94771234567");

        assertEquals("Dr. R. Silva", updated.name());
        assertEquals("Orthodontics", updated.specialization());
        assertEquals("+94771234567", updated.phone());
        // The fee is administrator-owned: an edit here must not move it.
        assertEquals(new BigDecimal("1500.00"), updated.consultationFee());
        assertTrue(updated.active());
    }

    @Test
    void blankNameIsRefused() {
        assertThrows(IllegalArgumentException.class, () ->
                reference.updateOwnDetails(SILVA, "u-dent1", "  ", "Orthodontics", null));
    }

    @Test
    void malformedPhoneIsRefused() {
        assertThrows(IllegalArgumentException.class, () ->
                reference.updateOwnDetails(SILVA, "u-dent1", "Dr. R. Silva", null, "o77 abc"));
    }

    @Test
    void shareDefaultsToSixtyPercent() {
        // The 3-arg service carries the shipped default until wired live.
        assertEquals(60, reference.dentistSharePercent());
    }

    @Test
    void unknownAccountHasNoProfile() {
        assertTrue(reference.ownProfile("u-nobody").isEmpty());
        assertThrows(ResourceNotFoundException.class, () ->
                reference.updateOwnDetails(SILVA, "u-nobody", "Name", null, null));
    }
}
