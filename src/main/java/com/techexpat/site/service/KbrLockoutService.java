package com.techexpat.site.service;

import com.techexpat.site.model.KbrUser;
import com.techexpat.site.model.KbrUserLockoutEvent;
import com.techexpat.site.repository.KbrUserLockoutEventRepository;
import com.techexpat.site.repository.KbrUserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class KbrLockoutService {

    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final KbrUserRepository users;
    private final KbrUserLockoutEventRepository events;

    public KbrLockoutService(KbrUserRepository users, KbrUserLockoutEventRepository events) {
        this.users = users;
        this.events = events;
    }

    @Transactional
    public void recordFailedAttempt(String email, String ipAddress, String userAgent) {
        Optional<KbrUser> maybe = users.findByEmail(normalize(email));
        if (maybe.isEmpty()) {
            return;
        }

        KbrUser user = maybe.get();
        int attempts = user.getFailedLoginAttempts() + 1;

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(Instant.now().plus(LOCK_DURATION));
            user.setFailedLoginAttempts(0);
            events.save(new KbrUserLockoutEvent(
                    UUID.randomUUID().toString(),
                    user.getId(),
                    attempts,
                    truncate(ipAddress, 45),
                    truncate(userAgent, 500)));
        } else {
            user.setFailedLoginAttempts(attempts);
        }
    }

    @Transactional
    public void recordSuccessfulLogin(String email) {
        users.findByEmail(normalize(email)).ifPresent(user -> {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
        });
    }

    private static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
