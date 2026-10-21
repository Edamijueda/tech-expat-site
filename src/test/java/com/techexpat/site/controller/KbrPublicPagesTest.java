package com.techexpat.site.controller;

import com.techexpat.site.config.KbrAuthFailureHandler;
import com.techexpat.site.config.KbrAuthSuccessHandler;
import com.techexpat.site.config.PosthogProperties;
import com.techexpat.site.config.WebSecurityConfig;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(KbrAuthController.class)
@Import(WebSecurityConfig.class)
class KbrPublicPagesTest {

    private static final String BASE = "/tool/keep-business-receipt";

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
    void termsIsPublic() throws Exception {
        mockMvc.perform(get(BASE + "/terms"))
                .andExpect(status().isOk())
                .andExpect(view().name("tool/keep-business-receipt/terms"));
    }

    @Test
    void privacyIsPublic() throws Exception {
        mockMvc.perform(get(BASE + "/privacy"))
                .andExpect(status().isOk())
                .andExpect(view().name("tool/keep-business-receipt/privacy"));
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

    @Test
    void signInOffersRememberMeUntickedByDefault() throws Exception {
        mockMvc.perform(get(BASE + "/sign-in"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"remember-me\"")))
                .andExpect(content().string(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("checked"))));
    }
}
