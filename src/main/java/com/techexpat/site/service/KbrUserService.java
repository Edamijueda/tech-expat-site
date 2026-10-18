package com.techexpat.site.service;

import com.techexpat.site.model.KbrUser;
import com.techexpat.site.model.KbrUserStatus;
import com.techexpat.site.repository.KbrUserLockoutEventRepository;
import com.techexpat.site.repository.KbrUserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class KbrUserService {

    private final KbrUserRepository users;
    private final KbrUserLockoutEventRepository lockoutEvents;
    private final PasswordEncoder encoder;

    public KbrUserService(KbrUserRepository users,
                          KbrUserLockoutEventRepository lockoutEvents,
                          PasswordEncoder encoder) {
        this.users = users;
        this.lockoutEvents = lockoutEvents;
        this.encoder = encoder;
    }

    @Transactional
    public boolean createPendingUser(String email, String pin) {
        String normalized = email.trim().toLowerCase();
        if (users.existsByEmail(normalized)) {
            return false;
        }
        KbrUser user = new KbrUser(
                UUID.randomUUID().toString(),
                normalized,
                encoder.encode(pin));
        users.save(user);
        return true;
    }

    public List<KbrUser> findAllNewestFirst() {
        return users.findAllByOrderByCreatedAtDesc();
    }

    public Optional<KbrUser> findById(String id) {
        return users.findById(id);
    }

    @Transactional
    public boolean approve(String id) {
        return users.findById(id).map(user -> {
            if (user.getStatus() == KbrUserStatus.PENDING) {
                user.setStatus(KbrUserStatus.APPROVED);
                user.setApprovedAt(Instant.now());
                return true;
            }
            return false;
        }).orElse(false);
    }

    @Transactional
    public boolean suspend(String id) {
        return users.findById(id).map(user -> {
            if (user.getStatus() == KbrUserStatus.APPROVED) {
                user.setStatus(KbrUserStatus.SUSPENDED);
                return true;
            }
            return false;
        }).orElse(false);
    }

    @Transactional
    public boolean unsuspend(String id) {
        return users.findById(id).map(user -> {
            if (user.getStatus() == KbrUserStatus.SUSPENDED) {
                user.setStatus(KbrUserStatus.APPROVED);
                return true;
            }
            return false;
        }).orElse(false);
    }

    @Transactional
    public boolean unlock(String id) {
        return users.findById(id).map(user -> {
            user.setLockedUntil(null);
            user.setFailedLoginAttempts(0);
            return true;
        }).orElse(false);
    }

    @Transactional
    public void clearLockoutHistory(String userId) {
        lockoutEvents.deleteByUserId(userId);
    }

    @Transactional
    public void deleteLockoutEvent(String eventId) {
        lockoutEvents.deleteById(eventId);
    }
}
