package io.github.hyinorbitcode.couponlab.drop.exception;

import io.github.hyinorbitcode.couponlab.global.error.BusinessException;

public class NoCodesException extends BusinessException {
    public NoCodesException() { super(DropErrorCode.NO_CODES); }
}
