package com.techexpat.site.config;

import com.techexpat.site.service.KbrLockoutService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class KbrAuthFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final KbrLockoutService lockoutService;

    public KbrAuthFailureHandler(KbrLockoutService lockoutService) {
        this.lockoutService = lockoutService;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception)
            throws IOException, ServletException {

        String email = request.getParameter("username");
        String reason;

        if (exception instanceof BadCredentialsException) {
            if (email != null && !email.isBlank()) {
                lockoutService.recordFailedAttempt(email, request.getRemoteAddr(),
                        request.getHeader("User-Agent"));
            }
            reason = "invalid";
        } else if (exception instanceof LockedException) {
            reason = "locked";
        } else if (exception instanceof DisabledException) {
            reason = "pending";
        } else {
            reason = "invalid";
        }

        response.sendRedirect(request.getContextPath()
                + "/tool/keep-business-receipt/sign-in?error="
                + URLEncoder.encode(reason, StandardCharsets.UTF_8));
    }
}
