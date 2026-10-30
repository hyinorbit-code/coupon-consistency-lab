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
        assertThatThrownBy(() -> User.createWithoutCredentials(" ", Role.USER, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("닉네임은 50자를 넘을 수 없다")
    void rejectsTooLongNickname() {
        String nickname = "a".repeat(User.MAX_NICKNAME_LENGTH + 1);

        assertThatThrownBy(() -> User.createWithoutCredentials(nickname, Role.USER, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("외부 계정 사용자는 이메일·비밀번호 없이 바로 활성 상태로 만들어진다")
    void createsUserWithoutCredentials() {
        User user = User.createWithoutCredentials("nick", Role.ADMIN, NOW);

        assertThat(user.getNickname()).isEqualTo("nick");
        assertThat(user.getRole()).isEqualTo(Role.ADMIN);
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getEmail()).isNull();
        assertThat(user.getPasswordHash()).isNull();
    }
}
