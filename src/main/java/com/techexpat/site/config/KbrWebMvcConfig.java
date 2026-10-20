package com.techexpat.site.config;

import com.techexpat.site.service.KbrUserService;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class KbrWebMvcConfig implements WebMvcConfigurer {

    private final KbrUserService userService;

    public KbrWebMvcConfig(KbrUserService userService) {
        this.userService = userService;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new KbrSetupInterceptor(userService))
                .addPathPatterns(
                        "/tool/keep-business-receipt/dashboard/**",
                        "/tool/keep-business-receipt/receipts/**",
                        "/tool/keep-business-receipt/files/**");
    }
}
