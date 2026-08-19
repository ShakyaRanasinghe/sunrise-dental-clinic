package com.sunrise.clinic.billing.service;

import com.sunrise.clinic.billing.domain.BillBreakdown;
import com.sunrise.clinic.billing.domain.RevenueSplit;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The clinic's default attribution:
 *
 * <ul>
 *   <li>the <b>dentist</b> takes the whole consultation fee plus a configurable share of
 *       the treatment cost - their professional fee;</li>
 *   <li>the <b>clinic</b> keeps the rest of the treatment cost - materials and facility;</li>
 *   <li>the <b>receptionist</b> takes the service charge - their handling fee.</li>
 * </ul>
 *
 * <p>The share is read from {@code clinic.revenue.dentist-treatment-share} and passed in,
 * so the policy changes by configuration rather than by edit.</p>
 *
 * <h2>Why the clinic's share is a subtraction and not a percentage</h2>
 *
 * <p>The dentist's share is rounded to the cent, and the clinic then takes
 * {@code treatmentCost - dentistShare} exactly. Computing both as percentages and rounding
 * each would let the pair miss the treatment cost by a cent - the classic rounding leak,
 * where a ledger is permanently short and nobody can say where. Subtracting makes the two
 * sum by construction, whatever the share is.</p>
 *
 * <p>The previous version did this with {@code Math.round(v * 100.0) / 100.0} on doubles.
 * The arithmetic mostly agreed; the type did not, and the same trick applied to a value
 * that could not be represented exactly is how a receipt ends up a cent out.</p>
 */
public class DefaultRevenueSplitStrategy implements RevenueSplitStrategy {

    private final BigDecimal dentistTreatmentShare;

    /** @param dentistTreatmentShare the dentist's fraction of the treatment cost, 0..1 */
    public DefaultRevenueSplitStrategy(BigDecimal dentistTreatmentShare) {
        if (dentistTreatmentShare == null
                || dentistTreatmentShare.compareTo(BigDecimal.ZERO) < 0
                || dentistTreatmentShare.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException(
                    "The dentist's treatment share must be between 0 and 1, not " + dentistTreatmentShare);
        }
        this.dentistTreatmentShare = dentistTreatmentShare;
    }

    @Override
    public RevenueSplit split(BillBreakdown breakdown) {
        BigDecimal dentistFromTreatment = breakdown.treatmentCost()
                .multiply(dentistTreatmentShare)
                .setScale(BillBreakdown.SCALE, RoundingMode.HALF_UP);

        BigDecimal dentistEarning = breakdown.consultationFee().add(dentistFromTreatment);
        // Exactly the remainder, so the two always sum to the treatment cost.
        BigDecimal clinicEarning = breakdown.treatmentCost().subtract(dentistFromTreatment);
        BigDecimal receptionistEarning = breakdown.serviceCharge();

        return new RevenueSplit(dentistEarning, clinicEarning, receptionistEarning);
    }
}
