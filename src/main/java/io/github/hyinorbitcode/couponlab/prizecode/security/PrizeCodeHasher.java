package io.github.hyinorbitcode.couponlab.prizecode.security;

import io.github.hyinorbitcode.couponlab.prizecode.config.PrizeCodeProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

@Component
public class PrizeCodeHasher {

    private static final String ALGORITHM = "HmacSHA256";

    private final SecretKeySpec key;

    public PrizeCodeHasher(PrizeCodeProperties properties) {
        this.key = new SecretKeySpec(properties.hashKeyBytes(), ALGORITHM);
    }

    public byte[] hash(String code) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            return mac.doFinal(code.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("prize code hashing failed", e);
        }
    }
}