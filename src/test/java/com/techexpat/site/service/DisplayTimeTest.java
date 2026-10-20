package com.techexpat.site.service;

import com.techexpat.site.config.DisplayProperties;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class DisplayTimeTest {

    private final DisplayTime lagos = new DisplayTime(new DisplayProperties("Africa/Lagos"));
    private final DisplayTime utc = new DisplayTime(new DisplayProperties("UTC"));

    @Test
    void formatsInTheConfiguredZoneNotTheServerZone() {
        Instant instant = Instant.parse("2026-10-20T08:30:00Z");

        assertThat(lagos.dateTime(instant)).isEqualTo("2026-10-20 09:30");
        assertThat(utc.dateTime(instant)).isEqualTo("2026-10-20 08:30");
    }

    /** The bug this class exists to prevent: a receipt kept just after midnight in Lagos. */
    @Test
    void keepsLateNightUploadsOnTheRightDay() {
        Instant justAfterMidnightInLagos = Instant.parse("2026-10-19T23:30:00Z");

        assertThat(lagos.day(justAfterMidnightInLagos)).isEqualTo("20 Oct 2026");
        assertThat(utc.day(justAfterMidnightInLagos)).isEqualTo("19 Oct 2026");
    }

    @Test
    void formatsEachShapeUsedByTheTemplates() {
        Instant instant = Instant.parse("2026-01-05T14:07:09Z");

        assertThat(lagos.day(instant)).isEqualTo("5 Jan 2026");
        assertThat(lagos.date(instant)).isEqualTo("2026-01-05");
        assertThat(lagos.dateTime(instant)).isEqualTo("2026-01-05 15:07");
        assertThat(lagos.dateTimeSeconds(instant)).isEqualTo("2026-01-05 15:07:09");
        assertThat(lagos.time(instant)).isEqualTo("15:07");
    }

    @Test
    void rendersNullAsADash() {
        assertThat(lagos.day(null)).isEqualTo("—");
        assertThat(lagos.date(null)).isEqualTo("—");
        assertThat(lagos.dateTime(null)).isEqualTo("—");
    }
}
