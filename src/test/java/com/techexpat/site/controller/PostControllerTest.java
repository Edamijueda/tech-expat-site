package com.techexpat.site.controller;

import com.techexpat.site.config.PosthogProperties;
import com.techexpat.site.config.WebSecurityConfig;
import com.techexpat.site.model.Post;
import com.techexpat.site.model.Section;
import com.techexpat.site.service.PostService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(PostController.class)
@Import(WebSecurityConfig.class)
class PostControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PosthogProperties posthogProperties;

    @MockitoBean
    PostService postService;

    private static final Post SAMPLE = new Post(
            "known", 1, "Known Post", "Tobi Omorubore",
            LocalDate.of(2026, 1, 1),
            "desc", "<p>body</p>", 1, 30, null,
            Set.of(Section.RESEARCH));

    @Test
    void sectionIndexReturnsOkAndExposesPostsList() throws Exception {
        when(postService.findBySection(Section.RESEARCH)).thenReturn(List.of(SAMPLE));

        mockMvc.perform(get("/research"))
               .andExpect(status().isOk())
               .andExpect(view().name("posts/index"))
               .andExpect(model().attributeExists("posts"))
               .andExpect(model().attribute("section", Section.RESEARCH));
    }

    @Test
    void sectionPostReturnsOkForKnownSlug() throws Exception {
        when(postService.findBySectionAndSlug(Section.RESEARCH, "known")).thenReturn(Optional.of(SAMPLE));

        mockMvc.perform(get("/research/known"))
               .andExpect(status().isOk())
               .andExpect(view().name("posts/post"))
               .andExpect(model().attribute("post", SAMPLE))
               .andExpect(model().attribute("section", Section.RESEARCH));
    }

    @Test
    void sectionPostReturnsNotFoundForUnknownSlug() throws Exception {
        when(postService.findBySectionAndSlug(Section.RESEARCH, "nope")).thenReturn(Optional.empty());

        mockMvc.perform(get("/research/nope"))
               .andExpect(status().isNotFound());
    }

    @Test
    void unknownSectionReturnsNotFound() throws Exception {
        mockMvc.perform(get("/marketing"))
               .andExpect(status().isNotFound());
    }
}
