package com.sunrise.clinic.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A treatment offered by the clinic. Admin maintains the catalogue &amp; pricing. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Treatment {
    private String id;
    private String name;
    private String description;
    private double baseCost;    // Rs
    @Builder.Default
    private boolean active = true;
}
