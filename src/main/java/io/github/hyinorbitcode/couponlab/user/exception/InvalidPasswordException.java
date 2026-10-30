package io.github.hyinorbitcode.couponlab.user.exception;

import io.github.hyinorbitcode.couponlab.global.error.BusinessException;

/** 비밀번호가 정책에 맞지 않는다. 메시지는 무엇을 고쳐야 하는지 사용자에게 그대로 보인다. */
public class InvalidPasswordException extends BusinessException {

    public InvalidPasswordException(String message) {
        super(UserErrorCode.INVALID_PASSWORD, message);
    }
}
