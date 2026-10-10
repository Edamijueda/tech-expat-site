package com.techexpat.site.service;

import com.techexpat.site.model.Course;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CourseCatalog {

    private final Map<String, Course> bySlug;

    public CourseCatalog() {
        List<Course> courses = List.of(
                new Course("practical-sql", "Practical SQL Path in a World of Automation & AI", new BigDecimal("29.00")),
                new Course("modern-java", "Modern Java Programming — Career Focused", new BigDecimal("49.00")),
                new Course("frontend-bootcamp", "Frontend Engineering 15 Hours Bootcamp", new BigDecimal("79.00"))
        );
        this.bySlug = courses.stream().collect(
                java.util.stream.Collectors.toUnmodifiableMap(Course::slug, c -> c));
    }

    public Optional<Course> findBySlug(String slug) {
        return Optional.ofNullable(bySlug.get(slug));
    }

    public List<Course> all() {
        return List.copyOf(bySlug.values());
    }
}
