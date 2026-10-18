package com.techexpat.site.controller;

import com.techexpat.site.model.KbrUser;
import com.techexpat.site.repository.KbrUserLockoutEventRepository;
import com.techexpat.site.service.KbrUserService;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Instant;

@Controller
@RequestMapping("/admin/users")
public class AdminKbrUsersController {

    private final KbrUserService userService;
    private final KbrUserLockoutEventRepository lockoutEvents;

    public AdminKbrUsersController(KbrUserService userService,
                                   KbrUserLockoutEventRepository lockoutEvents) {
        this.userService = userService;
        this.lockoutEvents = lockoutEvents;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("users", userService.findAllNewestFirst());
        model.addAttribute("now", Instant.now());
        return "admin/users/index";
    }

    @GetMapping("/{id}/lockouts")
    public String lockouts(@PathVariable String id, Model model) {
        KbrUser user = userService.findById(id)
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

    private static String finish(boolean ok, String okMsg, String failMsg, RedirectAttributes ra) {
        ra.addFlashAttribute(ok ? "flashMessage" : "flashError", ok ? okMsg : failMsg);
        return "redirect:/admin/users";
    }
}
