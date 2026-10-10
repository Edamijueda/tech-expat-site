package com.techexpat.site.service;

import com.techexpat.site.config.NowpaymentsProperties;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class IpnSignatureVerifierTest {

    private static final String SECRET = "test-ipn-secret";
    private static final byte[] BODY = "{\"payment_status\":\"finished\",\"order_id\":\"abc\"}".getBytes(StandardCharsets.UTF_8);

    private final IpnSignatureVerifier verifier = new IpnSignatureVerifier(propsWithSecret(SECRET));

    @Test
    void acceptsValidSignature() {
        String sig = hmacSha512Hex(BODY, SECRET);

        assertThat(verifier.verify(BODY, sig)).isTrue();
    }

    @Test
    void acceptsUppercaseSignature() {
        String sig = hmacSha512Hex(BODY, SECRET).toUpperCase();

        assertThat(verifier.verify(BODY, sig)).isTrue();
    }

    @Test
    void rejectsTamperedBody() {
        String sig = hmacSha512Hex(BODY, SECRET);
        byte[] tampered = "{\"payment_status\":\"finished\",\"order_id\":\"xyz\"}".getBytes(StandardCharsets.UTF_8);

        assertThat(verifier.verify(tampered, sig)).isFalse();
    }

    @Test
    void rejectsWrongSecretSignature() {
        String sig = hmacSha512Hex(BODY, "wrong-secret");

        assertThat(verifier.verify(BODY, sig)).isFalse();
    }

    @Test
    void rejectsNullSignature() {
        assertThat(verifier.verify(BODY, null)).isFalse();
    }

    @Test
    void rejectsEmptySecretConfig() {
        IpnSignatureVerifier blank = new IpnSignatureVerifier(propsWithSecret(""));

        assertThat(blank.verify(BODY, "any-signature")).isFalse();
    }

    private static NowpaymentsProperties propsWithSecret(String secret) {
        return new NowpaymentsProperties(null, secret, null, null, null, null);
    }

    private static String hmacSha512Hex(byte[] body, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] out = mac.doFinal(body);
            StringBuilder sb = new StringBuilder(out.length * 2);
            for (byte b : out) {
                sb.append(String.format("%02x", b & 0xFF));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
