package com.techexpat.site.service;

import com.techexpat.site.config.DisplayProperties;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Formats Instants for display in a configured zone.
 *
 * <p>Templates reach this through {@code ${@displayTime.day(...)}} rather than
 * {@code #temporals}, which falls back to the server's default zone — UTC on Railway,
 * but the local zone on a developer machine, so the difference only shows in production.
 */
@Component("displayTime")
public class DisplayTime {

    private final ZoneId zone;
    private final DateTimeFormatter day;
    private final DateTimeFormatter date;
    private final DateTimeFormatter dateTime;
    private final DateTimeFormatter dateTimeSeconds;
    private final DateTimeFormatter time;

    public DisplayTime(DisplayProperties props) {
        this.zone = ZoneId.of(props.zone());
        this.day = formatter("d MMM yyyy");
        this.date = formatter("yyyy-MM-dd");
        this.dateTime = formatter("yyyy-MM-dd HH:mm");
        this.dateTimeSeconds = formatter("yyyy-MM-dd HH:mm:ss");
        this.time = formatter("HH:mm");
    }

    private DateTimeFormatter formatter(String pattern) {
        return DateTimeFormatter.ofPattern(pattern, Locale.UK).withZone(zone);
    }

    /** "7 Oct 2026" — for pages end users read. */
    public String day(Instant instant) {
        return format(day, instant);
    }

    public String date(Instant instant) {
        return format(date, instant);
    }

    public String dateTime(Instant instant) {
        return format(dateTime, instant);
    }

    public String dateTimeSeconds(Instant instant) {
        return format(dateTimeSeconds, instant);
    }

    public String time(Instant instant) {
        return format(time, instant);
    }

    private static String format(DateTimeFormatter formatter, Instant instant) {
        return instant == null ? "—" : formatter.format(instant);
    }
}
