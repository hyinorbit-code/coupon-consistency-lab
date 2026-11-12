package io.github.hyinorbitcode.couponlab.prizecode.exception;

import io.github.hyinorbitcode.couponlab.global.error.BusinessException;

public class SoldOutException extends BusinessException {
    public SoldOutException() {
        super(PrizeCodeErrorCode.SOLD_OUT);
    }
}
