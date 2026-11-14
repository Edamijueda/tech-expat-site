package com.techexpat.site.controller;

import com.techexpat.site.model.KbdUser;
import com.techexpat.site.repository.KbdUserLockoutEventRepository;
import com.techexpat.site.service.KbdAccessStatus;
import com.techexpat.site.service.KbdSubscriptionService;
import com.techexpat.site.service.KbdUserService;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Instant;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/admin/users")
public class AdminKbdUsersController {

    private final KbdUserService userService;
    private final KbdUserLockoutEventRepository lockoutEvents;
    private final KbdSubscriptionService subscriptions;

    public AdminKbdUsersController(KbdUserService userService,
                                   KbdUserLockoutEventRepository lockoutEvents,
                                   KbdSubscriptionService subscriptions) {
        this.userService = userService;
        this.lockoutEvents = lockoutEvents;
        this.subscriptions = subscriptions;
    }

    @GetMapping
    public String list(Model model) {
        List<KbdUser> users = userService.findAllNewestFirst();
        Map<String, KbdAccessStatus> access = new HashMap<>();
        for (KbdUser user : users) {
            access.put(user.getId(), subscriptions.statusFor(user));
        }
        model.addAttribute("users", users);
        model.addAttribute("access", access);
        model.addAttribute("now", Instant.now());
        return "admin/users/index";
    }

    @GetMapping("/{id}/lockouts")
    public String lockouts(@PathVariable String id, Model model) {
        KbdUser user = userService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown user"));
        model.addAttribute("user", user);
        model.addAttribute("events", lockoutEvents.findByUserIdOrderByLockedAtDesc(id));
        model.addAttribute("now", Instant.now());
        return "admin/users/lockouts";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable String id, RedirectAttributes ra) {
        return finish(userService.approve(id), "User approved.", "Could not approve user.", ra);
    }

    @PostMapping("/{id}/suspend")
    public String suspend(@PathVariable String id, RedirectAttributes ra) {
        return finish(userService.suspend(id), "User suspended.", "Could not suspend user.", ra);
    }

    @PostMapping("/{id}/unsuspend")
    public String unsuspend(@PathVariable String id, RedirectAttributes ra) {
        return finish(userService.unsuspend(id), "User reinstated.", "Could not reinstate user.", ra);
    }

    @PostMapping("/{id}/extend-trial")
    public String extendTrial(@PathVariable String id, RedirectAttributes ra) {
        return finish(userService.extendTrial(id),
                "Trial extended by " + KbdUser.TRIAL_EXTENSION_DAYS + " days.",
                "Could not extend this trial. It must have run out, and can only be extended once.",
                ra);
    }

    @GetMapping("/{id}/subscription")
    public String subscription(@PathVariable String id, Model model) {
        KbdUser user = userService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown user"));
        model.addAttribute("user", user);
        model.addAttribute("payments", subscriptions.findFor(id));
        model.addAttribute("access", subscriptions.statusFor(user));
        return "admin/users/subscription";
    }

    @PostMapping("/{id}/subscription")
    public String recordPayment(@PathVariable String id,
                                @RequestParam(name = "startMonth", required = false) String startMonth,
                                @RequestParam(name = "endMonth", required = false) String endMonth,
                                @RequestParam(name = "note", required = false) String note,
                                RedirectAttributes ra) {
        Optional<String> refusal = subscriptions.record(
                id, parseMonth(startMonth), parseMonth(endMonth), note);
        refusal.ifPresentOrElse(
                message -> ra.addFlashAttribute("flashError", message),
                () -> ra.addFlashAttribute("flashMessage", "Payment recorded."));
        return "redirect:/admin/users/" + id + "/subscription";
    }

    @PostMapping("/{userId}/subscription/{paymentId}/delete")
    public String deletePayment(@PathVariable String userId,
                                @PathVariable String paymentId,
                                RedirectAttributes ra) {
        subscriptions.delete(paymentId);
        ra.addFlashAttribute("flashMessage", "Payment record deleted.");
        return "redirect:/admin/users/" + userId + "/subscription";
    }

    @PostMapping("/{id}/unlock")
    public String unlock(@PathVariable String id, RedirectAttributes ra) {
        return finish(userService.unlock(id), "User unlocked.", "Could not unlock user.", ra);
    }

    @PostMapping("/{id}/lockouts/clear")
    public String clearLockouts(@PathVariable String id, RedirectAttributes ra) {
        userService.clearLockoutHistory(id);
        ra.addFlashAttribute("flashMessage", "Lockout history cleared.");
        return "redirect:/admin/users/" + id + "/lockouts";
    }

    @PostMapping("/{userId}/lockouts/{eventId}/delete")
    public String deleteLockoutEvent(@PathVariable String userId,
                                     @PathVariable String eventId,
                                     RedirectAttributes ra) {
        userService.deleteLockoutEvent(eventId);
        ra.addFlashAttribute("flashMessage", "Lockout event deleted.");
        return "redirect:/admin/users/" + userId + "/lockouts";
    }

    /** "2027-01" from an {@code <input type="month">}; null when blank or malformed. */
    private static YearMonth parseMonth(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return YearMonth.parse(value.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String finish(boolean ok, String okMsg, String failMsg, RedirectAttributes ra) {
        ra.addFlashAttribute(ok ? "flashMessage" : "flashError", ok ? okMsg : failMsg);
        return "redirect:/admin/users";
    }
}
