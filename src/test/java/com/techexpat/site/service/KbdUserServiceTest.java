package com.techexpat.site.service;

import com.techexpat.site.model.KbdUser;
import com.techexpat.site.model.KbdUserStatus;
import com.techexpat.site.repository.KbdUserLockoutEventRepository;
import com.techexpat.site.repository.KbdUserRepository;

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
class KbdUserServiceTest {

    private final KbdUserRepository users = mock(KbdUserRepository.class);
    private final KbdUserLockoutEventRepository lockoutEvents =
            mock(KbdUserLockoutEventRepository.class);
    private final KbdSubscriptionService subscriptions = mock(KbdSubscriptionService.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final KbdUserService service =
            new KbdUserService(users, lockoutEvents, subscriptions, encoder);

    @Test
    void approvingStartsTheFirstTrial() {
        KbdUser user = pendingUser();
        when(users.findById("u-1")).thenReturn(Optional.of(user));

        assertThat(service.approve("u-1")).isTrue();

        assertThat(user.getStatus()).isEqualTo(KbdUserStatus.APPROVED);
        assertThat(user.getTrialStartedAt()).isEqualTo(user.getApprovedAt());
        assertThat(user.getTrialDays()).isEqualTo(KbdUser.FIRST_TRIAL_DAYS);
        assertThat(user.isTrialExtended()).isFalse();
    }

    @Test
    void anAccountAwaitingApprovalHasNoTrialRunning() {
        assertThat(pendingUser().getTrialStartedAt()).isNull();
    }

    @Test
    void extendingAnExpiredTrialAddsThirtyDays() {
        KbdUser user = approvedUser();
        when(users.findById("u-1")).thenReturn(Optional.of(user));
        givenTrialActive(false);

        assertThat(service.extendTrial("u-1")).isTrue();

        assertThat(user.getTrialDays()).isEqualTo(60);
        assertThat(user.isTrialExtended()).isTrue();
    }

    @Test
    void aTrialCannotBeExtendedTwice() {
        KbdUser user = approvedUser();
        user.setTrialDays(60);
        user.setTrialExtendedAt(Instant.now());
        when(users.findById("u-1")).thenReturn(Optional.of(user));
        givenTrialActive(false);

        assertThat(service.extendTrial("u-1")).isFalse();

        assertThat(user.getTrialDays()).isEqualTo(60);
    }

    @Test
    void aLiveTrialIsNotExtendedEarly() {
        KbdUser user = approvedUser();
        when(users.findById("u-1")).thenReturn(Optional.of(user));
        givenTrialActive(true);

        assertThat(service.extendTrial("u-1")).isFalse();

        assertThat(user.getTrialDays()).isEqualTo(KbdUser.FIRST_TRIAL_DAYS);
        assertThat(user.isTrialExtended()).isFalse();
    }

    @Test
    void anAccountWithNoTrialYetCannotBeExtended() {
        when(users.findById("u-1")).thenReturn(Optional.of(pendingUser()));

        assertThat(service.extendTrial("u-1")).isFalse();
    }

    private void givenTrialActive(boolean active) {
        when(subscriptions.statusFor(any()))
                .thenReturn(new KbdAccessStatus(true, 10, 30, active ? 21 : 0, null));
    }

    private static KbdUser pendingUser() {
        return new KbdUser("u-1", "a@b.com", "hash");
    }

    private static KbdUser approvedUser() {
        KbdUser user = pendingUser();
        user.setStatus(KbdUserStatus.APPROVED);
        user.setTrialStartedAt(Instant.now().minus(45, ChronoUnit.DAYS));
        return user;
    }
}
