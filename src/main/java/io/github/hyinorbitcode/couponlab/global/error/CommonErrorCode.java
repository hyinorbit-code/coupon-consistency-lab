package io.github.hyinorbitcode.couponlab.global.error;

import org.springframework.http.HttpStatus;

/** 특정 모듈에 속하지 않는 공통 오류. */
public enum CommonErrorCode implements ErrorCode {
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값을 확인해 주세요."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 대상을 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 요청 방식입니다."),
    CONFLICT(HttpStatus.CONFLICT, "요청이 현재 상태와 충돌합니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버에 문제가 생겼습니다. 잠시 후 다시 시도해 주세요.");

    private final HttpStatus status;
    private final String defaultMessage;

    CommonErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    @Override
    public HttpStatus status() {
        return status;
    }

    @Override
    public String defaultMessage() {
        return defaultMessage;
    }

    /** HTTP 상태에 대응하는 공통 코드. 대응하는 것이 없으면 4xx는 INVALID_INPUT, 5xx는 INTERNAL_ERROR. */
    public static CommonErrorCode fromStatus(int status) {
        for (CommonErrorCode code : values()) {
            if (code.status.value() == status) {
                return code;
            }
        }
        return status >= 500 ? INTERNAL_ERROR : INVALID_INPUT;
    }
}
