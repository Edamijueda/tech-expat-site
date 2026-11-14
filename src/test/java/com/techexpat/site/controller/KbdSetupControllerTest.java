package com.techexpat.site.controller;

import com.techexpat.site.config.KbdAuthFailureHandler;
import com.techexpat.site.config.KbdAuthSuccessHandler;
import com.techexpat.site.config.KbdWebMvcConfig;
import com.techexpat.site.config.PosthogProperties;
import com.techexpat.site.config.WebSecurityConfig;
import com.techexpat.site.model.KbdUser;
import com.techexpat.site.service.DisplayTime;
import com.techexpat.site.service.KbdAccessStatus;
import com.techexpat.site.service.KbdSubscriptionService;
import com.techexpat.site.service.KbdUserDetailsService;
import com.techexpat.site.service.KbdUserService;
import com.techexpat.site.service.BizDocService;
import com.techexpat.site.service.RegistrationRateLimiter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(KbdAuthController.class)
@Import({WebSecurityConfig.class, KbdWebMvcConfig.class})
class KbdSetupControllerTest {

    private static final String EMAIL = "owner@example.com";
    private static final String SETUP = "/tool/keep-business-docs/setup";
    private static final String DASHBOARD = "/tool/keep-business-docs/dashboard";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    KbdUserService userService;

    @MockitoBean
    KbdSubscriptionService subscriptions;

    @MockitoBean
    RegistrationRateLimiter rateLimiter;

    @MockitoBean
    BizDocService docService;

    @MockitoBean(name = "displayTime")
    DisplayTime displayTime;

    @MockitoBean
    PosthogProperties posthogProperties;

    @MockitoBean
    KbdUserDetailsService kbdUserDetailsService;

    @MockitoBean
    KbdAuthFailureHandler kbdAuthFailureHandler;

    @MockitoBean
    KbdAuthSuccessHandler kbdAuthSuccessHandler;

    @Test
    void setupRequiresSignIn() throws Exception {
        mockMvc.perform(get(SETUP))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tool/keep-business-docs/sign-in"));
    }

    @Test
    void dashboardRedirectsToSetupWhenBusinessNameMissing() throws Exception {
        when(userService.hasBusinessName(EMAIL)).thenReturn(false);

        mockMvc.perform(get(DASHBOARD).with(kbdUser()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(SETUP));
    }

    @Test
    void dashboardShowsTheTrialCounter() throws Exception {
        when(userService.hasBusinessName(EMAIL)).thenReturn(true);
        when(userService.findByEmail(EMAIL)).thenReturn(Optional.of(userWithBusinessName()));
        when(subscriptions.statusFor(any()))
                .thenReturn(new KbdAccessStatus(true, 4, 30, 27, null));

        mockMvc.perform(get(DASHBOARD).with(kbdUser()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("4/30")))
                .andExpect(content().string(containsString("name=\"file\"")));
    }

    /** Past the trial the dashboard still renders — it just stops offering the form. */
    @Test
    void readOnlyDashboardOffersTheSubscriptionEmailInsteadOfTheUploadForm() throws Exception {
        when(userService.hasBusinessName(EMAIL)).thenReturn(true);
        when(userService.findByEmail(EMAIL)).thenReturn(Optional.of(userWithBusinessName()));
        when(subscriptions.statusFor(any()))
                .thenReturn(new KbdAccessStatus(true, 31, 30, 0, null));

        mockMvc.perform(get(DASHBOARD).with(kbdUser()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("hello@tech-expat.com")))
                .andExpect(content().string(not(containsString("name=\"file\""))));
    }

    @Test
    void dashboardRendersBusinessNameOnceSet() throws Exception {
        when(userService.hasBusinessName(EMAIL)).thenReturn(true);
        when(userService.findByEmail(EMAIL)).thenReturn(Optional.of(userWithBusinessName()));

        mockMvc.perform(get(DASHBOARD).with(kbdUser()))
                .andExpect(status().isOk())
                .andExpect(view().name("tool/keep-business-docs/dashboard"))
                .andExpect(model().attribute("businessName", "Ocean Foods"));
    }

    @Test
    void setupRendersFormWhenBusinessNameMissing() throws Exception {
        when(userService.hasBusinessName(EMAIL)).thenReturn(false);

        mockMvc.perform(get(SETUP).with(kbdUser()))
                .andExpect(status().isOk())
                .andExpect(view().name("tool/keep-business-docs/setup"))
                .andExpect(model().attributeExists("form"));
    }

    @Test
    void setupRedirectsToDashboardWhenAlreadySet() throws Exception {
        when(userService.hasBusinessName(EMAIL)).thenReturn(true);

        mockMvc.perform(get(SETUP).with(kbdUser()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(DASHBOARD));
    }

    @Test
    void submitSavesTrimmedNameAndRedirectsToDashboard() throws Exception {
        when(userService.hasBusinessName(EMAIL)).thenReturn(false);

        mockMvc.perform(post(SETUP).with(kbdUser()).with(csrf())
                        .param("businessName", "  Ocean Foods  "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(DASHBOARD));

        verify(userService).saveBusinessName(EMAIL, "Ocean Foods");
    }

    @Test
    void submitRejectsBlankNameWithoutWriting() throws Exception {
        when(userService.hasBusinessName(EMAIL)).thenReturn(false);

        mockMvc.perform(post(SETUP).with(kbdUser()).with(csrf())
                        .param("businessName", "   "))
                .andExpect(status().isOk())
                .andExpect(view().name("tool/keep-business-docs/setup"))
                .andExpect(model().attributeExists("errors"));

        verify(userService, never()).saveBusinessName(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void submitRejectsOverlongNameWithoutWriting() throws Exception {
        when(userService.hasBusinessName(EMAIL)).thenReturn(false);

        mockMvc.perform(post(SETUP).with(kbdUser()).with(csrf())
                        .param("businessName", "x".repeat(256)))
                .andExpect(status().isOk())
                .andExpect(view().name("tool/keep-business-docs/setup"))
                .andExpect(model().attributeExists("errors"));

        verify(userService, never()).saveBusinessName(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void submitIgnoredWhenBusinessNameAlreadySet() throws Exception {
        when(userService.hasBusinessName(EMAIL)).thenReturn(true);

        mockMvc.perform(post(SETUP).with(kbdUser()).with(csrf())
                        .param("businessName", "Second Attempt"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(DASHBOARD));

        verify(userService, never()).saveBusinessName(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void submitWithoutCsrfIsForbidden() throws Exception {
        mockMvc.perform(post(SETUP).with(kbdUser()).param("businessName", "Ocean Foods"))
                .andExpect(status().isForbidden());
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor kbdUser() {
        return user(EMAIL).roles("KBD_USER");
    }

    private static KbdUser userWithBusinessName() {
        KbdUser user = new KbdUser("u-1", EMAIL, "hash");
        user.setBusinessName("Ocean Foods");
        return user;
    }
}
