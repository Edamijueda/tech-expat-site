package com.techexpat.site.service;

import com.techexpat.site.config.BizDocsProperties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;

@Service
public class SupabaseBizDocStorage implements BizDocStorage {

    private final RestClient client;
    private final BizDocsProperties props;

    @Autowired
    public SupabaseBizDocStorage(BizDocsProperties props) {
        this(props, defaultClient(props));
    }

    SupabaseBizDocStorage(BizDocsProperties props, RestClient client) {
        this.props = props;
        this.client = client;
    }

    private static RestClient defaultClient(BizDocsProperties props) {
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
            throw new BizDocStorageException("Could not store document file", e);
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
                throw new BizDocStorageException("Empty response for document file", null);
            }
            return body;
        } catch (RestClientException e) {
            throw new BizDocStorageException("Could not read document file", e);
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
            throw new BizDocStorageException("Could not delete document file", e);
        }
    }

    /**
     * Built as a URI rather than a uri template: keys contain a slash, which template
     * expansion would escape to %2F.
     */
    private URI objectUri(String key) {
        return URI.create(props.supabaseUrl() + "/storage/v1/object/" + props.bucket() + "/" + key);
    }

    public static class BizDocStorageException extends RuntimeException {
        public BizDocStorageException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
