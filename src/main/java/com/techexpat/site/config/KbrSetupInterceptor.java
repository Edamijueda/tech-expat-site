package com.techexpat.site.config;

import com.techexpat.site.service.KbrUserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Holds a signed-in KBR user on the setup screen until they've named their business.
 * Registered on the authenticated tool routes only, so public pages stay reachable.
 */
public class KbrSetupInterceptor implements HandlerInterceptor {

    static final String SETUP_PATH = "/tool/keep-business-receipt/setup";

    private final KbrUserService userService;

    public KbrSetupInterceptor(KbrUserService userService) {
        this.userService = userService;
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
        if (!isKbrUser || userService.hasBusinessName(auth.getName())) {
            return true;
        }
        response.sendRedirect(request.getContextPath() + SETUP_PATH);
        return false;
    }
}
