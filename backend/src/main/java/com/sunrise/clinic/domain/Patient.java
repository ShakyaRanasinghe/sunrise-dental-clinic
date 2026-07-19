package com.sunrise.clinic.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** A registered patient. PII (address/contact) is access-controlled. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Patient {
    private String id;
    private String userUid;        // link to UserAccount (role PATIENT)
    private String name;
    private String address;
    private String contactNumber;
    private String email;
    private LocalDate dob;
}
