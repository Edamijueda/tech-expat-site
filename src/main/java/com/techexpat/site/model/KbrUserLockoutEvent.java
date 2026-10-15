package com.techexpat.site.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "te_kbr_user_lockout_events")
public class KbrUserLockoutEvent {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "locked_at", nullable = false)
    private Instant lockedAt;

    @Column(name = "failed_attempt_count", nullable = false)
    private int failedAttemptCount;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    protected KbrUserLockoutEvent() {
    }

    public KbrUserLockoutEvent(String id, String userId, int failedAttemptCount,
                               String ipAddress, String userAgent) {
        this.id = id;
        this.userId = userId;
        this.failedAttemptCount = failedAttemptCount;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    @PrePersist
    void onCreate() {
        if (this.lockedAt == null) {
            this.lockedAt = Instant.now();
        }
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public Instant getLockedAt() {
        return lockedAt;
    }

    public int getFailedAttemptCount() {
        return failedAttemptCount;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }
}
