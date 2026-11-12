package io.github.hyinorbitcode.couponlab.prizecode.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PrizeCodeTests {

    private static final Instant NOW = Instant.parse("2026-11-10T00:00:00Z");
    private static final long DROP_EVENT_ID = 1L;
    private static final byte[] CIPHERTEXT = {1, 2, 3, 4};
    private static final byte[] HASH = new byte[PrizeCode.HASH_BYTES];

    @Test
    @DisplayName("등록한 코드는 미할당 상태이고 전달한 값과 시각을 가진다")
    void registerUnassigned() {
        PrizeCode code = PrizeCode.register(DROP_EVENT_ID, CIPHERTEXT, HASH, "0001", NOW);

        assertThat(code.getId()).isNull();
        assertThat(code.getDropEventId()).isEqualTo(DROP_EVENT_ID);
        assertThat(code.getCodeCiphertext()).isEqualTo(CIPHERTEXT);
        assertThat(code.getCodeHash()).isEqualTo(HASH);
        assertThat(code.getCodeHint()).isEqualTo("0001");
        assertThat(code.getCreatedAt()).isEqualTo(NOW);
        assertThat(code.isAssigned()).isFalse();
        assertThat(code.getAssignedUserId()).isNull();
        assertThat(code.getAssignedAt()).isNull();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 31, 33})
    @DisplayName("해시가 32바이트가 아니면 등록할 수 없다")
    void rejectsWrongHashLength(int length) {
        byte[] hash = new byte[length];

        assertThatThrownBy(() -> PrizeCode.register(DROP_EVENT_ID, CIPHERTEXT, hash, "0001", NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("암호문, 해시, 힌트, 시각이 없으면 등록할 수 없다")
    void rejectsMissingValues() {
        assertThatThrownBy(() -> PrizeCode.register(DROP_EVENT_ID, null, HASH, "0001", NOW))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> PrizeCode.register(DROP_EVENT_ID, CIPHERTEXT, null, "0001", NOW))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> PrizeCode.register(DROP_EVENT_ID, CIPHERTEXT, HASH, null, NOW))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> PrizeCode.register(DROP_EVENT_ID, CIPHERTEXT, HASH, "0001", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("코드를 사용자에게 할당하면 당첨 상태가 되고 사용자와 시각을 가진다")
    void assignToUser() {
        PrizeCode code = PrizeCode.register(DROP_EVENT_ID, CIPHERTEXT, HASH, "0001", NOW);
        Instant wonAt = NOW.plusSeconds(60);

        code.assignTo(7L, wonAt);

        assertThat(code.isAssigned()).isTrue();
        assertThat(code.getAssignedUserId()).isEqualTo(7L);
        assertThat(code.getAssignedAt()).isEqualTo(wonAt);
    }

    @Test
    @DisplayName("이미 할당된 코드는 다른 사용자에게 다시 할당할 수 없다")
    void rejectsAssigningTwice() {
        PrizeCode code = PrizeCode.register(DROP_EVENT_ID, CIPHERTEXT, HASH, "0001", NOW);
        code.assignTo(7L, NOW);

        assertThatThrownBy(() -> code.assignTo(8L, NOW))
                .isInstanceOf(IllegalStateException.class);
        assertThat(code.getAssignedUserId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("할당 시각이 없으면 할당할 수 없다")
    void rejectsAssigningWithoutTime() {
        PrizeCode code = PrizeCode.register(DROP_EVENT_ID, CIPHERTEXT, HASH, "0001", NOW);

        assertThatThrownBy(() -> code.assignTo(7L, null))
                .isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "COFFEE-0001, 0001",
            "ABCDE, BCDE",
            "ABCD, ABCD",
            "AB, AB",
            "아메리카노쿠폰, 카노쿠폰"
    })
    @DisplayName("힌트는 코드의 끝 4자리이고 4자 이하면 코드 전체다")
    void hintIsLastFourCharacters(String code, String expected) {
        assertThat(PrizeCode.hintOf(code)).isEqualTo(expected);
    }
}
