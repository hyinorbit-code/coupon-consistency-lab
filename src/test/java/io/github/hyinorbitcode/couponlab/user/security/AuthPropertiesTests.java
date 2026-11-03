package io.github.hyinorbitcode.couponlab.user.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthPropertiesTests {

    private static final String SECRET = "a".repeat(AuthProperties.MIN_SECRET_BYTES);
    private static final String ISSUER = "coupon-consistency-lab";
    private static final Duration TOKEN_TTL = Duration.ofHours(24);

    @Test
    @DisplayName("올바른 값을 전달하면 시크릿, 발급자, 유효기간을 가진다")
    void createWithValidValues(){
        AuthProperties properties = new AuthProperties(SECRET, ISSUER, TOKEN_TTL);

        assertThat(properties.jwtSecret()).isEqualTo(SECRET);
        assertThat(properties.issuer()).isEqualTo(ISSUER);
        assertThat(properties.tokenTtl()).isEqualTo(TOKEN_TTL);
    }

    @Test
    @DisplayName("시크릿은 최소 바이트 수까지 허용한다")
    void acceptsSecretAtMinBytes() {
        String secret = "a".repeat(AuthProperties.MIN_SECRET_BYTES);

        AuthProperties properties = new AuthProperties(secret, ISSUER, TOKEN_TTL);

        assertThat(properties.jwtSecret()).isEqualTo(secret);
    }

    @Test
    @DisplayName("시크릿이 최소 바이트 수보다 짧으면 만들 수 없다")
    void rejectsTooShortSecret() {
        String secret = "a".repeat(AuthProperties.MIN_SECRET_BYTES - 1);

        assertThatThrownBy(() -> new AuthProperties(secret, ISSUER, TOKEN_TTL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("jwt-secret");
    }

    @Test
    @DisplayName("시크릿 길이는 글자 수가 아니라 UTF-8 바이트 수로 판단한다")
    void countsSecretInBytes() {
        String secret = "가".repeat(11); // 11글자, 33바이트

        AuthProperties properties = new AuthProperties(secret, ISSUER, TOKEN_TTL);

        assertThat(properties.jwtSecret()).hasSize(11);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("시크릿이 비어 있으면 만들 수 없다")
    void rejectsBlankSecret(String secret) {
        assertThatThrownBy(() -> new AuthProperties(secret, ISSUER, TOKEN_TTL))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    @DisplayName("발급자가 비어 있으면 만들 수 없다")
    void rejectsBlankIssuer(String issuer) {
        assertThatThrownBy(() -> new AuthProperties(SECRET, issuer, TOKEN_TTL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("issuer");
    }

    @Test
    @DisplayName("유효기간이 없으면 만들 수 없다")
    void rejectsNullTokenTtl() {
        assertThatThrownBy(() -> new AuthProperties(SECRET, ISSUER, null))
                .isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"PT0S", "-PT1H"})
    @DisplayName("유효기간이 0이거나 음수면 만들 수 없다")
    void rejectsNonPositiveTokenTtl(String tokenTtl) {
        Duration ttl = Duration.parse(tokenTtl);

        assertThatThrownBy(() -> new AuthProperties(SECRET, ISSUER, ttl))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("token-ttl");
    }

}
