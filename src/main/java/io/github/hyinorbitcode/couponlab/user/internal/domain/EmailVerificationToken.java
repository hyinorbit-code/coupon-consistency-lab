package io.github.hyinorbitcode.couponlab.user.internal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 이메일 인증 토큰. 원문은 메일로만 보내고 DB에는 SHA-256 해시만 저장한다. 한 번만 쓸 수 있다.
 */
@Entity
@Table(name = "email_verification_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailVerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(length = 32)
    private byte[] tokenHash;

    private Instant expiresAt;

    private Instant usedAt;

    private Instant createdAt;

    private EmailVerificationToken(Long userId, byte[] tokenHash, Instant expiresAt, Instant createdAt) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public static EmailVerificationToken issue(Long userId, byte[] tokenHash, Instant now, Duration ttl) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(now, "now");
        if (tokenHash == null || tokenHash.length != 32) {
            throw new IllegalArgumentException("tokenHash must be a 32-byte SHA-256 hash");
        }
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("ttl must be positive");
        }
        return new EmailVerificationToken(userId, tokenHash.clone(), now.plus(ttl), now);
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }
}
