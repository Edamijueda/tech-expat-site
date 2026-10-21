package com.techexpat.site.config;

import com.techexpat.site.service.KbrSubscriptionService;
import com.techexpat.site.service.KbrUserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Drops an account to read-only once its trial has run out and no paid period covers
 * today. Registered on the receipt routes only, so the dashboard, previews and downloads
 * stay reachable and the user keeps everything they already kept.
 *
 * <p>The dashboard hides the upload form and delete buttons in the same state; this is
 * the backstop for a bookmarked URL or a stale page.
 */
public class KbrAccessInterceptor implements HandlerInterceptor {

    static final String DASHBOARD_PATH = "/tool/keep-business-receipt/dashboard";

    private final KbrUserService userService;
    private final KbrSubscriptionService subscriptions;

    public KbrAccessInterceptor(KbrUserService userService, KbrSubscriptionService subscriptions) {
        this.userService = userService;
        this.subscriptions = subscriptions;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return true;
        }
        boolean isKbrUser = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_KBR_USER".equals(a.getAuthority()));
        if (!isKbrUser) {
            return true;
        }

        boolean canWrite = userService.findByEmail(auth.getName())
                .map(user -> subscriptions.statusFor(user).canWrite())
                .orElse(false);
        if (canWrite) {
            return true;
        }

        response.sendRedirect(request.getContextPath() + DASHBOARD_PATH);
        return false;
    }
}
