package com.sunrise.clinic.service;

import com.sunrise.clinic.domain.Appointment;
import com.sunrise.clinic.domain.AppointmentStatus;
import com.sunrise.clinic.domain.Bill;
import com.sunrise.clinic.domain.Dentist;
import com.sunrise.clinic.domain.Treatment;
import com.sunrise.clinic.exception.ResourceNotFoundException;
import com.sunrise.clinic.pattern.billing.BillBreakdown;
import com.sunrise.clinic.pattern.billing.BillingStrategy;
import com.sunrise.clinic.pattern.billing.RevenueSplit;
import com.sunrise.clinic.pattern.billing.RevenueSplitStrategy;
import com.sunrise.clinic.repository.AppointmentRepository;
import com.sunrise.clinic.repository.BillRepository;
import com.sunrise.clinic.repository.DentistRepository;
import com.sunrise.clinic.repository.TreatmentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Produces a patient bill and its three-way revenue split.
 * Delegates the "how" to two injected Strategies ({@link BillingStrategy} and
 * {@link RevenueSplitStrategy}), so pricing and commission policy can change
 * without touching this service.
 */
@Service
public class BillingService {

    private final AppointmentRepository appointments;
    private final BillRepository bills;
    private final DentistRepository dentists;
    private final TreatmentRepository treatments;
    private final BillingStrategy billingStrategy;
    private final RevenueSplitStrategy revenueSplitStrategy;
    private final double serviceCharge;

    public BillingService(AppointmentRepository appointments,
                          BillRepository bills,
                          DentistRepository dentists,
                          TreatmentRepository treatments,
                          BillingStrategy billingStrategy,
                          RevenueSplitStrategy revenueSplitStrategy,
                          @Value("${clinic.billing.service-charge:200}") double serviceCharge) {
        this.appointments = appointments;
        this.bills = bills;
        this.dentists = dentists;
        this.treatments = treatments;
        this.billingStrategy = billingStrategy;
        this.revenueSplitStrategy = revenueSplitStrategy;
        this.serviceCharge = serviceCharge;
    }

    public Bill generateBill(String appointmentNo, String issuedByUid) {
        Appointment appointment = appointments.findById(appointmentNo)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + appointmentNo));
        Dentist dentist = dentists.findById(appointment.getDentistId())
                .orElseThrow(() -> new ResourceNotFoundException("Dentist not found: " + appointment.getDentistId()));
        Treatment treatment = treatments.findById(appointment.getTreatmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Treatment not found: " + appointment.getTreatmentId()));

        BillBreakdown breakdown = billingStrategy.calculate(
                dentist.getConsultationFee(), treatment.getBaseCost(), serviceCharge);
        RevenueSplit split = revenueSplitStrategy.split(breakdown);

        Bill bill = Bill.builder()
                .id(UUID.randomUUID().toString())
                .appointmentNo(appointmentNo)
                .patientId(appointment.getPatientId())
                .dentistId(dentist.getId())
                .receptionistUid(issuedByUid)
                .consultationFee(breakdown.consultationFee())
                .treatmentCost(breakdown.treatmentCost())
                .serviceCharge(breakdown.serviceCharge())
                .discount(breakdown.discount())
                .tax(breakdown.tax())
                .total(breakdown.total())
                .dentistEarning(split.dentistEarning())
                .clinicEarning(split.clinicEarning())
                .receptionistEarning(split.receptionistEarning())
                .issuedAt(Instant.now())
                .issuedByUid(issuedByUid)
                .build();
        bills.save(bill);

        appointment.setStatus(AppointmentStatus.BILLED);
        appointments.save(appointment);
        return bill;
    }
}
