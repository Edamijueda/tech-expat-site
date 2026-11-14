package com.techexpat.site.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("docs")
public record BizDocsProperties(
        String supabaseUrl,
        String serviceRoleKey,
        String bucket,
        long maxBytes
) {
}
