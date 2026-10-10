package com.techexpat.site.controller;

import com.techexpat.site.config.PosthogProperties;
import com.techexpat.site.config.WebSecurityConfig;
import com.techexpat.site.model.Course;
import com.techexpat.site.model.Order;
import com.techexpat.site.service.CourseCatalog;
import com.techexpat.site.service.NowPaymentsClient;
import com.techexpat.site.service.NowPaymentsClient.InvoiceResponse;
import com.techexpat.site.service.OrderService;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(CheckoutController.class)
@Import(WebSecurityConfig.class)
class CheckoutControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CourseCatalog catalog;

    @MockitoBean
    OrderService orderService;

    @MockitoBean
    NowPaymentsClient nowpayments;

    @MockitoBean
    PosthogProperties posthogProperties;

    private final Course course = new Course("practical-sql", "Practical SQL", new BigDecimal("29.00"));

    @Test
    void getConfirmRendersCoursePage() throws Exception {
        when(catalog.findBySlug("practical-sql")).thenReturn(Optional.of(course));

        mockMvc.perform(get("/checkout/practical-sql"))
                .andExpect(status().isOk())
                .andExpect(view().name("checkout/confirm"))
                .andExpect(model().attribute("course", course));
    }

    @Test
    void getConfirmReturns404ForUnknownSlug() throws Exception {
        when(catalog.findBySlug("nope")).thenReturn(Optional.empty());

        mockMvc.perform(get("/checkout/nope"))
                .andExpect(status().isNotFound());
    }

    @Test
    void postInitiateCreatesOrderAndRedirectsToInvoiceUrl() throws Exception {
        when(catalog.findBySlug("practical-sql")).thenReturn(Optional.of(course));
        Order order = new Order("order-123", "practical-sql", "buyer@example.com", new BigDecimal("29.00"));
        when(orderService.create(eq("practical-sql"), eq("buyer@example.com"), any())).thenReturn(order);
        when(nowpayments.createInvoice(any(), any()))
                .thenReturn(new InvoiceResponse("inv_abc", "https://nowpayments.io/payment/?iid=inv_abc"));

        mockMvc.perform(post("/checkout/practical-sql").param("email", "buyer@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://nowpayments.io/payment/?iid=inv_abc"));

        verify(orderService).attachInvoice("order-123", "inv_abc");
    }

    @Test
    void postInitiateRedirectsBackWhenEmailMissingAtSign() throws Exception {
        when(catalog.findBySlug("practical-sql")).thenReturn(Optional.of(course));

        mockMvc.perform(post("/checkout/practical-sql").param("email", "notanemail"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/checkout/practical-sql?error=invalid-email"));
    }

    @Test
    void postInitiateTrimsEmailBeforePersisting() throws Exception {
        when(catalog.findBySlug("practical-sql")).thenReturn(Optional.of(course));
        Order order = new Order("order-xyz", "practical-sql", "buyer@example.com", new BigDecimal("29.00"));
        when(orderService.create(any(), any(), any())).thenReturn(order);
        when(nowpayments.createInvoice(any(), any()))
                .thenReturn(new InvoiceResponse("inv_x", "https://nowpayments.io/x"));

        mockMvc.perform(post("/checkout/practical-sql").param("email", "  buyer@example.com  "))
                .andExpect(status().is3xxRedirection());

        ArgumentCaptor<String> email = ArgumentCaptor.forClass(String.class);
        verify(orderService).create(eq("practical-sql"), email.capture(), any());
        org.assertj.core.api.Assertions.assertThat(email.getValue()).isEqualTo("buyer@example.com");
    }

    @Test
    void getOrderStatusRendersOrder() throws Exception {
        Order order = new Order("order-123", "practical-sql", "buyer@example.com", new BigDecimal("29.00"));
        when(orderService.findById("order-123")).thenReturn(Optional.of(order));

        mockMvc.perform(get("/orders/order-123"))
                .andExpect(status().isOk())
                .andExpect(view().name("checkout/order-status"))
                .andExpect(model().attribute("order", order));
    }

    @Test
    void getOrderStatusPassesResultQueryParamToModel() throws Exception {
        Order order = new Order("order-123", "practical-sql", "buyer@example.com", new BigDecimal("29.00"));
        when(orderService.findById("order-123")).thenReturn(Optional.of(order));

        mockMvc.perform(get("/orders/order-123").param("result", "success"))
                .andExpect(model().attribute("result", is("success")));
    }

    @Test
    void getOrderStatusReturns404ForUnknownOrder() throws Exception {
        when(orderService.findById("nope")).thenReturn(Optional.empty());

        mockMvc.perform(get("/orders/nope"))
                .andExpect(status().isNotFound());
    }
}
