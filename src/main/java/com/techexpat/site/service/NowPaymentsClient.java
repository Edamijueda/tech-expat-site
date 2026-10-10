package com.techexpat.site.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.techexpat.site.config.NowpaymentsProperties;
import com.techexpat.site.model.Course;
import com.techexpat.site.model.Order;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

@Service
public class NowPaymentsClient {

    private static final String ORDER_ID_PLACEHOLDER = "{orderId}";

    private final RestClient client;
    private final NowpaymentsProperties props;

    @Autowired
    public NowPaymentsClient(NowpaymentsProperties props) {
        this(props, defaultClient(props));
    }

    NowPaymentsClient(NowpaymentsProperties props, RestClient client) {
        this.props = props;
        this.client = client;
    }

    private static RestClient defaultClient(NowpaymentsProperties props) {
        return RestClient.builder()
                .baseUrl(props.baseUrl())
                .defaultHeader("x-api-key", props.apiKey() == null ? "" : props.apiKey())
                .build();
    }

    public InvoiceResponse createInvoice(Order order, Course course) {
        CreateInvoiceRequest request = new CreateInvoiceRequest(
                course.priceUsd(),
                "USD",
                order.getId(),
                course.title(),
                props.ipnCallbackUrl(),
                resolveReturnUrl(props.successUrl(), order.getId()),
                resolveReturnUrl(props.cancelUrl(), order.getId())
        );

        CreateInvoiceResponse response = client.post()
                .uri("/invoice")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(CreateInvoiceResponse.class);

        if (response == null) {
            throw new IllegalStateException("Empty response from NOWPayments createInvoice");
        }
        return new InvoiceResponse(response.id(), response.invoiceUrl());
    }

    private static String resolveReturnUrl(String template, String orderId) {
        if (template == null) return null;
        return template.replace(ORDER_ID_PLACEHOLDER, orderId);
    }

    public record InvoiceResponse(String invoiceId, String invoiceUrl) {
    }

    record CreateInvoiceRequest(
            @JsonProperty("price_amount") BigDecimal priceAmount,
            @JsonProperty("price_currency") String priceCurrency,
            @JsonProperty("order_id") String orderId,
            @JsonProperty("order_description") String orderDescription,
            @JsonProperty("ipn_callback_url") String ipnCallbackUrl,
            @JsonProperty("success_url") String successUrl,
            @JsonProperty("cancel_url") String cancelUrl
    ) {
    }

    record CreateInvoiceResponse(
            @JsonProperty("id") String id,
            @JsonProperty("invoice_url") String invoiceUrl
    ) {
    }
}
