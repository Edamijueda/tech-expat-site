package com.techexpat.site.service;

import com.techexpat.site.model.KbrUser;
import com.techexpat.site.model.KbrUserStatus;
import com.techexpat.site.repository.KbrUserLockoutEventRepository;
import com.techexpat.site.repository.KbrUserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Trial handling. The lockout and PIN sides are covered through the controller tests. */
class KbrUserServiceTest {

    private final KbrUserRepository users = mock(KbrUserRepository.class);
    private final KbrUserLockoutEventRepository lockoutEvents =
            mock(KbrUserLockoutEventRepository.class);
    private final KbrSubscriptionService subscriptions = mock(KbrSubscriptionService.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final KbrUserService service =
            new KbrUserService(users, lockoutEvents, subscriptions, encoder);

    @Test
    void approvingStartsTheFirstTrial() {
        KbrUser user = pendingUser();
        when(users.findById("u-1")).thenReturn(Optional.of(user));

        assertThat(service.approve("u-1")).isTrue();

        assertThat(user.getStatus()).isEqualTo(KbrUserStatus.APPROVED);
        assertThat(user.getTrialStartedAt()).isEqualTo(user.getApprovedAt());
        assertThat(user.getTrialDays()).isEqualTo(KbrUser.FIRST_TRIAL_DAYS);
        assertThat(user.isTrialExtended()).isFalse();
    }

    @Test
    void anAccountAwaitingApprovalHasNoTrialRunning() {
        assertThat(pendingUser().getTrialStartedAt()).isNull();
    }

    @Test
    void extendingAnExpiredTrialAddsThirtyDays() {
        KbrUser user = approvedUser();
        when(users.findById("u-1")).thenReturn(Optional.of(user));
        givenTrialActive(false);

        assertThat(service.extendTrial("u-1")).isTrue();

        assertThat(user.getTrialDays()).isEqualTo(60);
        assertThat(user.isTrialExtended()).isTrue();
    }

    @Test
    void aTrialCannotBeExtendedTwice() {
        KbrUser user = approvedUser();
        user.setTrialDays(60);
        user.setTrialExtendedAt(Instant.now());
        when(users.findById("u-1")).thenReturn(Optional.of(user));
        givenTrialActive(false);

        assertThat(service.extendTrial("u-1")).isFalse();

        assertThat(user.getTrialDays()).isEqualTo(60);
    }

    @Test
    void aLiveTrialIsNotExtendedEarly() {
        KbrUser user = approvedUser();
        when(users.findById("u-1")).thenReturn(Optional.of(user));
        givenTrialActive(true);

        assertThat(service.extendTrial("u-1")).isFalse();

        assertThat(user.getTrialDays()).isEqualTo(KbrUser.FIRST_TRIAL_DAYS);
        assertThat(user.isTrialExtended()).isFalse();
    }

    @Test
    void anAccountWithNoTrialYetCannotBeExtended() {
        when(users.findById("u-1")).thenReturn(Optional.of(pendingUser()));

        assertThat(service.extendTrial("u-1")).isFalse();
    }

    private void givenTrialActive(boolean active) {
        when(subscriptions.statusFor(any()))
                .thenReturn(new KbrAccessStatus(true, 10, 30, active ? 21 : 0, null));
    }

    private static KbrUser pendingUser() {
        return new KbrUser("u-1", "a@b.com", "hash");
    }

    private static KbrUser approvedUser() {
        KbrUser user = pendingUser();
        user.setStatus(KbrUserStatus.APPROVED);
        user.setTrialStartedAt(Instant.now().minus(45, ChronoUnit.DAYS));
        return user;
    }
}
