package com.techexpat.site.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

/** One request-access submission, kept so the per-IP daily limit outlives a restart. */
@Entity
@Table(name = "te_kbd_request_attempts")
public class KbdRequestAttempt {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt;

    protected KbdRequestAttempt() {
    }

    public KbdRequestAttempt(String id, String ipAddress) {
        this.id = id;
        this.ipAddress = ipAddress;
    }

    @PrePersist
    void onCreate() {
        if (this.attemptedAt == null) {
            this.attemptedAt = Instant.now();
        }
    }

    public String getId() {
        return id;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public Instant getAttemptedAt() {
        return attemptedAt;
    }
}
