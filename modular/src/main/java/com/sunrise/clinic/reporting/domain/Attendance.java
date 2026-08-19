package com.sunrise.clinic.reporting.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Who turned up, and who did not - FR-ADM-15 and FR-ADM-19.
 *
 * <h2>What counts as a no-show, and why it needs no new column</h2>
 *
 * <p>There is no {@code NO_SHOW} status, and adding one would mean somebody at the desk
 * remembering to set it - which is exactly the record that never gets kept. A no-show is
 * derivable instead: an appointment whose <b>date has passed</b> and whose status is still
 * {@code CONFIRMED}. Nobody cancelled it and no dentist recorded a treatment, so the patient
 * did not come.</p>
 *
 * <p>The derivation is honest about one thing: it also catches a visit the dentist simply
 * forgot to record. That is a fair reading - an appointment with no clinical record is not
 * evidence of a visit - and it gives the practice a reason to keep the records straight.</p>
 *
 * @param attended appointments completed or billed
 * @param noShows  past appointments still sitting at CONFIRMED
 * @param upcoming appointments still to come, excluded from the rate
 */
public record Attendance(int attended, int noShows, int upcoming) {

    public static final Attendance NONE = new Attendance(0, 0, 0);

    /** @return appointments that were expected and have now been settled either way. */
    public int concluded() {
        return attended + noShows;
    }

    /**
     * @return no-shows as a percentage of concluded appointments, to one decimal place
     *
     * <p>Of <em>concluded</em>, not of all: counting appointments that have not happened yet
     * as attended would make the rate drift down every time somebody books, which is the
     * opposite of what the figure is for.</p>
     */
    public BigDecimal noShowRate() {
        if (concluded() == 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(noShows)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(concluded()), 1, RoundingMode.HALF_UP);
    }
}
