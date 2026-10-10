package com.techexpat.site.service;

import com.techexpat.site.config.NowpaymentsProperties;
import com.techexpat.site.model.Course;
import com.techexpat.site.model.Order;
import com.techexpat.site.service.NowPaymentsClient.InvoiceResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class NowPaymentsClientTest {

    private MockRestServiceServer server;
    private NowPaymentsClient client;

    @BeforeEach
    void setUp() {
        NowpaymentsProperties props = new NowpaymentsProperties(
                "test-api-key",
                "test-ipn-secret",
                "https://api-sandbox.nowpayments.io/v1",
                "https://tech-expat.com/orders/{orderId}?result=success",
                "https://tech-expat.com/orders/{orderId}?result=cancel",
                "https://tech-expat.com/webhooks/nowpayments"
        );
        RestClient.Builder builder = RestClient.builder();
        this.server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder
                .baseUrl(props.baseUrl())
                .defaultHeader("x-api-key", props.apiKey())
                .build();
        this.client = new NowPaymentsClient(props, restClient);
    }

    @Test
    void createInvoiceSendsExpectedPayloadAndMapsResponse() {
        server.expect(requestTo("https://api-sandbox.nowpayments.io/v1/invoice"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-api-key", "test-api-key"))
                .andExpect(content().contentType(APPLICATION_JSON))
                .andExpect(jsonPath("$.price_amount").value(29.00))
                .andExpect(jsonPath("$.price_currency").value("USD"))
                .andExpect(jsonPath("$.order_id").value("order-123"))
                .andExpect(jsonPath("$.order_description").value("Practical SQL"))
                .andExpect(jsonPath("$.ipn_callback_url").value("https://tech-expat.com/webhooks/nowpayments"))
                .andExpect(jsonPath("$.success_url").value("https://tech-expat.com/orders/order-123?result=success"))
                .andExpect(jsonPath("$.cancel_url").value("https://tech-expat.com/orders/order-123?result=cancel"))
                .andRespond(withSuccess(
                        """
                        {
                            "id": "inv_abc123",
                            "invoice_url": "https://nowpayments.io/payment/?iid=inv_abc123"
                        }
                        """,
                        APPLICATION_JSON));

        Order order = new Order("order-123", "practical-sql", "buyer@example.com", new BigDecimal("29.00"));
        Course course = new Course("practical-sql", "Practical SQL", new BigDecimal("29.00"));

        InvoiceResponse response = client.createInvoice(order, course);

        assertThat(response.invoiceId()).isEqualTo("inv_abc123");
        assertThat(response.invoiceUrl()).isEqualTo("https://nowpayments.io/payment/?iid=inv_abc123");
        server.verify();
    }
}
