package com.techexpat.site.service;

import com.techexpat.site.config.ReceiptsProperties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;

@Service
public class SupabaseReceiptStorage implements ReceiptStorage {

    private final RestClient client;
    private final ReceiptsProperties props;

    @Autowired
    public SupabaseReceiptStorage(ReceiptsProperties props) {
        this(props, defaultClient(props));
    }

    SupabaseReceiptStorage(ReceiptsProperties props, RestClient client) {
        this.props = props;
        this.client = client;
    }

    private static RestClient defaultClient(ReceiptsProperties props) {
        String key = props.serviceRoleKey() == null ? "" : props.serviceRoleKey();
        return RestClient.builder()
                .defaultHeader("Authorization", "Bearer " + key)
                .build();
    }

    @Override
    public void store(String key, String contentType, byte[] content) {
        try {
            client.post()
                    .uri(objectUri(key))
                    .contentType(MediaType.parseMediaType(contentType))
                    .header("x-upsert", "false")
                    .body(content)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new ReceiptStorageException("Could not store receipt file", e);
        }
    }

    @Override
    public byte[] retrieve(String key) {
        try {
            byte[] body = client.get()
                    .uri(objectUri(key))
                    .retrieve()
                    .body(byte[].class);
            if (body == null) {
                throw new ReceiptStorageException("Empty response for receipt file", null);
            }
            return body;
        } catch (RestClientException e) {
            throw new ReceiptStorageException("Could not read receipt file", e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            client.delete()
                    .uri(objectUri(key))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new ReceiptStorageException("Could not delete receipt file", e);
        }
    }

    /**
     * Built as a URI rather than a uri template: keys contain a slash, which template
     * expansion would escape to %2F.
     */
    private URI objectUri(String key) {
        return URI.create(props.supabaseUrl() + "/storage/v1/object/" + props.bucket() + "/" + key);
    }

    public static class ReceiptStorageException extends RuntimeException {
        public ReceiptStorageException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
