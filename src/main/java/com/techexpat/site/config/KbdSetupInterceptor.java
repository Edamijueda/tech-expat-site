package com.techexpat.site.config;

import com.techexpat.site.service.KbdUserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Holds a signed-in KBD user on the setup screen until they've named their business.
 * Registered on the authenticated tool routes only, so public pages stay reachable.
 */
public class KbdSetupInterceptor implements HandlerInterceptor {

    static final String SETUP_PATH = "/tool/keep-business-docs/setup";

    private final KbdUserService userService;

    public KbdSetupInterceptor(KbdUserService userService) {
        this.userService = userService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return true;
        }
        boolean isKbdUser = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_KBD_USER".equals(a.getAuthority()));
        if (!isKbdUser || userService.hasBusinessName(auth.getName())) {
            return true;
        }
        response.sendRedirect(request.getContextPath() + SETUP_PATH);
        return false;
    }
}
