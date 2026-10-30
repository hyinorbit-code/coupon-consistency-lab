package io.github.hyinorbitcode.couponlab.global.error;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * 모든 모듈의 오류를 ProblemDetail(RFC 9457) + code 속성으로 응답한다(ADR-0013).
 * Spring MVC 표준 예외(검증 실패, 잘못된 JSON, 없는 경로 등)는 부모 클래스가 처리하고, 여기서 code를 덧붙인다.
 * 예상하지 못한 예외는 로그에만 자세히 남기고 응답에는 일반 메시지만 보낸다.
 */
@Slf4j
@RestControllerAdvice
class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    ProblemDetail handleBusiness(BusinessException e) {
        return Problems.of(e.getErrorCode(), e.getMessage());
    }

    /** 도메인 객체의 값 검증 실패. 메시지는 개발자용이라 응답에는 일반 메시지를 쓴다. */
    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleIllegalArgument(IllegalArgumentException e) {
        log.debug("Rejected invalid argument: {}", e.getMessage());
        return Problems.of(CommonErrorCode.INVALID_INPUT);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception e) {
        log.error("Unexpected error", e);
        return Problems.of(CommonErrorCode.INTERNAL_ERROR);
    }

    /** @Valid 검증 실패: 어떤 필드가 왜 틀렸는지 errors로 함께 준다. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = Problems.of(CommonErrorCode.INVALID_INPUT);
        List<FieldViolation> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldViolation(error.getField(), messageOf(error)))
                .toList();
        problem.setProperty(Problems.ERRORS_PROPERTY, errors);
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    /**
     * 부모 클래스가 만든 모든 표준 오류 응답에 상태에 맞는 공통 code를 붙인다.
     * 일부 예외(없는 경로 등)는 본문을 null로 넘기므로 예외가 가진 ProblemDetail을 먼저 꺼낸다.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        if (body == null && ex instanceof ErrorResponse errorResponse) {
            body = errorResponse.getBody();
        }
        if (body instanceof ProblemDetail problem && !hasCode(problem)) {
            CommonErrorCode code = CommonErrorCode.fromStatus(statusCode.value());
            problem.setProperty(Problems.CODE_PROPERTY, code.code());
            if (statusCode.is4xxClientError() && code == CommonErrorCode.INVALID_INPUT) {
                problem.setDetail(code.defaultMessage());
            }
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }

    private static boolean hasCode(ProblemDetail problem) {
        return problem.getProperties() != null && problem.getProperties().containsKey(Problems.CODE_PROPERTY);
    }

    private static String messageOf(FieldError error) {
        return error.getDefaultMessage() == null ? "올바르지 않은 값입니다." : error.getDefaultMessage();
    }

    record FieldViolation(String field, String message) {
    }
}
