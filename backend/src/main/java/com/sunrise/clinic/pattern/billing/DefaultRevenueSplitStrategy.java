package com.sunrise.clinic.pattern.billing;

/**
 * Default revenue-attribution rule:
 * <ul>
 *   <li>Dentist earns the full <b>consultation fee</b> plus a configurable share
 *       (default 60%) of the <b>treatment cost</b> (their professional fee).</li>
 *   <li>Clinic keeps the remaining treatment cost (default 40% — materials/facility).</li>
 *   <li>Receptionist earns the <b>service charge</b> (their handling fee).</li>
 * </ul>
 * The dentist's treatment share is read from {@code clinic.revenue.dentist-treatment-share}
 * (see {@code AppConfig}) and passed in by {@code AppContext}, so the policy is
 * configurable without a code change.
 */
public class DefaultRevenueSplitStrategy implements RevenueSplitStrategy {

    private final double dentistTreatmentShare;

    public DefaultRevenueSplitStrategy(double dentistTreatmentShare) {
        this.dentistTreatmentShare = dentistTreatmentShare;
    }

    @Override
    public RevenueSplit split(BillBreakdown b) {
        double dentistFromTreatment = round(b.treatmentCost() * dentistTreatmentShare);
        double dentistEarning = round(b.consultationFee() + dentistFromTreatment);
        double clinicEarning = round(b.treatmentCost() - dentistFromTreatment);
        double receptionistEarning = round(b.serviceCharge());
        return new RevenueSplit(dentistEarning, clinicEarning, receptionistEarning);
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
