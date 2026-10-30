package io.github.hyinorbitcode.couponlab.user.exception;

import io.github.hyinorbitcode.couponlab.global.error.BusinessException;

/** 이메일 주소가 비어 있거나 형식이 맞지 않는다. */
public class InvalidEmailException extends BusinessException {

    public InvalidEmailException(String message) {
        super(UserErrorCode.INVALID_EMAIL, message);
    }
}
