package io.github.hyinorbitcode.couponlab.prizecode.exception;

import io.github.hyinorbitcode.couponlab.global.error.ErrorCode;
import org.springframework.http.HttpStatus;

public enum PrizeCodeErrorCode implements ErrorCode {
    SOLD_OUT(HttpStatus.CONFLICT, "준비된 경품이 모두 소진되었습니다.");

    private final HttpStatus status;
    private final String defaultMessage;

    PrizeCodeErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    @Override public HttpStatus status() { return status; }
    @Override public String defaultMessage() { return defaultMessage; }
}
