package io.github.hyinorbitcode.couponlab.user.exception;

import io.github.hyinorbitcode.couponlab.global.error.BusinessException;

/**
 * 인증 토큰이 없거나 만료됐거나 이미 사용됐다. 어느 경우인지는 구분해서 알려 주지 않는다.
 */
public class InvalidVerificationTokenException extends BusinessException {

    public InvalidVerificationTokenException() {
        super(UserErrorCode.INVALID_VERIFICATION_TOKEN);
    }
}
