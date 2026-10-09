package com.techexpat.site.repository;

import com.techexpat.site.model.Order;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, String> {

    List<Order> findAllByOrderByCreatedAtDesc();

    Optional<Order> findByNowpaymentsInvoiceId(String nowpaymentsInvoiceId);
}
