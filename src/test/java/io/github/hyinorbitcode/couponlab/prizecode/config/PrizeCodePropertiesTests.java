package io.github.hyinorbitcode.couponlab.prizecode.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.util.Arrays;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PrizeCodePropertiesTests {

    private static final String ENCRYPTION_KEY = key(1, PrizeCodeProperties.KEY_BYTES);
    private static final String HASH_KEY = key(2, PrizeCodeProperties.KEY_BYTES);

    @Test
    @DisplayName("32바이트 base64 키 두 개를 받으면 키 바이트를 돌려준다")
    void createWithValidKeys() {
        PrizeCodeProperties properties = new PrizeCodeProperties(ENCRYPTION_KEY, HASH_KEY);

        assertThat(properties.encryptionKeyBytes()).hasSize(PrizeCodeProperties.KEY_BYTES);
        assertThat(properties.hashKeyBytes()).hasSize(PrizeCodeProperties.KEY_BYTES);
        assertThat(properties.encryptionKeyBytes()).isNotEqualTo(properties.hashKeyBytes());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("암호화 키가 없으면 만들 수 없다")
    void rejectsMissingEncryptionKey(String encryptionKey) {
        assertThatThrownBy(() -> new PrizeCodeProperties(encryptionKey, HASH_KEY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("encryption-key");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("해시 키가 없으면 만들 수 없다")
    void rejectsMissingHashKey(String hashKey) {
        assertThatThrownBy(() -> new PrizeCodeProperties(ENCRYPTION_KEY, hashKey))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("hash-key");
    }

    @Test
    @DisplayName("키가 32바이트가 아니면 만들 수 없다")
    void rejectsWrongKeyLength() {
        String shortKey = key(3, PrizeCodeProperties.KEY_BYTES - 1);
        String longKey = key(3, PrizeCodeProperties.KEY_BYTES + 1);

        assertThatThrownBy(() -> new PrizeCodeProperties(shortKey, HASH_KEY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("encryption-key");
        assertThatThrownBy(() -> new PrizeCodeProperties(ENCRYPTION_KEY, longKey))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("hash-key");
    }

    @Test
    @DisplayName("base64가 아니면 만들 수 없다")
    void rejectsNonBase64Key() {
        assertThatThrownBy(() -> new PrizeCodeProperties("not base64!!", HASH_KEY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("encryption-key");
    }

    @Test
    @DisplayName("두 키가 같으면 만들 수 없다")
    void rejectsSameKeys() {
        assertThatThrownBy(() -> new PrizeCodeProperties(ENCRYPTION_KEY, ENCRYPTION_KEY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("differ");
    }

    private static String key(int fill, int length) {
        byte[] bytes = new byte[length];
        Arrays.fill(bytes, (byte) fill);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
