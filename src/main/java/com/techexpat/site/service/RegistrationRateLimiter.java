package com.techexpat.site.service;

import com.techexpat.site.model.KbdRequestAttempt;
import com.techexpat.site.repository.KbdRequestAttemptRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Caps request-access submissions per IP address. Attempts live in Postgres rather than
 * in memory: a Railway deploy used to empty the map and hand everyone a fresh allowance,
 * and a second instance would have counted against its own copy.
 */
@Component
public class RegistrationRateLimiter {

    private static final int MAX_PER_WINDOW = 3;
    private static final Duration WINDOW = Duration.ofDays(1);

    private final KbdRequestAttemptRepository attempts;

    public RegistrationRateLimiter(KbdRequestAttemptRepository attempts) {
        this.attempts = attempts;
    }

    @Transactional
    public boolean tryAcquire(String ip) {
        if (ip == null || ip.isBlank()) {
            return true;
        }
        Instant cutoff = Instant.now().minus(WINDOW);

        // Nothing older than the window can affect any decision, so clearing it here keeps
        // the table bounded without a scheduled job.
        attempts.deleteByAttemptedAtBefore(cutoff);

        if (attempts.countByIpAddressAndAttemptedAtAfter(ip, cutoff) >= MAX_PER_WINDOW) {
            return false;
        }
        attempts.save(new KbdRequestAttempt(UUID.randomUUID().toString(), ip));
        return true;
    }
}
