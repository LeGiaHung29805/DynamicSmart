package com.dynamicmart.payment_service.client;

import com.dynamicmart.payment_service.service.PaymentGatewayRouter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

final class HmacSigner {
    private HmacSigner() { }

    static String sha256(String secret, String value) { return sign("HmacSHA256", secret, value); }

    static boolean equalsHex(String expected, String received) {
        return expected != null && received != null && MessageDigest.isEqual(
                expected.toLowerCase().getBytes(StandardCharsets.UTF_8), received.toLowerCase().getBytes(StandardCharsets.UTF_8));
    }

    private static String sign(String algorithm, String secret, String value) {
        try {
            Mac mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), algorithm));
            return java.util.HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) { throw new IllegalStateException("Cannot sign provider request", exception); }
    }
}
