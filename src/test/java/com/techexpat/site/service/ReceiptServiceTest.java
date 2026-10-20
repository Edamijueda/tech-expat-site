package com.techexpat.site.service;

import com.techexpat.site.config.ReceiptsProperties;
import com.techexpat.site.model.Receipt;
import com.techexpat.site.repository.ReceiptRepository;
import com.techexpat.site.service.ReceiptService.ReceiptValidationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReceiptServiceTest {

    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x01};
    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00};
    private static final byte[] PDF = "%PDF-1.4 body".getBytes(StandardCharsets.US_ASCII);

    private ReceiptRepository receipts;
    private ReceiptStorage storage;
    private ReceiptService service;

    @BeforeEach
    void setUp() {
        receipts = mock(ReceiptRepository.class);
        storage = mock(ReceiptStorage.class);
        service = new ReceiptService(receipts, storage,
                new ReceiptsProperties("https://x.supabase.co", "key", "bucket", 5 * 1024 * 1024));
        when(receipts.save(any(Receipt.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void sniffsJpegPngPdfAndHeic() {
        assertThat(ReceiptService.sniffContentType(JPEG)).isEqualTo("image/jpeg");
        assertThat(ReceiptService.sniffContentType(PNG)).isEqualTo("image/png");
        assertThat(ReceiptService.sniffContentType(PDF)).isEqualTo("application/pdf");
        assertThat(ReceiptService.sniffContentType(heic("heic"))).isEqualTo("image/heic");
        assertThat(ReceiptService.sniffContentType(heic("mif1"))).isEqualTo("image/heic");
    }

    @Test
    void sniffRejectsUnknownAndTruncatedContent() {
        assertThat(ReceiptService.sniffContentType("plain text".getBytes(StandardCharsets.UTF_8)))
                .isNull();
        assertThat(ReceiptService.sniffContentType(new byte[0])).isNull();
        assertThat(ReceiptService.sniffContentType(new byte[]{(byte) 0xFF})).isNull();
    }

    @Test
    void uploadStoresUnderUserScopedKeyAndSavesRow() {
        Receipt saved = service.upload("user-1", "  Fish supplier  ", file("r.jpg", JPEG));

        assertThat(saved.getUserId()).isEqualTo("user-1");
        assertThat(saved.getDescription()).isEqualTo("Fish supplier");
        assertThat(saved.getStorageKey()).startsWith("user-1/");
        assertThat(saved.getContentType()).isEqualTo("image/jpeg");
        assertThat(saved.getSizeBytes()).isEqualTo(JPEG.length);
        verify(storage).store(saved.getStorageKey(), "image/jpeg", JPEG);
    }

    @Test
    void uploadRejectsAnExecutableRenamedAsAnImage() {
        assertThatThrownBy(() -> service.upload("user-1", "sneaky",
                file("totally.jpg", "MZ\u0090binary".getBytes(StandardCharsets.ISO_8859_1))))
                .isInstanceOf(ReceiptValidationException.class)
                .hasMessageContaining("photo");

        verify(storage, never()).store(anyString(), anyString(), any());
        verify(receipts, never()).save(any());
    }

    @Test
    void uploadRejectsBlankDescription() {
        assertThatThrownBy(() -> service.upload("user-1", "   ", file("r.jpg", JPEG)))
                .isInstanceOf(ReceiptValidationException.class);
        verify(storage, never()).store(anyString(), anyString(), any());
    }

    @Test
    void uploadRejectsOverlongDescription() {
        assertThatThrownBy(() -> service.upload("user-1", "x".repeat(501), file("r.jpg", JPEG)))
                .isInstanceOf(ReceiptValidationException.class);
        verify(storage, never()).store(anyString(), anyString(), any());
    }

    @Test
    void uploadRejectsEmptyFile() {
        assertThatThrownBy(() -> service.upload("user-1", "desc", file("r.jpg", new byte[0])))
                .isInstanceOf(ReceiptValidationException.class);
        verify(storage, never()).store(anyString(), anyString(), any());
    }

    @Test
    void uploadRejectsFileOverTheCap() {
        ReceiptService tiny = new ReceiptService(receipts, storage,
                new ReceiptsProperties("https://x.supabase.co", "key", "bucket", 2 * 1024 * 1024));

        assertThatThrownBy(() -> tiny.upload("user-1", "desc", file("big.jpg", new byte[3 * 1024 * 1024])))
                .isInstanceOf(ReceiptValidationException.class)
                .hasMessageContaining("2 MB");

        verify(storage, never()).store(anyString(), anyString(), any());
    }

    @Test
    void deleteRemovesRowAndStoredObject() {
        Receipt receipt = new Receipt("r-1", "user-1", "desc", "user-1/r-1",
                "r.jpg", "image/jpeg", 10);

        service.delete(receipt);

        verify(receipts).delete(receipt);
        verify(storage).delete("user-1/r-1");
    }

    private static MockMultipartFile file(String name, byte[] content) {
        return new MockMultipartFile("file", name, "image/jpeg", content);
    }

    private static byte[] heic(String brand) {
        byte[] out = new byte[16];
        System.arraycopy("....ftyp".getBytes(StandardCharsets.US_ASCII), 0, out, 0, 8);
        System.arraycopy(brand.getBytes(StandardCharsets.US_ASCII), 0, out, 8, 4);
        return out;
    }
}
