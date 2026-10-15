package com.techexpat.site.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/tool/keep-business-receipt")
public class KbrAuthController {

    @GetMapping("/sign-in")
    public String signIn(@RequestParam(required = false) String error,
                         @RequestParam(name = "signed-out", required = false) String signedOut,
                         Model model) {
        if ("invalid".equals(error)) {
            model.addAttribute("errorMessage", "Invalid email or PIN.");
        } else if ("locked".equals(error)) {
            model.addAttribute("errorMessage",
                    "Account locked due to too many failed attempts. Try again in about 15 minutes.");
        } else if ("pending".equals(error)) {
            model.addAttribute("errorMessage",
                    "Access not yet approved, or the account is no longer active. "
                            + "If you believe this is a mistake, email hello@tech-expat.com.");
        }
        if (signedOut != null) {
            model.addAttribute("infoMessage", "You've been signed out.");
        }
        return "tool/keep-business-receipt/sign-in";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        model.addAttribute("userEmail", authentication.getName());
        return "tool/keep-business-receipt/dashboard";
    }

    @GetMapping("/forgot-pin")
    public String forgotPin() {
        return "tool/keep-business-receipt/forgot-pin";
    }
}
