package com.sunrise.clinic.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A dentist. {@code consultationFee} feeds the billing + revenue-split calculation. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Dentist {
    private String id;
    private String userUid;         // optional link to UserAccount (role DENTIST)
    private String name;
    private String specialization;
    @Builder.Default
    private double consultationFee = 1500.0;   // Rs
    @Builder.Default
    private boolean active = true;
}
