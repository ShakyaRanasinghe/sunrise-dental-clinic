package com.sunrise.clinic.billing.domain;

import java.math.BigDecimal;

/**
 * The three-way split of a bill's revenue.
 *
 * <p>Visible to the administrator. Each member of staff additionally sees their own
 * figure, and never the others' - a receptionist has no business knowing what a dentist
 * earns.</p>
 *
 * @param dentistEarning      attributed to the treating dentist
 * @param clinicEarning       retained by the clinic
 * @param receptionistEarning attributed to the receptionist who handled it
 */
public record RevenueSplit(BigDecimal dentistEarning,
                           BigDecimal clinicEarning,
                           BigDecimal receptionistEarning) {

    public RevenueSplit {
        dentistEarning = BillBreakdown.money(dentistEarning);
        clinicEarning = BillBreakdown.money(clinicEarning);
        receptionistEarning = BillBreakdown.money(receptionistEarning);
    }

    /**
     * @return the three shares added up
     *
     * <p>Exists to be asserted against the bill total. A split whose parts do not sum to
     * what the patient paid means money has been attributed to nobody or to two people,
     * and rounding each share independently is the usual way that happens.</p>
     */
    public BigDecimal sum() {
        return dentistEarning.add(clinicEarning).add(receptionistEarning);
    }
}
