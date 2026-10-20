package com.techexpat.site.service;

import com.techexpat.site.config.ReceiptsProperties;
import com.techexpat.site.model.Receipt;
import com.techexpat.site.repository.ReceiptRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ReceiptService {

    static final int MAX_DESCRIPTION_LENGTH = 500;

    private final ReceiptRepository receipts;
    private final ReceiptStorage storage;
    private final ReceiptsProperties props;

    public ReceiptService(ReceiptRepository receipts, ReceiptStorage storage,
                          ReceiptsProperties props) {
        this.receipts = receipts;
        this.storage = storage;
        this.props = props;
    }

    public List<Receipt> listFor(String userId) {
        return receipts.findByUserIdOrderByUploadedAtDesc(userId);
    }

    public Optional<Receipt> findOwned(String id, String userId) {
        return receipts.findByIdAndUserId(id, userId);
    }

    public byte[] contentOf(Receipt receipt) {
        return storage.retrieve(receipt.getStorageKey());
    }

    @Transactional
    public Receipt upload(String userId, String description, MultipartFile file) {
        String trimmed = description == null ? "" : description.trim();
        if (trimmed.isEmpty()) {
            throw new ReceiptValidationException("Add a short description so you can find this later.");
        }
        if (trimmed.length() > MAX_DESCRIPTION_LENGTH) {
            throw new ReceiptValidationException(
                    "Description must be " + MAX_DESCRIPTION_LENGTH + " characters or fewer.");
        }
        if (file == null || file.isEmpty()) {
            throw new ReceiptValidationException("Choose a receipt file to keep.");
        }
        if (file.getSize() > props.maxBytes()) {
            throw new ReceiptValidationException(
                    "That file is larger than " + (props.maxBytes() / (1024 * 1024)) + " MB.");
        }

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new ReceiptValidationException("That file could not be read. Try again.");
        }

        String contentType = sniffContentType(content);
        if (contentType == null) {
            throw new ReceiptValidationException(
                    "Receipts must be a photo (JPG, PNG, HEIC) or a PDF.");
        }

        String receiptId = UUID.randomUUID().toString();
        String storageKey = userId + "/" + receiptId;
        storage.store(storageKey, contentType, content);

        return receipts.save(new Receipt(
                receiptId,
                userId,
                trimmed,
                storageKey,
                file.getOriginalFilename(),
                contentType,
                content.length));
    }

    @Transactional
    public void delete(Receipt receipt) {
        receipts.delete(receipt);
        storage.delete(receipt.getStorageKey());
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

    public static class ReceiptValidationException extends RuntimeException {
        public ReceiptValidationException(String message) {
            super(message);
        }
    }
}
