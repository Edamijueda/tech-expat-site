package com.techexpat.site.service;

import com.techexpat.site.model.ResearchPost;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ResearchServiceTest {

    @Test
    void parsesFrontMatterAndRendersBody() {
        String markdown = """
                ---
                slug: my-slug
                order: 3
                title: My Title
                author: Tobi Omorubore
                date: 2026-03-14
                description: One line.
                ---

                **TL;DR — a fixture.**

                A short body paragraph so the reading-time formula runs.
                """;

        ResearchPost post = ResearchService.parse(markdown);

        assertThat(post.slug()).isEqualTo("my-slug");
        assertThat(post.order()).isEqualTo(3);
        assertThat(post.title()).isEqualTo("My Title");
        assertThat(post.author()).isEqualTo("Tobi Omorubore");
        assertThat(post.date()).isEqualTo(LocalDate.of(2026, 3, 14));
        assertThat(post.description()).isEqualTo("One line.");
        assertThat(post.htmlBody()).contains("<strong>TL;DR");
        assertThat(post.htmlBody()).doesNotContain("---");
        assertThat(post.readingMinutes()).isEqualTo(1);
    }
}
