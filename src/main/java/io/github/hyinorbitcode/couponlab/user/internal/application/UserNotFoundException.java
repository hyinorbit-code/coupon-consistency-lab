package io.github.hyinorbitcode.couponlab.user.internal.application;

import io.github.hyinorbitcode.couponlab.common.BusinessException;

public class UserNotFoundException extends BusinessException {

    public UserNotFoundException() {
        super(UserErrorCode.USER_NOT_FOUND);
    }
}
