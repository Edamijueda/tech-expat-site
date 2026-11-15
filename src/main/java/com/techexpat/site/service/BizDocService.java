package com.techexpat.site.service;

import com.techexpat.site.config.BizDocsProperties;
import com.techexpat.site.model.BizDoc;
import com.techexpat.site.model.KbdUser;
import com.techexpat.site.repository.BizDocRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class BizDocService {

    static final int MAX_DESCRIPTION_LENGTH = 500;

    /** Long enough to read, short enough to leave room in the 500-char storage_key. */
    static final int MAX_SLUG_LENGTH = 60;

    /** Business names are not unique, so the folder carries part of the user's id too. */
    static final int ID_SUFFIX_LENGTH = 8;

    static final String SLUG_FALLBACK = "business";

    /** The width of te_kbd_docs.original_filename, which the device's name must fit. */
    static final int MAX_ORIGINAL_FILENAME_LENGTH = 255;

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
    public BizDoc upload(KbdUser user, String description, MultipartFile file) {
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
        String storageKey = storageFolder(user) + "/" + docId + extensionFor(contentType);
        storage.store(storageKey, contentType, content);

        return docs.save(new BizDoc(
                docId,
                user.getId(),
                trimmed,
                storageKey,
                boundedFilename(file.getOriginalFilename()),
                contentType,
                content.length));
    }

    @Transactional
    public void delete(BizDoc doc) {
        docs.delete(doc);
        storage.delete(doc.getStorageKey());
    }

    /**
     * The name comes off the user's device, so it is the one value here with no length of
     * its own. Over the column's width the insert would fail after the file had already
     * been stored, leaving a file in the bucket with no row pointing at it.
     */
    static String boundedFilename(String originalFilename) {
        if (originalFilename == null
                || originalFilename.length() <= MAX_ORIGINAL_FILENAME_LENGTH) {
            return originalFilename;
        }
        return originalFilename.substring(0, MAX_ORIGINAL_FILENAME_LENGTH);
    }

    /**
     * The folder a user's files sit in: their business name, slugged, with the first few
     * characters of their id appended. Two businesses may share a name, and the suffix is
     * what keeps their folders apart.
     */
    static String storageFolder(KbdUser user) {
        String id = user.getId();
        return slug(user.getBusinessName())
                + "-" + id.substring(0, Math.min(ID_SUFFIX_LENGTH, id.length()));
    }

    /**
     * Storage keys go into a URI without escaping, so everything outside [a-z0-9-] has to
     * go. A name with no ASCII form at all — written entirely in another script — slugs to
     * an empty string, which is what the fallback is for.
     */
    static String slug(String businessName) {
        if (businessName == null) {
            return SLUG_FALLBACK;
        }
        String unaccented = Normalizer.normalize(businessName, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        String slug = unaccented.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (slug.length() > MAX_SLUG_LENGTH) {
            slug = slug.substring(0, MAX_SLUG_LENGTH).replaceAll("-+$", "");
        }
        return slug.isEmpty() ? SLUG_FALLBACK : slug;
    }

    /**
     * The extension a stored file is given, from the type sniffed out of its own bytes.
     * Unknown types can't reach the upload path, but a stored row's type still comes
     * through here when it is served back.
     */
    public static String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/heic" -> ".heic";
            case "application/pdf" -> ".pdf";
            default -> "";
        };
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
