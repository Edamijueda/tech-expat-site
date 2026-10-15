package com.techexpat.site.config;

import com.techexpat.site.service.KbrLockoutService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class KbrAuthSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final KbrLockoutService lockoutService;

    public KbrAuthSuccessHandler(KbrLockoutService lockoutService) {
        this.lockoutService = lockoutService;
        setDefaultTargetUrl("/tool/keep-business-receipt/dashboard");
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
