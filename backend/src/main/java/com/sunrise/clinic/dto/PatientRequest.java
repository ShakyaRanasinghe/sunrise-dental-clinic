package com.sunrise.clinic.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

/** Request to register / update a patient. */
public record PatientRequest(
        @NotBlank(message = "name is required") String name,
        String address,
        @NotBlank(message = "contactNumber is required") String contactNumber,
        @Email(message = "email must be valid") String email,
        LocalDate dob) {
}
