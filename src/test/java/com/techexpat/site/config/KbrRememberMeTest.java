package com.techexpat.site.config;

import com.techexpat.site.controller.KbrAuthController;
import com.techexpat.site.service.DisplayTime;
import com.techexpat.site.service.KbrSubscriptionService;
import com.techexpat.site.service.KbrUserDetailsService;
import com.techexpat.site.service.KbrUserService;
import com.techexpat.site.service.ReceiptService;
import com.techexpat.site.service.RegistrationRateLimiter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;

@WebMvcTest(KbrAuthController.class)
@Import(WebSecurityConfig.class)
class KbrRememberMeTest {

    private static final String SIGN_IN = "/tool/keep-business-receipt/sign-in";
    private static final String COOKIE = "kbr-remember-me";
    private static final String EMAIL = "owner@example.com";
    private static final String PIN = "123456";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    KbrUserDetailsService kbrUserDetailsService;

    @MockitoBean
    KbrUserService userService;

    @MockitoBean
    KbrSubscriptionService subscriptions;

    @MockitoBean
    RegistrationRateLimiter rateLimiter;

    @MockitoBean
    ReceiptService receiptService;

    @MockitoBean(name = "displayTime")
    DisplayTime displayTime;

    @MockitoBean
    PosthogProperties posthogProperties;

    @MockitoBean
    KbrAuthFailureHandler kbrAuthFailureHandler;

    @MockitoBean
    KbrAuthSuccessHandler kbrAuthSuccessHandler;

    @BeforeEach
    void setUp() {
        // A fresh User per call, as the real service does. Returning one shared instance
        // fails: ProviderManager erases its credentials after login, so the remember-me
        // lookup that follows finds a null password and quietly issues no cookie.
        when(kbrUserDetailsService.loadUserByUsername(anyString()))
                .thenAnswer(invocation -> User.withUsername(EMAIL)
                        .password(new BCryptPasswordEncoder().encode(PIN))
                        .authorities(AuthorityUtils.createAuthorityList("ROLE_KBR_USER"))
                        .build());
    }

    @Test
    void tickingRememberMeIssuesTheCookie() throws Exception {
        mockMvc.perform(post(SIGN_IN).with(csrf())
                        .param("username", EMAIL)
                        .param("password", PIN)
                        .param("remember-me", "on"))
                .andExpect(cookie().exists(COOKIE))
                .andExpect(cookie().httpOnly(COOKIE, true))
                .andExpect(cookie().maxAge(COOKIE, 30 * 24 * 60 * 60));
    }

    @Test
    void leavingItUntickedIssuesNoCookie() throws Exception {
        mockMvc.perform(post(SIGN_IN).with(csrf())
                        .param("username", EMAIL)
                        .param("password", PIN))
                .andExpect(cookie().doesNotExist(COOKIE));
    }

    @Test
    void signOutClearsTheCookie() throws Exception {
        mockMvc.perform(post("/tool/keep-business-receipt/sign-out").with(csrf()))
                .andExpect(cookie().maxAge(COOKIE, 0));
    }
}
