package com.techexpat.site.service;

import com.techexpat.site.config.NowpaymentsProperties;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class IpnSignatureVerifier {

    private static final String HMAC_ALGO = "HmacSHA512";
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private final String secret;

    public IpnSignatureVerifier(NowpaymentsProperties props) {
        this.secret = props.ipnSecret();
    }

    public boolean verify(byte[] rawBody, String headerSignature) {
        if (secret == null || secret.isBlank() || rawBody == null || headerSignature == null) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGO));
            byte[] computed = mac.doFinal(rawBody);
            byte[] computedHex = toHex(computed).getBytes(StandardCharsets.US_ASCII);
            byte[] expectedHex = headerSignature.trim().toLowerCase().getBytes(StandardCharsets.US_ASCII);
            return MessageDigest.isEqual(computedHex, expectedHex);
        } catch (Exception e) {
            return false;
        }
    }

    private static String toHex(byte[] bytes) {
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            out[i * 2] = HEX[v >>> 4];
            out[i * 2 + 1] = HEX[v & 0x0F];
        }
        return new String(out);
    }
}
