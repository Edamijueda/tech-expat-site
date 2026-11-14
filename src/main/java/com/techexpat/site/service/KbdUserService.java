package com.techexpat.site.service;

import com.techexpat.site.model.KbdUser;
import com.techexpat.site.model.KbdUserStatus;
import com.techexpat.site.repository.KbdUserLockoutEventRepository;
import com.techexpat.site.repository.KbdUserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class KbdUserService {

    private final KbdUserRepository users;
    private final KbdUserLockoutEventRepository lockoutEvents;
    private final KbdSubscriptionService subscriptions;
    private final PasswordEncoder encoder;

    public KbdUserService(KbdUserRepository users,
                          KbdUserLockoutEventRepository lockoutEvents,
                          KbdSubscriptionService subscriptions,
                          PasswordEncoder encoder) {
        this.users = users;
        this.lockoutEvents = lockoutEvents;
        this.subscriptions = subscriptions;
        this.encoder = encoder;
    }

    @Transactional
    public boolean createPendingUser(String email, String pin) {
        String normalized = normalize(email);
        if (users.existsByEmail(normalized)) {
            return false;
        }
        KbdUser user = new KbdUser(
                UUID.randomUUID().toString(),
                normalized,
                encoder.encode(pin));
        users.save(user);
        return true;
    }

    public List<KbdUser> findAllNewestFirst() {
        return users.findAllByOrderByCreatedAtDesc();
    }

    public Optional<KbdUser> findByEmail(String email) {
        return users.findByEmail(normalize(email));
    }

    public boolean hasBusinessName(String email) {
        return findByEmail(email)
                .map(user -> user.getBusinessName() != null && !user.getBusinessName().isBlank())
                .orElse(false);
    }

    @Transactional
    public boolean saveBusinessName(String email, String businessName) {
        return users.findByEmail(normalize(email)).map(user -> {
            user.setBusinessName(businessName.trim());
            return true;
        }).orElse(false);
    }

    public Optional<KbdUser> findById(String id) {
        return users.findById(id);
    }

    @Transactional
    public boolean approve(String id) {
        return users.findById(id).map(user -> {
            if (user.getStatus() == KbdUserStatus.PENDING) {
                Instant now = Instant.now();
                user.setStatus(KbdUserStatus.APPROVED);
                user.setApprovedAt(now);
                // Approval is what starts the free trial; nothing runs before it.
                user.setTrialStartedAt(now);
                user.setTrialDays(KbdUser.FIRST_TRIAL_DAYS);
                return true;
            }
            return false;
        }).orElse(false);
    }

    /**
     * Grants the single trial extension. Offered only once the first trial has run out,
     * so the decision is made after seeing how the account was used.
     */
    @Transactional
    public boolean extendTrial(String id) {
        return users.findById(id).map(user -> {
            if (user.getTrialStartedAt() == null || user.isTrialExtended()
                    || subscriptions.statusFor(user).trialActive()) {
                return false;
            }
            user.setTrialDays(user.getTrialDays() + KbdUser.TRIAL_EXTENSION_DAYS);
            user.setTrialExtendedAt(Instant.now());
            return true;
        }).orElse(false);
    }

    @Transactional
    public boolean suspend(String id) {
        return users.findById(id).map(user -> {
            if (user.getStatus() == KbdUserStatus.APPROVED) {
                user.setStatus(KbdUserStatus.SUSPENDED);
                return true;
            }
            return false;
        }).orElse(false);
    }

    @Transactional
    public boolean unsuspend(String id) {
        return users.findById(id).map(user -> {
            if (user.getStatus() == KbdUserStatus.SUSPENDED) {
                user.setStatus(KbdUserStatus.APPROVED);
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

    private static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
