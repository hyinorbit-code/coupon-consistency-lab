package io.github.hyinorbitcode.couponlab.drop.exception;

import io.github.hyinorbitcode.couponlab.global.error.BusinessException;

public class InvalidDropStatusException extends BusinessException {
    public InvalidDropStatusException() {
        super(DropErrorCode.INVALID_DROP_STATUS);
    }
}
