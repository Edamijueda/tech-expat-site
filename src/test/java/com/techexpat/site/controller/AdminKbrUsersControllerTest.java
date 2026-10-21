package com.techexpat.site.controller;

import com.techexpat.site.config.KbrAuthFailureHandler;
import com.techexpat.site.config.KbrAuthSuccessHandler;
import com.techexpat.site.config.PosthogProperties;
import com.techexpat.site.config.WebSecurityConfig;
import com.techexpat.site.model.KbrSubscriptionPayment;
import com.techexpat.site.model.KbrUser;
import com.techexpat.site.model.KbrUserLockoutEvent;
import com.techexpat.site.model.KbrUserStatus;
import com.techexpat.site.repository.KbrUserLockoutEventRepository;
import com.techexpat.site.service.DisplayTime;
import com.techexpat.site.service.KbrAccessStatus;
import com.techexpat.site.service.KbrSubscriptionService;
import com.techexpat.site.service.KbrUserDetailsService;
import com.techexpat.site.service.KbrUserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AdminKbrUsersController.class)
@Import(WebSecurityConfig.class)
class AdminKbrUsersControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    KbrUserService userService;

    @MockitoBean
    KbrSubscriptionService subscriptions;

    @MockitoBean
    KbrUserLockoutEventRepository lockoutEvents;

    @MockitoBean
    PosthogProperties posthogProperties;

    @MockitoBean
    KbrUserDetailsService kbrUserDetailsService;

    @MockitoBean
    KbrAuthFailureHandler kbrAuthFailureHandler;

    @MockitoBean
    KbrAuthSuccessHandler kbrAuthSuccessHandler;

    @MockitoBean(name = "displayTime")
    DisplayTime displayTime;

    @BeforeEach
    void setUp() {
        when(subscriptions.statusFor(any())).thenReturn(new KbrAccessStatus(true, 4, 30, 27, null));
    }

    @Test
    void listRequiresAuth() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listRendersUsersModel() throws Exception {
        KbrUser user = sampleUser("u-1", "a@b.com", KbrUserStatus.PENDING);
        when(userService.findAllNewestFirst()).thenReturn(List.of(user));

        mockMvc.perform(get("/admin/users").with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users/index"))
                .andExpect(model().attribute("users", List.of(user)))
                .andExpect(model().attributeExists("now"));
    }

    @Test
    void lockoutsRendersEventsForUser() throws Exception {
        KbrUser user = sampleUser("u-1", "a@b.com", KbrUserStatus.APPROVED);
        KbrUserLockoutEvent event = new KbrUserLockoutEvent("e-1", "u-1", 5, "1.2.3.4", "ua");
        when(userService.findById("u-1")).thenReturn(Optional.of(user));
        when(lockoutEvents.findByUserIdOrderByLockedAtDesc("u-1")).thenReturn(List.of(event));

        mockMvc.perform(get("/admin/users/u-1/lockouts").with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users/lockouts"))
                .andExpect(model().attribute("user", user))
                .andExpect(model().attribute("events", List.of(event)));
    }

    @Test
    void lockoutsReturns404ForUnknownUser() throws Exception {
        when(userService.findById("missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/admin/users/missing/lockouts").with(httpBasic("admin", "admin")))
                .andExpect(status().isNotFound());
    }

    @Test
    void approvePostRedirectsBackToList() throws Exception {
        when(userService.approve("u-1")).thenReturn(true);

        mockMvc.perform(post("/admin/users/u-1/approve").with(csrf()).with(httpBasic("admin", "admin")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        verify(userService).approve("u-1");
    }

    @Test
    void suspendPostCallsService() throws Exception {
        when(userService.suspend("u-1")).thenReturn(true);

        mockMvc.perform(post("/admin/users/u-1/suspend").with(csrf()).with(httpBasic("admin", "admin")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        verify(userService).suspend("u-1");
    }

    @Test
    void unlockPostCallsService() throws Exception {
        when(userService.unlock("u-1")).thenReturn(true);

        mockMvc.perform(post("/admin/users/u-1/unlock").with(csrf()).with(httpBasic("admin", "admin")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        verify(userService).unlock("u-1");
    }

    @Test
    void clearLockoutsPostCallsService() throws Exception {
        mockMvc.perform(post("/admin/users/u-1/lockouts/clear").with(csrf()).with(httpBasic("admin", "admin")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/u-1/lockouts"));

        verify(userService).clearLockoutHistory("u-1");
    }

    @Test
    void deleteLockoutEventPostCallsService() throws Exception {
        mockMvc.perform(post("/admin/users/u-1/lockouts/e-1/delete").with(csrf()).with(httpBasic("admin", "admin")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/u-1/lockouts"));

        verify(userService).deleteLockoutEvent("e-1");
    }

    @Test
    void mutatingPostsRequireAuth() throws Exception {
        mockMvc.perform(post("/admin/users/u-1/approve").with(csrf()))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(userService);
    }

    @Test
    void extendTrialPostCallsService() throws Exception {
        when(userService.extendTrial("u-1")).thenReturn(true);

        mockMvc.perform(post("/admin/users/u-1/extend-trial").with(csrf()).with(httpBasic("admin", "admin")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attributeExists("flashMessage"));

        verify(userService).extendTrial("u-1");
    }

    @Test
    void refusedExtensionIsReportedAsAnError() throws Exception {
        when(userService.extendTrial("u-1")).thenReturn(false);

        mockMvc.perform(post("/admin/users/u-1/extend-trial").with(csrf()).with(httpBasic("admin", "admin")))
                .andExpect(flash().attributeExists("flashError"));
    }

    @Test
    void trialAndSubscriptionActionsRequireAuth() throws Exception {
        mockMvc.perform(post("/admin/users/u-1/extend-trial").with(csrf()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/admin/users/u-1/subscription").with(csrf())
                        .param("startMonth", "2027-01").param("endMonth", "2027-03"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/admin/users/u-1/subscription/p-1/delete").with(csrf()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/admin/users/u-1/subscription"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
        verifyNoInteractions(subscriptions);
    }

    @Test
    void subscriptionPageRendersPaymentsForUser() throws Exception {
        KbrUser user = sampleUser("u-1", "a@b.com", KbrUserStatus.APPROVED);
        KbrSubscriptionPayment payment = new KbrSubscriptionPayment(
                "p-1", "u-1", LocalDate.of(2027, 1, 1), LocalDate.of(2027, 4, 1), "cash");
        when(userService.findById("u-1")).thenReturn(Optional.of(user));
        when(subscriptions.findFor("u-1")).thenReturn(List.of(payment));

        mockMvc.perform(get("/admin/users/u-1/subscription").with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users/subscription"))
                .andExpect(model().attribute("user", user))
                .andExpect(model().attribute("payments", List.of(payment)))
                .andExpect(model().attributeExists("access"));
    }

    @Test
    void subscriptionPageReturns404ForUnknownUser() throws Exception {
        when(userService.findById("missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/admin/users/missing/subscription").with(httpBasic("admin", "admin")))
                .andExpect(status().isNotFound());
    }

    @Test
    void recordingAPaymentPassesTheChosenMonthsThrough() throws Exception {
        when(subscriptions.record(anyString(), any(), any(), any())).thenReturn(Optional.empty());

        mockMvc.perform(post("/admin/users/u-1/subscription").with(csrf()).with(httpBasic("admin", "admin"))
                        .param("startMonth", "2027-01")
                        .param("endMonth", "2027-03")
                        .param("note", "bank transfer"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/u-1/subscription"))
                .andExpect(flash().attributeExists("flashMessage"));

        verify(subscriptions).record("u-1", YearMonth.of(2027, 1), YearMonth.of(2027, 3),
                "bank transfer");
    }

    @Test
    void aRefusedPaymentIsReportedAsAnError() throws Exception {
        when(subscriptions.record(anyString(), any(), any(), any()))
                .thenReturn(Optional.of("Those months overlap a period already recorded."));

        mockMvc.perform(post("/admin/users/u-1/subscription").with(csrf()).with(httpBasic("admin", "admin"))
                        .param("startMonth", "2027-01")
                        .param("endMonth", "2027-03"))
                .andExpect(flash().attribute("flashError",
                        "Those months overlap a period already recorded."));
    }

    @Test
    void aMalformedMonthReachesTheServiceAsNull() throws Exception {
        when(subscriptions.record(anyString(), any(), any(), any()))
                .thenReturn(Optional.of("Choose both a start month and an end month."));

        mockMvc.perform(post("/admin/users/u-1/subscription").with(csrf()).with(httpBasic("admin", "admin"))
                        .param("startMonth", "not-a-month")
                        .param("endMonth", "2027-03"))
                .andExpect(status().is3xxRedirection());

        verify(subscriptions).record("u-1", null, YearMonth.of(2027, 3), null);
    }

    @Test
    void deletingAPaymentReturnsToTheSubscriptionPage() throws Exception {
        mockMvc.perform(post("/admin/users/u-1/subscription/p-1/delete").with(csrf())
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/u-1/subscription"));

        verify(subscriptions).delete("p-1");
    }

    private static KbrUser sampleUser(String id, String email, KbrUserStatus status) {
        try {
            KbrUser u = new KbrUser(id, email, "hash");
            u.setStatus(status);
            Field createdAt = KbrUser.class.getDeclaredField("createdAt");
            createdAt.setAccessible(true);
            createdAt.set(u, Instant.parse("2026-01-01T00:00:00Z"));
            return u;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
