package com.techexpat.site.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "te_orders")
public class Order {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "course_slug", nullable = false, length = 100)
    private String courseSlug;

    @Column(name = "buyer_email", nullable = false, length = 255)
    private String buyerEmail;

    @Column(name = "price_usd", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceUsd;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "nowpayments_invoice_id", length = 100)
    private String nowpaymentsInvoiceId;

    @Column(name = "nowpayments_payment_id", length = 100)
    private String nowpaymentsPaymentId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Order() {
    }

    public Order(String id, String courseSlug, String buyerEmail, BigDecimal priceUsd) {
        this.id = id;
        this.courseSlug = courseSlug;
        this.buyerEmail = buyerEmail;
        this.priceUsd = priceUsd;
        this.status = OrderStatus.PENDING;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getCourseSlug() {
        return courseSlug;
    }

    public String getBuyerEmail() {
        return buyerEmail;
    }

    public BigDecimal getPriceUsd() {
        return priceUsd;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public String getNowpaymentsInvoiceId() {
        return nowpaymentsInvoiceId;
    }

    public void setNowpaymentsInvoiceId(String nowpaymentsInvoiceId) {
        this.nowpaymentsInvoiceId = nowpaymentsInvoiceId;
    }

    public String getNowpaymentsPaymentId() {
        return nowpaymentsPaymentId;
    }

    public void setNowpaymentsPaymentId(String nowpaymentsPaymentId) {
        this.nowpaymentsPaymentId = nowpaymentsPaymentId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
