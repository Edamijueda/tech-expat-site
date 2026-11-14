package com.techexpat.site.service;

/**
 * Where document files live. Files are capped at a few MB, so they're passed whole
 * rather than streamed.
 */
public interface BizDocStorage {

    void store(String key, String contentType, byte[] content);

    byte[] retrieve(String key);

    void delete(String key);
}
