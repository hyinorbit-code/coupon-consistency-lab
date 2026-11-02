package io.github.hyinorbitcode.couponlab.global.error;

import org.springframework.http.ProblemDetail;

public final class Problems {

    public static final String CODE_PROPERTY = "code";
    public static final String ERRORS_PROPERTY = "errors";

    private Problems() {
    }

    public static ProblemDetail of(ErrorCode errorCode, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(errorCode.status(), detail);
        problem.setProperty(CODE_PROPERTY, errorCode.code());
        return problem;
    }

    public static ProblemDetail of(ErrorCode errorCode) {
        return of(errorCode, errorCode.defaultMessage());
    }
}
