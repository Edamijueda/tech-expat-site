package com.techexpat.site.service;

import com.techexpat.site.config.BizDocsProperties;
import com.techexpat.site.model.BizDoc;
import com.techexpat.site.model.KbdUser;
import com.techexpat.site.repository.BizDocRepository;
import com.techexpat.site.service.BizDocService.BizDocValidationException;

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

class BizDocServiceTest {

    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x01};
    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00};
    private static final byte[] PDF = "%PDF-1.4 body".getBytes(StandardCharsets.US_ASCII);

    private BizDocRepository docs;
    private BizDocStorage storage;
    private BizDocService service;

    @BeforeEach
    void setUp() {
        docs = mock(BizDocRepository.class);
        storage = mock(BizDocStorage.class);
        service = new BizDocService(docs, storage,
                new BizDocsProperties("https://x.supabase.co", "key", "bucket", 5 * 1024 * 1024));
        when(docs.save(any(BizDoc.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void sniffsJpegPngPdfAndHeic() {
        assertThat(BizDocService.sniffContentType(JPEG)).isEqualTo("image/jpeg");
        assertThat(BizDocService.sniffContentType(PNG)).isEqualTo("image/png");
        assertThat(BizDocService.sniffContentType(PDF)).isEqualTo("application/pdf");
        assertThat(BizDocService.sniffContentType(heic("heic"))).isEqualTo("image/heic");
        assertThat(BizDocService.sniffContentType(heic("mif1"))).isEqualTo("image/heic");
    }

    @Test
    void sniffRejectsUnknownAndTruncatedContent() {
        assertThat(BizDocService.sniffContentType("plain text".getBytes(StandardCharsets.UTF_8)))
                .isNull();
        assertThat(BizDocService.sniffContentType(new byte[0])).isNull();
        assertThat(BizDocService.sniffContentType(new byte[]{(byte) 0xFF})).isNull();
    }

    @Test
    void uploadStoresUnderABusinessNamedKeyAndSavesRow() {
        BizDoc saved = service.upload(user("Fish Market Ltd"), "  Fish supplier  ", file("r.jpg", JPEG));

        assertThat(saved.getUserId()).isEqualTo("user-1");
        assertThat(saved.getDescription()).isEqualTo("Fish supplier");
        assertThat(saved.getStorageKey()).startsWith("fish-market-ltd-user-1/");
        assertThat(saved.getStorageKey()).endsWith(".jpg");
        assertThat(saved.getContentType()).isEqualTo("image/jpeg");
        assertThat(saved.getSizeBytes()).isEqualTo(JPEG.length);
        verify(storage).store(saved.getStorageKey(), "image/jpeg", JPEG);
    }

    @Test
    void uploadRejectsAnExecutableRenamedAsAnImage() {
        assertThatThrownBy(() -> service.upload(user("Fish Market Ltd"), "sneaky",
                file("totally.jpg", "MZ\u0090binary".getBytes(StandardCharsets.ISO_8859_1))))
                .isInstanceOf(BizDocValidationException.class)
                .hasMessageContaining("photo");

        verify(storage, never()).store(anyString(), anyString(), any());
        verify(docs, never()).save(any());
    }

    @Test
    void uploadRejectsBlankDescription() {
        assertThatThrownBy(() -> service.upload(user("Fish Market Ltd"), "   ", file("r.jpg", JPEG)))
                .isInstanceOf(BizDocValidationException.class);
        verify(storage, never()).store(anyString(), anyString(), any());
    }

    @Test
    void uploadRejectsOverlongDescription() {
        assertThatThrownBy(() -> service.upload(user("Fish Market Ltd"), "x".repeat(501), file("r.jpg", JPEG)))
                .isInstanceOf(BizDocValidationException.class);
        verify(storage, never()).store(anyString(), anyString(), any());
    }

    @Test
    void uploadRejectsEmptyFile() {
        assertThatThrownBy(() -> service.upload(user("Fish Market Ltd"), "desc", file("r.jpg", new byte[0])))
                .isInstanceOf(BizDocValidationException.class);
        verify(storage, never()).store(anyString(), anyString(), any());
    }

    @Test
    void uploadRejectsFileOverTheCap() {
        BizDocService tiny = new BizDocService(docs, storage,
                new BizDocsProperties("https://x.supabase.co", "key", "bucket", 2 * 1024 * 1024));

        assertThatThrownBy(() -> tiny.upload(user("Fish Market Ltd"), "desc", file("big.jpg", new byte[3 * 1024 * 1024])))
                .isInstanceOf(BizDocValidationException.class)
                .hasMessageContaining("2 MB");

        verify(storage, never()).store(anyString(), anyString(), any());
    }

    @Test
    void deleteRemovesRowAndStoredObject() {
        BizDoc doc = new BizDoc("r-1", "user-1", "desc", "user-1/r-1",
                "r.jpg", "image/jpeg", 10);

        service.delete(doc);

        verify(docs).delete(doc);
        verify(storage).delete("user-1/r-1");
    }

    /**
     * The column is varchar(255), and the file is already in storage by the time the row
     * is inserted, so an over-long name would strand it there.
     */
    @Test
    void uploadTruncatesAnOverlongDeviceFilename() {
        String longName = "n".repeat(300) + ".jpg";

        BizDoc saved = service.upload(user("Fish Market Ltd"), "desc", file(longName, JPEG));

        assertThat(saved.getOriginalFilename()).hasSize(255);
        assertThat(saved.getOriginalFilename()).isEqualTo("n".repeat(255));
    }

    @Test
    void uploadLeavesAnOrdinaryFilenameAlone() {
        BizDoc saved = service.upload(user("Fish Market Ltd"), "desc", file("IMG_4821.jpg", JPEG));

        assertThat(saved.getOriginalFilename()).isEqualTo("IMG_4821.jpg");
    }

    @Test
    void boundedFilenameHandlesNullAndTheExactWidth() {
        assertThat(BizDocService.boundedFilename(null)).isNull();
        assertThat(BizDocService.boundedFilename("")).isEmpty();
        assertThat(BizDocService.boundedFilename("x".repeat(255))).hasSize(255);
        assertThat(BizDocService.boundedFilename("x".repeat(256))).hasSize(255);
    }

    @Test
    void slugLowercasesStripsAccentsAndPunctuation() {
        assertThat(BizDocService.slug("Café Olú & Sons, Ltd.")).isEqualTo("cafe-olu-sons-ltd");
        assertThat(BizDocService.slug("  spaced   out  ")).isEqualTo("spaced-out");
        assertThat(BizDocService.slug("A/B Trading")).isEqualTo("a-b-trading");
    }

    /** A name with no ASCII form at all would otherwise slug to an empty folder. */
    @Test
    void slugFallsBackWhenNothingSurvives() {
        assertThat(BizDocService.slug("的店铺")).isEqualTo("business");
        assertThat(BizDocService.slug("!!!")).isEqualTo("business");
        assertThat(BizDocService.slug(null)).isEqualTo("business");
    }

    @Test
    void slugIsTruncatedWithoutATrailingHyphen() {
        String slug = BizDocService.slug("a".repeat(40) + " " + "b".repeat(40));
        assertThat(slug).hasSize(60).doesNotEndWith("-");
    }

    /** Business names aren't unique, so the id suffix is what keeps folders apart. */
    @Test
    void sameBusinessNameStillGetsSeparateFolders() {
        KbdUser one = new KbdUser("11111111-aaaa", "a@b.com", "hash");
        one.setBusinessName("Pethona");
        KbdUser two = new KbdUser("22222222-bbbb", "c@d.com", "hash");
        two.setBusinessName("Pethona");

        assertThat(BizDocService.storageFolder(one)).isEqualTo("pethona-11111111");
        assertThat(BizDocService.storageFolder(two)).isEqualTo("pethona-22222222");
    }

    @Test
    void extensionComesFromTheSniffedType() {
        assertThat(BizDocService.extensionFor("image/jpeg")).isEqualTo(".jpg");
        assertThat(BizDocService.extensionFor("image/png")).isEqualTo(".png");
        assertThat(BizDocService.extensionFor("image/heic")).isEqualTo(".heic");
        assertThat(BizDocService.extensionFor("application/pdf")).isEqualTo(".pdf");
        assertThat(BizDocService.extensionFor("application/octet-stream")).isEmpty();
    }

    /** Older rows were stored with no extension, and must still be readable. */
    @Test
    void contentIsFetchedByTheRowsOwnStoredKey() {
        BizDoc old = new BizDoc("doc-1", "user-1", "desc", "user-1/doc-1",
                "r.jpg", "image/jpeg", JPEG.length);
        when(storage.retrieve("user-1/doc-1")).thenReturn(JPEG);

        assertThat(service.contentOf(old)).isEqualTo(JPEG);
        verify(storage).retrieve("user-1/doc-1");
    }

    private static KbdUser user(String businessName) {
        KbdUser user = new KbdUser("user-1", "a@b.com", "hash");
        user.setBusinessName(businessName);
        return user;
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
