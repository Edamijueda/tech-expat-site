package com.techexpat.site.controller;

import com.techexpat.site.config.KbdAuthFailureHandler;
import com.techexpat.site.config.KbdAuthSuccessHandler;
import com.techexpat.site.config.PosthogProperties;
import com.techexpat.site.config.WebSecurityConfig;
import com.techexpat.site.model.KbdUser;
import com.techexpat.site.model.BizDoc;
import com.techexpat.site.service.DisplayTime;
import com.techexpat.site.service.KbdAccessStatus;
import com.techexpat.site.service.KbdSubscriptionService;
import com.techexpat.site.service.KbdUserDetailsService;
import com.techexpat.site.service.KbdUserService;
import com.techexpat.site.service.BizDocService;
import com.techexpat.site.service.BizDocService.BizDocValidationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(KbdBizDocController.class)
@Import(WebSecurityConfig.class)
class KbdBizDocControllerTest {

    private static final String EMAIL = "owner@example.com";
    private static final String USER_ID = "user-1";
    private static final String BASE = "/tool/keep-business-docs";
    private static final byte[] CONTENT = {1, 2, 3, 4};

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    BizDocService docService;

    @MockitoBean
    KbdUserService userService;

    @MockitoBean
    KbdSubscriptionService subscriptions;

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

    @BeforeEach
    void setUp() {
        when(userService.findByEmail(EMAIL)).thenReturn(Optional.of(new KbdUser(USER_ID, EMAIL, "hash")));
        // past the first-run business-name gate, which guards these routes
        when(userService.hasBusinessName(EMAIL)).thenReturn(true);
        // and inside a live trial, which is what makes these routes writable
        when(subscriptions.statusFor(any())).thenReturn(new KbdAccessStatus(true, 4, 30, 27, null));
    }

    @Test
    void uploadRequiresSignIn() throws Exception {
        mockMvc.perform(multipart(BASE + "/docs").file(jpeg()).param("description", "d").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(BASE + "/sign-in"));
    }

    @Test
    void uploadWithoutCsrfIsForbidden() throws Exception {
        mockMvc.perform(multipart(BASE + "/docs").file(jpeg())
                        .param("description", "d").with(kbdUser()))
                .andExpect(status().isForbidden());

        verify(docService, never()).upload(any(KbdUser.class), anyString(), any());
    }

    @Test
    void uploadPassesDescriptionAndFileThroughThenRedirects() throws Exception {
        mockMvc.perform(multipart(BASE + "/docs").file(jpeg())
                        .param("description", "Fish supplier")
                        .with(kbdUser()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(BASE + "/dashboard"))
                .andExpect(flash().attributeExists("infoMessage"));

        verify(docService).upload(argThat(u -> USER_ID.equals(u.getId())),
                eq("Fish supplier"), any());
    }

    @Test
    void uploadSurfacesValidationMessageAsFlash() throws Exception {
        when(docService.upload(any(KbdUser.class), anyString(), any()))
                .thenThrow(new BizDocValidationException("That file is larger than 5 MB."));

        mockMvc.perform(multipart(BASE + "/docs").file(jpeg())
                        .param("description", "d").with(kbdUser()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(BASE + "/dashboard"))
                .andExpect(flash().attribute("errorMessage", "That file is larger than 5 MB."));
    }

    @Test
    void previewServesFileInline() throws Exception {
        when(docService.findOwned("r-1", USER_ID)).thenReturn(Optional.of(doc("r.jpg")));
        when(docService.contentOf(any())).thenReturn(CONTENT);

        mockMvc.perform(get(BASE + "/files/r-1").with(kbdUser()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(CONTENT))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.startsWith("inline")));
    }

    @Test
    void downloadServesFileAsAttachment() throws Exception {
        when(docService.findOwned("r-1", USER_ID)).thenReturn(Optional.of(doc("r.jpg")));
        when(docService.contentOf(any())).thenReturn(CONTENT);

        mockMvc.perform(get(BASE + "/files/r-1/download").with(kbdUser()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.containsString("attachment")));
    }

    @Test
    void anotherUsersBizDocIsNotFound() throws Exception {
        when(docService.findOwned("r-9", USER_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get(BASE + "/files/r-9").with(kbdUser()))
                .andExpect(status().isNotFound());

        verify(docService, never()).contentOf(any());
    }

    @Test
    void deviceSuppliedFilenameIsStrippedToABareName() throws Exception {
        when(docService.findOwned("r-1", USER_ID))
                .thenReturn(Optional.of(doc("../../etc/passwd")));
        when(docService.contentOf(any())).thenReturn(CONTENT);

        mockMvc.perform(get(BASE + "/files/r-1/download").with(kbdUser()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.containsString("passwd")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(".."))));
    }

    @Test
    void deleteConfirmPageRenders() throws Exception {
        when(docService.findOwned("r-1", USER_ID)).thenReturn(Optional.of(doc("r.jpg")));

        mockMvc.perform(get(BASE + "/docs/r-1/delete").with(kbdUser()))
                .andExpect(status().isOk())
                .andExpect(view().name("tool/keep-business-docs/delete-doc"));
    }

    @Test
    void deleteRemovesBizDocAndRedirects() throws Exception {
        BizDoc doc = doc("r.jpg");
        when(docService.findOwned("r-1", USER_ID)).thenReturn(Optional.of(doc));

        mockMvc.perform(post(BASE + "/docs/r-1/delete").with(kbdUser()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(BASE + "/dashboard"));

        verify(docService).delete(doc);
    }

    @Test
    void deletingAnotherUsersBizDocIsNotFound() throws Exception {
        when(docService.findOwned("r-9", USER_ID)).thenReturn(Optional.empty());

        mockMvc.perform(post(BASE + "/docs/r-9/delete").with(kbdUser()).with(csrf()))
                .andExpect(status().isNotFound());

        verify(docService, never()).delete(any());
    }

    @Test
    void readOnlyAccountCannotUpload() throws Exception {
        givenReadOnly();

        mockMvc.perform(multipart(BASE + "/docs").file(jpeg())
                        .param("description", "Fish supplier")
                        .with(kbdUser()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(BASE + "/dashboard"));

        verify(docService, never()).upload(any(KbdUser.class), anyString(), any());
    }

    @Test
    void readOnlyAccountCannotDelete() throws Exception {
        givenReadOnly();
        when(docService.findOwned("r-1", USER_ID)).thenReturn(Optional.of(doc("r.jpg")));

        mockMvc.perform(get(BASE + "/docs/r-1/delete").with(kbdUser()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(BASE + "/dashboard"));
        mockMvc.perform(post(BASE + "/docs/r-1/delete").with(kbdUser()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(BASE + "/dashboard"));

        verify(docService, never()).delete(any());
    }

    /** The point of read-only: what the user already kept stays reachable. */
    @Test
    void readOnlyAccountCanStillPreviewAndDownload() throws Exception {
        givenReadOnly();
        when(docService.findOwned("r-1", USER_ID)).thenReturn(Optional.of(doc("r.jpg")));
        when(docService.contentOf(any())).thenReturn(CONTENT);

        mockMvc.perform(get(BASE + "/files/r-1").with(kbdUser()))
                .andExpect(status().isOk())
                .andExpect(content().bytes(CONTENT));
        mockMvc.perform(get(BASE + "/files/r-1/download").with(kbdUser()))
                .andExpect(status().isOk())
                .andExpect(content().bytes(CONTENT));
    }

    private void givenReadOnly() {
        when(subscriptions.statusFor(any())).thenReturn(new KbdAccessStatus(true, 31, 30, 0, null));
    }

    private static RequestPostProcessor kbdUser() {
        return user(EMAIL).roles("KBD_USER");
    }

    private static MockMultipartFile jpeg() {
        return new MockMultipartFile("file", "r.jpg", "image/jpeg", CONTENT);
    }

    private static BizDoc doc(String originalFilename) {
        return new BizDoc("r-1", USER_ID, "Fish supplier", USER_ID + "/r-1",
                originalFilename, "image/jpeg", CONTENT.length);
    }
}
