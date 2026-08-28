package com.sunrise.clinic.billing.service;

import com.sunrise.clinic.billing.domain.BillBreakdown;
import com.sunrise.clinic.billing.domain.RevenueSplit;

/**
 * Strategy - how a bill's revenue is attributed to the dentist, the clinic and the
 * receptionist.
 *
 * <p>Behind an interface so the commission policy can change without touching billing.
 * The one rule an implementation must not break: the three shares have to sum to the
 * bill's total, or money has been attributed to nobody.</p>
 */
public interface RevenueSplitStrategy {

    RevenueSplit split(BillBreakdown breakdown);
}
