package com.techexpat.site.service;

import com.techexpat.site.config.DisplayProperties;
import com.techexpat.site.model.KbrSubscriptionPayment;
import com.techexpat.site.model.KbrUser;
import com.techexpat.site.repository.KbrSubscriptionPaymentRepository;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KbrSubscriptionServiceTest {

    private final KbrSubscriptionPaymentRepository payments =
            mock(KbrSubscriptionPaymentRepository.class);
    private final DisplayTime displayTime = new DisplayTime(new DisplayProperties("Africa/Lagos"));
    private final KbrSubscriptionService service = new KbrSubscriptionService(payments, displayTime);

    @Test
    void trialCounterStartsAtOneOnTheDayOfApproval() {
        KbrAccessStatus status = service.statusFor(userApprovedDaysAgo(0, 30));

        assertThat(status.trialDaysUsed()).isEqualTo(1);
        assertThat(status.trialDaysRemaining()).isEqualTo(30);
        assertThat(status.trialActive()).isTrue();
        assertThat(status.canWrite()).isTrue();
    }

    @Test
    void trialIsStillLiveOnItsLastDay() {
        KbrAccessStatus status = service.statusFor(userApprovedDaysAgo(29, 30));

        assertThat(status.trialDaysUsed()).isEqualTo(30);
        assertThat(status.trialDaysRemaining()).isEqualTo(1);
        assertThat(status.trialActive()).isTrue();
    }

    @Test
    void trialExpiresTheDayAfterTheCap() {
        KbrAccessStatus status = service.statusFor(userApprovedDaysAgo(30, 30));

        assertThat(status.trialDaysRemaining()).isZero();
        assertThat(status.trialActive()).isFalse();
        assertThat(status.canWrite()).isFalse();
        // the counter stops at the cap rather than running on to 31
        assertThat(status.trialDaysShown()).isEqualTo(30);
    }

    @Test
    void anExtendedTrialKeepsCountingTowardsSixty() {
        KbrAccessStatus status = service.statusFor(userApprovedDaysAgo(34, 60));

        assertThat(status.trialDaysShown()).isEqualTo(35);
        assertThat(status.trialDays()).isEqualTo(60);
        assertThat(status.trialActive()).isTrue();
    }

    @Test
    void warnsOnlyInsideTheLastSevenDays() {
        assertThat(service.statusFor(userApprovedDaysAgo(22, 30)).trialEndingSoon()).isFalse();
        assertThat(service.statusFor(userApprovedDaysAgo(23, 30)).trialEndingSoon()).isTrue();
    }

    /** Approved before the trial shipped: approval stands in, rather than no trial at all. */
    @Test
    void anApprovedAccountWithNoTrialStartFallsBackToItsApprovalDate() {
        KbrUser user = pendingUser();
        user.setApprovedAt(Instant.now().minus(3, ChronoUnit.DAYS));

        KbrAccessStatus status = service.statusFor(user);

        assertThat(status.trialStarted()).isTrue();
        assertThat(status.trialDaysUsed()).isEqualTo(4);
        assertThat(status.canWrite()).isTrue();
    }

    @Test
    void anExplicitTrialStartWinsOverTheApprovalDate() {
        KbrUser user = userApprovedDaysAgo(9, 30);
        user.setApprovedAt(Instant.now().minus(40, ChronoUnit.DAYS));

        assertThat(service.statusFor(user).trialDaysUsed()).isEqualTo(10);
    }

    @Test
    void anAccountWithNeitherDateHasNoTrial() {
        KbrAccessStatus status = service.statusFor(pendingUser());

        assertThat(status.trialStarted()).isFalse();
        assertThat(status.canWrite()).isFalse();
    }

    @Test
    void anExpiredTrialIsCarriedByAPaidPeriod() {
        LocalDate today = displayTime.today();
        givenPayments(payment(today.minusMonths(1), today.plusMonths(2)));

        KbrAccessStatus status = service.statusFor(userApprovedDaysAgo(90, 30));

        assertThat(status.trialActive()).isFalse();
        assertThat(status.paid()).isTrue();
        assertThat(status.canWrite()).isTrue();
    }

    @Test
    void paidThroughFollowsAQuarterRecordedInAdvance() {
        givenPayments(
                payment(LocalDate.of(2027, 4, 1), LocalDate.of(2027, 7, 1)),
                payment(LocalDate.of(2027, 1, 1), LocalDate.of(2027, 4, 1)));

        KbrSubscriptionService february = serviceOn(LocalDate.of(2027, 2, 15));

        assertThat(february.statusFor(userApprovedDaysAgo(90, 30)).paidThrough())
                .isEqualTo(LocalDate.of(2027, 6, 30));
    }

    @Test
    void aLapsedPeriodNoLongerCoversTheUser() {
        givenPayments(payment(LocalDate.of(2027, 1, 1), LocalDate.of(2027, 4, 1)));

        KbrSubscriptionService april = serviceOn(LocalDate.of(2027, 4, 1));

        assertThat(april.statusFor(userApprovedDaysAgo(90, 30)).paid()).isFalse();
    }

    @Test
    void lastPaidDayIsTheDayBeforeTheExclusiveEnd() {
        KbrSubscriptionPayment march = payment(LocalDate.of(2027, 1, 1), LocalDate.of(2027, 4, 1));

        assertThat(march.getLastPaidDay()).isEqualTo(LocalDate.of(2027, 3, 31));
        assertThat(march.covers(LocalDate.of(2027, 3, 31))).isTrue();
        assertThat(march.covers(LocalDate.of(2027, 4, 1))).isFalse();
    }

    @Test
    void recordsWholeCalendarMonthsInclusiveOfBothEnds() {
        when(payments.findByUserIdAndPeriodStartLessThanAndPeriodEndGreaterThan(
                anyString(), any(), any())).thenReturn(List.of());

        Optional<String> refusal = service.record(
                "u-1", YearMonth.of(2027, 1), YearMonth.of(2027, 3), "  bank transfer  ");

        assertThat(refusal).isEmpty();
        verify(payments).save(org.mockito.ArgumentMatchers.argThat(saved ->
                saved.getPeriodStart().equals(LocalDate.of(2027, 1, 1))
                        && saved.getPeriodEnd().equals(LocalDate.of(2027, 4, 1))
                        && saved.getMonthsCount() == 3
                        && "bank transfer".equals(saved.getNote())));
    }

    @Test
    void refusesAnOverlappingPeriodAndSavesNothing() {
        when(payments.findByUserIdAndPeriodStartLessThanAndPeriodEndGreaterThan(
                eq("u-1"), any(), any()))
                .thenReturn(List.of(payment(LocalDate.of(2027, 1, 1), LocalDate.of(2027, 4, 1))));

        Optional<String> refusal = service.record(
                "u-1", YearMonth.of(2027, 3), YearMonth.of(2027, 5), null);

        assertThat(refusal).isPresent();
        assertThat(refusal.get()).contains("1 Jan 2027").contains("31 Mar 2027");
        verify(payments, never()).save(any());
    }

    @Test
    void refusesMonthsInTheWrongOrderOrMissingAltogether() {
        assertThat(service.record("u-1", YearMonth.of(2027, 5), YearMonth.of(2027, 3), null))
                .isPresent();
        assertThat(service.record("u-1", null, YearMonth.of(2027, 3), null)).isPresent();
        verify(payments, never()).save(any());
    }

    @Test
    void refusesAPeriodLongerThanTwoYears() {
        Optional<String> refusal = service.record(
                "u-1", YearMonth.of(2027, 1), YearMonth.of(2127, 1), null);

        assertThat(refusal).isPresent();
        verify(payments, never()).save(any());
    }

    private void givenPayments(KbrSubscriptionPayment... rows) {
        when(payments.findByUserIdOrderByPeriodStartDesc(anyString())).thenReturn(List.of(rows));
    }

    private KbrSubscriptionService serviceOn(LocalDate today) {
        return new KbrSubscriptionService(payments, new DisplayTime(new DisplayProperties("UTC")) {
            @Override
            public LocalDate today() {
                return today;
            }
        });
    }

    private static KbrSubscriptionPayment payment(LocalDate start, LocalDate end) {
        return new KbrSubscriptionPayment("p-" + start, "u-1", start, end, null);
    }

    private static KbrUser pendingUser() {
        return new KbrUser("u-1", "a@b.com", "hash");
    }

    private static KbrUser userApprovedDaysAgo(int days, int trialDays) {
        KbrUser user = new KbrUser("u-1", "a@b.com", "hash");
        user.setTrialStartedAt(Instant.now().minus(days, ChronoUnit.DAYS));
        user.setTrialDays(trialDays);
        return user;
    }
}
