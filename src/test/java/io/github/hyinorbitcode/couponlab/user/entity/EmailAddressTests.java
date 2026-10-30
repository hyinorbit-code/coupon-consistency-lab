package io.github.hyinorbitcode.couponlab.user.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.hyinorbitcode.couponlab.user.exception.InvalidEmailException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EmailAddressTests {

    @Test
    @DisplayName("생성될 때 스스로 앞뒤 공백을 지우고 소문자로 정규화한다")
    void normalizesItself() {
        assertThat(new EmailAddress("  User@Example.COM ").value()).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("대소문자와 공백만 다른 주소는 같은 값이다")
    void equalAfterNormalization() {
        assertThat(new EmailAddress("User@Example.com")).isEqualTo(new EmailAddress("user@example.com "));
    }

    @Test
    @DisplayName("비어 있으면 입력을 요구하는 메시지로 거절한다")
    void rejectsBlank() {
        assertThatThrownBy(() -> new EmailAddress(" "))
                .isInstanceOf(InvalidEmailException.class)
                .hasMessage("이메일을 입력해 주세요.");
        assertThatThrownBy(() -> new EmailAddress(null))
                .isInstanceOf(InvalidEmailException.class);
    }

    @Test
    @DisplayName("@가 없거나 여러 개이거나 앞뒤가 비어 있으면 거절한다")
    void rejectsMalformed() {
        for (String malformed : new String[] {"no-at-sign", "a@b@c", "@example.com", "user@"}) {
            assertThatThrownBy(() -> new EmailAddress(malformed))
                    .as(malformed)
                    .isInstanceOf(InvalidEmailException.class)
                    .hasMessage("올바른 이메일 주소를 입력해 주세요.");
        }
    }

    @Test
    @DisplayName("254자를 넘으면 거절한다")
    void rejectsTooLong() {
        String local = "a".repeat(EmailAddress.MAX_LENGTH);
        assertThatThrownBy(() -> new EmailAddress(local + "@example.com"))
                .isInstanceOf(InvalidEmailException.class);
    }
}
