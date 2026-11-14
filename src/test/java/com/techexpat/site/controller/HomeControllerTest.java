package com.techexpat.site.controller;

import com.techexpat.site.config.KbdAuthFailureHandler;
import com.techexpat.site.config.KbdAuthSuccessHandler;
import com.techexpat.site.config.PosthogProperties;
import com.techexpat.site.config.WebSecurityConfig;
import com.techexpat.site.service.KbdSubscriptionService;
import com.techexpat.site.service.KbdUserDetailsService;
import com.techexpat.site.service.KbdUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(HomeController.class)
@Import(WebSecurityConfig.class)
class HomeControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PosthogProperties posthogProperties;

    @MockitoBean
    KbdUserDetailsService kbdUserDetailsService;

    @MockitoBean
    KbdAuthFailureHandler kbdAuthFailureHandler;

    @MockitoBean
    KbdAuthSuccessHandler kbdAuthSuccessHandler;

    @MockitoBean
    KbdUserService kbdUserService;

    @MockitoBean
    KbdSubscriptionService kbdSubscriptionService;

    @Test
    void homePageReturnsOk() throws Exception {
        mockMvc.perform(get("/"))
               .andExpect(status().isOk());
    }

    @Test
    void homePageRendersIndexTemplate() throws Exception {
        mockMvc.perform(get("/"))
               .andExpect(view().name("index"));
    }

    /** The footer's terms and privacy cover the doc tool only, not the whole site. */
    @Test
    void homePageFooterOmitsTheKbdTermsAndPrivacyLinks() throws Exception {
        mockMvc.perform(get("/"))
               .andExpect(content().string(
                       not(containsString("/tool/keep-business-docs/terms"))))
               .andExpect(content().string(
                       not(containsString("/tool/keep-business-docs/privacy"))));
    }

}
