package com.techexpat.site.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("kbd")
public record KbdProperties(String rememberMeKey) {
}
