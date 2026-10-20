package com.techexpat.site.controller;

import com.techexpat.site.config.KbrAuthFailureHandler;
import com.techexpat.site.config.KbrAuthSuccessHandler;
import com.techexpat.site.config.PosthogProperties;
import com.techexpat.site.config.WebSecurityConfig;
import com.techexpat.site.model.KbrUser;
import com.techexpat.site.model.Receipt;
import com.techexpat.site.service.DisplayTime;
import com.techexpat.site.service.KbrUserDetailsService;
import com.techexpat.site.service.KbrUserService;
import com.techexpat.site.service.ReceiptService;
import com.techexpat.site.service.ReceiptService.ReceiptValidationException;

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

@WebMvcTest(KbrReceiptController.class)
@Import(WebSecurityConfig.class)
class KbrReceiptControllerTest {

    private static final String EMAIL = "owner@example.com";
    private static final String USER_ID = "user-1";
    private static final String BASE = "/tool/keep-business-receipt";
    private static final byte[] CONTENT = {1, 2, 3, 4};

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ReceiptService receiptService;

    @MockitoBean
    KbrUserService userService;

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

    @BeforeEach
    void setUp() {
        when(userService.findByEmail(EMAIL)).thenReturn(Optional.of(new KbrUser(USER_ID, EMAIL, "hash")));
        // past the first-run business-name gate, which guards these routes
        when(userService.hasBusinessName(EMAIL)).thenReturn(true);
    }

    @Test
    void uploadRequiresSignIn() throws Exception {
        mockMvc.perform(multipart(BASE + "/receipts").file(jpeg()).param("description", "d").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(BASE + "/sign-in"));
    }

    @Test
    void uploadWithoutCsrfIsForbidden() throws Exception {
        mockMvc.perform(multipart(BASE + "/receipts").file(jpeg())
                        .param("description", "d").with(kbrUser()))
                .andExpect(status().isForbidden());

        verify(receiptService, never()).upload(anyString(), anyString(), any());
    }

    @Test
    void uploadPassesDescriptionAndFileThroughThenRedirects() throws Exception {
        mockMvc.perform(multipart(BASE + "/receipts").file(jpeg())
                        .param("description", "Fish supplier")
                        .with(kbrUser()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(BASE + "/dashboard"))
                .andExpect(flash().attributeExists("infoMessage"));

        verify(receiptService).upload(eq(USER_ID), eq("Fish supplier"), any());
    }

    @Test
    void uploadSurfacesValidationMessageAsFlash() throws Exception {
        when(receiptService.upload(anyString(), anyString(), any()))
                .thenThrow(new ReceiptValidationException("That file is larger than 5 MB."));

        mockMvc.perform(multipart(BASE + "/receipts").file(jpeg())
                        .param("description", "d").with(kbrUser()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(BASE + "/dashboard"))
                .andExpect(flash().attribute("errorMessage", "That file is larger than 5 MB."));
    }

    @Test
    void previewServesFileInline() throws Exception {
        when(receiptService.findOwned("r-1", USER_ID)).thenReturn(Optional.of(receipt("r.jpg")));
        when(receiptService.contentOf(any())).thenReturn(CONTENT);

        mockMvc.perform(get(BASE + "/files/r-1").with(kbrUser()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(CONTENT))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.startsWith("inline")));
    }

    @Test
    void downloadServesFileAsAttachment() throws Exception {
        when(receiptService.findOwned("r-1", USER_ID)).thenReturn(Optional.of(receipt("r.jpg")));
        when(receiptService.contentOf(any())).thenReturn(CONTENT);

        mockMvc.perform(get(BASE + "/files/r-1/download").with(kbrUser()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.containsString("attachment")));
    }

    @Test
    void anotherUsersReceiptIsNotFound() throws Exception {
        when(receiptService.findOwned("r-9", USER_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get(BASE + "/files/r-9").with(kbrUser()))
                .andExpect(status().isNotFound());

        verify(receiptService, never()).contentOf(any());
    }

    @Test
    void deviceSuppliedFilenameIsStrippedToABareName() throws Exception {
        when(receiptService.findOwned("r-1", USER_ID))
                .thenReturn(Optional.of(receipt("../../etc/passwd")));
        when(receiptService.contentOf(any())).thenReturn(CONTENT);

        mockMvc.perform(get(BASE + "/files/r-1/download").with(kbrUser()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.containsString("passwd")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(".."))));
    }

    @Test
    void deleteConfirmPageRenders() throws Exception {
        when(receiptService.findOwned("r-1", USER_ID)).thenReturn(Optional.of(receipt("r.jpg")));

        mockMvc.perform(get(BASE + "/receipts/r-1/delete").with(kbrUser()))
                .andExpect(status().isOk())
                .andExpect(view().name("tool/keep-business-receipt/delete-receipt"));
    }

    @Test
    void deleteRemovesReceiptAndRedirects() throws Exception {
        Receipt receipt = receipt("r.jpg");
        when(receiptService.findOwned("r-1", USER_ID)).thenReturn(Optional.of(receipt));

        mockMvc.perform(post(BASE + "/receipts/r-1/delete").with(kbrUser()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(BASE + "/dashboard"));

        verify(receiptService).delete(receipt);
    }

    @Test
    void deletingAnotherUsersReceiptIsNotFound() throws Exception {
        when(receiptService.findOwned("r-9", USER_ID)).thenReturn(Optional.empty());

        mockMvc.perform(post(BASE + "/receipts/r-9/delete").with(kbrUser()).with(csrf()))
                .andExpect(status().isNotFound());

        verify(receiptService, never()).delete(any());
    }

    private static RequestPostProcessor kbrUser() {
        return user(EMAIL).roles("KBR_USER");
    }

    private static MockMultipartFile jpeg() {
        return new MockMultipartFile("file", "r.jpg", "image/jpeg", CONTENT);
    }

    private static Receipt receipt(String originalFilename) {
        return new Receipt("r-1", USER_ID, "Fish supplier", USER_ID + "/r-1",
                originalFilename, "image/jpeg", CONTENT.length);
    }
}
