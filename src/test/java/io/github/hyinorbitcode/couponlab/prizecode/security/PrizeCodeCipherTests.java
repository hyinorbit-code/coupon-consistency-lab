package io.github.hyinorbitcode.couponlab.prizecode.security;

import io.github.hyinorbitcode.couponlab.prizecode.config.PrizeCodeProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PrizeCodeCipherTests {

    private static final int IV_BYTES = 12;
    private static final int TAG_BYTES = 16;

    private static final String ENCRYPTION_KEY = key(1);
    private static final String HASH_KEY = key(2);
    private static final String OTHER_ENCRYPTION_KEY = key(3);

    private final PrizeCodeCipher cipher = new PrizeCodeCipher(new PrizeCodeProperties(ENCRYPTION_KEY, HASH_KEY));

    @ParameterizedTest
    @ValueSource(strings = {"COFFEE-0001", "a", "아메리카노-쿠폰-1234"})
    @DisplayName("암호화한 코드를 복호화하면 원문이 나온다")
    void roundTrip(String code) {
        byte[] encrypted = cipher.encrypt(code);

        assertThat(cipher.decrypt(encrypted)).isEqualTo(code);
    }

    @Test
    @DisplayName("암호문 길이는 IV와 인증 태그를 더한 길이다")
    void ciphertextLayout() {
        String code = "COFFEE-0001";

        byte[] encrypted = cipher.encrypt(code);

        assertThat(encrypted).hasSize(IV_BYTES + code.getBytes(StandardCharsets.UTF_8).length + TAG_BYTES);
    }

    @Test
    @DisplayName("같은 코드를 두 번 암호화하면 결과가 다르다")
    void sameCodeDifferentCiphertext() {
        byte[] first = cipher.encrypt("COFFEE-0001");
        byte[] second = cipher.encrypt("COFFEE-0001");

        assertThat(first).isNotEqualTo(second);
        assertThat(Arrays.copyOf(first, IV_BYTES)).isNotEqualTo(Arrays.copyOf(second, IV_BYTES));
    }

    @Test
    @DisplayName("암호문에 원문 바이트가 그대로 들어 있지 않다")
    void ciphertextHidesPlaintext() {
        String code = "COFFEE-0001";

        byte[] encrypted = cipher.encrypt(code);

        String asText = new String(encrypted, StandardCharsets.ISO_8859_1);
        assertThat(asText).doesNotContain(code);
    }

    @Test
    @DisplayName("암호문이 한 바이트라도 바뀌면 복호화할 수 없다")
    void rejectsTamperedCiphertext() {
        byte[] encrypted = cipher.encrypt("COFFEE-0001");
        encrypted[IV_BYTES] ^= 0x01;

        assertThatThrownBy(() -> cipher.decrypt(encrypted))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("다른 키로는 복호화할 수 없다")
    void rejectsOtherKey() {
        PrizeCodeCipher other = new PrizeCodeCipher(new PrizeCodeProperties(OTHER_ENCRYPTION_KEY, HASH_KEY));
        byte[] encrypted = cipher.encrypt("COFFEE-0001");

        assertThatThrownBy(() -> other.decrypt(encrypted))
                .isInstanceOf(IllegalStateException.class);
    }

    private static String key(int fill) {
        byte[] bytes = new byte[PrizeCodeProperties.KEY_BYTES];
        Arrays.fill(bytes, (byte) fill);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
