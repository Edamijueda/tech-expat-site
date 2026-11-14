package com.techexpat.site.config;

import com.techexpat.site.controller.KbdAuthController;
import com.techexpat.site.service.DisplayTime;
import com.techexpat.site.service.KbdSubscriptionService;
import com.techexpat.site.service.KbdUserDetailsService;
import com.techexpat.site.service.KbdUserService;
import com.techexpat.site.service.BizDocService;
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

@WebMvcTest(KbdAuthController.class)
@Import(WebSecurityConfig.class)
class KbdRememberMeTest {

    private static final String SIGN_IN = "/tool/keep-business-docs/sign-in";
    private static final String COOKIE = "kbd-remember-me";
    private static final String EMAIL = "owner@example.com";
    private static final String PIN = "123456";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    KbdUserDetailsService kbdUserDetailsService;

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
    KbdAuthFailureHandler kbdAuthFailureHandler;

    @MockitoBean
    KbdAuthSuccessHandler kbdAuthSuccessHandler;

    @BeforeEach
    void setUp() {
        // A fresh User per call, as the real service does. Returning one shared instance
        // fails: ProviderManager erases its credentials after login, so the remember-me
        // lookup that follows finds a null password and quietly issues no cookie.
        when(kbdUserDetailsService.loadUserByUsername(anyString()))
                .thenAnswer(invocation -> User.withUsername(EMAIL)
                        .password(new BCryptPasswordEncoder().encode(PIN))
                        .authorities(AuthorityUtils.createAuthorityList("ROLE_KBD_USER"))
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
        mockMvc.perform(post("/tool/keep-business-docs/sign-out").with(csrf()))
                .andExpect(cookie().maxAge(COOKIE, 0));
    }
}
