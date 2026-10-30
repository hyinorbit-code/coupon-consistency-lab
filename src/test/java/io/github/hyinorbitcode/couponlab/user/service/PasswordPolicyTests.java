package io.github.hyinorbitcode.couponlab.user.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.hyinorbitcode.couponlab.user.exception.InvalidPasswordException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PasswordPolicyTests {

    @Test
    @DisplayName("8~64자 비밀번호는 통과한다")
    void acceptsValidLength() {
        assertThatCode(() -> PasswordPolicy.validate("a".repeat(PasswordPolicy.MIN_LENGTH))).doesNotThrowAnyException();
        assertThatCode(() -> PasswordPolicy.validate("a".repeat(PasswordPolicy.MAX_LENGTH))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("비어 있거나 공백뿐이면 입력을 요구하는 메시지로 거절한다")
    void rejectsBlank() {
        assertThatThrownBy(() -> PasswordPolicy.validate(null))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessage("비밀번호를 입력해 주세요.");
        assertThatThrownBy(() -> PasswordPolicy.validate(" ".repeat(10)))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessage("비밀번호를 입력해 주세요.");
    }

    @Test
    @DisplayName("너무 짧거나 길면 길이 규칙 메시지로 거절한다")
    void rejectsWrongLength() {
        assertThatThrownBy(() -> PasswordPolicy.validate("a".repeat(PasswordPolicy.MIN_LENGTH - 1)))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessage("비밀번호는 8~64자여야 합니다.");
        assertThatThrownBy(() -> PasswordPolicy.validate("a".repeat(PasswordPolicy.MAX_LENGTH + 1)))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessage("비밀번호는 8~64자여야 합니다.");
    }
}
