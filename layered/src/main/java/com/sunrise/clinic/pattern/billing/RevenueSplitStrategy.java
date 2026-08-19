package com.sunrise.clinic.pattern.billing;

/**
 * STRATEGY pattern — the rule for attributing a bill's revenue to the
 * dentist, the clinic, and the receptionist. Keeping this behind an interface
 * lets the clinic change its commission policy without touching billing code.
 */
public interface RevenueSplitStrategy {

    /**
     * Split a computed bill three ways.
     *
     * @param breakdown the computed bill line items
     * @return the {@link RevenueSplit}
     */
    RevenueSplit split(BillBreakdown breakdown);
}
