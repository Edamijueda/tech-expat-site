package com.techexpat.site.service;

import com.techexpat.site.model.KbdRequestAttempt;
import com.techexpat.site.repository.KbdRequestAttemptRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RegistrationRateLimiterTest {

    private static final String IP = "1.2.3.4";

    private KbdRequestAttemptRepository attempts;
    private RegistrationRateLimiter limiter;

    @BeforeEach
    void setUp() {
        attempts = mock(KbdRequestAttemptRepository.class);
        limiter = new RegistrationRateLimiter(attempts);
    }

    @Test
    void allowsThreeAttemptsFromOneAddressThenRefuses() {
        when(attempts.countByIpAddressAndAttemptedAtAfter(eq(IP), any()))
                .thenReturn(0L, 1L, 2L, 3L);

        assertThat(limiter.tryAcquire(IP)).isTrue();
        assertThat(limiter.tryAcquire(IP)).isTrue();
        assertThat(limiter.tryAcquire(IP)).isTrue();
        assertThat(limiter.tryAcquire(IP)).isFalse();

        verify(attempts, times(3)).save(any());
    }

    @Test
    void oneAddressesAttemptsDoNotCountAgainstAnother() {
        when(attempts.countByIpAddressAndAttemptedAtAfter(eq(IP), any())).thenReturn(3L);
        when(attempts.countByIpAddressAndAttemptedAtAfter(eq("5.6.7.8"), any())).thenReturn(0L);

        assertThat(limiter.tryAcquire(IP)).isFalse();
        assertThat(limiter.tryAcquire("5.6.7.8")).isTrue();
    }

    /** Anything older than the window is invisible to the count, so it cannot block. */
    @Test
    void countsOnlyWithinTheWindow() {
        limiter.tryAcquire(IP);

        ArgumentCaptor<Instant> cutoff = ArgumentCaptor.forClass(Instant.class);
        verify(attempts).countByIpAddressAndAttemptedAtAfter(eq(IP), cutoff.capture());

        assertThat(cutoff.getValue())
                .isCloseTo(Instant.now().minus(Duration.ofDays(1)), within(5, ChronoUnit.SECONDS));
    }

    /** Clearing on the way past keeps the table bounded without a scheduled job. */
    @Test
    void clearsRowsOlderThanTheWindow() {
        limiter.tryAcquire(IP);

        verify(attempts).deleteByAttemptedAtBefore(any());
    }

    @Test
    void theSavedRowCarriesTheAddress() {
        limiter.tryAcquire(IP);

        ArgumentCaptor<KbdRequestAttempt> saved = ArgumentCaptor.forClass(KbdRequestAttempt.class);
        verify(attempts).save(saved.capture());

        assertThat(saved.getValue().getIpAddress()).isEqualTo(IP);
        assertThat(saved.getValue().getId()).isNotBlank();
    }

    /** An address we cannot read is let through rather than blocking a real person. */
    @Test
    void aMissingAddressIsAllowedAndRecordsNothing() {
        assertThat(limiter.tryAcquire(null)).isTrue();
        assertThat(limiter.tryAcquire("   ")).isTrue();

        verifyNoInteractions(attempts);
    }
}
