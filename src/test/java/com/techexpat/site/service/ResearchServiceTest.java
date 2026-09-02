package com.techexpat.site.service;

import com.techexpat.site.model.ResearchPost;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
                ai_percent: 30
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
        assertThat(post.aiPercent()).isEqualTo(30);
        assertThat(post.humanPercent()).isEqualTo(70);
        assertThat(post.updatedDate()).isNull();
        assertThat(post.formattedUpdatedDate()).isNull();
    }

    @Test
    void parsesOptionalUpdatedDateWhenPresent() {
        String markdown = """
                ---
                slug: my-slug
                order: 1
                title: My Title
                author: Tobi Omorubore
                date: 2026-03-14
                description: One line.
                ai_percent: 30
                updated: 2026-08-31
                ---

                Body.
                """;

        ResearchPost post = ResearchService.parse(markdown);

        assertThat(post.updatedDate()).isEqualTo(LocalDate.of(2026, 8, 31));
        assertThat(post.formattedUpdatedDate()).isEqualTo("31 Aug, 2026");
    }

    @Test
    void acceptsAiPercentBoundaries() {
        assertThat(ResearchService.parse(fixture(0)).aiPercent()).isEqualTo(0);
        assertThat(ResearchService.parse(fixture(0)).humanPercent()).isEqualTo(100);
        assertThat(ResearchService.parse(fixture(100)).aiPercent()).isEqualTo(100);
        assertThat(ResearchService.parse(fixture(100)).humanPercent()).isEqualTo(0);
    }

    @Test
    void rejectsMissingAiPercent() {
        String markdown = """
                ---
                slug: my-slug
                order: 1
                title: My Title
                author: Tobi Omorubore
                date: 2026-03-14
                description: One line.
                ---

                Body.
                """;

        assertThatThrownBy(() -> ResearchService.parse(markdown))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ai_percent");
    }

    @Test
    void rejectsNegativeAiPercent() {
        assertThatThrownBy(() -> ResearchService.parse(fixture(-1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ai_percent")
                .hasMessageContaining("my-slug");
    }

    @Test
    void rejectsAiPercentAboveHundred() {
        assertThatThrownBy(() -> ResearchService.parse(fixture(101)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ai_percent")
                .hasMessageContaining("my-slug");
    }

    private static String fixture(int aiPercent) {
        return """
                ---
                slug: my-slug
                order: 1
                title: My Title
                author: Tobi Omorubore
                date: 2026-03-14
                description: One line.
                ai_percent: %d
                ---

                Body.
                """.formatted(aiPercent);
    }
}
