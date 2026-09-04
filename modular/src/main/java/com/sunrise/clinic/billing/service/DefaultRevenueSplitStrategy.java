package com.sunrise.clinic.billing.service;

import com.sunrise.clinic.billing.domain.BillBreakdown;
import com.sunrise.clinic.billing.domain.RevenueSplit;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The clinic's default attribution, with two dials and one rule.
 *
 * <table>
 *   <caption>Where each part of a bill goes</caption>
 *   <tr><th>Charge</th><th>Dentist</th><th>Clinic (the owner)</th><th>Receptionist</th></tr>
 *   <tr><td>Consultation fee</td><td>all of it</td><td>&mdash;</td><td>&mdash;</td></tr>
 *   <tr><td>Treatment cost</td><td>{@code dentistTreatmentShare}</td><td>the remainder</td><td>&mdash;</td></tr>
 *   <tr><td>Service charge</td><td>&mdash;</td><td>the remainder</td><td>{@code receptionistServiceShare}</td></tr>
 * </table>
 *
 * <p>With the defaults - 60% of treatment to the dentist, 0% of the service charge to the
 * receptionist - a bill of Rs 5,200.00 (1500 consultation + 3500 treatment + 200 service
 * charge) attributes Rs 3,600.00 to the dentist and Rs 1,600.00 to the clinic. That is the
 * whole difference between what the patient pays and what the dentist is paid, which is
 * what the owner's margin means.</p>
 *
 * <h2>Why the consultation fee is not shared</h2>
 *
 * <p>The column is {@code dentist.consultation_fee} - <em>this dentist's</em> fee. Taking a
 * percentage of something the schema names as the dentist's would be internally
 * inconsistent, and it would add a second percentage that does nothing the treatment share
 * cannot already do. One dial on the professional fee is easier to reason about, and easier
 * to defend, than two.</p>
 *
 * <h2>Why the receptionist's share defaults to nothing</h2>
 *
 * <p>Reception is a salaried role. A commission on every bill would be unusual for a dental
 * practice, and it puts a mild incentive on the person who decides what to charge. So the
 * default is zero and the service charge is the clinic's - what a service charge is for:
 * the premises, the front desk, the booking system.</p>
 *
 * <p>It is a dial rather than a deletion because a practice that <em>does</em> pay handling
 * commission should be able to say so without a code change - {@code =1.0} gives the
 * receptionist the whole service charge, {@code =0.25} a quarter of it. That also keeps
 * FR-BIL-03 honest: the bill really does divide three ways, and the third way is a policy
 * decision rather than a column that is always zero.</p>
 *
 * <h2>Why the clinic's share is always a subtraction</h2>
 *
 * <p>Each of the other two shares is rounded to the cent; the clinic then takes exactly
 * what is left of each charge. Computing every share as its own percentage and rounding
 * each would let the three miss the bill by a cent - the classic rounding leak, where a
 * ledger is permanently short and nobody can say where. Subtracting makes them sum by
 * construction, whatever the dials are set to.</p>
 */
public class DefaultRevenueSplitStrategy implements RevenueSplitStrategy {

    // GAP-ADM-10: suppliers, not fixed values, so a Pricing-tab save applies to the
    // next bill without restart. Each read is validated — a bad row falls back rather
    // than mispricing a bill.
    private final java.util.function.Supplier<BigDecimal> dentistTreatmentShare;
    private final java.util.function.Supplier<BigDecimal> receptionistServiceShare;

    /**
     * @param dentistTreatmentShare    the dentist's fraction of the treatment cost, 0..1
     * @param receptionistServiceShare the receptionist's fraction of the service charge,
     *                                 0..1. Zero means the whole service charge is the
     *                                 clinic's
     */
    public DefaultRevenueSplitStrategy(BigDecimal dentistTreatmentShare,
                                       BigDecimal receptionistServiceShare) {
        this(() -> dentistTreatmentShare, () -> receptionistServiceShare);
    }

    public DefaultRevenueSplitStrategy(
            java.util.function.Supplier<BigDecimal> dentistTreatmentShare,
            java.util.function.Supplier<BigDecimal> receptionistServiceShare) {
        this.dentistTreatmentShare = dentistTreatmentShare;
        this.receptionistServiceShare = receptionistServiceShare;
    }

    @Override
    public RevenueSplit split(BillBreakdown breakdown) {
        BigDecimal dentistFromTreatment =
                share(breakdown.treatmentCost(), fraction(dentistTreatmentShare, "0.60"));
        BigDecimal receptionistEarning =
                share(breakdown.serviceCharge(), fraction(receptionistServiceShare, "0"));

        BigDecimal dentistEarning = breakdown.consultationFee().add(dentistFromTreatment);
        // Exactly what is left of each charge, so the three always sum to the bill.
        BigDecimal clinicEarning = breakdown.treatmentCost().subtract(dentistFromTreatment)
                .add(breakdown.serviceCharge().subtract(receptionistEarning));

        return new RevenueSplit(dentistEarning, clinicEarning, receptionistEarning);
    }

    private static BigDecimal share(BigDecimal amount, BigDecimal fraction) {
        return amount.multiply(fraction).setScale(BillBreakdown.SCALE, RoundingMode.HALF_UP);
    }

    /** The supplied fraction, or the fallback when the row is missing or corrupt. */
    private static BigDecimal fraction(java.util.function.Supplier<BigDecimal> read,
                                       String fallback) {
        try {
            BigDecimal value = read.get();
            if (value == null
                    || value.compareTo(BigDecimal.ZERO) < 0
                    || value.compareTo(BigDecimal.ONE) > 0) {
                return new BigDecimal(fallback);
            }
            return value;
        } catch (RuntimeException e) {
            return new BigDecimal(fallback);
        }
    }

}
