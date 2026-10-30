package io.github.hyinorbitcode.couponlab.user.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuthPropertiesTests {

    private static final String SECRET_32_BYTES = "0123456789abcdef0123456789abcdef";

    @Test
    @DisplayName("32바이트 이상의 시크릿은 허용한다")
    void acceptsLongEnoughSecret() {
        assertThatCode(() -> new AuthProperties(SECRET_32_BYTES, "issuer", Duration.ofHours(1)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("32바이트보다 짧은 시크릿이면 기동하지 않는다")
    void rejectsShortSecret() {
        assertThatThrownBy(() -> new AuthProperties(SECRET_32_BYTES.substring(1), "issuer", Duration.ofHours(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 32 bytes");
    }

    @Test
    @DisplayName("시크릿이 비어 있으면 기동하지 않는다")
    void rejectsMissingSecret() {
        assertThatThrownBy(() -> new AuthProperties("", "issuer", Duration.ofHours(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("토큰 유효기간은 0보다 커야 한다")
    void rejectsNonPositiveTtl() {
        assertThatThrownBy(() -> new AuthProperties(SECRET_32_BYTES, "issuer", Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
