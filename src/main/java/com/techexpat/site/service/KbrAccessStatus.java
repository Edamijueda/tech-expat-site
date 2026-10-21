package com.techexpat.site.service;

import java.time.LocalDate;

/**
 * What a signed-in user may do today: the trial counter the dashboard shows, and whether
 * a paid period is covering them.
 *
 * <p>Account status is not part of this. A PENDING or SUSPENDED account is disabled at
 * sign-in, so anything asking this question is already an approved user.
 *
 * @param trialDaysRemaining days left including today; 1 on the last day, 0 once expired
 * @param paidThrough        last paid day, or null when no recorded period covers today
 */
public record KbrAccessStatus(boolean trialStarted, int trialDaysUsed, int trialDays,
                              int trialDaysRemaining, LocalDate paidThrough) {

    /** How close to the end of a trial the dashboard starts warning. */
    private static final int WARN_WITHIN_DAYS = 7;

    public boolean trialActive() {
        return trialStarted && trialDaysRemaining > 0;
    }

    public boolean paid() {
        return paidThrough != null;
    }

    /** Uploading and deleting need either a live trial or a paid period. */
    public boolean canWrite() {
        return trialActive() || paid();
    }

    public boolean trialEndingSoon() {
        return trialActive() && trialDaysRemaining <= WARN_WITHIN_DAYS;
    }

    /** Numerator of the "35/60" counter — never runs past the cap once expired. */
    public int trialDaysShown() {
        return Math.min(trialDaysUsed, trialDays);
    }
}
