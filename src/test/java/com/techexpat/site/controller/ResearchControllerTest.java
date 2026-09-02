package com.techexpat.site.controller;

import com.techexpat.site.config.PosthogProperties;
import com.techexpat.site.config.WebSecurityConfig;
import com.techexpat.site.model.ResearchPost;
import com.techexpat.site.service.ResearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(ResearchController.class)
@Import(WebSecurityConfig.class)
class ResearchControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PosthogProperties posthogProperties;

    @MockitoBean
    ResearchService researchService;

    private static final ResearchPost SAMPLE = new ResearchPost(
            "known", 1, "Known Post", "Tobi Omorubore",
            LocalDate.of(2026, 1, 1),
            "desc", "<p>body</p>", 1, 30, null);

    @Test
    void researchIndexReturnsOkAndExposesPostsList() throws Exception {
        when(researchService.findAll()).thenReturn(List.of(SAMPLE));

        mockMvc.perform(get("/research"))
               .andExpect(status().isOk())
               .andExpect(view().name("research/index"))
               .andExpect(model().attributeExists("posts"));
    }

    @Test
    void researchPostReturnsOkForKnownSlug() throws Exception {
        when(researchService.findBySlug("known")).thenReturn(Optional.of(SAMPLE));

        mockMvc.perform(get("/research/known"))
               .andExpect(status().isOk())
               .andExpect(view().name("research/post"))
               .andExpect(model().attribute("post", SAMPLE));
    }

    @Test
    void researchPostReturnsNotFoundForUnknownSlug() throws Exception {
        when(researchService.findBySlug("nope")).thenReturn(Optional.empty());

        mockMvc.perform(get("/research/nope"))
               .andExpect(status().isNotFound());
    }
}
