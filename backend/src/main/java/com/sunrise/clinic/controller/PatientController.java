package com.sunrise.clinic.controller;

import com.sunrise.clinic.domain.Patient;
import com.sunrise.clinic.dto.PatientRequest;
import com.sunrise.clinic.exception.ResourceNotFoundException;
import com.sunrise.clinic.repository.PatientRepository;
import com.sunrise.clinic.security.ClinicPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** Patient registration and lookup. */
@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientRepository patients;

    public PatientController(PatientRepository patients) {
        this.patients = patients;
    }

    @PostMapping
    public ResponseEntity<Patient> register(@Valid @RequestBody PatientRequest req,
                                            @AuthenticationPrincipal ClinicPrincipal user) {
        Patient p = Patient.builder()
                .id(UUID.randomUUID().toString())
                .userUid(user != null ? user.uid() : null)
                .name(req.name())
                .address(req.address())
                .contactNumber(req.contactNumber())
                .email(req.email())
                .dob(req.dob())
                .build();
        patients.save(p);
        return ResponseEntity.status(HttpStatus.CREATED).body(p);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','ADMIN')")
    public Patient get(@PathVariable String id) {
        return patients.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + id));
    }
}
