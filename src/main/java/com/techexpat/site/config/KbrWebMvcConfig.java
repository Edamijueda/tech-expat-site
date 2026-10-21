package com.techexpat.site.config;

import com.techexpat.site.service.KbrSubscriptionService;
import com.techexpat.site.service.KbrUserService;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class KbrWebMvcConfig implements WebMvcConfigurer {

    private final KbrUserService userService;
    private final KbrSubscriptionService subscriptions;

    public KbrWebMvcConfig(KbrUserService userService, KbrSubscriptionService subscriptions) {
        this.userService = userService;
        this.subscriptions = subscriptions;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new KbrSetupInterceptor(userService))
                .addPathPatterns(
                        "/tool/keep-business-receipt/dashboard/**",
                        "/tool/keep-business-receipt/receipts/**",
                        "/tool/keep-business-receipt/files/**");

        // Receipt routes only: every one of them writes or confirms a delete, while the
        // dashboard and files stay open so a read-only account can still look and download.
        registry.addInterceptor(new KbrAccessInterceptor(userService, subscriptions))
                .addPathPatterns("/tool/keep-business-receipt/receipts/**");
    }
}
