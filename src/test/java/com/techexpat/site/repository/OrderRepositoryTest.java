package com.techexpat.site.repository;

import com.techexpat.site.model.Order;
import com.techexpat.site.model.OrderStatus;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orders;

    @Autowired
    private TestEntityManager em;

    @Test
    void savesAndRetrievesOrder() {
        Order saved = orders.save(newOrder("practical-sql", "buyer@example.com", "49.00"));

        Optional<Order> found = orders.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getCourseSlug()).isEqualTo("practical-sql");
        assertThat(found.get().getBuyerEmail()).isEqualTo("buyer@example.com");
        assertThat(found.get().getPriceUsd()).isEqualByComparingTo("49.00");
        assertThat(found.get().getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(found.get().getCreatedAt()).isNotNull();
        assertThat(found.get().getUpdatedAt()).isNotNull();
    }

    @Test
    void findAllByOrderByCreatedAtDescReturnsNewestFirst() throws InterruptedException {
        Order first = em.persistAndFlush(newOrder("practical-sql", "a@example.com", "49.00"));
        Thread.sleep(5);
        Order second = em.persistAndFlush(newOrder("modern-java", "b@example.com", "79.00"));
        Thread.sleep(5);
        Order third = em.persistAndFlush(newOrder("frontend-bootcamp", "c@example.com", "99.00"));

        List<Order> all = orders.findAllByOrderByCreatedAtDesc();

        assertThat(all).extracting(Order::getId)
                .containsExactly(third.getId(), second.getId(), first.getId());
    }

    @Test
    void findByNowpaymentsInvoiceIdFindsOrder() {
        Order order = newOrder("practical-sql", "buyer@example.com", "49.00");
        order.setNowpaymentsInvoiceId("inv_abc123");
        orders.save(order);

        Optional<Order> found = orders.findByNowpaymentsInvoiceId("inv_abc123");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(order.getId());
    }

    @Test
    void updatesTimestampOnModification() throws InterruptedException {
        Order saved = em.persistAndFlush(newOrder("practical-sql", "buyer@example.com", "49.00"));
        var createdAt = saved.getCreatedAt();
        var initialUpdatedAt = saved.getUpdatedAt();

        Thread.sleep(5);
        saved.setStatus(OrderStatus.PAID);
        em.persistAndFlush(saved);

        assertThat(saved.getCreatedAt()).isEqualTo(createdAt);
        assertThat(saved.getUpdatedAt()).isAfter(initialUpdatedAt);
    }

    private Order newOrder(String slug, String email, String price) {
        return new Order(UUID.randomUUID().toString(), slug, email, new BigDecimal(price));
    }
}
