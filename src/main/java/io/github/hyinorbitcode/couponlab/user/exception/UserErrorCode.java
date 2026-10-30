package io.github.hyinorbitcode.couponlab.user.exception;

import io.github.hyinorbitcode.couponlab.global.error.ErrorCode;
import org.springframework.http.HttpStatus;

/** user 모듈의 업무 오류(ADR-0013). 응답 code는 상수 이름 그대로다. */
public enum UserErrorCode implements ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    INVALID_VERIFICATION_TOKEN(HttpStatus.BAD_REQUEST, "인증 링크가 유효하지 않거나 만료되었습니다. 인증 메일을 다시 요청해 주세요.");

    private final HttpStatus status;
    private final String defaultMessage;

    UserErrorCode(HttpStatus status, String defaultMessage) {
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
}
