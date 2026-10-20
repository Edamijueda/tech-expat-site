package com.techexpat.site.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("receipts")
public record ReceiptsProperties(
        String supabaseUrl,
        String serviceRoleKey,
        String bucket,
        long maxBytes
) {
}
