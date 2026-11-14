package com.techexpat.site.config;

import com.techexpat.site.service.KbdSubscriptionService;
import com.techexpat.site.service.KbdUserService;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class KbdWebMvcConfig implements WebMvcConfigurer {

    private final KbdUserService userService;
    private final KbdSubscriptionService subscriptions;

    public KbdWebMvcConfig(KbdUserService userService, KbdSubscriptionService subscriptions) {
        this.userService = userService;
        this.subscriptions = subscriptions;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new KbdSetupInterceptor(userService))
                .addPathPatterns(
                        "/tool/keep-business-docs/dashboard/**",
                        "/tool/keep-business-docs/docs/**",
                        "/tool/keep-business-docs/files/**");

        // Document routes only: every one of them writes or confirms a delete, while the
        // dashboard and files stay open so a read-only account can still look and download.
        registry.addInterceptor(new KbdAccessInterceptor(userService, subscriptions))
                .addPathPatterns("/tool/keep-business-docs/docs/**");
    }
}
