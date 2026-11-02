package io.github.hyinorbitcode.couponlab.global.error;

import org.springframework.http.HttpStatus;

public interface ErrorCode {

    String name();

    HttpStatus status();

    String defaultMessage();

    default String code() {
        return name();
    }

}
