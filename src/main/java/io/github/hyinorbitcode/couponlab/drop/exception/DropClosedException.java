package io.github.hyinorbitcode.couponlab.drop.exception;

import io.github.hyinorbitcode.couponlab.global.error.BusinessException;

public class DropClosedException extends BusinessException {
    public DropClosedException() {
        super(DropErrorCode.DROP_CLOSED);
    }
}
