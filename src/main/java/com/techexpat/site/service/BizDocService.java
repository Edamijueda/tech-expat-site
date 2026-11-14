package com.techexpat.site.service;

import com.techexpat.site.config.BizDocsProperties;
import com.techexpat.site.model.BizDoc;
import com.techexpat.site.repository.BizDocRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class BizDocService {

    static final int MAX_DESCRIPTION_LENGTH = 500;

    private final BizDocRepository docs;
    private final BizDocStorage storage;
    private final BizDocsProperties props;

    public BizDocService(BizDocRepository docs, BizDocStorage storage,
                          BizDocsProperties props) {
        this.docs = docs;
        this.storage = storage;
        this.props = props;
    }

    public List<BizDoc> listFor(String userId) {
        return docs.findByUserIdOrderByUploadedAtDesc(userId);
    }

    public Optional<BizDoc> findOwned(String id, String userId) {
        return docs.findByIdAndUserId(id, userId);
    }

    public byte[] contentOf(BizDoc doc) {
        return storage.retrieve(doc.getStorageKey());
    }

    @Transactional
    public BizDoc upload(String userId, String description, MultipartFile file) {
        String trimmed = description == null ? "" : description.trim();
        if (trimmed.isEmpty()) {
            throw new BizDocValidationException("Add a short description so you can find this later.");
        }
        if (trimmed.length() > MAX_DESCRIPTION_LENGTH) {
            throw new BizDocValidationException(
                    "Description must be " + MAX_DESCRIPTION_LENGTH + " characters or fewer.");
        }
        if (file == null || file.isEmpty()) {
            throw new BizDocValidationException("Choose a document to keep.");
        }
        if (file.getSize() > props.maxBytes()) {
            throw new BizDocValidationException(
                    "That file is larger than " + (props.maxBytes() / (1024 * 1024)) + " MB.");
        }

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new BizDocValidationException("That file could not be read. Try again.");
        }

        String contentType = sniffContentType(content);
        if (contentType == null) {
            throw new BizDocValidationException(
                    "Documents must be a photo (JPG, PNG, HEIC) or a PDF.");
        }

        String docId = UUID.randomUUID().toString();
        String storageKey = userId + "/" + docId;
        storage.store(storageKey, contentType, content);

        return docs.save(new BizDoc(
                docId,
                userId,
                trimmed,
                storageKey,
                file.getOriginalFilename(),
                contentType,
                content.length));
    }

    @Transactional
    public void delete(BizDoc doc) {
        docs.delete(doc);
        storage.delete(doc.getStorageKey());
    }

    /**
     * Reads the file's own leading bytes rather than trusting its name or the browser's
     * declared type, either of which the user can set to anything.
     */
    static String sniffContentType(byte[] c) {
        if (startsWith(c, 0xFF, 0xD8, 0xFF)) {
            return "image/jpeg";
        }
        if (startsWith(c, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) {
            return "image/png";
        }
        if (startsWith(c, 0x25, 0x50, 0x44, 0x46)) {
            return "application/pdf";
        }
        if (isHeic(c)) {
            return "image/heic";
        }
        return null;
    }

    private static boolean startsWith(byte[] c, int... magic) {
        if (c.length < magic.length) {
            return false;
        }
        for (int i = 0; i < magic.length; i++) {
            if ((c[i] & 0xFF) != magic[i]) {
                return false;
            }
        }
        return true;
    }

    /** HEIC is an ISO-BMFF container: bytes 4-8 are "ftyp", then a brand. */
    private static boolean isHeic(byte[] c) {
        if (c.length < 12) {
            return false;
        }
        String marker = new String(c, 4, 4, java.nio.charset.StandardCharsets.US_ASCII);
        if (!"ftyp".equals(marker)) {
            return false;
        }
        String brand = new String(c, 8, 4, java.nio.charset.StandardCharsets.US_ASCII);
        return List.of("heic", "heix", "hevc", "hevx", "mif1", "msf1").contains(brand);
    }

    public static class BizDocValidationException extends RuntimeException {
        public BizDocValidationException(String message) {
            super(message);
        }
    }
}
