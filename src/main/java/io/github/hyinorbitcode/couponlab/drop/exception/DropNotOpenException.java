package io.github.hyinorbitcode.couponlab.drop.exception;

import io.github.hyinorbitcode.couponlab.global.error.BusinessException;

public class DropNotOpenException extends BusinessException {

    public DropNotOpenException() {
        super(DropErrorCode.DROP_NOT_OPEN);
    }
}
