package io.github.hyinorbitcode.couponlab.user.internal.application;

/**
 * 인증 토큰이 없거나 만료됐거나 이미 사용됐다. 어느 경우인지는 구분해서 알려 주지 않는다.
 */
public class InvalidVerificationTokenException extends RuntimeException {

    public InvalidVerificationTokenException() {
        super("인증 링크가 유효하지 않거나 만료되었습니다. 인증 메일을 다시 요청해 주세요.");
    }
}
