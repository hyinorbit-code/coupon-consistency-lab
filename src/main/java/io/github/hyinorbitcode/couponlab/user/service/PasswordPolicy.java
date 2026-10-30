package io.github.hyinorbitcode.couponlab.user.service;

import io.github.hyinorbitcode.couponlab.user.exception.InvalidPasswordException;

/**
 * 비밀번호 규칙. 가입과 이후 비밀번호 변경·재설정에서 함께 쓴다.
 * 요청 DTO의 길이 검증도 이 상수를 참조해 웹 검증과 서비스 검증이 어긋나지 않게 한다.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 64;

    private PasswordPolicy() {
    }

    public static void validate(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new InvalidPasswordException("비밀번호를 입력해 주세요.");
        }
        if (rawPassword.length() < MIN_LENGTH || rawPassword.length() > MAX_LENGTH) {
            throw new InvalidPasswordException("비밀번호는 %d~%d자여야 합니다.".formatted(MIN_LENGTH, MAX_LENGTH));
        }
    }
}
