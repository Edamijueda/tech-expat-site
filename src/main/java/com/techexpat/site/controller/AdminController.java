package com.techexpat.site.controller;

import com.techexpat.site.model.Course;
import com.techexpat.site.model.Order;
import com.techexpat.site.service.CourseCatalog;
import com.techexpat.site.service.OrderService;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final OrderService orderService;
    private final CourseCatalog catalog;

    public AdminController(OrderService orderService, CourseCatalog catalog) {
        this.orderService = orderService;
        this.catalog = catalog;
    }

    @GetMapping("/orders")
    public String listOrders(Model model) {
        model.addAttribute("orders", orderService.findAll());
        return "admin/orders";
    }

    @GetMapping("/testing/{orderId}")
    public String testingOrder(@PathVariable String orderId, Model model) {
        Order order = orderService.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown order"));
        Course course = catalog.findBySlug(order.getCourseSlug()).orElse(null);
        model.addAttribute("order", order);
        model.addAttribute("course", course);
        return "admin/testing";
    }

    @GetMapping("/course-outline")
    public String courseOutline(Model model) {
        model.addAttribute("courses", catalog.all());
        return "admin/course-outline";
    }
}
