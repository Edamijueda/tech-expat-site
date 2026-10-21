package com.techexpat.site.controller;

import com.techexpat.site.service.KbrSubscriptionService;
import com.techexpat.site.service.KbrUserService;
import com.techexpat.site.service.ReceiptService;
import com.techexpat.site.service.RegistrationRateLimiter;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/tool/keep-business-receipt")
public class KbrAuthController {

    private final KbrUserService userService;
    private final RegistrationRateLimiter rateLimiter;
    private final ReceiptService receiptService;
    private final KbrSubscriptionService subscriptions;

    public KbrAuthController(KbrUserService userService, RegistrationRateLimiter rateLimiter,
                             ReceiptService receiptService,
                             KbrSubscriptionService subscriptions) {
        this.userService = userService;
        this.rateLimiter = rateLimiter;
        this.receiptService = receiptService;
        this.subscriptions = subscriptions;
    }

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
        userService.findByEmail(authentication.getName()).ifPresent(user -> {
            model.addAttribute("businessName", user.getBusinessName());
            model.addAttribute("receipts", receiptService.listFor(user.getId()));
            model.addAttribute("access", subscriptions.statusFor(user));
        });
        return "tool/keep-business-receipt/dashboard";
    }

    @GetMapping("/setup")
    public String setupForm(Authentication authentication, Model model) {
        if (userService.hasBusinessName(authentication.getName())) {
            return "redirect:/tool/keep-business-receipt/dashboard";
        }
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new SetupForm(""));
        }
        return "tool/keep-business-receipt/setup";
    }

    @PostMapping("/setup")
    public String submitSetup(@ModelAttribute("form") SetupForm form,
                              Authentication authentication,
                              Model model) {
        if (userService.hasBusinessName(authentication.getName())) {
            return "redirect:/tool/keep-business-receipt/dashboard";
        }

        String businessName = form.businessName() == null ? "" : form.businessName().trim();
        if (businessName.length() < 2 || businessName.length() > 255) {
            model.addAttribute("form", form);
            model.addAttribute("errors",
                    List.of("Business name must be between 2 and 255 characters."));
            return "tool/keep-business-receipt/setup";
        }

        userService.saveBusinessName(authentication.getName(), businessName);
        return "redirect:/tool/keep-business-receipt/dashboard";
    }

    @GetMapping("/forgot-pin")
    public String forgotPin() {
        return "tool/keep-business-receipt/forgot-pin";
    }

    @GetMapping("/terms")
    public String terms() {
        return "tool/keep-business-receipt/terms";
    }

    @GetMapping("/privacy")
    public String privacy() {
        return "tool/keep-business-receipt/privacy";
    }

    @GetMapping("/request-access")
    public String requestAccessForm(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new RequestAccessForm("", "", ""));
        }
        return "tool/keep-business-receipt/request-access";
    }

    @PostMapping("/request-access")
    public String submitRequestAccess(@ModelAttribute("form") RequestAccessForm form,
                                      HttpServletRequest request,
                                      Model model) {
        List<String> errors = validate(form);
        if (!errors.isEmpty()) {
            model.addAttribute("form", form.cleared());
            model.addAttribute("errors", errors);
            return "tool/keep-business-receipt/request-access";
        }

        if (!rateLimiter.tryAcquire(request.getRemoteAddr())) {
            model.addAttribute("form", form.cleared());
            model.addAttribute("errors",
                    List.of("Too many requests from your network. Please try again later, "
                            + "or email support@tech-expat.com for help."));
            return "tool/keep-business-receipt/request-access";
        }

        userService.createPendingUser(form.email(), form.pin());
        return "redirect:/tool/keep-business-receipt/request-received";
    }

    @GetMapping("/request-received")
    public String requestReceived() {
        return "tool/keep-business-receipt/request-received";
    }

    private static List<String> validate(RequestAccessForm f) {
        List<String> errs = new ArrayList<>();
        String email = f.email() == null ? "" : f.email().trim();
        if (email.length() < 3 || email.length() > 255 || !email.contains("@")
                || email.indexOf("@") == 0 || email.indexOf("@") == email.length() - 1) {
            errs.add("Enter a valid email address.");
        }
        if (f.pin() == null || !f.pin().matches("\\d{6}")) {
            errs.add("PIN must be exactly 6 digits.");
        } else if (!f.pin().equals(f.pinConfirm())) {
            errs.add("The two PINs don't match.");
        }
        return errs;
    }

    public record SetupForm(String businessName) {
    }

    public record RequestAccessForm(String email, String pin, String pinConfirm) {
        public RequestAccessForm cleared() {
            return new RequestAccessForm(email, "", "");
        }
    }
}
