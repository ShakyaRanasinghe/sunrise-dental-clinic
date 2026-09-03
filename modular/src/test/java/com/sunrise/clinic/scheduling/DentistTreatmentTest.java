package com.sunrise.clinic.scheduling;

import com.sunrise.clinic.access.domain.ClinicPrincipal;
import com.sunrise.clinic.access.domain.Role;
import com.sunrise.clinic.scheduling.data.InMemoryDentistRepository;
import com.sunrise.clinic.scheduling.data.InMemoryDentistTreatmentRepository;
import com.sunrise.clinic.scheduling.data.InMemoryTreatmentRepository;
import com.sunrise.clinic.scheduling.domain.Dentist;
import com.sunrise.clinic.scheduling.domain.Treatment;
import com.sunrise.clinic.scheduling.domain.TreatmentResponse;
import com.sunrise.clinic.scheduling.service.ReferenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** GAP-FTB-07: a dentist's treatment list gates what a patient may book with them. */
class DentistTreatmentTest {

    private final InMemoryDentistRepository dentists = new InMemoryDentistRepository();
    private final InMemoryTreatmentRepository treatments = new InMemoryTreatmentRepository();
    private final InMemoryDentistTreatmentRepository links = new InMemoryDentistTreatmentRepository();
    private ReferenceService reference;

    private static final ClinicPrincipal SILVA =
            new ClinicPrincipal("u-dent1", "Dr. Ranil Silva", Role.DENTIST);

    @BeforeEach
    void setUp() {
        dentists.save(Dentist.builder().id("d-silva").userUid("u-dent1").name("Dr. Ranil Silva")
                .consultationFee(new BigDecimal("1500.00")).active(true).build());
        treatments.save(Treatment.builder().id("t-checkup").name("Routine check-up")
                .baseCost(new BigDecimal("1000.00")).active(true).build());
        treatments.save(Treatment.builder().id("t-rootcanal").name("Root canal therapy")
                .baseCost(new BigDecimal("18000.00")).active(true).build());
        reference = new ReferenceService(dentists, treatments, links);
    }

    @Test
    void withNoLinkListedEveryActiveTreatmentIsOffered() {
        List<TreatmentResponse> offered = reference.treatmentsFor(SILVA, "d-silva");
        assertEquals(2, offered.size());
    }

    @Test
    void aDisabledTreatmentIsHiddenFromThatDentistsBooking() {
        // Mimic a dentist who configured a list, then removed root canal therapy.
        links.enable("d-silva", "t-checkup");
        links.enable("d-silva", "t-rootcanal");
        links.disable("d-silva", "t-rootcanal");

        List<TreatmentResponse> offered = reference.treatmentsFor(SILVA, "d-silva");
        assertEquals(1, offered.size());
        assertEquals("t-checkup", offered.get(0).id());
    }

    @Test
    void enablingAndDisablingFlipsTheToggle() {
        reference.setTreatmentOffered(SILVA, "d-silva", "t-rootcanal", false);
        List<ReferenceService.TreatmentToggle> toggles =
                reference.dentistTreatmentToggles("d-silva");
        assertTrue(toggles.stream().noneMatch(t -> t.id().equals("t-rootcanal") && t.offered()));

        reference.setTreatmentOffered(SILVA, "d-silva", "t-rootcanal", true);
        assertTrue(reference.dentistTreatmentToggles("d-silva").stream()
                .anyMatch(t -> t.id().equals("t-rootcanal") && t.offered()));
    }
}
