package io.github.hyinorbitcode.couponlab.user.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.hyinorbitcode.couponlab.user.Role;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserTests {

    private static final Instant NOW = Instant.parse("2026-11-01T00:00:00Z");
    private static final EmailAddress EMAIL = new EmailAddress("user@example.com");

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

    @Test
    @DisplayName("이메일 가입자는 인증 대기 상태의 USER로 만들어진다")
    void signsUpWithEmailAsPendingUser() {
        User user = User.signUpWithEmail(EMAIL, "{bcrypt}hash", "nick", NOW);

        assertThat(user.getStatus()).isEqualTo(UserStatus.PENDING_VERIFICATION);
        assertThat(user.getRole()).isEqualTo(Role.USER);
        assertThat(user.isPendingVerification()).isTrue();
    }

    @Test
    @DisplayName("이메일 없이, 또는 비어 있는 비밀번호 해시로는 가입시킬 수 없다")
    void rejectsMissingEmailOrBlankPasswordHash() {
        assertThatThrownBy(() -> User.signUpWithEmail(null, "{bcrypt}hash", "nick", NOW))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> User.signUpWithEmail(EMAIL, " ", "nick", NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("이메일 인증을 마치면 활성화되고 인증 시각이 기록된다")
    void verifiesEmail() {
        User user = User.signUpWithEmail(EMAIL, "{bcrypt}hash", "nick", NOW);
        Instant verifiedAt = NOW.plusSeconds(60);

        user.verifyEmail(verifiedAt);

        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getEmailVerifiedAt()).isEqualTo(verifiedAt);
    }
}
