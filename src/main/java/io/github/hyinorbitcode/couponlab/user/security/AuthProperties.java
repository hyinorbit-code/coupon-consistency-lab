package io.github.hyinorbitcode.couponlab.user.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 설정. 시크릿이 HS256 최소 길이(32바이트)보다 짧으면 바인딩 단계에서 실패해 애플리케이션이 기동하지 않는다.
 */
@ConfigurationProperties("couponlab.auth")
public record AuthProperties(String jwtSecret, String issuer, Duration tokenTtl) {

    static final int MIN_SECRET_BYTES = 32;

    public AuthProperties {
        if (jwtSecret == null || jwtSecret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException(
                    "couponlab.auth.jwt-secret must be at least " + MIN_SECRET_BYTES + " bytes");
        }
        Objects.requireNonNull(issuer, "couponlab.auth.issuer");
        Objects.requireNonNull(tokenTtl, "couponlab.auth.token-ttl");
        if (tokenTtl.isNegative() || tokenTtl.isZero()) {
            throw new IllegalArgumentException("couponlab.auth.token-ttl must be positive");
        }
    }

    byte[] jwtSecretBytes() {
        return jwtSecret.getBytes(StandardCharsets.UTF_8);
    }
}
