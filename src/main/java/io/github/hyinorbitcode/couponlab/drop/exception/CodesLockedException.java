package io.github.hyinorbitcode.couponlab.drop.exception;

import io.github.hyinorbitcode.couponlab.global.error.BusinessException;

public class CodesLockedException extends BusinessException {
    public CodesLockedException() {
        super(DropErrorCode.CODES_LOCKED);
    }
}