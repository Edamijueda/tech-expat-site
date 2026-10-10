package com.techexpat.site.service;

import com.techexpat.site.model.Course;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CourseCatalogTest {

    private final CourseCatalog catalog = new CourseCatalog();

    @Test
    void returnsAllCourses() {
        assertThat(catalog.all()).extracting(Course::slug)
                .containsExactlyInAnyOrder("practical-sql", "modern-java", "frontend-bootcamp");
    }

    @Test
    void findBySlugReturnsCourse() {
        assertThat(catalog.findBySlug("practical-sql"))
                .get()
                .satisfies(c -> {
                    assertThat(c.title()).contains("Practical SQL");
                    assertThat(c.priceUsd()).isEqualByComparingTo("29.00");
                });
    }

    @Test
    void findBySlugReturnsEmptyForUnknown() {
        assertThat(catalog.findBySlug("nope")).isEmpty();
    }
}
