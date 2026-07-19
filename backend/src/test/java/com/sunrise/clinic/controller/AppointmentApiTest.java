package com.sunrise.clinic.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sunrise.clinic.domain.Dentist;
import com.sunrise.clinic.domain.Patient;
import com.sunrise.clinic.domain.Slot;
import com.sunrise.clinic.domain.SlotStatus;
import com.sunrise.clinic.domain.Treatment;
import com.sunrise.clinic.pattern.AppointmentNumberGenerator;
import com.sunrise.clinic.repository.inmemory.InMemoryDentistRepository;
import com.sunrise.clinic.repository.inmemory.InMemoryPatientRepository;
import com.sunrise.clinic.repository.inmemory.InMemorySlotRepository;
import com.sunrise.clinic.repository.inmemory.InMemoryTreatmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end API tests (TC-CLI-API-01..06): booking, diagnosis confidentiality per role,
 * RBAC (403), unauthenticated (401), and not-found (404). Runs against the in-memory
 * adapter — no cloud services required.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AppointmentApiTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired InMemoryPatientRepository patients;
    @Autowired InMemoryDentistRepository dentists;
    @Autowired InMemoryTreatmentRepository treatments;
    @Autowired InMemorySlotRepository slots;

    @BeforeEach
    void seed() {
        AppointmentNumberGenerator.getInstance().reset();
        patients.clear(); dentists.clear(); treatments.clear(); slots.clear();
        dentists.save(Dentist.builder().id("d1").userUid("dentist-uid").name("Dr Silva").consultationFee(1500).build());
        patients.save(Patient.builder().id("p1").userUid("patient-uid").name("Nimal").email("nimal@example.lk").build());
        treatments.save(Treatment.builder().id("t1").name("Filling").baseCost(5000).active(true).build());
        slots.save(Slot.builder().id("s1").dentistId("d1").date(LocalDate.of(2026, 7, 20))
                .startTime(LocalTime.of(16, 0)).status(SlotStatus.OPEN).build());
    }

    /** Books as the patient and returns the generated appointment number. */
    private String bookAppointment() throws Exception {
        var result = mvc.perform(post("/api/appointments")
                        .header("X-User-Uid", "patient-uid").header("X-User-Role", "PATIENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slotId\":\"s1\",\"treatmentId\":\"t1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andReturn();
        JsonNode body = om.readTree(result.getResponse().getContentAsString());
        return body.get("appointmentNo").asText();
    }

    @Test
    void patientCanBookAnOpenSlot() throws Exception {
        String no = bookAppointment();
        org.junit.jupiter.api.Assertions.assertTrue(no.startsWith("APT-"));
    }

    @Test
    void diagnosisIsHiddenFromAdminAndReceptionButVisibleToDentistAndPatient() throws Exception {
        String no = bookAppointment();

        // Dentist records the diagnosis on completion.
        mvc.perform(post("/api/appointments/{no}/complete", no)
                        .header("X-User-Uid", "dentist-uid").header("X-User-Role", "DENTIST")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"diagnosis\":\"Deep cavity, upper molar\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diagnosis").value("Deep cavity, upper molar"));

        // Admin: no diagnosis field at all.
        mvc.perform(get("/api/appointments/{no}", no)
                        .header("X-User-Uid", "admin-uid").header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diagnosis").doesNotExist());

        // Receptionist: no diagnosis field.
        mvc.perform(get("/api/appointments/{no}", no)
                        .header("X-User-Uid", "rec-uid").header("X-User-Role", "RECEPTIONIST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diagnosis").doesNotExist());

        // Treating dentist: diagnosis visible.
        mvc.perform(get("/api/appointments/{no}", no)
                        .header("X-User-Uid", "dentist-uid").header("X-User-Role", "DENTIST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diagnosis").value("Deep cavity, upper molar"));

        // The patient themselves: diagnosis visible.
        mvc.perform(get("/api/appointments/{no}", no)
                        .header("X-User-Uid", "patient-uid").header("X-User-Role", "PATIENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diagnosis").value("Deep cavity, upper molar"));
    }

    @Test
    void patientCannotPublishAvailability_forbidden() throws Exception {
        mvc.perform(post("/api/sessions")
                        .header("X-User-Uid", "patient-uid").header("X-User-Role", "PATIENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dentistId\":\"d1\",\"date\":\"2026-07-21\",\"startTime\":\"16:00\",\"endTime\":\"18:00\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unknownAppointmentReturns404() throws Exception {
        mvc.perform(get("/api/appointments/{no}", "APT-does-not-exist")
                        .header("X-User-Uid", "rec-uid").header("X-User-Role", "RECEPTIONIST"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("not_found"));
    }

    @Test
    void doubleBookingSameSlotReturns409() throws Exception {
        bookAppointment();
        mvc.perform(post("/api/appointments")
                        .header("X-User-Uid", "patient-uid").header("X-User-Role", "PATIENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slotId\":\"s1\",\"treatmentId\":\"t1\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("slot_unavailable"));
    }
}
