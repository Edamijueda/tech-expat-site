package com.techexpat.site.controller;

import com.techexpat.site.config.PosthogProperties;
import com.techexpat.site.config.WebSecurityConfig;
import com.techexpat.site.service.IpnSignatureVerifier;
import com.techexpat.site.service.OrderService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NowpaymentsWebhookController.class)
@Import(WebSecurityConfig.class)
class NowpaymentsWebhookControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    IpnSignatureVerifier verifier;

    @MockitoBean
    OrderService orderService;

    @MockitoBean
    PosthogProperties posthogProperties;

    private static final String PAID_BODY = """
            {"order_id":"order-123","payment_id":"pay-456","payment_status":"finished"}""";

    private static final String PENDING_BODY = """
            {"order_id":"order-123","payment_id":"pay-456","payment_status":"waiting"}""";

    @Test
    void validSignatureAndPaidStatusMarksOrderPaid() throws Exception {
        when(verifier.verify(any(), eq("good-sig"))).thenReturn(true);
        when(orderService.markPaidIfPending("order-123", "pay-456")).thenReturn(true);

        mockMvc.perform(post("/webhooks/nowpayments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("x-nowpayments-sig", "good-sig")
                        .content(PAID_BODY))
                .andExpect(status().isOk());

        verify(orderService).markPaidIfPending("order-123", "pay-456");
    }

    @Test
    void invalidSignatureReturns400AndSkipsService() throws Exception {
        when(verifier.verify(any(), any())).thenReturn(false);

        mockMvc.perform(post("/webhooks/nowpayments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("x-nowpayments-sig", "bad-sig")
                        .content(PAID_BODY))
                .andExpect(status().isBadRequest());

        verify(orderService, never()).markPaidIfPending(any(), any());
    }

    @Test
    void missingSignatureHeaderReturns400() throws Exception {
        when(verifier.verify(any(), eq(null))).thenReturn(false);

        mockMvc.perform(post("/webhooks/nowpayments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAID_BODY))
                .andExpect(status().isBadRequest());

        verify(orderService, never()).markPaidIfPending(any(), any());
    }

    @Test
    void nonTerminalStatusDoesNotMarkPaid() throws Exception {
        when(verifier.verify(any(), eq("good-sig"))).thenReturn(true);

        mockMvc.perform(post("/webhooks/nowpayments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("x-nowpayments-sig", "good-sig")
                        .content(PENDING_BODY))
                .andExpect(status().isOk());

        verify(orderService, never()).markPaidIfPending(any(), any());
    }

    @Test
    void unparseablePayloadReturns400() throws Exception {
        when(verifier.verify(any(), eq("good-sig"))).thenReturn(true);

        mockMvc.perform(post("/webhooks/nowpayments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("x-nowpayments-sig", "good-sig")
                        .content("not json at all"))
                .andExpect(status().isBadRequest());

        verify(orderService, never()).markPaidIfPending(any(), any());
    }

    @Test
    void unknownOrderStillReturns200() throws Exception {
        when(verifier.verify(any(), eq("good-sig"))).thenReturn(true);
        when(orderService.markPaidIfPending(any(), any()))
                .thenThrow(new IllegalArgumentException("Unknown order: order-123"));

        mockMvc.perform(post("/webhooks/nowpayments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("x-nowpayments-sig", "good-sig")
                        .content(PAID_BODY))
                .andExpect(status().isOk());
    }
}
