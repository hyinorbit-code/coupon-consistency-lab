package io.github.hyinorbitcode.couponlab.drop.exception;

import io.github.hyinorbitcode.couponlab.global.error.BusinessException;

public class DropNotFoundException extends BusinessException {
    public DropNotFoundException() {
        super(DropErrorCode.DROP_NOT_FOUND);
    }
}
