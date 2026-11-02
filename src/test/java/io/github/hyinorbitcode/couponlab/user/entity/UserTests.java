package io.github.hyinorbitcode.couponlab.user.entity;

import io.github.hyinorbitcode.couponlab.user.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTests {

    private static final Instant NOW = Instant.parse("2026-11-02T00:00:00Z");

    @Test
    @DisplayName("외부 계정 사용자는 활성 상태로 만들어지고 전달한 값과 시각을 가진다")
    void createActiveUser(){
        User user = User.createWithoutCredentials("tester", Role.USER,NOW);

        assertThat(user.getId()).isNull();
        assertThat(user.getNickname()).isEqualTo("tester");
        assertThat(user.getRole()).isEqualTo(Role.USER);
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("닉네임은 최대 길이까지 허용한다")
    void acceptsNicknameAtMaxLength(){
        String nickname = "가".repeat(User.MAX_NICKNAME_LENGTH);

        User user = User.createWithoutCredentials(nickname, Role.USER, NOW);

        assertThat(user.getNickname()).hasSize(User.MAX_NICKNAME_LENGTH);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    @DisplayName("닉네임이 비어 있으면 만들 수 없다")
    void rejectsBlankNickname(String nickname) {
        assertThatThrownBy(() -> User.createWithoutCredentials(nickname, Role.USER, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("닉네임이 최대 길이를 넘으면 만들 수 없다")
    void rejectsTooLongNickname() {
        String nickname = "가".repeat(User.MAX_NICKNAME_LENGTH + 1);

        assertThatThrownBy(() -> User.createWithoutCredentials(nickname, Role.USER, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("역할이나 시각이 없으면 만들 수 없다")
    void rejectsNullRoleOrTime() {
        assertThatThrownBy(() -> User.createWithoutCredentials("tester", null, NOW))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> User.createWithoutCredentials("tester", Role.USER, null))
                .isInstanceOf(NullPointerException.class);
    }

}
