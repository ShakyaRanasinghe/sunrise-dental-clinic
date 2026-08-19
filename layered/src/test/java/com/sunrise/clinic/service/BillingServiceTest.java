package com.sunrise.clinic.service;

import com.sunrise.clinic.domain.Appointment;
import com.sunrise.clinic.domain.AppointmentStatus;
import com.sunrise.clinic.domain.Bill;
import com.sunrise.clinic.domain.Dentist;
import com.sunrise.clinic.domain.Treatment;
import com.sunrise.clinic.pattern.billing.DefaultRevenueSplitStrategy;
import com.sunrise.clinic.pattern.billing.StandardBillingStrategy;
import com.sunrise.clinic.repository.inmemory.InMemoryAppointmentRepository;
import com.sunrise.clinic.repository.inmemory.InMemoryBillRepository;
import com.sunrise.clinic.repository.inmemory.InMemoryDentistRepository;
import com.sunrise.clinic.repository.inmemory.InMemoryTreatmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * TC-CLI-B05 — billing: computes total and three-way split, marks appointment BILLED.
 */
class BillingServiceTest {

    private InMemoryAppointmentRepository appointments;
    private InMemoryBillRepository bills;
    private BillingService service;

    @BeforeEach
    void setUp() {
        appointments = new InMemoryAppointmentRepository();
        bills = new InMemoryBillRepository();
        InMemoryDentistRepository dentists = new InMemoryDentistRepository();
        InMemoryTreatmentRepository treatments = new InMemoryTreatmentRepository();

        dentists.save(Dentist.builder().id("d1").name("Dr Silva").consultationFee(1500).build());
        treatments.save(Treatment.builder().id("t1").name("Filling").baseCost(5000).active(true).build());
        appointments.save(Appointment.builder()
                .appointmentNo("APT-20260720-0001").patientId("p1").dentistId("d1").treatmentId("t1")
                .status(AppointmentStatus.COMPLETED).build());

        service = new BillingService(appointments, bills, dentists, treatments,
                new StandardBillingStrategy(), new DefaultRevenueSplitStrategy(0.60), 200);
    }

    @Test
    void generatesBillWithCorrectTotalAndSplit() {
        Bill bill = service.generateBill("APT-20260720-0001", "reception1");

        assertEquals(6700, bill.getTotal(), 0.001);
        assertEquals(4500, bill.getDentistEarning(), 0.001);
        assertEquals(2000, bill.getClinicEarning(), 0.001);
        assertEquals(200, bill.getReceptionistEarning(), 0.001);
        assertEquals(AppointmentStatus.BILLED,
                appointments.findById("APT-20260720-0001").orElseThrow().getStatus());
    }
}
