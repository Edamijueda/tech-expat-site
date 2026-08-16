package com.techexpat.site.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("posthog")
public record PosthogProperties(String key) {
}
