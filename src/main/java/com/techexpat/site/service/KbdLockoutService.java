package com.techexpat.site.service;

import com.techexpat.site.model.KbdUser;
import com.techexpat.site.model.KbdUserLockoutEvent;
import com.techexpat.site.repository.KbdUserLockoutEventRepository;
import com.techexpat.site.repository.KbdUserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class KbdLockoutService {

    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final KbdUserRepository users;
    private final KbdUserLockoutEventRepository events;

    public KbdLockoutService(KbdUserRepository users, KbdUserLockoutEventRepository events) {
        this.users = users;
        this.events = events;
    }

    @Transactional
    public void recordFailedAttempt(String email, String ipAddress, String userAgent) {
        Optional<KbdUser> maybe = users.findByEmail(normalize(email));
        if (maybe.isEmpty()) {
            return;
        }

        KbdUser user = maybe.get();
        int attempts = user.getFailedLoginAttempts() + 1;

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(Instant.now().plus(LOCK_DURATION));
            user.setFailedLoginAttempts(0);
            events.save(new KbdUserLockoutEvent(
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
