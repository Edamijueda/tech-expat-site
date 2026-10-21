package com.techexpat.site.service;

import com.techexpat.site.model.KbrSubscriptionPayment;
import com.techexpat.site.model.KbrUser;
import com.techexpat.site.repository.KbrSubscriptionPaymentRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Trial and subscription state. Payments are collected by hand, so this only records
 * which calendar months were paid for and answers whether a user may write today.
 */
@Service
public class KbrSubscriptionService {

    /** A sanity bound on a single record, so a mistyped year can't grant a century. */
    private static final int MAX_MONTHS = 24;

    private final KbrSubscriptionPaymentRepository payments;
    private final DisplayTime displayTime;

    public KbrSubscriptionService(KbrSubscriptionPaymentRepository payments,
                                  DisplayTime displayTime) {
        this.payments = payments;
        this.displayTime = displayTime;
    }

    public List<KbrSubscriptionPayment> findFor(String userId) {
        return payments.findByUserIdOrderByPeriodStartDesc(userId);
    }

    public KbrAccessStatus statusFor(KbrUser user) {
        LocalDate today = displayTime.today();
        LocalDate trialStart = displayTime.toLocalDate(user.getTrialStartedAt());

        int used = 0;
        int remaining = 0;
        if (trialStart != null) {
            used = Math.max(1, (int) ChronoUnit.DAYS.between(trialStart, today) + 1);
            remaining = Math.max(0, user.getTrialDays() - used + 1);
        }

        return new KbrAccessStatus(trialStart != null, used, user.getTrialDays(), remaining,
                paidThrough(findFor(user.getId()), today));
    }

    /**
     * Records months paid for, inclusive of both ends — January to March 2027 is stored
     * as 2027-01-01 / 2027-04-01. Future periods are fine; overlapping ones are not.
     *
     * @return the reason it was refused, or empty once saved
     */
    @Transactional
    public Optional<String> record(String userId, YearMonth start, YearMonth end, String note) {
        if (start == null || end == null) {
            return Optional.of("Choose both a start month and an end month.");
        }
        if (end.isBefore(start)) {
            return Optional.of("The end month cannot be before the start month.");
        }

        long months = ChronoUnit.MONTHS.between(start, end) + 1;
        if (months > MAX_MONTHS) {
            return Optional.of("A single payment can cover at most " + MAX_MONTHS + " months.");
        }

        LocalDate periodStart = start.atDay(1);
        LocalDate periodEnd = end.plusMonths(1).atDay(1);

        List<KbrSubscriptionPayment> clashes = payments
                .findByUserIdAndPeriodStartLessThanAndPeriodEndGreaterThan(
                        userId, periodEnd, periodStart);
        if (!clashes.isEmpty()) {
            KbrSubscriptionPayment clash = clashes.getFirst();
            return Optional.of("Those months overlap a period already recorded: "
                    + displayTime.dayOf(clash.getPeriodStart())
                    + " to " + displayTime.dayOf(clash.getLastPaidDay()) + ".");
        }

        String trimmed = note == null || note.isBlank() ? null : note.trim();
        payments.save(new KbrSubscriptionPayment(
                UUID.randomUUID().toString(), userId, periodStart, periodEnd, trimmed));
        return Optional.empty();
    }

    @Transactional
    public void delete(String paymentId) {
        payments.deleteById(paymentId);
    }

    /**
     * The last day the user is paid up to, following contiguous periods so a quarter
     * recorded in advance extends the date the user already sees. Null when nothing
     * covers today.
     */
    private static LocalDate paidThrough(List<KbrSubscriptionPayment> userPayments,
                                         LocalDate today) {
        LocalDate end = userPayments.stream()
                .filter(payment -> payment.covers(today))
                .map(KbrSubscriptionPayment::getPeriodEnd)
                .findFirst()
                .orElse(null);
        if (end == null) {
            return null;
        }

        // Overlaps are refused, so at most one period can begin where another ends.
        Optional<KbrSubscriptionPayment> next;
        while ((next = startingOn(userPayments, end)).isPresent()) {
            end = next.get().getPeriodEnd();
        }
        return end.minusDays(1);
    }

    private static Optional<KbrSubscriptionPayment> startingOn(
            List<KbrSubscriptionPayment> userPayments, LocalDate day) {
        return userPayments.stream()
                .filter(payment -> payment.getPeriodStart().equals(day))
                .findFirst();
    }
}
