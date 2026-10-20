package com.techexpat.site.controller;

import com.techexpat.site.config.KbrAuthFailureHandler;
import com.techexpat.site.config.KbrAuthSuccessHandler;
import com.techexpat.site.config.KbrWebMvcConfig;
import com.techexpat.site.config.PosthogProperties;
import com.techexpat.site.config.WebSecurityConfig;
import com.techexpat.site.model.KbrUser;
import com.techexpat.site.service.DisplayTime;
import com.techexpat.site.service.KbrUserDetailsService;
import com.techexpat.site.service.KbrUserService;
import com.techexpat.site.service.ReceiptService;
import com.techexpat.site.service.RegistrationRateLimiter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(KbrAuthController.class)
@Import({WebSecurityConfig.class, KbrWebMvcConfig.class})
class KbrSetupControllerTest {

    private static final String EMAIL = "owner@example.com";
    private static final String SETUP = "/tool/keep-business-receipt/setup";
    private static final String DASHBOARD = "/tool/keep-business-receipt/dashboard";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    KbrUserService userService;

    @MockitoBean
    RegistrationRateLimiter rateLimiter;

    @MockitoBean
    ReceiptService receiptService;

    @MockitoBean(name = "displayTime")
    DisplayTime displayTime;

    @MockitoBean
    PosthogProperties posthogProperties;

    @MockitoBean
    KbrUserDetailsService kbrUserDetailsService;

    @MockitoBean
    KbrAuthFailureHandler kbrAuthFailureHandler;

    @MockitoBean
    KbrAuthSuccessHandler kbrAuthSuccessHandler;

    @Test
    void setupRequiresSignIn() throws Exception {
        mockMvc.perform(get(SETUP))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tool/keep-business-receipt/sign-in"));
    }

    @Test
    void dashboardRedirectsToSetupWhenBusinessNameMissing() throws Exception {
        when(userService.hasBusinessName(EMAIL)).thenReturn(false);

        mockMvc.perform(get(DASHBOARD).with(kbrUser()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(SETUP));
    }

    @Test
    void dashboardRendersBusinessNameOnceSet() throws Exception {
        when(userService.hasBusinessName(EMAIL)).thenReturn(true);
        when(userService.findByEmail(EMAIL)).thenReturn(Optional.of(userWithBusinessName()));

        mockMvc.perform(get(DASHBOARD).with(kbrUser()))
                .andExpect(status().isOk())
                .andExpect(view().name("tool/keep-business-receipt/dashboard"))
                .andExpect(model().attribute("businessName", "Ocean Foods"));
    }

    @Test
    void setupRendersFormWhenBusinessNameMissing() throws Exception {
        when(userService.hasBusinessName(EMAIL)).thenReturn(false);

        mockMvc.perform(get(SETUP).with(kbrUser()))
                .andExpect(status().isOk())
                .andExpect(view().name("tool/keep-business-receipt/setup"))
                .andExpect(model().attributeExists("form"));
    }

    @Test
    void setupRedirectsToDashboardWhenAlreadySet() throws Exception {
        when(userService.hasBusinessName(EMAIL)).thenReturn(true);

        mockMvc.perform(get(SETUP).with(kbrUser()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(DASHBOARD));
    }

    @Test
    void submitSavesTrimmedNameAndRedirectsToDashboard() throws Exception {
        when(userService.hasBusinessName(EMAIL)).thenReturn(false);

        mockMvc.perform(post(SETUP).with(kbrUser()).with(csrf())
                        .param("businessName", "  Ocean Foods  "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(DASHBOARD));

        verify(userService).saveBusinessName(EMAIL, "Ocean Foods");
    }

    @Test
    void submitRejectsBlankNameWithoutWriting() throws Exception {
        when(userService.hasBusinessName(EMAIL)).thenReturn(false);

        mockMvc.perform(post(SETUP).with(kbrUser()).with(csrf())
                        .param("businessName", "   "))
                .andExpect(status().isOk())
                .andExpect(view().name("tool/keep-business-receipt/setup"))
                .andExpect(model().attributeExists("errors"));

        verify(userService, never()).saveBusinessName(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void submitRejectsOverlongNameWithoutWriting() throws Exception {
        when(userService.hasBusinessName(EMAIL)).thenReturn(false);

        mockMvc.perform(post(SETUP).with(kbrUser()).with(csrf())
                        .param("businessName", "x".repeat(256)))
                .andExpect(status().isOk())
                .andExpect(view().name("tool/keep-business-receipt/setup"))
                .andExpect(model().attributeExists("errors"));

        verify(userService, never()).saveBusinessName(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void submitIgnoredWhenBusinessNameAlreadySet() throws Exception {
        when(userService.hasBusinessName(EMAIL)).thenReturn(true);

        mockMvc.perform(post(SETUP).with(kbrUser()).with(csrf())
                        .param("businessName", "Second Attempt"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(DASHBOARD));

        verify(userService, never()).saveBusinessName(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void submitWithoutCsrfIsForbidden() throws Exception {
        mockMvc.perform(post(SETUP).with(kbrUser()).param("businessName", "Ocean Foods"))
                .andExpect(status().isForbidden());
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor kbrUser() {
        return user(EMAIL).roles("KBR_USER");
    }

    private static KbrUser userWithBusinessName() {
        KbrUser user = new KbrUser("u-1", EMAIL, "hash");
        user.setBusinessName("Ocean Foods");
        return user;
    }
}
