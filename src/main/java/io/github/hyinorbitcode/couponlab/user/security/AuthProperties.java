package io.github.hyinorbitcode.couponlab.user.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

@ConfigurationProperties("couponlab.auth")
public record AuthProperties(
        String jwtSecret,
        String issuer,
        @DefaultValue("24h") Duration tokenTtl
) {

    public static final int MIN_SECRET_BYTES = 32;

    public AuthProperties {
        if (jwtSecret == null || jwtSecret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException(
                    "couponlab.auth.jwt-secret must be at least " + MIN_SECRET_BYTES + " bytes");
        }
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalArgumentException("couponlab.auth.issuer must not be blank");
        }
        Objects.requireNonNull(tokenTtl, "tokenTtl");
        if (tokenTtl.isZero() || tokenTtl.isNegative()) {
            throw new IllegalArgumentException("couponlab.auth.token-ttl must be positive");
        }
    }
}