package com.techexpat.site.config;

import com.techexpat.site.service.KbdLockoutService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class KbdAuthSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final KbdLockoutService lockoutService;

    public KbdAuthSuccessHandler(KbdLockoutService lockoutService) {
        this.lockoutService = lockoutService;
        setDefaultTargetUrl("/tool/keep-business-docs/dashboard");
        setAlwaysUseDefaultTargetUrl(false);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {
        lockoutService.recordSuccessfulLogin(authentication.getName());
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
