package com.techexpat.site.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "te_kbr_users")
public class KbrUser {

    /** Length of the first free trial, in days. The clock starts when the admin approves. */
    public static final int FIRST_TRIAL_DAYS = 30;

    /** Days added by the single extension an admin may grant, taking the cap to 60. */
    public static final int TRIAL_EXTENSION_DAYS = 30;

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "pin_hash", nullable = false, length = 72)
    private String pinHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private KbrUserStatus status;

    @Column(name = "business_name", length = 255)
    private String businessName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "trial_started_at")
    private Instant trialStartedAt;

    @Column(name = "trial_days", nullable = false)
    private int trialDays;

    @Column(name = "trial_extended_at")
    private Instant trialExtendedAt;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    protected KbrUser() {
    }

    public KbrUser(String id, String email, String pinHash) {
        this.id = id;
        this.email = email;
        this.pinHash = pinHash;
        this.status = KbrUserStatus.PENDING;
        this.failedLoginAttempts = 0;
        this.trialDays = FIRST_TRIAL_DAYS;
    }

    @PrePersist
    void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPinHash() {
        return pinHash;
    }

    public void setPinHash(String pinHash) {
        this.pinHash = pinHash;
    }

    public KbrUserStatus getStatus() {
        return status;
    }

    public void setStatus(KbrUserStatus status) {
        this.status = status;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Instant approvedAt) {
        this.approvedAt = approvedAt;
    }

    public Instant getTrialStartedAt() {
        return trialStartedAt;
    }

    public void setTrialStartedAt(Instant trialStartedAt) {
        this.trialStartedAt = trialStartedAt;
    }

    public int getTrialDays() {
        return trialDays;
    }

    public void setTrialDays(int trialDays) {
        this.trialDays = trialDays;
    }

    public Instant getTrialExtendedAt() {
        return trialExtendedAt;
    }

    public void setTrialExtendedAt(Instant trialExtendedAt) {
        this.trialExtendedAt = trialExtendedAt;
    }

    public boolean isTrialExtended() {
        return trialExtendedAt != null;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(Instant lockedUntil) {
        this.lockedUntil = lockedUntil;
    }
}
