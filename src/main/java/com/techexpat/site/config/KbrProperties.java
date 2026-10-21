package com.techexpat.site.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("kbr")
public record KbrProperties(String rememberMeKey) {
}
