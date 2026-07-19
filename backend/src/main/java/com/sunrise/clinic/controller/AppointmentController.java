package com.sunrise.clinic.controller;

import com.sunrise.clinic.domain.Appointment;
import com.sunrise.clinic.domain.Patient;
import com.sunrise.clinic.domain.Role;
import com.sunrise.clinic.dto.AppointmentResponse;
import com.sunrise.clinic.dto.BookingRequest;
import com.sunrise.clinic.exception.ResourceNotFoundException;
import com.sunrise.clinic.mapper.ClinicMapper;
import com.sunrise.clinic.repository.PatientRepository;
import com.sunrise.clinic.security.ClinicAccess;
import com.sunrise.clinic.security.ClinicPrincipal;
import com.sunrise.clinic.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Appointment booking, retrieval (with diagnosis confidentiality), cancel and complete. */
@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final ClinicMapper mapper;
    private final ClinicAccess access;
    private final PatientRepository patients;

    public AppointmentController(AppointmentService appointmentService, ClinicMapper mapper,
                                 ClinicAccess access, PatientRepository patients) {
        this.appointmentService = appointmentService;
        this.mapper = mapper;
        this.access = access;
        this.patients = patients;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PATIENT','RECEPTIONIST','ADMIN')")
    public ResponseEntity<AppointmentResponse> book(@Valid @RequestBody BookingRequest req,
                                                    @AuthenticationPrincipal ClinicPrincipal user) {
        String patientId = req.patientId();
        if (patientId == null && user != null && user.role() == Role.PATIENT) {
            patientId = patients.findByUserUid(user.uid()).map(Patient::getId)
                    .orElseThrow(() -> new ResourceNotFoundException("No patient profile for user " + user.uid()));
        }
        if (patientId == null) {
            throw new IllegalArgumentException("patientId is required when booking on behalf of a patient");
        }
        Appointment appt = appointmentService.book(patientId, req.slotId(), req.treatmentId(),
                user != null ? user.uid() : "system", user != null ? user.role() : Role.PATIENT);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toAppointmentResponse(appt));
    }

    /** Returns the clinical view (with diagnosis) only to the treating dentist or the patient. */
    @GetMapping("/{appointmentNo}")
    public ResponseEntity<?> get(@PathVariable String appointmentNo,
                                 @AuthenticationPrincipal ClinicPrincipal user) {
        Appointment appt = appointmentService.findByNo(appointmentNo);
        if (access.canViewClinical(appt, user)) {
            return ResponseEntity.ok(mapper.toAppointmentDetail(appt));
        }
        return ResponseEntity.ok(mapper.toAppointmentResponse(appt));
    }

    @PostMapping("/{appointmentNo}/cancel")
    @PreAuthorize("hasAnyRole('PATIENT','RECEPTIONIST','ADMIN')")
    public AppointmentResponse cancel(@PathVariable String appointmentNo) {
        return mapper.toAppointmentResponse(appointmentService.cancel(appointmentNo));
    }

    @PostMapping("/{appointmentNo}/complete")
    @PreAuthorize("hasRole('DENTIST')")
    public ResponseEntity<?> complete(@PathVariable String appointmentNo,
                                      @RequestBody Map<String, String> body) {
        Appointment appt = appointmentService.complete(appointmentNo, body.get("diagnosis"));
        return ResponseEntity.ok(mapper.toAppointmentDetail(appt));
    }
}
