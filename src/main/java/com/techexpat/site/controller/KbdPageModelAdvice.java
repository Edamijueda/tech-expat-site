package com.techexpat.site.controller;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Tells the shared layout whether the current page belongs to Keep Business Docs, so
 * the footer can offer the tool's terms and privacy links only where they apply.
 */
@ControllerAdvice
public class KbdPageModelAdvice {

    static final String KBD_PREFIX = "/tool/keep-business-docs";

    @ModelAttribute("onKbdPage")
    public boolean onKbdPage(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.startsWith(KBD_PREFIX);
    }
}
