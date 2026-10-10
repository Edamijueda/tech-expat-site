package com.techexpat.site.controller;

import com.techexpat.site.config.PosthogProperties;
import com.techexpat.site.config.WebSecurityConfig;
import com.techexpat.site.model.Course;
import com.techexpat.site.model.Order;
import com.techexpat.site.service.CourseCatalog;
import com.techexpat.site.service.OrderService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AdminController.class)
@Import(WebSecurityConfig.class)
class AdminControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    OrderService orderService;

    @MockitoBean
    CourseCatalog catalog;

    @MockitoBean
    PosthogProperties posthogProperties;

    @Test
    void listOrdersWithoutAuthReturns401() throws Exception {
        mockMvc.perform(get("/admin/orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listOrdersWithAuthRendersOrdersModel() throws Exception {
        Order order = new Order("order-1", "practical-sql", "a@b.com", new BigDecimal("29.00"));
        when(orderService.findAll()).thenReturn(List.of(order));

        mockMvc.perform(get("/admin/orders").with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/orders"))
                .andExpect(model().attribute("orders", List.of(order)));
    }

    @Test
    void testingOrderWithoutAuthReturns401() throws Exception {
        mockMvc.perform(get("/admin/testing/order-1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testingOrderWithAuthRendersOrderAndCourse() throws Exception {
        Order order = new Order("order-1", "practical-sql", "a@b.com", new BigDecimal("29.00"));
        Course course = new Course("practical-sql", "Practical SQL", new BigDecimal("29.00"));
        when(orderService.findById("order-1")).thenReturn(Optional.of(order));
        when(catalog.findBySlug("practical-sql")).thenReturn(Optional.of(course));

        mockMvc.perform(get("/admin/testing/order-1").with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/testing"))
                .andExpect(model().attribute("order", order))
                .andExpect(model().attribute("course", course));
    }

    @Test
    void testingUnknownOrderReturns404() throws Exception {
        when(orderService.findById("missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/admin/testing/missing").with(httpBasic("admin", "admin")))
                .andExpect(status().isNotFound());
    }

    @Test
    void courseOutlineWithoutAuthReturns401() throws Exception {
        mockMvc.perform(get("/admin/course-outline"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void courseOutlineWithAuthRendersCoursesModel() throws Exception {
        Course course = new Course("practical-sql", "Practical SQL", new BigDecimal("29.00"));
        when(catalog.all()).thenReturn(List.of(course));

        mockMvc.perform(get("/admin/course-outline").with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/course-outline"))
                .andExpect(model().attribute("courses", List.of(course)));
    }
}
