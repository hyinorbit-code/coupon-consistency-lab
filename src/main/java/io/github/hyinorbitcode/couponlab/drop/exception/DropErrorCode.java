package io.github.hyinorbitcode.couponlab.drop.exception;

import io.github.hyinorbitcode.couponlab.global.error.ErrorCode;
import org.springframework.http.HttpStatus;

public enum DropErrorCode implements ErrorCode {
    DROP_NOT_FOUND(HttpStatus.NOT_FOUND, "이벤트를 찾을 수 없습니다."),
    CODES_LOCKED(HttpStatus.CONFLICT, "오픈 이후나 취소된 이벤트에는 코드를 등록할 수 없습니다."),
    NO_CODES(HttpStatus.CONFLICT, "등록된 경품 코드가 없어 공개할 수 없습니다."),
    INVALID_DROP_STATUS(HttpStatus.CONFLICT, "현재 이벤트 상태에서는 처리할 수 없습니다."),
    DROP_NOT_OPEN(HttpStatus.CONFLICT, "아직 응모 시간이 아닙니다."),
    DROP_CLOSED(HttpStatus.CONFLICT, "종료된 이벤트입니다.");

    private final HttpStatus status;
    private final String defaultMessage;

    DropErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    @Override public HttpStatus status() { return status; }
    @Override public String defaultMessage() { return defaultMessage; }
}