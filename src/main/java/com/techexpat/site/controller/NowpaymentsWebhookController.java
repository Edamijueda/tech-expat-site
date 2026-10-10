package com.techexpat.site.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.techexpat.site.service.IpnSignatureVerifier;
import com.techexpat.site.service.OrderService;

import tools.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhooks")
public class NowpaymentsWebhookController {

    private static final Logger log = LoggerFactory.getLogger(NowpaymentsWebhookController.class);
    private static final String SIGNATURE_HEADER = "x-nowpayments-sig";

    private final IpnSignatureVerifier verifier;
    private final OrderService orderService;
    private final ObjectMapper mapper;

    public NowpaymentsWebhookController(IpnSignatureVerifier verifier,
                                        OrderService orderService,
                                        ObjectMapper mapper) {
        this.verifier = verifier;
        this.orderService = orderService;
        this.mapper = mapper;
    }

    @PostMapping("/nowpayments")
    public ResponseEntity<String> handle(@RequestBody byte[] rawBody,
                                         @RequestHeader(value = SIGNATURE_HEADER, required = false) String signature) {
        if (!verifier.verify(rawBody, signature)) {
            log.warn("Rejected NOWPayments IPN with invalid signature");
            return ResponseEntity.badRequest().body("invalid signature");
        }

        IpnPayload payload;
        try {
            payload = mapper.readValue(rawBody, IpnPayload.class);
        } catch (Exception e) {
            log.warn("Rejected NOWPayments IPN with unparseable body", e);
            return ResponseEntity.badRequest().body("invalid payload");
        }

        log.info("NOWPayments IPN received order_id={} payment_id={} status={}",
                payload.orderId(), payload.paymentId(), payload.paymentStatus());

        if (isPaidStatus(payload.paymentStatus())) {
            try {
                boolean transitioned = orderService.markPaidIfPending(payload.orderId(), payload.paymentId());
                if (transitioned) {
                    log.info("Marked order {} as PAID", payload.orderId());
                }
            } catch (IllegalArgumentException e) {
                log.warn("IPN references unknown order {}", payload.orderId());
                return ResponseEntity.ok("unknown order");
            }
        }

        return ResponseEntity.ok("ok");
    }

    private static boolean isPaidStatus(String status) {
        return "finished".equalsIgnoreCase(status);
    }

    record IpnPayload(
            @JsonProperty("order_id") String orderId,
            @JsonProperty("payment_id") String paymentId,
            @JsonProperty("payment_status") String paymentStatus
    ) {
    }
}
