package io.github.hyinorbitcode.couponlab.user.exception;

import io.github.hyinorbitcode.couponlab.global.error.BusinessException;

public class UserNotFoundException extends BusinessException {

    public UserNotFoundException() {
        super(UserErrorCode.USER_NOT_FOUND);
    }
}
