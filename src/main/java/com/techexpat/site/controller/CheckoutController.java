package com.techexpat.site.controller;

import com.techexpat.site.model.Course;
import com.techexpat.site.model.Order;
import com.techexpat.site.service.CourseCatalog;
import com.techexpat.site.service.NowPaymentsClient;
import com.techexpat.site.service.NowPaymentsClient.InvoiceResponse;
import com.techexpat.site.service.OrderService;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class CheckoutController {

    private final CourseCatalog catalog;
    private final OrderService orderService;
    private final NowPaymentsClient nowpayments;

    public CheckoutController(CourseCatalog catalog, OrderService orderService, NowPaymentsClient nowpayments) {
        this.catalog = catalog;
        this.orderService = orderService;
        this.nowpayments = nowpayments;
    }

    @GetMapping("/checkout/{slug}")
    public String confirm(@PathVariable String slug,
                          @RequestParam(required = false) String error,
                          Model model) {
        Course course = requireCourse(slug);
        model.addAttribute("course", course);
        model.addAttribute("error", error);
        return "checkout/confirm";
    }

    @PostMapping("/checkout/{slug}")
    public String initiate(@PathVariable String slug, @RequestParam String email) {
        Course course = requireCourse(slug);

        if (!isValidEmail(email)) {
            return "redirect:/checkout/" + slug + "?error=invalid-email";
        }

        Order order = orderService.create(slug, email.trim(), course.priceUsd());
        InvoiceResponse invoice = nowpayments.createInvoice(order, course);
        orderService.attachInvoice(order.getId(), invoice.invoiceId());
        return "redirect:" + invoice.invoiceUrl();
    }

    @GetMapping("/orders/{orderId}")
    public String status(@PathVariable String orderId,
                         @RequestParam(required = false) String result,
                         Model model) {
        Order order = orderService.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown order"));
        model.addAttribute("order", order);
        model.addAttribute("result", result);
        return "checkout/order-status";
    }

    private Course requireCourse(String slug) {
        return catalog.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown course"));
    }

    private static boolean isValidEmail(String email) {
        if (email == null) return false;
        String trimmed = email.trim();
        return trimmed.length() >= 3 && trimmed.contains("@") && trimmed.indexOf("@") < trimmed.length() - 1;
    }
}
