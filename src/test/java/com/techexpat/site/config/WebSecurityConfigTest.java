package com.techexpat.site.config;

import com.techexpat.site.controller.HomeController;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HomeController.class)
@Import(WebSecurityConfig.class)
class WebSecurityConfigTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PosthogProperties posthogProperties;

    @Test
    void adminRouteWithoutCredentialsReturns401() throws Exception {
        mockMvc.perform(get("/admin/anything"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminRouteWithWrongCredentialsReturns401() throws Exception {
        mockMvc.perform(get("/admin/anything").with(httpBasic("admin", "wrong")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminRouteWithValidCredentialsPassesAuth() throws Exception {
        mockMvc.perform(get("/admin/anything").with(httpBasic("admin", "admin")))
                .andExpect(status().isNotFound());
    }

    @Test
    void publicRouteRemainsOpenWithoutCredentials() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());
    }
}
