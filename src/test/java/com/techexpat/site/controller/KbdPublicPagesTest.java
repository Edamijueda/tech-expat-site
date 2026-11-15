package com.techexpat.site.controller;

import com.techexpat.site.config.KbdAuthFailureHandler;
import com.techexpat.site.config.KbdAuthSuccessHandler;
import com.techexpat.site.config.PosthogProperties;
import com.techexpat.site.config.WebSecurityConfig;
import com.techexpat.site.service.DisplayTime;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.mockito.Mockito.when;

@WebMvcTest(KbdAuthController.class)
@Import(WebSecurityConfig.class)
class KbdPublicPagesTest {

    private static final String BASE = "/tool/keep-business-docs";

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
    void termsIsPublic() throws Exception {
        mockMvc.perform(get(BASE + "/terms"))
                .andExpect(status().isOk())
                .andExpect(view().name("tool/keep-business-docs/terms"));
    }

    @Test
    void privacyIsPublic() throws Exception {
        mockMvc.perform(get(BASE + "/privacy"))
                .andExpect(status().isOk())
                .andExpect(view().name("tool/keep-business-docs/privacy"));
    }

    @Test
    void kbdPagesCarryTheToolsTermsAndPrivacyInTheFooter() throws Exception {
        mockMvc.perform(get(BASE + "/terms"))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString("/tool/keep-business-docs/privacy")))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString("/tool/keep-business-docs/terms")));
    }

    @Test
    void privacySaysWhatIsStored() throws Exception {
        mockMvc.perform(get(BASE + "/privacy"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("business name")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("hello@tech-expat.com")));
    }

    @Test
    void footerLinksToTermsAndPrivacy() throws Exception {
        mockMvc.perform(get(BASE + "/sign-in"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(BASE + "/terms")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(BASE + "/privacy")));
    }

    /** The public funnel is where analytics earns its place. */
    @Test
    void publicToolPagesStillLoadAnalytics() throws Exception {
        when(posthogProperties.key()).thenReturn("phc_test_key");

        mockMvc.perform(get(BASE + "/sign-in"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString("posthog.init")));
    }

    @Test
    void signInOffersRememberMeUntickedByDefault() throws Exception {
        mockMvc.perform(get(BASE + "/sign-in"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"remember-me\"")))
                .andExpect(content().string(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("checked"))));
    }
}
