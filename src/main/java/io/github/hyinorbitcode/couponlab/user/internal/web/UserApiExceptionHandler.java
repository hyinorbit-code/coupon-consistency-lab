package io.github.hyinorbitcode.couponlab.user.internal.web;

import io.github.hyinorbitcode.couponlab.user.internal.application.InvalidVerificationTokenException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * user 모듈 API의 업무 오류를 ProblemDetail(RFC 9457)로 응답한다.
 */
@RestControllerAdvice(basePackageClasses = UserApiExceptionHandler.class)
class UserApiExceptionHandler {

    @ExceptionHandler(InvalidVerificationTokenException.class)
    ProblemDetail invalidVerificationToken(InvalidVerificationTokenException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail invalidArgument(IllegalArgumentException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }
}
