package com.techexpat.site.service;

import com.techexpat.site.model.KbrUser;
import com.techexpat.site.repository.KbrUserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class KbrUserService {

    private final KbrUserRepository users;
    private final PasswordEncoder encoder;

    public KbrUserService(KbrUserRepository users, PasswordEncoder encoder) {
        this.users = users;
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
}
