package com.techexpat.site.repository;

import com.techexpat.site.model.KbrSubscriptionPayment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface KbrSubscriptionPaymentRepository
        extends JpaRepository<KbrSubscriptionPayment, String> {

    List<KbrSubscriptionPayment> findByUserIdOrderByPeriodStartDesc(String userId);

    /**
     * Periods for this user that overlap [start, end), both ends exclusive at the far
     * side — two periods clash unless one ends on or before the other begins.
     */
    List<KbrSubscriptionPayment> findByUserIdAndPeriodStartLessThanAndPeriodEndGreaterThan(
            String userId, LocalDate end, LocalDate start);
}
