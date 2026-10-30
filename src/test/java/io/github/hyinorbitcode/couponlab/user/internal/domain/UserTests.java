package io.github.hyinorbitcode.couponlab.user.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.hyinorbitcode.couponlab.user.Role;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserTests {

    private static final Instant NOW = Instant.parse("2026-11-01T00:00:00Z");

    @Test
    @DisplayName("닉네임은 비어 있을 수 없다")
    void rejectsBlankNickname() {
        assertThatThrownBy(() -> User.create(AuthProvider.EXPERIMENT, "id-1", " ", Role.USER, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("닉네임은 50자를 넘을 수 없다")
    void rejectsTooLongNickname() {
        String nickname = "a".repeat(User.MAX_NICKNAME_LENGTH + 1);

        assertThatThrownBy(() -> User.create(AuthProvider.EXPERIMENT, "id-1", nickname, Role.USER, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("제공자 식별자는 64자를 넘을 수 없다")
    void rejectsTooLongProviderUserId() {
        String providerUserId = "a".repeat(User.MAX_PROVIDER_USER_ID_LENGTH + 1);

        assertThatThrownBy(() -> User.create(AuthProvider.EXPERIMENT, providerUserId, "nick", Role.USER, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("유효한 값으로 사용자를 만든다")
    void createsUser() {
        User user = User.create(AuthProvider.EXPERIMENT, "id-1", "nick", Role.ADMIN, NOW);

        assertThat(user.getNickname()).isEqualTo("nick");
        assertThat(user.getRole()).isEqualTo(Role.ADMIN);
    }
}
