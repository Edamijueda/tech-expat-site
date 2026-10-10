package com.techexpat.site.service;

import com.techexpat.site.model.Order;
import com.techexpat.site.model.OrderStatus;
import com.techexpat.site.repository.OrderRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orders;

    public OrderService(OrderRepository orders) {
        this.orders = orders;
    }

    @Transactional
    public Order create(String courseSlug, String buyerEmail, BigDecimal priceUsd) {
        Order order = new Order(UUID.randomUUID().toString(), courseSlug, buyerEmail, priceUsd);
        return orders.save(order);
    }

    @Transactional
    public Order attachInvoice(String orderId, String invoiceId) {
        Order order = orders.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown order: " + orderId));
        order.setNowpaymentsInvoiceId(invoiceId);
        return orders.save(order);
    }

    @Transactional
    public boolean markPaidIfPending(String orderId, String paymentId) {
        Order order = orders.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown order: " + orderId));
        if (order.getStatus() != OrderStatus.PENDING) {
            return false;
        }
        order.setStatus(OrderStatus.PAID);
        order.setNowpaymentsPaymentId(paymentId);
        return true;
    }

    @Transactional(readOnly = true)
    public Optional<Order> findById(String orderId) {
        return orders.findById(orderId);
    }

    @Transactional(readOnly = true)
    public List<Order> findAll() {
        return orders.findAllByOrderByCreatedAtDesc();
    }
}
