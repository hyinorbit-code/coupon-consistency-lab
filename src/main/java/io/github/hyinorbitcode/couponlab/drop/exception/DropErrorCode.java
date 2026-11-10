package io.github.hyinorbitcode.couponlab.drop.exception;

import io.github.hyinorbitcode.couponlab.global.error.ErrorCode;
import org.springframework.http.HttpStatus;

public enum DropErrorCode implements ErrorCode {
    DROP_NOT_FOUND(HttpStatus.NOT_FOUND, "이벤트를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String defaultMessage;

    DropErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    @Override public HttpStatus status() { return status; }
    @Override public String defaultMessage() { return defaultMessage; }
}