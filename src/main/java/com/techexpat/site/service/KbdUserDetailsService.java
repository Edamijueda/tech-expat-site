package com.techexpat.site.service;

import com.techexpat.site.model.KbdUser;
import com.techexpat.site.model.KbdUserStatus;
import com.techexpat.site.repository.KbdUserRepository;

import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class KbdUserDetailsService implements UserDetailsService {

    private final KbdUserRepository users;

    public KbdUserDetailsService(KbdUserRepository users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalized = email == null ? "" : email.trim().toLowerCase();
        KbdUser user = users.findByEmail(normalized)
                .orElseThrow(() -> new UsernameNotFoundException("No account for " + normalized));

        boolean enabled = user.getStatus() == KbdUserStatus.APPROVED;
        boolean nonLocked = user.getLockedUntil() == null
                || user.getLockedUntil().isBefore(Instant.now());

        return User.withUsername(user.getEmail())
                .password(user.getPinHash())
                .authorities(AuthorityUtils.createAuthorityList("ROLE_KBD_USER"))
                .accountExpired(false)
                .accountLocked(!nonLocked)
                .credentialsExpired(false)
                .disabled(!enabled)
                .build();
    }
}
