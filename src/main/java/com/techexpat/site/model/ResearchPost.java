package com.techexpat.site.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public record ResearchPost(
        String slug,
        int order,
        String title,
        String author,
        LocalDate date,
        String description,
        String htmlBody,
        int readingMinutes,
        int aiPercent,
        LocalDate updatedDate
) {
    private static final DateTimeFormatter DISPLAY_DATE =
            DateTimeFormatter.ofPattern("d MMM, yyyy", Locale.ENGLISH);

    public String formattedDate() {
        return date.format(DISPLAY_DATE);
    }

    public String formattedUpdatedDate() {
        return updatedDate == null ? null : updatedDate.format(DISPLAY_DATE);
    }

    public int humanPercent() {
        return 100 - aiPercent;
    }
}
