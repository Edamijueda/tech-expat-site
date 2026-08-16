package com.techexpat.site.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestClient;

@Controller
@RequestMapping("/ph")
public class PosthogProxyController {

    private static final String PREFIX = "/ph";
    private static final String ASSETS_UPSTREAM = "https://eu-assets.i.posthog.com";
    private static final String EVENTS_UPSTREAM = "https://eu.i.posthog.com";

    private final RestClient assets = RestClient.create(ASSETS_UPSTREAM);
    private final RestClient events = RestClient.create(EVENTS_UPSTREAM);

    @RequestMapping({"/static/**", "/array/**"})
    public ResponseEntity<byte[]> proxyAsset(HttpServletRequest req) {
        return forward(assets, req, null);
    }

    @RequestMapping("/**")
    public ResponseEntity<byte[]> proxyEvent(HttpServletRequest req,
                                             @RequestBody(required = false) byte[] body) {
        return forward(events, req, body);
    }

    private ResponseEntity<byte[]> forward(RestClient client, HttpServletRequest req, byte[] body) {
        String path = req.getRequestURI().substring(PREFIX.length());
        String qs = req.getQueryString();
        String uri = qs != null ? path + "?" + qs : path;

        RestClient.RequestBodySpec spec = client
                .method(HttpMethod.valueOf(req.getMethod()))
                .uri(uri);

        String contentType = req.getContentType();
        if (contentType != null) {
            spec = spec.contentType(MediaType.parseMediaType(contentType));
        }
        String ua = req.getHeader("User-Agent");
        if (ua != null) {
            spec = spec.header("User-Agent", ua);
        }
        if (body != null && body.length > 0) {
            spec.body(body);
        }

        return spec.exchange((request, response) -> {
            byte[] respBody = response.bodyTo(byte[].class);
            MediaType ct = response.getHeaders().getContentType();
            return ResponseEntity.status(response.getStatusCode())
                    .contentType(ct != null ? ct : MediaType.APPLICATION_OCTET_STREAM)
                    .body(respBody != null ? respBody : new byte[0]);
        });
    }
}
