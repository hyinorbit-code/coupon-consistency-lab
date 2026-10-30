package io.github.hyinorbitcode.couponlab.common;

import org.springframework.http.HttpStatus;

/**
 * 업무 오류 코드. 각 모듈이 자기 오류를 enum으로 정의하고 이 인터페이스를 구현한다(ADR-0013).
 * 응답의 ProblemDetail에 {@code code} 속성으로 실려 프론트가 분기에 쓴다.
 */
public interface ErrorCode {

    /** enum 상수 이름이 그대로 응답 코드가 된다. */
    String name();

    HttpStatus status();

    /** 사용자에게 보여 줄 기본 메시지. */
    String defaultMessage();

    default String code() {
        return name();
    }
}
