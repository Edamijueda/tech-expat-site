package com.techexpat.site.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("nowpayments")
public record NowpaymentsProperties(
        String apiKey,
        String ipnSecret,
        String baseUrl,
        String successUrl,
        String cancelUrl,
        String ipnCallbackUrl
) {
}
