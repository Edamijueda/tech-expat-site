package com.techexpat.site.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("admin")
public record AdminProperties(String username, String password) {
}
