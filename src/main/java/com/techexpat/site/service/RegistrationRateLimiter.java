package com.techexpat.site.service;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RegistrationRateLimiter {

    private static final int MAX_PER_WINDOW = 3;
    private static final Duration WINDOW = Duration.ofDays(1);

    private final ConcurrentHashMap<String, Deque<Instant>> attempts = new ConcurrentHashMap<>();

    public synchronized boolean tryAcquire(String ip) {
        if (ip == null || ip.isBlank()) {
            return true;
        }
        Instant now = Instant.now();
        Instant cutoff = now.minus(WINDOW);
        Deque<Instant> list = attempts.computeIfAbsent(ip, k -> new ArrayDeque<>());
        while (!list.isEmpty() && list.peekFirst().isBefore(cutoff)) {
            list.pollFirst();
        }
        if (list.size() >= MAX_PER_WINDOW) {
            return false;
        }
        list.offerLast(now);
        return true;
    }
}
