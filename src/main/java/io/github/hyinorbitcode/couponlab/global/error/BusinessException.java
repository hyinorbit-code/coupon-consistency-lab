package io.github.hyinorbitcode.couponlab.global.error;

import java.util.Objects;
import lombok.Getter;

/**
 * 업무 규칙 위반. 전역 핸들러가 {@link ErrorCode}의 상태·코드로 ProblemDetail 응답을 만든다.
 * 메시지는 사용자에게 그대로 보이므로 내부 정보를 넣지 않는다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, errorCode.defaultMessage());
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode");
    }
}
