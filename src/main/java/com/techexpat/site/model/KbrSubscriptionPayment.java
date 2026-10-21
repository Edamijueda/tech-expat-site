package com.techexpat.site.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * One subscription payment collected by hand, recorded as the whole calendar months it
 * covers. {@code periodStart} is the first day of the first paid month and
 * {@code periodEnd} the first day of the month after the last, exclusive — January to
 * March 2027 is stored as 2027-01-01 / 2027-04-01.
 *
 * <p>Dates, not Instants: months are calendar months in the display zone, so comparing
 * them against {@code DisplayTime.today()} keeps the boundaries out of instant arithmetic.
 */
@Entity
@Table(name = "te_kbr_subscription_payments")
public class KbrSubscriptionPayment {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "months_count", nullable = false)
    private int monthsCount;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    protected KbrSubscriptionPayment() {
    }

    public KbrSubscriptionPayment(String id, String userId, LocalDate periodStart,
                                  LocalDate periodEnd, String note) {
        this.id = id;
        this.userId = userId;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.monthsCount = (int) ChronoUnit.MONTHS.between(periodStart, periodEnd);
        this.note = note;
    }

    @PrePersist
    void onCreate() {
        if (this.recordedAt == null) {
            this.recordedAt = Instant.now();
        }
    }

    /** True if {@code day} falls inside this paid period, with the end exclusive. */
    public boolean covers(LocalDate day) {
        return !day.isBefore(periodStart) && day.isBefore(periodEnd);
    }

    /** The last paid day, for display — 31 Mar where {@code periodEnd} holds 1 Apr. */
    public LocalDate getLastPaidDay() {
        return periodEnd.minusDays(1);
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public int getMonthsCount() {
        return monthsCount;
    }

    public String getNote() {
        return note;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
