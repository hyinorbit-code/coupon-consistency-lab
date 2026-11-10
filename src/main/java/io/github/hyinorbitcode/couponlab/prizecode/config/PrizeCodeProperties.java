package io.github.hyinorbitcode.couponlab.prizecode.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Base64;

@ConfigurationProperties("couponlab.prize-code")
public record PrizeCodeProperties(String encryptionKey, String hashKey) {

    public static final int KEY_BYTES = 32;

    public PrizeCodeProperties {
        requireKey(encryptionKey, "encryption-key");
        requireKey(hashKey, "hash-key");
        if (encryptionKey.equals(hashKey)) {
            throw new IllegalArgumentException("couponlab.prize-code keys must differ");
        }
    }

    private static void requireKey(String base64, String name) {
        byte[] key;
        try {
            key = Base64.getDecoder().decode(base64 == null ? "" : base64);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("couponlab.prize-code." + name + " must be base64", e);
        }
        if (key.length != KEY_BYTES) {
            throw new IllegalArgumentException(
                    "couponlab.prize-code." + name + " must be " + KEY_BYTES + " bytes");
        }
    }

    public byte[] encryptionKeyBytes() {
        return Base64.getDecoder().decode(encryptionKey);
    }

    public byte[] hashKeyBytes() {
        return Base64.getDecoder().decode(hashKey);
    }
}