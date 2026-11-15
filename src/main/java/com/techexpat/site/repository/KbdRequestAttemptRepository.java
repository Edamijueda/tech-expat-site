package com.techexpat.site.repository;

import com.techexpat.site.model.KbdRequestAttempt;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface KbdRequestAttemptRepository extends JpaRepository<KbdRequestAttempt, String> {

    long countByIpAddressAndAttemptedAtAfter(String ipAddress, Instant cutoff);

    void deleteByAttemptedAtBefore(Instant cutoff);
}
