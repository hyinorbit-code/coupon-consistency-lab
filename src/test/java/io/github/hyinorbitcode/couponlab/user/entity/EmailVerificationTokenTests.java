package io.github.hyinorbitcode.couponlab.user.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EmailVerificationTokenTests {

    private static final Instant NOW = Instant.parse("2026-11-01T00:00:00Z");
    private static final byte[] HASH = new byte[32];

    @Test
    @DisplayName("유효기간이 지나기 전에는 만료되지 않고, 만료 시각 정각부터 만료된다")
    void expiresAtExactTime() {
        EmailVerificationToken token = EmailVerificationToken.issue(1L, HASH, NOW, Duration.ofHours(24));

        assertThat(token.isExpired(NOW.plus(Duration.ofHours(24)).minusNanos(1))).isFalse();
        assertThat(token.isExpired(NOW.plus(Duration.ofHours(24)))).isTrue();
    }

    @Test
    @DisplayName("해시는 32바이트 SHA-256 값이어야 한다")
    void rejectsWrongHashLength() {
        assertThatThrownBy(() -> EmailVerificationToken.issue(1L, new byte[16], NOW, Duration.ofHours(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
