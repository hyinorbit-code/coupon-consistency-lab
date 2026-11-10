package io.github.hyinorbitcode.couponlab.prizecode.security;

import io.github.hyinorbitcode.couponlab.prizecode.config.PrizeCodeProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class PrizeCodeHasherTests {

    private static final String ENCRYPTION_KEY = key(1);
    private static final String HASH_KEY = key(2);
    private static final String OTHER_HASH_KEY = key(3);

    private final PrizeCodeHasher hasher = new PrizeCodeHasher(new PrizeCodeProperties(ENCRYPTION_KEY, HASH_KEY));

    @Test
    @DisplayName("같은 코드는 항상 같은 해시가 된다")
    void sameCodeSameHash() {
        assertThat(hasher.hash("COFFEE-0001")).isEqualTo(hasher.hash("COFFEE-0001"));
    }

    @Test
    @DisplayName("다른 코드는 다른 해시가 된다")
    void differentCodeDifferentHash() {
        assertThat(hasher.hash("COFFEE-0001")).isNotEqualTo(hasher.hash("COFFEE-0002"));
    }

    @Test
    @DisplayName("해시는 32바이트다")
    void hashIs32Bytes() {
        assertThat(hasher.hash("COFFEE-0001")).hasSize(32);
        assertThat(hasher.hash("가".repeat(100))).hasSize(32);
    }

    @Test
    @DisplayName("해시 키가 다르면 같은 코드라도 해시가 다르다")
    void differentKeyDifferentHash() {
        PrizeCodeHasher other = new PrizeCodeHasher(new PrizeCodeProperties(ENCRYPTION_KEY, OTHER_HASH_KEY));

        assertThat(other.hash("COFFEE-0001")).isNotEqualTo(hasher.hash("COFFEE-0001"));
    }

    private static String key(int fill) {
        byte[] bytes = new byte[PrizeCodeProperties.KEY_BYTES];
        Arrays.fill(bytes, (byte) fill);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
