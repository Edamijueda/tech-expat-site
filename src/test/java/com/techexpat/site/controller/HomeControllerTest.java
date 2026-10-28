package com.techexpat.site.controller;

import com.techexpat.site.config.KbrAuthFailureHandler;
import com.techexpat.site.config.KbrAuthSuccessHandler;
import com.techexpat.site.config.PosthogProperties;
import com.techexpat.site.config.WebSecurityConfig;
import com.techexpat.site.service.KbrSubscriptionService;
import com.techexpat.site.service.KbrUserDetailsService;
import com.techexpat.site.service.KbrUserService;
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
    KbrUserDetailsService kbrUserDetailsService;

    @MockitoBean
    KbrAuthFailureHandler kbrAuthFailureHandler;

    @MockitoBean
    KbrAuthSuccessHandler kbrAuthSuccessHandler;

    @MockitoBean
    KbrUserService kbrUserService;

    @MockitoBean
    KbrSubscriptionService kbrSubscriptionService;

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

    /** The footer's terms and privacy cover the receipt tool only, not the whole site. */
    @Test
    void homePageFooterOmitsTheKbrTermsAndPrivacyLinks() throws Exception {
        mockMvc.perform(get("/"))
               .andExpect(content().string(
                       not(containsString("/tool/keep-business-receipt/terms"))))
               .andExpect(content().string(
                       not(containsString("/tool/keep-business-receipt/privacy"))));
    }

}
